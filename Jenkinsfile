pipeline {
  agent any

  tools {
    jdk 'JDK17'
    maven 'mvn'
  }

  environment {
    SONAR_HOST   = "http://localhost:9000"
    REPORT_DIR   = "code-quality-reports"
    REPO_ORG     = "Sudhakar-09"
    REPO_NAME    = "document-management-spring-security"
    REPO_BRANCH  = "main"
  }

  stages {

    /* ------------------------ 1. CHECKOUT ------------------------ */
    stage('Checkout') {
      steps {
        echo "[1/9] Checkout"
        checkout scm
        sh "mkdir -p ${REPORT_DIR}"
        sh "git --version || true"
        sh "jq --version || true"
      }
    }

    /* ------------------------ 2. BUILD ------------------------ */
    stage('Build') {
      steps {
        echo "[2/9] Build"
        sh "mvn clean package -DskipTests"
      }
    }

    /* ------------------------ 3. SONAR SCAN ------------------------ */
    stage('Sonar Scan') {
      steps {
        echo "[3/9] Sonar Scan"
        withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
          sh """
            mvn sonar:sonar \
              -Dsonar.token=\$SONAR_TOKEN \
              -Dsonar.host.url=${SONAR_HOST} \
              -Dsonar.projectKey=ai-code-assistant \
              -Dsonar.sources=src/main/java \
              -Dsonar.java.binaries=target/classes \
              -Dsonar.projectName="AI Code Assistant"
          """
        }
      }
    }

    /* ------------------------ 4. QUALITY GATE ------------------------ */
    stage('Quality Gate') {
      steps {
        echo "[4/9] Quality Gate"
        script {
          withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {

            def ceTaskId = sh(
              script: "grep -o 'ceTaskId=[A-Za-z0-9\\-]*' target/sonar/report-task.txt | cut -d= -f2 || true",
              returnStdout: true
            ).trim()

            if (!ceTaskId) error("CE Task ID NOT FOUND")

            echo "CE Task ID = ${ceTaskId}"

            timeout(time: 5, unit: 'MINUTES') {
              waitUntil {
                def resp = sh(script: "curl -s -u \$SONAR_TOKEN: ${SONAR_HOST}/api/ce/task?id=${ceTaskId}", returnStdout: true).trim()
                def status = sh(script: "echo '${resp}' | jq -r '.task.status'", returnStdout: true).trim()
                echo "CE Status = ${status}"
                return (status == "SUCCESS" || status == "FAILED")
              }
            }

            def finalResp = sh(script: "curl -s -u \$SONAR_TOKEN: ${SONAR_HOST}/api/ce/task?id=${ceTaskId}", returnStdout: true).trim()
            def analysisId = sh(script: "echo '${finalResp}' | jq -r '.task.analysisId'", returnStdout: true).trim()

            env.ANALYSIS_ID = analysisId

            def qgJson = sh(
              script: "curl -s -u \$SONAR_TOKEN: ${SONAR_HOST}/api/qualitygates/project_status?analysisId=${analysisId}",
              returnStdout: true
            ).trim()

            writeFile file: "${REPORT_DIR}/qualitygate-${BUILD_NUMBER}.json", text: qgJson
          }
        }
      }
    }

    /* ------------------------ 5. FETCH ISSUES ------------------------ */
    stage('Fetch Issues') {
      steps {
        echo "[5/9] Fetching Issues"
        withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
          sh """
            curl -s -u \$SONAR_TOKEN: \
              "${SONAR_HOST}/api/issues/search?componentKeys=ai-code-assistant&ps=500" \
              > ${REPORT_DIR}/sonar-issues-${BUILD_NUMBER}.json
          """
        }
      }
    }

    /* ------------------------ 6. AI ANALYSIS ------------------------ */
    stage('AI Analysis & Report Generation') {
      steps {
        echo "[6/9] AI Report Generation (FAST + OLD & NEW + HYBRID SNIPPET)"
        withCredentials([string(credentialsId: 'OPENAI_KEY', variable: 'OPENAI_KEY')]) {

          script {
            // Read JSON issues
            def issues = readJSON file: "${REPORT_DIR}/sonar-issues-${BUILD_NUMBER}.json"
            def mdFile = "${REPORT_DIR}/code-quality-report-${BUILD_NUMBER}.md"

            // Write initial header
            writeFile file: mdFile, text: """
# 🧾 AI Code Quality Report
**Build:** ${BUILD_NUMBER}  
**Generated:** ${new Date().format("yyyy-MM-dd HH:mm:ss")}  
**Analysis ID:** ${env.ANALYSIS_ID}  
---

"""

            int idx = 0

            for (issue in issues.issues) {
              idx++

              def sevBadge = [
                "BLOCKER": "🔥 BLOCKER",
                "CRITICAL": "🔴 CRITICAL",
                "MAJOR": "🟠 MAJOR",
                "MINOR": "🟡 MINOR",
                "INFO": "🔵 INFO"
              ][issue.severity] ?: issue.severity

              def filepath = issue.component.replace("ai-code-assistant:", "")
              int line = (issue.line ?: 1) as int

              // Hybrid snippet logic
              int start = line - 2
              int end   = line + 2
              if (issue.textRange) {
                if (issue.textRange.startLine) start = issue.textRange.startLine
                if (issue.textRange.endLine)   end   = issue.textRange.endLine
              }
              if (start < 1) start = 1

              // Extract snippet
              def oldSnippet = fileExists(filepath)
                ? sh(script: "sed -n '${start},${end}p' ${filepath}", returnStdout: true)
                : "// File missing"

              // Git blame
              def blame = fileExists(filepath)
                ? sh(script: "git blame -L ${line},${line} -- ${filepath}", returnStdout: true)
                : "N/A"

              // GitHub link
              def link = "https://github.com/${REPO_ORG}/${REPO_NAME}/blob/${REPO_BRANCH}/${filepath}#L${start}-L${end}"

              // AI prompt
              def prompt = """
You are an expert Java code reviewer.

Analyze this SonarQube issue.

Issue ID: ${issue.key}
Severity: ${issue.severity}
Rule: ${issue.rule}
Message: ${issue.message}
File: ${filepath}
Line: ${line}

OLD CODE SNIPPET:
${oldSnippet}

Provide:
1. Summary
2. Root cause
3. NEW FIXED CODE (only corrected snippet)
4. Explanation of fix
5. Priority
6. Risk Score
"""

              def payload = groovy.json.JsonOutput.toJson([
                model: "gpt-4.1-mini",
                messages: [[role: "user", content: prompt]],
                max_tokens: 700
              ])

              writeFile file: "${REPORT_DIR}/payload-${idx}.json", text: payload

              // Safe AI call
              def aiOut = sh(
                script: """
                  #!/bin/bash
                  set -e
                  curl -s -X POST https://api.openai.com/v1/chat/completions \
                    -H "Authorization: Bearer \$OPENAI_KEY" \
                    -H "Content-Type: application/json" \
                    -d @${REPORT_DIR}/payload-${idx}.json \
                  | jq -r '.choices[0].message.content'
                """,
                returnStdout: true
              ).trim()

              // Append report
              sh """
                cat <<'EOF' >> ${mdFile}

---

## 🔹 Issue ${idx} — **${sevBadge}**
**Issue ID:** ${issue.key}  
**Message:** ${issue.message}  
**File:** ${filepath}  
**Line:** ${line}  
**Git Blame:** ${blame}  
**GitHub Link:** ${link}

### 🧩 OLD CODE
\`\`\`java
${oldSnippet}
\`\`\`

### 🤖 AI FIX (NEW CODE)
${aiOut}

EOF
              """
            }
          }
        }
      }
    }

    /* ------------------------ 7. ARCHIVE ------------------------ */
    stage('Archive Reports') {
      steps {
        echo "[7/9] Archiving"
        archiveArtifacts artifacts: "${REPORT_DIR}/*", allowEmptyArchive: true
        archiveArtifacts artifacts: "target/*.jar", allowEmptyArchive: true
      }
    }

    /* ------------------------ 8. FINISH ------------------------ */
    stage('Finish') {
      steps {
        echo "[8/9] Pipeline Complete!"
      }
    }
  }

  post {
    always { echo "Pipeline finished." }
    success { echo "SUCCESS" }
    failure { echo "FAILED — Check logs" }
  }
}
