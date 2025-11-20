pipeline {
  agent any

  tools {
    jdk 'JDK17'
    maven 'mvn'
  }

  environment {
    SONAR_HOST  = "http://localhost:9000"
    REPORT_DIR  = "code-quality-reports"
    REPO_ORG    = "Sudhakar-09"
    REPO_NAME   = "document-management-spring-security"
    REPO_BRANCH = "main"
    MAX_ISSUES_PROCESS = "20"
  }

  stages {

    /* ---------------------- 1. CHECKOUT ---------------------- */
    stage('Checkout') {
      steps {
        echo "[1/9] Checkout started"
        checkout scm
        sh '''
          echo "[CHECKOUT] Creating report directory..."
          mkdir -p "${REPORT_DIR}"
          echo "[CHECKOUT] Git Version:"
          git --version || true
          echo "[CHECKOUT] jq Version:"
          jq --version || true
        '''
      }
    }

    /* ---------------------- 2. BUILD ------------------------- */
    stage('Build') {
      steps {
        echo "[2/9] Build started"
        sh '''
          echo "[BUILD] Running Maven..."
          mvn clean package -DskipTests
          echo "[BUILD] Build completed."
        '''
      }
    }

    /* ---------------------- 3. SONAR SCAN -------------------- */
    stage('Sonar Scan') {
      steps {
        echo "[3/9] Sonar Scan started"
        withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
          sh '''
            echo "[SONAR] Executing sonar:sonar..."
            mvn sonar:sonar \
              -Dsonar.token="$SONAR_TOKEN" \
              -Dsonar.host.url="${SONAR_HOST}" \
              -Dsonar.projectKey=ai-code-assistant \
              -Dsonar.sources=src/main/java \
              -Dsonar.java.binaries=target/classes \
              -Dsonar.projectName="AI Code Assistant"
            echo "[SONAR] Sonar analysis triggered."
          '''
        }
      }
    }

    /* ---------------------- 4. QUALITY GATE ------------------ */
    stage('Quality Gate') {
      steps {
        echo "[4/9] Checking Quality Gate"
        script {
          withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
            sh '''
              echo "[QGATE] Reading report-task.txt..."
              CE_FILE="target/sonar/report-task.txt"

              if [ ! -f "$CE_FILE" ]; then
                echo "[QGATE][ERROR] report-task.txt missing"
                exit 1
              fi

              ceTaskId=$(grep -o 'ceTaskId=[A-Za-z0-9\\-]*' "$CE_FILE" | cut -d= -f2)
              echo "[QGATE] CE Task ID: $ceTaskId"

              echo "[QGATE] Polling CE task status..."
              while true; do
                resp=$(curl -s -u "$SONAR_TOKEN": "${SONAR_HOST}/api/ce/task?id=$ceTaskId")
                status=$(echo "$resp" | jq -r '.task.status')
                echo "[QGATE] CE Status: $status"

                if [ "$status" = "SUCCESS" ] || [ "$status" = "FAILED" ]; then
                  break
                fi
                sleep 1
              done

              analysisId=$(echo "$resp" | jq -r '.task.analysisId')
              echo "[QGATE] Analysis ID: ${analysisId}"

              echo "[QGATE] Fetching Quality Gate result..."
              curl -s -u "$SONAR_TOKEN": \
                "${SONAR_HOST}/api/qualitygates/project_status?analysisId=$analysisId" \
                > "${REPORT_DIR}/qualitygate-${BUILD_NUMBER}.json"

              echo "[QGATE] Quality Gate results saved."
            '''
          }
        }
      }
    }

    /* ---------------------- 5. FETCH ISSUES ------------------ */
    stage('Fetch Issues') {
      steps {
        echo "[5/9] Fetching Issues"
        withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
          sh '''
            echo "[FETCH] Fetching all issues..."
            OUT="${REPORT_DIR}/sonar-issues-${BUILD_NUMBER}.json"

            curl -s -u "$SONAR_TOKEN": \
              "${SONAR_HOST}/api/issues/search?componentKeys=ai-code-assistant&ps=500" \
              > "$OUT"

            echo "[FETCH] Issues saved to: $OUT"
          '''
        }
      }
    }

    /* ---------------------- 6. AI REPORT --------------------- */
    stage('AI Analysis & Report Generation') {
      steps {
        echo "[6/9] AI Report Generation started"
        withCredentials([
          string(credentialsId: 'OPENAI_KEY', variable: 'OPENAI_KEY'),
          string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')
        ]) {

          sh '''#!/bin/bash
set -euo pipefail

echo "[AI] Generating report..."

REPORT_DIR="${REPORT_DIR}"
ISSUES_FILE="${REPORT_DIR}/sonar-issues-${BUILD_NUMBER}.json"
MD_FILE="${REPORT_DIR}/code-quality-report-${BUILD_NUMBER}.md"
MAX=${MAX_ISSUES_PROCESS:-20}

mkdir -p "${REPORT_DIR}"

# -------------------------------------------------
# REPORT HEADER
# -------------------------------------------------
echo "Writing header..."
cat > "${MD_FILE}" <<EOF
# SonarQube Code Quality Report

Project: ai-code-assistant  
Repository: ${REPO_ORG}/${REPO_NAME}  
Branch: ${REPO_BRANCH}  
Build: ${BUILD_NUMBER}  
Generated: $(date '+%Y-%m-%d %H:%M:%S')  
---
EOF


# -------------------------------------------------
# SUMMARY (Severity + Emoji + Dynamic Counts)
# -------------------------------------------------
echo "[AI] Computing summary..."

# Generate severity summary JSON
jq -r '[.issues[]?.severity] | group_by(.) | map({(.[0]): length}) | add' \
  "$ISSUES_FILE" > "${REPORT_DIR}/severity-summary.json"

# Read dynamic values (UPPERCASE KEYS, default 0 if key not present)
BLOCKER=$(jq '.BLOCKER // 0' "${REPORT_DIR}/severity-summary.json")
CRITICAL=$(jq '.CRITICAL // 0' "${REPORT_DIR}/severity-summary.json")
MAJOR=$(jq '.MAJOR // 0' "${REPORT_DIR}/severity-summary.json")
MINOR=$(jq '.MINOR // 0' "${REPORT_DIR}/severity-summary.json")
INFO=$(jq '.INFO // 0' "${REPORT_DIR}/severity-summary.json")

TOTAL=$((BLOCKER + CRITICAL + MAJOR + MINOR + INFO))

# Write markdown summary
cat >> "${MD_FILE}" <<EOF

## Summary

| Severity        | Count |
|-----------------|------:|
| Blocker 🟥       | ${BLOCKER} |
| Critical 🔴      | ${CRITICAL} |
| Major 🟠         | ${MAJOR} |
| Minor 🟡         | ${MINOR} |
| Info 🔵          | ${INFO} |

Total issues: ${TOTAL}

EOF


# -------------------------------------------------
# ISSUE-BY-ISSUE AI PROCESSING (EXACTLY SAME)
# -------------------------------------------------

count=0

jq -c '.issues[]' "${ISSUES_FILE}" | while read -r issue; do

  count=$((count+1))
  if [ "$MAX" -gt 0 ] && [ "$count" -gt "$MAX" ]; then
    break
  fi

  rule=$(echo "$issue" | jq -r '.rule')
  msg=$(echo "$issue" | jq -r '.message')
  severity=$(echo "$issue" | jq -r '.severity')
  component=$(echo "$issue" | jq -r '.component')
  filepath=$(echo "$component" | sed 's/^[^:]*://')
  line=$(echo "$issue" | jq -r '.line')
  issue_id=$(echo "$issue" | jq -r '.key')

  start=$(( line > 2 ? line-2 : 1 ))
  end=$(( line + 2 ))

  snippet="// File not found"
  if [ -f "$filepath" ]; then
    snippet=$(sed -n "${start},${end}p" "$filepath")
  fi

  PROMPT="Analyze SonarQube issue and provide: summary, root cause, old code, updated code, priority, risk.

Rule: ${rule}
Message: ${msg}
Severity: ${severity}
File: ${filepath}
Line: ${line}

Code:
${snippet}
"

  printf "%s" "$PROMPT" | jq -Rs '{model:"gpt-4o-mini", messages:[{role:"user", content:.}], max_tokens:700}' \
    > "${REPORT_DIR}/payload-${count}.json"

  aiOut=$(curl -s -X POST "https://api.openai.com/v1/chat/completions" \
    -H "Authorization: Bearer ${OPENAI_KEY}" \
    -H "Content-Type: application/json" \
    --data-binary @"${REPORT_DIR}/payload-${count}.json" \
    | jq -r '.choices[0].message.content // "(no response)"')

  cat >> "${MD_FILE}" <<EOF

---

### Issue ${count} — ${severity} — ${issue_id}
File: ${filepath}  
Line: ${line}  
Rule: ${rule}

#### Code Snippet
<code>
${snippet}
</code>

#### AI Recommendation
${aiOut}

EOF

done

echo "[AI] Report generation completed."

'''
        }
      }
    }

    /* ---------------------- 7. ARCHIVE ------------------------ */
    stage('Archive Reports') {
      steps {
        echo "[7/9] Archiving artifacts..."
        archiveArtifacts artifacts: "${REPORT_DIR}/*", allowEmptyArchive: true
      }
    }

    /* ---------------------- 8. FINISH ------------------------- */
    stage('Finish') {
      steps {
        echo "[8/9] Pipeline Complete."
      }
    }
  }

  post {
    always { echo "Pipeline finished." }
    success { echo "SUCCESS" }
    failure { echo "FAILED — Check logs" }
  }
}
