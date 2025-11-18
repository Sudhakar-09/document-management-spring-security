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

    stage('Checkout') {
      steps {
        echo "[1/9] Checkout"
        checkout scm
        sh "mkdir -p ${REPORT_DIR}"
      }
    }

    stage('Build') {
      steps {
        echo "[2/9] Build"
        sh "mvn clean package -DskipTests"
      }
    }

    stage('Sonar Scan') {
      steps {
        echo "[3/9] Sonar Scan"
        withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
          sh """
            mvn sonar:sonar \
              -Dsonar.token=$SONAR_TOKEN \
              -Dsonar.host.url=${SONAR_HOST} \
              -Dsonar.projectKey=ai-code-assistant \
              -Dsonar.sources=src/main/java \
              -Dsonar.java.binaries=target/classes \
              -Dsonar.projectName="AI Code Assistant"
          """
        }
      }
    }

    stage('Quality Gate') {
      steps {
        echo "[4/9] Check Quality Gate"
        script {
          withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {

            def ceTaskId = sh(
              script: "grep -o 'ceTaskId=[A-Za-z0-9\\-]*' target/sonar/report-task.txt | cut -d= -f2 || true",
              returnStdout: true
            ).trim()

            if (!ceTaskId) {
              error "CE Task ID not found"
            }

            timeout(time: 5, unit: 'MINUTES') {
              waitUntil {
                def ceResp = sh(
                  script: "curl -s -u ${SONAR_TOKEN}: ${SONAR_HOST}/api/ce/task?id=${ceTaskId}",
                  returnStdout: true
                ).trim()

                def status = sh(script: "echo '${ceResp}' | jq -r '.task.status'", returnStdout: true).trim()
                echo "CE Status = ${status}"

                return (status == "SUCCESS")
              }
            }

            def finalCe = sh(script: "curl -s -u ${SONAR_TOKEN}: ${SONAR_HOST}/api/ce/task?id=${ceTaskId}", returnStdout: true).trim()
            env.ANALYSIS_ID = sh(script: "echo '${finalCe}' | jq -r '.task.analysisId'", returnStdout: true).trim()

            def qgRaw = sh(script: "curl -s -u ${SONAR_TOKEN}: ${SONAR_HOST}/api/qualitygates/project_status?analysisId=${env.ANALYSIS_ID}", returnStdout: true).trim()
            writeFile file: "${REPORT_DIR}/quality-gate-${BUILD_NUMBER}.json", text: qgRaw

            echo "Quality Gate JSON saved."
          }
        }
      }
    }

    stage('Fetch Issues') {
      steps {
        echo "[5/9] Fetch Issues"
        withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
          sh """
            curl -s -u ${SONAR_TOKEN}: "${SONAR_HOST}/api/issues/search?componentKeys=ai-code-assistant&ps=500" \
            > ${REPORT_DIR}/sonar-issues-${BUILD_NUMBER}.json
          """
        }
      }
    }

    stage('AI Analysis & Report Build') {
      steps {
        echo "[6/9] Generate AI Report"
        withCredentials([
          string(credentialsId: 'OPENAI_KEY', variable: 'OPENAI_KEY'),
          string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')
        ]) {

          script {

            def issuesJson = readFile("${REPORT_DIR}/sonar-issues-${BUILD_NUMBER}.json")
            def parsed = readJSON(text: issuesJson)
            def issues = parsed.issues

            def md = "${REPORT_DIR}/code-quality-report-${BUILD_NUMBER}.md"

            // HEADER
            def header = """
# SonarQube Code Quality Report

**Project:** ai-code-assistant  
**Repository:** ${REPO_ORG}/${REPO_NAME}  
**Branch:** ${REPO_BRANCH}  
**Build Number:** ${BUILD_NUMBER}  
**Generated On:** ${new Date().format("yyyy-MM-dd HH:mm:ss")}  
**Analysis ID:** ${env.ANALYSIS_ID}

---

"""
            writeFile file: md, text: header

            // SUMMARY
            def counts = [BLOCKER:0,CRITICAL:0,MAJOR:0,MINOR:0,INFO:0]
            issues.each { i ->
              def s = (i.severity ?: "INFO").toUpperCase()
              if (counts.containsKey(s)) counts[s]++
            }

            def summary = """
## Summary

| Severity | Emoji | Count |
|---------:|:-----:|:-----:|
| Blocker | 🟥 | ${counts.BLOCKER} |
| Critical | 🔴 | ${counts.CRITICAL} |
| Major | 🟠 | ${counts.MAJOR} |
| Minor | 🟡 | ${counts.MINOR} |
| Info | 🔵 | ${counts.INFO} |

---

"""
            writeFile file: md, text: summary, append: true

            // ISSUE LOOP
            int idx = 0
            for (issue in issues) {
              idx++

              def filePath = issue.component.replace("ai-code-assistant:", "")
              def line = issue.line ?: 1
              def start = Math.max(1, line - 2)
              def end = line + 2

              def snippet = ""
              if (fileExists(filePath)) {
                snippet = sh(script: "sed -n '${start},${end}p' ${filePath}", returnStdout: true).trim()
              } else {
                snippet = "File not found in workspace."
              }

              // indent snippet (Markdown safe)
              def indentedSnippet = snippet.replaceAll("(?m)^", "    ")

              def github = "https://github.com/${REPO_ORG}/${REPO_NAME}/blob/${REPO_BRANCH}/${filePath}#L${start}-L${end}"

              def prompt = """
You are an expert Java reviewer. Analyze the following Sonar issue and produce a Markdown section with:
- Short issue summary
- Root cause
- Code fix with a suggested patch
- Priority (High/Medium/Low)
- Estimated fix time
- Risk score (0-100)
- One-line git action advice

Issue: ${issue.message}
Severity: ${issue.severity}
Rule: ${issue.rule}
File: ${filePath}
Lines: ${start}-${end}

Code:
${indentedSnippet}

Only return Markdown.
"""

              writeFile file: "${REPORT_DIR}/prompt-${idx}.txt", text: prompt

              def aiOut = sh(
                script: """
PROMPT=$(jq -Rs --arg p "$(cat ${REPORT_DIR}/prompt-${idx}.txt)" '{"model":"gpt-4.1-mini","messages":[{"role":"user","content":$p}]}' <<< '')
curl -s -X POST "https://api.openai.com/v1/chat/completions" \
  -H "Authorization: Bearer $OPENAI_KEY" \
  -H "Content-Type: application/json" \
  -d "$PROMPT" | jq -r '.choices[0].message.content'
""",
                returnStdout: true
              ).trim()

              def block = """
---

## Issue ${idx} — ${issue.severity}
**Message:** ${issue.message}  
**Rule:** ${issue.rule}  
**File:** ${filePath}  
**Lines:** ${start}-${end}  
**GitHub:** ${github}

### Code Snippet
${indentedSnippet}

### AI Analysis
${aiOut}

"""
              writeFile file: md, text: block, append: true
            }
          }
        }
      }
    }

    stage('Archive Reports') {
      steps {
        echo "[7/9] Archiving Reports"
        archiveArtifacts artifacts: "${REPORT_DIR}/*", allowEmptyArchive: true
        archiveArtifacts artifacts: "target/*.jar", allowEmptyArchive: true
      }
    }

    stage('Finish') {
      steps {
        echo "[8/9] Completed Successfully"
      }
    }
  }

  post {
    always {
      echo "Pipeline Completed."
    }
    success {
      echo "SUCCESS"
    }
    failure {
      echo "FAILED"
    }
  }
}
