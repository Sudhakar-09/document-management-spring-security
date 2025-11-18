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
        sh "git --version || true"
        sh "jq --version || true"
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
        echo "[3/9] Running sonar scan"
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
        echo "[4/9] Waiting for CE + Quality Gate"
        script {
          withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
            def ceTaskId = sh(
              script: "grep -o 'ceTaskId=[A-Za-z0-9\\-]*' target/sonar/report-task.txt | cut -d= -f2 || true",
              returnStdout: true
            ).trim()
            if (!ceTaskId) error "CE Task ID NOT FOUND"
            echo "CE Task ID = ${ceTaskId}"

            timeout(time: 5, unit: 'MINUTES') {
              waitUntil {
                def resp = sh(
                  script: "curl -s -u \$SONAR_TOKEN: ${SONAR_HOST}/api/ce/task?id=${ceTaskId}",
                  returnStdout: true
                ).trim()
                def status = sh(script: "echo '${resp}' | jq -r '.task.status'", returnStdout: true).trim()
                echo "CE Status = ${status}"
                return (status == "SUCCESS" || status == "FAILED")
              }
            }

            def finalResp = sh(script: "curl -s -u \$SONAR_TOKEN: ${SONAR_HOST}/api/ce/task?id=${ceTaskId}", returnStdout: true).trim()
            def analysisId = sh(script: "echo '${finalResp}' | jq -r '.task.analysisId'", returnStdout: true).trim()
            env.ANALYSIS_ID = analysisId

            def qgJson = sh(script: "curl -s -u \$SONAR_TOKEN: ${SONAR_HOST}/api/qualitygates/project_status?analysisId=${analysisId}", returnStdout: true).trim()
            writeFile file: "${REPORT_DIR}/qualitygate-${BUILD_NUMBER}.json", text: qgJson
            def qgStatus = sh(script: "echo '${qgJson}' | jq -r '.projectStatus.status'", returnStdout: true).trim()
            echo "Quality Gate = ${qgStatus}"
          }
        }
      }
    }

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

    stage('AI Analysis & Report Generation') {
      steps {
        echo "[6/9] AI report generation"
        withCredentials([
          string(credentialsId: 'OPENAI_KEY', variable: 'OPENAI_KEY'),
          string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')
        ]) {
          script {
            // read issues JSON
            def issuesJson = readFile("${REPORT_DIR}/sonar-issues-${BUILD_NUMBER}.json")
            def issues = readJSON(text: issuesJson)

            def mdFile = "${REPORT_DIR}/code-quality-report-${BUILD_NUMBER}.md"

            // header (write via here-doc to preserve exact content)
            sh """cat <<'EOF' > ${mdFile}
# 🧾 SonarQube Code Quality Report

**Project:** ai-code-assistant  
**Repository:** ${REPO_ORG}/${REPO_NAME}  
**Branch:** ${REPO_BRANCH}  
**Build:** ${BUILD_NUMBER}  
**Generated:** ${new Date().format("yyyy-MM-dd HH:mm:ss")}  
**Analysis ID:** ${env.ANALYSIS_ID ?: 'N/A'}

---
EOF
"""

            // compute counts
            def counts = [BLOCKER:0,CRITICAL:0,MAJOR:0,MINOR:0,INFO:0]
            issues.issues.each { issue ->
              def sev = (issue.severity ?: "INFO").toUpperCase()
              if (sev == 'BLOCKER') counts.BLOCKER++
              else if (sev == 'CRITICAL') counts.CRITICAL++
              else if (sev == 'MAJOR') counts.MAJOR++
              else if (sev == 'MINOR') counts.MINOR++
              else counts.INFO++
            }

            def summary = """## 📊 Summary

| Severity | Emoji | Count |
|---------|:-----:|------:|
| Blocker | 🟥 | ${counts.BLOCKER} |
| Critical | 🔴 | ${counts.CRITICAL} |
| Major | 🟠 | ${counts.MAJOR} |
| Minor | 🟡 | ${counts.MINOR} |
| Info | 🔵 | ${counts.INFO} |

Total issues: ${issues.paging?.total ?: issues.issues.size()}

---
"""

            // append summary using a single-quoted here-doc to avoid expansion/execution
            sh """cat <<'EOF' >> ${mdFile}
${summary}
EOF
"""

            // iterate issues
            def idx = 0
            for (issue in issues.issues) {
              idx++
              def comp = issue.component ?: ''
              def filepath = comp.replaceFirst('^ai-code-assistant:', '')
              def line = (issue.line ?: 1) as Integer
              def startLine = Math.max(1, line - 2)
              def endLine = line + 2

              def snippet = ""
              if (fileExists(filepath)) {
                snippet = sh(script: "sed -n '${startLine},${endLine}p' ${filepath} || true", returnStdout: true).trim()
              } else {
                snippet = "// file not found in workspace: ${filepath}"
              }

              def blameInfo = fileExists(filepath) ? sh(script: "git blame -L ${line},${line} -- ${filepath} | sed -n '1,1p' || true", returnStdout: true).trim() : "N/A"
              def githubLink = "https://github.com/${REPO_ORG}/${REPO_NAME}/blob/${REPO_BRANCH}/${filepath}#L${startLine}-L${endLine}"

              // build prompt (escape handled by writeFile)
              def userPrompt = """You are an expert Java developer and code reviewer. Given the Sonar issue below, produce a high-quality Markdown analysis section with:
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

              // write payload safely
              def payloadJson = groovy.json.JsonOutput.toJson([
                model: "gpt-4.1-mini",
                messages: [[role: "user", content: userPrompt]],
                max_tokens: 1200
              ])
              writeFile file: "${REPORT_DIR}/payload-${idx}.json", text: payloadJson

              // call OpenAI
              def aiOut = sh(
                script: """curl -s -X POST "https://api.openai.com/v1/chat/completions" \\
  -H "Authorization: Bearer \$OPENAI_KEY" \\
  -H "Content-Type: application/json" \\
  -d @"${REPORT_DIR}/payload-${idx}.json" | jq -r '.choices[0].message.content' || true""",
                returnStdout: true
              ).trim()

              // build markdown block (use here-doc single-quoted to preserve everything)
              def blockHeader = """
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
"""
              // append header
              sh """cat <<'EOF' >> ${mdFile}
${blockHeader}
EOF
"""

              // append code snippet (indented)
              def indented = snippet.replaceAll("(?m)^", "    ")
              sh """cat <<'EOF' >> ${mdFile}
\`\`\`java
${indented}
\`\`\`
EOF
"""

              // append AI output
              sh """cat <<'EOF' >> ${mdFile}
#### AI analysis & suggestion
${aiOut}

EOF
"""
            } // end issues loop

            echo "[INFO] Markdown report written to ${mdFile}"

            // optional HTML via pandoc (if installed)
            def htmlFile = "${REPORT_DIR}/code-quality-report-${BUILD_NUMBER}.html"
            def hasPandoc = sh(script: "command -v pandoc >/dev/null 2>&1 && echo yes || echo no", returnStdout: true).trim()
            if (hasPandoc == 'yes') {
              sh "pandoc ${mdFile} -o ${htmlFile} -s -V title='Code Quality Report' || true"
              echo "[INFO] HTML report generated: ${htmlFile}"
            } else {
              echo "[INFO] pandoc not found — skipping HTML generation."
            }
          } // script
        } // withCredentials
      } // steps
    } // stage

    stage('Archive Reports') {
      steps {
        echo "[7/9] Archiving reports"
        archiveArtifacts artifacts: "${REPORT_DIR}/*", allowEmptyArchive: true
        archiveArtifacts artifacts: "target/*.jar", allowEmptyArchive: true
        script {
          echo "Markdown report: ${env.BUILD_URL}artifact/${REPORT_DIR}/code-quality-report-${BUILD_NUMBER}.md"
        }
      }
    }

    stage('Finish') {
      steps {
        echo "[8/9] Pipeline complete!"
      }
    }
  } // stages

  post {
    always { echo "Pipeline finished." }
    success { echo "SUCCESS" }
    failure { echo "FAILED — Check logs" }
  }
}
