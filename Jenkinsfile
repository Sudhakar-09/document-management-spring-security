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
        sh 'mkdir -p ${REPORT_DIR}'
        sh 'git --version || true'
        sh 'jq --version || true'
      }
    }

    stage('Build') {
      steps {
        echo "[2/9] Build (mvn clean package -DskipTests)"
        sh 'mvn clean package -DskipTests'
      }
    }

    stage('Sonar Scan') {
      steps {
        echo "[3/9] Sonar scan (upload analysis)"
        withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
          sh '''
            mvn sonar:sonar \
              -Dsonar.token=$SONAR_TOKEN \
              -Dsonar.host.url=${SONAR_HOST} \
              -Dsonar.projectKey=ai-code-assistant \
              -Dsonar.sources=src/main/java \
              -Dsonar.java.binaries=target/classes \
              -Dsonar.projectName="AI Code Assistant"
          '''
        }
      }
    }

    stage('Quality Gate') {
      steps {
        echo "[4/9] Waiting for Sonar CE and Quality Gate"
        script {
          withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
            // Read CE task id
            def ceTaskId = sh(
              script: "grep -o 'ceTaskId=[A-Za-z0-9\\-]*' target/sonar/report-task.txt | cut -d= -f2 || true",
              returnStdout: true
            ).trim()
            if (!ceTaskId) {
              error "CE Task ID not found in target/sonar/report-task.txt - ensure sonar scanner writes report-task.txt"
            }
            echo "CE Task ID = ${ceTaskId}"

            // Poll CE until SUCCESS (with timeout)
            timeout(time: 5, unit: 'MINUTES') {
              waitUntil {
                def ceResp = sh(
                  script: "curl -s -u ${SONAR_TOKEN}: ${SONAR_HOST}/api/ce/task?id=${ceTaskId}",
                  returnStdout: true
                ).trim()
                def status = sh(script: "echo '${ceResp}' | jq -r '.task.status'", returnStdout: true).trim()
                echo "CE status = ${status}"
                return (status == 'SUCCESS' || status == 'FAILED' || status == 'CANCELED')
              }
            }

            // fetch final CE & analysisId
            def finalCe = sh(script: "curl -s -u ${SONAR_TOKEN}: ${SONAR_HOST}/api/ce/task?id=${ceTaskId}", returnStdout: true).trim()
            def analysisId = sh(script: "echo '${finalCe}' | jq -r '.task.analysisId'", returnStdout: true).trim()
            env.ANALYSIS_ID = analysisId
            echo "Analysis ID = ${analysisId}"

            // fetch quality gate
            def qgRaw = sh(script: "curl -s -u ${SONAR_TOKEN}: ${SONAR_HOST}/api/qualitygates/project_status?analysisId=${analysisId}", returnStdout: true).trim()
            def qgStatus = sh(script: "echo '${qgRaw}' | jq -r '.projectStatus.status'", returnStdout: true).trim()
            echo "Quality Gate status = ${qgStatus}"
            if (qgStatus != 'OK') {
              echo "Quality Gate is not OK (status: ${qgStatus}). Continuing to collect issues for report."
            }
            writeFile file: "${REPORT_DIR}/qualitygate-${env.BUILD_NUMBER}.json", text: qgRaw
          }
        } // withCredentials
      } // steps
    } // stage

    stage('Fetch Issues') {
      steps {
        echo "[5/9] Fetch Sonar issues via REST API"
        withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
          sh """
            curl -s -u ${SONAR_TOKEN}: "${SONAR_HOST}/api/issues/search?componentKeys=ai-code-assistant&ps=500" > ${REPORT_DIR}/sonar-issues-${BUILD_NUMBER}.json
          """
          sh "ls -lah ${REPORT_DIR} || true"
        }
      }
    }

    stage('Enrich Issues & AI Analysis') {
      steps {
        echo "[6/9] Enrich issues (extract snippets, git blame, call OpenAI) and build report.md"
        withCredentials([string(credentialsId: 'OPENAI_KEY', variable: 'OPENAI_KEY'), string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
          script {
            // read issues json into Groovy object
            def issuesJson = readFile file: "${REPORT_DIR}/sonar-issues-${BUILD_NUMBER}.json"
            def issues = readJSON text: issuesJson
            // Prepare markdown start
            def mdFile = "${REPORT_DIR}/code-quality-report-${BUILD_NUMBER}.md"
            def header = """# 🧾 SonarQube Code Quality Report

**Project:** ai-code-assistant  
**Repository:** ${REPO_ORG}/${REPO_NAME}  
**Branch:** ${REPO_BRANCH}  
**Build Number:** ${BUILD_NUMBER}  
**Generated On:** ${new Date().format("yyyy-MM-dd HH:mm:ss")}  
**Sonar Analysis ID:** ${env.ANALYSIS_ID ?: 'N/A'}

---

"""
            writeFile file: mdFile, text: header

            // compute summary counts
            def counts = [BLOCKER:0,CRITICAL:0,MAJOR:0,MINOR:0,INFO:0]
            issues.issues.each { issue ->
              def sev = (issue.severity ?: "INFO").toUpperCase()
              if (sev == 'BLOCKER') counts.BLOCKER++
              else if (sev == 'CRITICAL') counts.CRITICAL++
              else if (sev == 'MAJOR') counts.MAJOR++
              else if (sev == 'MINOR') counts.MINOR++
              else counts.INFO++
            }

            def summaryTbl = """
## 📊 Summary

| Severity | Emoji | Count |
|---------:|:-----:|:-----:|
| Blocker | 🟥 | ${counts.BLOCKER} |
| Critical | 🔴 | ${counts.CRITICAL} |
| Major | 🟠 | ${counts.MAJOR} |
| Minor | 🟡 | ${counts.MINOR} |
| Info | 🔵 | ${counts.INFO} |

Total issues: ${issues.paging?.total ?: issues.issues.size()}
---

"""
            writeFile file: mdFile, text: summaryTbl, append: true

            // iterate issues and enrich
            def idx = 0
            for (issue in issues.issues) {
              idx++
              def comp = issue.component ?: ''
              // component has form 'ai-code-assistant:src/main/.../X.java'
              def filepath = comp.replaceFirst('^ai-code-assistant:', '')
              def line = (issue.line ?: 1) as Integer
              def startLine = Math.max(1, line - 2)
              def endLine = line + 2

              // ensure file exists
              def snippet = ""
              if (fileExists(filepath)) {
                snippet = sh(script: "sed -n '${startLine},${endLine}p' ${filepath} || true", returnStdout: true).trim()
              } else {
                snippet = "// file not found in workspace: ${filepath}"
              }

              // git blame for the target lines (if file exists)
              def blameInfo = ""
              if (fileExists(filepath)) {
                blameInfo = sh(script: "git blame -L ${line},${line} -- ${filepath} | sed -n '1,1p' || true", returnStdout: true).trim()
              } else {
                blameInfo = "N/A"
              }

              // build GitHub link to file and lines
              def githubLink = "https://github.com/${REPO_ORG}/${REPO_NAME}/blob/${REPO_BRANCH}/${filepath}#L${startLine}-L${endLine}"

              // build OpenAI prompt payload safely using jq to escape
              def userPrompt = """
You are an expert Java developer and code reviewer. Given the Sonar issue below, produce a high-quality Markdown analysis section with:
- Short summary (1-2 lines)
- Root cause explanation
- Proposed fix (code block, diff-style if possible)
- Estimated fix time in minutes
- Priority (High/Medium/Low)
- Risk score (0-100)
- One-line git action suggestion (e.g., "create branch fix/sonar-<key>" or "open PR to main")

Issue rule: ${issue.rule}
Message: ${issue.message}
Severity: ${issue.severity}
File: ${filepath}
Lines: ${startLine}-${endLine}
Code snippet:
\`\`\`java
${snippet}
\`\`\`

Make the output pure Markdown (no extra commentary). Keep it concise but actionable.
"""
              // escape prompt into JSON string via jq -R -s
              writeFile file: "${REPORT_DIR}/prompt-${idx}.txt", text: userPrompt

              // call OpenAI
              def aiOut = sh(
                script: """set -o pipefail
PROMPT_JSON=$(jq -Rs --arg p "$(cat ${REPORT_DIR}/prompt-${idx}.txt)" '{model: "gpt-4.1-mini", messages:[{role:"user", content:$p}], max_tokens:1200}' <<< '')
curl -s -X POST "https://api.openai.com/v1/chat/completions" \\
  -H "Authorization: Bearer $OPENAI_KEY" \\
  -H "Content-Type: application/json" \\
  -d "$PROMPT_JSON" | jq -r '.choices[0].message.content'
""", returnStdout: true
              ).trim()

              // Build issue block in markdown
              def issueBlock = """
---

### Issue ${idx} — **${issue.severity ?: 'INFO'}**
**Rule:** ${issue.rule}
**Message:** ${issue.message}
**File:** ${filepath}
**Lines:** ${startLine}-${endLine}
**Issue Key:** ${issue.key}
**Git blame (line ${line}):** ${blameInfo}
**GitHub link:** ${githubLink}

#### Code snippet
\`\`\`java
${snippet}
\`\`\`

#### AI analysis & suggestion
${aiOut}

"""
              writeFile file: mdFile, text: issueBlock, append: true
            } // end issues loop

            echo "[INFO] Markdown report written to ${mdFile}"

            // Try convert to HTML if pandoc is present
            def htmlFile = "${REPORT_DIR}/code-quality-report-${BUILD_NUMBER}.html"
            def hasPandoc = sh(script: "command -v pandoc >/dev/null 2>&1 && echo yes || echo no", returnStdout: true).trim()
            if (hasPandoc == 'yes') {
              sh "pandoc ${mdFile} -o ${htmlFile} --css= -s -V title='Code Quality Report' || true"
              echo "[INFO] HTML report generated: ${htmlFile}"
            } else {
              echo "[INFO] pandoc not found — skipping HTML generation (install pandoc to enable HTML output)."
            }

          } // script
        } // withCredentials
      } // steps
    } // stage Enrich

    stage('Archive & Show Links') {
      steps {
        echo "[7/9] Archive artifacts and print download URLs"
        sh "ls -lah ${REPORT_DIR} || true"
        archiveArtifacts artifacts: "${REPORT_DIR}/*", allowEmptyArchive: true
        archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true

        script {
          def mdName = "code-quality-report-${BUILD_NUMBER}.md"
          def htmlName = "code-quality-report-${BUILD_NUMBER}.html"
          echo "Markdown report: ${env.BUILD_URL}artifact/${REPORT_DIR}/${mdName}"
          echo "HTML report (if generated): ${env.BUILD_URL}artifact/${REPORT_DIR}/${htmlName}"
        }
      }
    }

    stage('Finish') {
      steps {
        echo "[8/9] Done. Reports archived and available in Artifacts."
      }
    }
  } // stages

  post {
    always {
      echo "Pipeline finished. Artifacts archived."
      // keep artifacts already archived above
    }
    success {
      echo "✅ Build & analysis successful"
    }
    failure {
      echo "❌ Pipeline failed — check console log"
    }
  }
}
