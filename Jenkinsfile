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
        echo "[1/9] Checkout"
        checkout scm
        sh 'mkdir -p "${REPORT_DIR}"'
        sh 'git --version || true'
        sh 'jq --version || true'
      }
    }

    /* ---------------------- 2. BUILD ------------------------- */
    stage('Build') {
      steps {
        echo "[2/9] Build"
        sh 'mvn clean package -DskipTests'
      }
    }

    /* ---------------------- 3. SONAR SCAN -------------------- */
    stage('Sonar Scan') {
      steps {
        echo "[3/9] Sonar Scan"
        withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
          sh '''#!/bin/bash
set -euo pipefail

mvn sonar:sonar \
  -Dsonar.token="$SONAR_TOKEN" \
  -Dsonar.host.url="${SONAR_HOST}" \
  -Dsonar.projectKey=ai-code-assistant \
  -Dsonar.sources=src/main/java \
  -Dsonar.java.binaries=target/classes \
  -Dsonar.projectName="AI Code Assistant"
'''
        }
      }
    }

    /* ---------------------- 4. QUALITY GATE ------------------ */
    stage('Quality Gate') {
      steps {
        echo "[4/9] Quality Gate"
        script {
          withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
            sh '''#!/bin/bash
set -euo pipefail

CE_FILE="target/sonar/report-task.txt"
if [ ! -f "$CE_FILE" ]; then
  echo "ERROR: report-task.txt not found"
  exit 1
fi

ceTaskId=$(grep -o 'ceTaskId=[A-Za-z0-9\\-]*' "$CE_FILE" | cut -d= -f2 || true)
if [ -z "$ceTaskId" ]; then
  echo "CE Task ID NOT FOUND"
  exit 1
fi

echo "CE Task ID = $ceTaskId"

# Poll SonarQube Compute Engine
while true; do
  resp=$(curl -s -u "$SONAR_TOKEN": "${SONAR_HOST}/api/ce/task?id=$ceTaskId")
  status=$(echo "$resp" | jq -r '.task.status')
  echo "CE Status = $status"

  if [ "$status" = "SUCCESS" ] || [ "$status" = "FAILED" ]; then
    break
  fi
  sleep 1
done

analysisId=$(echo "$resp" | jq -r '.task.analysisId')
echo "Analysis ID = $analysisId"

curl -s -u "$SONAR_TOKEN": \
  "${SONAR_HOST}/api/qualitygates/project_status?analysisId=$analysisId" \
  > "${REPORT_DIR}/qualitygate-${BUILD_NUMBER}.json"

echo "QGate JSON saved."
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
          sh '''#!/bin/bash
set -euo pipefail

OUT="${REPORT_DIR}/sonar-issues-${BUILD_NUMBER}.json"
curl -s -u "$SONAR_TOKEN": \
  "${SONAR_HOST}/api/issues/search?componentKeys=ai-code-assistant&ps=500" \
  > "$OUT"

echo "Issues stored: $OUT"
'''
        }
      }
    }

    /* ---------------------- 6. AI REPORT --------------------- */
    stage('AI Analysis & Report Generation') {
      steps {
        echo "[6/9] AI Report (Fast, Old+New, Safe)"
        withCredentials([
          string(credentialsId: 'OPENAI_KEY', variable: 'OPENAI_KEY'),
          string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')
        ]) {
          sh '''#!/bin/bash
set -euo pipefail

REPORT_DIR="${REPORT_DIR}"
ISSUES_FILE="${REPORT_DIR}/sonar-issues-${BUILD_NUMBER}.json"
MD_FILE="${REPORT_DIR}/code-quality-report-${BUILD_NUMBER}.md"
MAX=${MAX_ISSUES_PROCESS:-20}

mkdir -p "${REPORT_DIR}"

# Header
echo "# SonarQube Code Quality Report" > "${MD_FILE}"
echo "**Build:** ${BUILD_NUMBER}" >> "${MD_FILE}"
echo "**Generated:** $(date '+%Y-%m-%d %H:%M:%S')" >> "${MD_FILE}"
echo "---" >> "${MD_FILE}"

# Summary
jq -r '[.issues[]?.severity] | group_by(.) | map({(.[0]): length}) | add' \
  "$ISSUES_FILE" > "${REPORT_DIR}/severity-summary.json"

echo "## Summary" >> "${MD_FILE}"
jq -r 'to_entries[] | "- " + .key + ": " + (.value|tostring)' \
  "${REPORT_DIR}/severity-summary.json" >> "${MD_FILE}"

count=0

# Loop issues
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

  # Snippet
  snippet="// File not found"
  if [ -f "$filepath" ]; then
    snippet=$(sed -n "${start},${end}p" "$filepath")
  fi

  # AI prompt
  PROMPT="Analyze Sonar issue & produce short summary, root cause, OLD CODE, NEW CODE patch, priority, risk.
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

  # Append to report WITHOUT BACKTICKS
  cat >> "${MD_FILE}" <<EOF

---

### Issue ${count} — ${severity} — ${issue_id}
**File:** ${filepath}  
**Line:** ${line}  
**Rule:** ${rule}

#### Code Snippet
<code java>
${snippet}
</code>

#### AI Recommendation
${aiOut}

EOF

done

echo "---" >> "${MD_FILE}"
echo "## End of Report" >> "${MD_FILE}"
'''
        }
      }
    }

    /* ---------------------- 7. ARCHIVE ------------------------ */
    stage('Archive Reports') {
      steps {
        echo "[7/9] Archiving"
        archiveArtifacts artifacts: "${REPORT_DIR}/*", allowEmptyArchive: true
      }
    }

    /* ---------------------- 8. FINISH ------------------------- */
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
