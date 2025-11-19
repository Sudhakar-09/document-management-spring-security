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

            if (!ceTaskId) error "CE Task ID NOT FOUND"

            timeout(time: 5, unit: 'MINUTES') {
              waitUntil {
                def resp = sh(script: "curl -s -u \$SONAR_TOKEN: ${SONAR_HOST}/api/ce/task?id=${ceTaskId}", returnStdout: true).trim()
                def status = sh(script: "echo '${resp}' | jq -r '.task.status'", returnStdout: true).trim()
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

    /* ------------------------ 6. FAST AI ANALYSIS ------------------------ */
    stage('AI Analysis & Report Generation') {
      steps {
        echo "[6/9] AI Report Generation (FAST + OLD + NEW)"

        withCredentials([string(credentialsId: 'OPENAI_KEY', variable: 'OPENAI_KEY')]) {
          script {

            def issues = readJSON file: "${REPORT_DIR}/sonar-issues-${BUILD_NUMBER}.json"

            // Load all source code once (FAST)
            def sourceCache = [:]
            new File("src/main/java").eachFileRecurse { f ->
              if (f.isFile() && f.name.endsWith(".java")) {
                sourceCache[f.path] = f.readLines()
              }
            }

            // Build compact issue list
            def issueList = []
            int idx = 0

            issues.issues.each { issue ->
              idx++
              def filePath = issue.component.replace("ai-code-assistant:", "")
              def line = (issue.line ?: 1) as int

              def snippet = "N/A"
              if (sourceCache.containsKey(filePath)) {
                def lines = sourceCache[filePath]
                def start = Math.max(1, line - 1)
                def end = Math.min(lines.size(), line + 1)
                snippet = lines.subList(start - 1, end).join("\n")
              }

              issueList << [
                id      : idx,
                severity: issue.severity,
                rule    : issue.rule,
                message : issue.message,
                file    : filePath,
                line    : line,
                snippet : snippet
              ]
            }

            // Full AI Prompt
            def prompt = """
You are a senior Java + SonarQube auditor.

Produce a MARKDOWN REPORT that includes BOTH STYLES:

============================================================
## 1️⃣ EXECUTIVE SUMMARY (NEW STYLE)
- Overall quality rating
- Major risks
- 5 most important issues
- Suggested refactors
- Score out of 10

============================================================
## 2️⃣ DETAILED ISSUE ANALYSIS (OLD STYLE)
For EACH issue below:
- Issue ID
- Severity (with emoji)
- Rule
- Message
- File + line
- 3-line code snippet in a Java block
- Detailed analysis (root cause)
- Fix explanation
- Effort estimate
- Risk level

============================================================
## 3️⃣ FINAL RECOMMENDATIONS (NEW STYLE)
High-level actions to improve the repo.

============================================================
### ISSUES:
${groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(issueList))}
"""

            def payload = groovy.json.JsonOutput.toJson([
              model: "gpt-4.1-mini",
              messages: [[role: "user", content: prompt]],
              max_tokens: 6000
            ])

            def aiOut = sh(
              script: """#!/bin/bash
curl -s -X POST https://api.openai.com/v1/chat/completions \
  -H "Authorization: Bearer \$OPENAI_KEY" \
  -H "Content-Type: application/json" \
  -d '${payload}' \
  | jq -r '.choices[0].message.content'
""",
              returnStdout: true
            ).trim()

            writeFile file: "${REPORT_DIR}/code-quality-report-${BUILD_NUMBER}.md", text: aiOut
          }
        }
      }
    }

    /* ------------------------ 7. ARCHIVE ------------------------ */
    stage('Archive Reports') {
      steps {
        echo "[7/9] Archiving"
        archiveArtifacts artifacts: "${REPORT_DIR}/*", allowEmptyArchive: true
      }
    }

    /* ------------------------ 8. FINISH ------------------------ */
    stage('Finish') {
      steps { echo "[8/9] Pipeline Complete!" }
    }
  }

  post {
    always { echo "Pipeline finished." }
    success { echo "SUCCESS" }
    failure { echo "FAILED — Check logs" }
  }
}
