pipeline {
  agent any

  tools {
    jdk 'JDK17'
    maven 'mvn'
  }

  environment {
    SONAR_HOST = "http://localhost:9000"
    REPORT_DIR = "code-quality-reports"
    REPO_ORG = "Sudhakar-09"
    REPO_NAME = "document-management-spring-security"
    REPO_BRANCH = "main"
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
        echo "[3/9] Running sonar scan"

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
        echo "[4/9] Waiting for CE + Quality Gate"
        script {
          withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {

            /* ---- Get CE Task ID ---- */
            def ceTaskId = sh(
              script: "grep -o 'ceTaskId=[A-Za-z0-9\\-]*' target/sonar/report-task.txt | cut -d= -f2 || true",
              returnStdout: true
            ).trim()

            if (!ceTaskId) error "CE Task ID NOT FOUND"

            echo "CE Task ID = ${ceTaskId}"

            /* ---- Wait until CE finishes ---- */
            timeout(time: 5, unit: 'MINUTES') {
              waitUntil {
                def resp = sh(
                  script: "curl -s -u \$SONAR_TOKEN: ${SONAR_HOST}/api/ce/task?id=${ceTaskId}",
                  returnStdout: true
                ).trim()

                def status = sh(
                  script: "echo '${resp}' | jq -r '.task.status'",
                  returnStdout: true
                ).trim()

                echo "CE Status = ${status}"

                return (status == "SUCCESS" || status == "FAILED")
              }
            }

            /* ---- Last response ---- */
            def finalResp = sh(
              script: "curl -s -u \$SONAR_TOKEN: ${SONAR_HOST}/api/ce/task?id=${ceTaskId}",
              returnStdout: true
            ).trim()

            def analysisId = sh(
              script: "echo '${finalResp}' | jq -r '.task.analysisId'",
              returnStdout: true
            ).trim()

            env.ANALYSIS_ID = analysisId

            /* ---- Get Quality Gate ---- */
            def qgJson = sh(
              script: "curl -s -u \$SONAR_TOKEN: ${SONAR_HOST}/api/qualitygates/project_status?analysisId=${analysisId}",
              returnStdout: true
            ).trim()

            writeFile file: "${REPORT_DIR}/qualitygate-${BUILD_NUMBER}.json", text: qgJson

            def qgStatus = sh(
              script: "echo '${qgJson}' | jq -r '.projectStatus.status'",
              returnStdout: true
            ).trim()

            echo "Quality Gate = ${qgStatus}"
          }
        }
      }
    }

    /* ------------------------ 5. FETCH ISSUES ------------------------ */
    stage('Fetch Issues') {
      steps {
        echo "[5/9] Fetching Sonar issues"

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
        echo "[6/9] AI report generation"

        withCredentials([
          string(credentialsId: 'OPENAI_KEY', variable: 'OPENAI_KEY'),
          string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')
        ]) {

          script {

            def issuesJson = readFile("${REPORT_DIR}/sonar-issues-${BUILD_NUMBER}.json")
            def issues = readJSON(text: issuesJson)

            def mdFile = "${REPORT_DIR}/code-quality-report-${BUILD_NUMBER}.md"

            /* ---- Create header ---- */
            def header = """
# 🧾 SonarQube Code Quality Report

**Project:** ai-code-assistant  
**Repository:** ${REPO_ORG}/${REPO_NAME}  
**Branch:** ${REPO_BRANCH}  
**Build:** ${BUILD_NUMBER}  
**Generated:** ${new Date().format("yyyy-MM-dd HH:mm:ss")}  
**Analysis ID:** ${env.ANALYSIS_ID}  

---
"""
            writeFile file: mdFile, text: header

            /* ---- Severity summary ---- */
            def counts = [BLOCKER:0, CRITICAL:0, MAJOR:0, MINOR:0, INFO:0]
            issues.issues.each { i -> counts[(i.severity ?: "INFO")]++ }

            def summary = """
## 📊 Summary

| Severity | Emoji | Count |
|---------|:-----:|------:|
| Blocker | 🟥 | ${counts.BLOCKER} |
| Critical | 🔴 | ${counts.CRITICAL} |
| Major | 🟠 | ${counts.MAJOR} |
| Minor | 🟡 | ${counts.MINOR} |
| Info | 🔵 | ${counts.INFO} |

Total issues: ${issues.total ?: issues.issues.size()}

---
"""
            sh """echo "${summary.replace('"','\\"')}" >> ${mdFile}"""

            /* ---- Iterate issues ---- */
            def idx = 0

            for (issue in issues.issues) {
              idx++

              def filepath = issue.component.replace("ai-code-assistant:", "")
              def line = (issue.line ?: 1) as int
              def start = Math.max(1, line - 2)
              def end = line + 2

              def snippet = fileExists(filepath)
                ? sh(script: "sed -n '${start},${end}p' ${filepath}", returnStdout: true)
                : "// File not found: ${filepath}"

              def blame = fileExists(filepath)
                ? sh(script: "git blame -L ${line},${line} -- ${filepath}", returnStdout: true)
                : "N/A"

              def link = "https://github.com/${REPO_ORG}/${REPO_NAME}/blob/${REPO_BRANCH}/${filepath}#L${start}-L${end}"

              def prompt = """
Analyze this Sonar issue:

Rule: ${issue.rule}
Message: ${issue.message}
Severity: ${issue.severity}
File: ${filepath}
Line: ${line}

Code:
${snippet}

Provide:
- Summary
- Root cause
- Fix with Java code block
- Priority
- Time estimate
- Risk score
"""

              def esc = prompt
                  .replace("\\","\\\\")
                  .replace("\"","\\\\\"")
                  .replace("\n","\\n")

              def payload = """
{
  "model": "gpt-4.1-mini",
  "messages": [
    {"role": "user", "content": "${esc}"}
  ]
}
"""
              writeFile file: "${REPORT_DIR}/payload-${idx}.json", text: payload

              def aiOut = sh(
                script: """
                  curl -s -X POST "https://api.openai.com/v1/chat/completions" \
                    -H "Authorization: Bearer \$OPENAI_KEY" \
                    -H "Content-Type: application/json" \
                    -d @"${REPORT_DIR}/payload-${idx}.json" \
                    | jq -r '.choices[0].message.content'
                """,
                returnStdout: true
              ).trim()

              def block = """
---

### Issue ${idx} — **${issue.severity}**
**Message:** ${issue.message}  
**File:** ${filepath}  
**Line:** ${line}  
**GitHub:** ${link}  
**Blame:** ${blame}

#### Code Snippet
${snippet.replaceAll("(?m)^", "    ")}

#### AI Recommendation
${aiOut}

"""

              sh """echo "${block.replace('"','\\"')}" >> ${mdFile}"""
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
        echo "[8/9] Pipeline complete!"
      }
    }
  }

  post {
    always { echo "Pipeline finished." }
    success { echo "SUCCESS" }
    failure { echo "FAILED — Check logs" }
  }
}
