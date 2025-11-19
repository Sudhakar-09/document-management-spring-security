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
    MAX_ISSUES_PROCESS = "0"   // reduce to speed up AI stage; set to 0 to process all
  }

  stages {
    stage('Checkout') {
      steps {
        echo "[1/9] Checkout"
        checkout scm
        sh 'mkdir -p "${REPORT_DIR}"'
        sh 'git --version || true'
        sh 'jq --version || true'
      }
    }

    stage('Build') {
      steps {
        echo "[2/9] Build"
        sh 'mvn clean package -DskipTests'
      }
    }

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

    stage('Quality Gate') {
      steps {
        echo "[4/9] Quality Gate"
        script {
          withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
            sh '''#!/bin/bash
set -euo pipefail

CE_FILE="target/sonar/report-task.txt"
if [ ! -f "$CE_FILE" ]; then
  echo "ERROR: $CE_FILE not found"
  exit 1
fi
ceTaskId=$(grep -o 'ceTaskId=[A-Za-z0-9\\-]*' "$CE_FILE" | cut -d= -f2 || true)
if [ -z "$ceTaskId" ]; then
  echo "CE Task ID NOT FOUND"
  exit 1
fi

echo "CE Task ID = $ceTaskId"

# wait for Sonar Compute Engine to finish
start=$(date +%s)
while true; do
  resp=$(curl -s -u "$SONAR_TOKEN": "${SONAR_HOST}/api/ce/task?id=$ceTaskId") || true
  status=$(echo "$resp" | jq -r '.task.status // "UNKNOWN"')
  echo "CE Status = $status"
  if [ "$status" = "SUCCESS" ] || [ "$status" = "FAILED" ]; then
    break
  fi
  sleep 1
  # simple timeout: 5 minutes
  now=$(date +%s)
  if [ $((now-start)) -gt 300 ]; then
    echo "Timeout waiting for CE task"
    exit 1
  fi
done

analysisId=$(echo "$resp" | jq -r '.task.analysisId')
if [ -z "$analysisId" ] || [ "$analysisId" = "null" ]; then
  echo "No analysisId returned"
  exit 1
fi

echo "Analysis ID = $analysisId"

# fetch quality gate
curl -s -u "$SONAR_TOKEN": "${SONAR_HOST}/api/qualitygates/project_status?analysisId=$analysisId" > "${REPORT_DIR}/qualitygate-${BUILD_NUMBER}.json"

echo "Quality Gate JSON stored: ${REPORT_DIR}/qualitygate-${BUILD_NUMBER}.json"
'''
          }
        }
      }
    }

    stage('Fetch Issues') {
      steps {
        echo "[5/9] Fetching Issues"
        withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
          sh '''#!/bin/bash
set -euo pipefail

OUT="${REPORT_DIR}/sonar-issues-${BUILD_NUMBER}.json"
curl -s -u "$SONAR_TOKEN": "${SONAR_HOST}/api/issues/search?componentKeys=ai-code-assistant&ps=500" > "$OUT"
jq --version > /dev/null 2>&1 || true
if [ ! -s "$OUT" ]; then
  echo "No issues fetched"
  exit 0
fi
echo "Issues stored at $OUT"
'''
        }
      }
    }

    stage('AI Analysis & Report Generation') {
      steps {
        echo "[6/9] AI Report Generation (FAST + OLD + NEW)"
        withCredentials([string(credentialsId: 'OPENAI_KEY', variable: 'OPENAI_KEY'), string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
          sh '''#!/bin/bash
set -euo pipefail

ISSUES_FILE="${REPORT_DIR}/sonar-issues-${BUILD_NUMBER}.json"
MD_FILE="${REPORT_DIR}/code-quality-report-${BUILD_NUMBER}.md"
MAX=${MAX_ISSUES_PROCESS}

if [ ! -f "$ISSUES_FILE" ]; then
  echo "No issues file found: $ISSUES_FILE"
  exit 0
fi

# header
cat > "$MD_FILE" <<'MD'
# SonarQube Code Quality Report

**Project:** ai-code-assistant
**Repository:** ${REPO_ORG}/${REPO_NAME}
**Branch:** ${REPO_BRANCH}
**Build:** ${BUILD_NUMBER}
**Generated:** $(date '+%Y-%m-%d %H:%M:%S')

---
MD

# quick summary (by severity) - human readable
jq -r '[.issues[]?.severity] | group_by(.) | map({(.[0]): length}) | add' "$ISSUES_FILE" > "${REPORT_DIR}/severity-summary.json" || true

# append summary
cat >> "$MD_FILE" <<'MD'
## Summary (count by severity)

MD
jq -r 'to_entries[] | "- "+.key+": "+(.value|tostring)' "${REPORT_DIR}/severity-summary.json" >> "$MD_FILE" 2>/dev/null || true

# iterate issues (bash loop, limited by MAX)
count=0
jq -c '.issues[]' "$ISSUES_FILE" | while read -r issue; do
  count=$((count+1))
  if [ "$MAX" -gt 0 ] && [ "$count" -gt "$MAX" ]; then
    echo "Reached MAX ($MAX) issues. Skipping remaining..."
    break
  fi

  # extract fields safely
  rule=$(echo "$issue" | jq -r '.rule // "-"')
  message=$(echo "$issue" | jq -r '.message // "-"')
  severity=$(echo "$issue" | jq -r '.severity // "INFO"')
  component=$(echo "$issue" | jq -r '.component')
  # component looks like ai-code-assistant:src/main/java/... -> strip prefix after last colon
  filepath=$(echo "$component" | sed 's/^[^:]*://')
  line=$(echo "$issue" | jq -r '.line // 1')
  issue_id=$(echo "$issue" | jq -r '.key // ""')

  # compute snippet window (default: line-2 .. line+2)
  start=$(( line > 2 ? line-2 : 1 ))
  end=$(( line + 2 ))

  snippet="// File not found"
  if [ -f "$filepath" ]; then
    snippet=$(sed -n "${start},${end}p" "$filepath" || true)
  fi

  blame="N/A"
  if git rev-parse --is-inside-work-tree >/dev/null 2>&1 && [ -f "$filepath" ]; then
    blame=$(git blame -L ${line},${line} -- "$filepath" 2>/dev/null || echo "N/A")
  fi

  github_link="https://github.com/${REPO_ORG}/${REPO_NAME}/blob/${REPO_BRANCH}/${filepath}#L${line}"

  # Prepare prompt for AI (concise)
  read -r -d '' PROMPT <<'PROMPT'
Analyze this Sonar issue. Provide:
- Short summary (1-2 lines)
- Root cause (1-2 lines)
- Fix (show new code snippet / minimal patch)
- Priority (P1/P2/P3)
- Risk (low/medium/high)
Also include an "OLD CODE" and "NEW CODE" section. Keep response compact.

Rule: __RULE__
Message: __MSG__
Severity: __SEV__
File: __FILE__
Line: __LINE__

Code context:
__SNIPPET__
PROMPT

  # substitute placeholders (use bash parameter expansion safely)
  PROMPT=${PROMPT//'__RULE__'/$rule}
  PROMPT=${PROMPT//'__MSG__'/$message}
  PROMPT=${PROMPT//'__SEV__'/$severity}
  PROMPT=${PROMPT//'__FILE__'/$filepath}
  PROMPT=${PROMPT//'__LINE__'/$line}
  # insert snippet, ensure no EOF collision
  PROMPT=${PROMPT//'__SNIPPET__'/$snippet}

  # create payload
  cat > "${REPORT_DIR}/payload-${count}.json" <<PAY
{
  "model": "gpt-4o-mini",
  "messages": [{"role":"user","content": ${PROMPT@Q}}],
  "max_tokens": 700
}
PAY

  # call OpenAI (single-shot, minimal tokens)
  aiOut=$(curl -s -X POST https://api.openai.com/v1/chat/completions \
    -H "Authorization: Bearer $OPENAI_KEY" \
    -H "Content-Type: application/json" \
    -d @"${REPORT_DIR}/payload-${count}.json" \
    | jq -r '.choices[0].message.content // "(no AI response)"') || aiOut="(AI call failed)"

  # append to markdown
  cat >> "$MD_FILE" <<EOF

---

### Issue ${count} — ${severity} — ${issue_id}
**File:** ${filepath}
**Line:** ${line}
**Rule:** ${rule}
**Blame:** ${blame}
**GitHub:** ${github_link}

#### Code Snippet (context: ${start}-${end})
\`\`\`java
${snippet}
\`\`\`

#### AI Recommendation
${aiOut}

EOF

done

# final insights - simple stats
cat >> "$MD_FILE" <<'MD'

---

## Insights & Next steps
- Report generated automatically. Fix high-severity issues first.
- Use IDE refactor tools for renames (packages/methods) to avoid missing references.
- Consider lowering MAX_ISSUES_PROCESS to speed up runs further or run full analysis nightly.

MD

echo "Report generation complete: $MD_FILE"
'''
        }
      }
    }

    stage('Archive Reports') {
      steps {
        echo "[7/9] Archiving"
        archiveArtifacts artifacts: "${REPORT_DIR}/*", allowEmptyArchive: true
        archiveArtifacts artifacts: "target/*.jar", allowEmptyArchive: true
      }
    }

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
