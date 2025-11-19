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
    MAX_ISSUES_PROCESS = "20"   // reduce to speed up AI stage; set to 0 to process all
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
    echo "[6/9] AI Report Generation (FAST + OLD + NEW) -- verbose"
    withCredentials([string(credentialsId: 'OPENAI_KEY', variable: 'OPENAI_KEY'),
                     string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
      sh '''#!/bin/bash
set -euo pipefail
set -x

REPORT_DIR="${REPORT_DIR:-code-quality-reports}"
ISSUES_FILE="${REPORT_DIR}/sonar-issues-${BUILD_NUMBER}.json"
MD_FILE="${REPORT_DIR}/code-quality-report-${BUILD_NUMBER}.md"
LOG_FILE="${REPORT_DIR}/ai-stage-${BUILD_NUMBER}.log"
MAX=${MAX_ISSUES_PROCESS:-20}

# ensure report dir
mkdir -p "${REPORT_DIR}"

# redirect all stdout/stderr to a log too (so Jenkins shows it as artifact)
exec > >(tee -a "${LOG_FILE}") 2>&1

echo "AI stage log: ${LOG_FILE}"
echo "ISSUES_FILE=${ISSUES_FILE}"
[ -f "${ISSUES_FILE}" ] || { echo "No issues file: ${ISSUES_FILE}"; exit 0; }

# Minimal header
cat > "${MD_FILE}" <<'MD'
# SonarQube Code Quality Report (generated)
MD

# quick severity summary
jq -r '[.issues[]?.severity] | group_by(.) | map({(.[0]): length}) | add' "${ISSUES_FILE}" > "${REPORT_DIR}/severity-summary.json" || true
echo -e "\n## Summary (by severity)\n" >> "${MD_FILE}"
jq -r 'to_entries[] | "- "+.key+": "+(.value|tostring)' "${REPORT_DIR}/severity-summary.json" >> "${MD_FILE}" 2>/dev/null || true

# iterate issues
count=0
jq -c '.issues[]' "${ISSUES_FILE}" | while IFS= read -r issue; do
  count=$((count+1))
  if [ "${MAX}" -gt 0 ] && [ "${count}" -gt "${MAX}" ]; then
    echo "Reached MAX (${MAX}) issues, stopping." 
    break
  fi

  rule=$(echo "${issue}" | jq -r '.rule // "-"')
  message=$(echo "${issue}" | jq -r '.message // "-"')
  severity=$(echo "${issue}" | jq -r '.severity // "INFO"')
  component=$(echo "${issue}" | jq -r '.component // "-"')
  filepath=$(echo "${component}" | sed 's/^[^:]*://')
  line=$(echo "${issue}" | jq -r '.line // 1')
  issue_id=$(echo "${issue}" | jq -r '.key // ""')

  # snippet window (default 2 lines around)
  start=$(( line > 2 ? line-2 : 1 ))
  end=$(( line + 2 ))

  snippet="// File not found"
  if [ -f "${filepath}" ]; then
    snippet=$(sed -n "${start},${end}p" "${filepath}" || true)
  fi

  blame="N/A"
  if git rev-parse --is-inside-work-tree >/dev/null 2>&1 && [ -f "${filepath}" ]; then
    blame=$(git blame -L ${line},${line} -- "${filepath}" 2>/dev/null || echo "N/A")
  fi

  github_link="https://github.com/${REPO_ORG}/${REPO_NAME}/blob/${REPO_BRANCH}/${filepath}#L${line}"

  # build a safe compact prompt (avoid weird expansions)
  PROMPT="Analyze this Sonar issue. Provide: 1) Short summary (1-2 lines) 2) Root cause 3) Fix (show OLD CODE and NEW CODE minimal patch) 4) Priority 5) Risk.
Rule: ${rule}
Message: ${message}
Severity: ${severity}
File: ${filepath}
Line: ${line}
Code context:
${snippet}
"

  # prepare payload file (escape JSON safely using jq)
  printf '%s' "${PROMPT}" | jq -Rs '{model:"gpt-4o-mini", messages:[{role:"user", content:.}], max_tokens:700}' > "${REPORT_DIR}/payload-${count}.json

  aiOut="(skipped)"
  if [ -z "${OPENAI_KEY}" ]; then
    echo "OPENAI_KEY not provided; skipping OpenAI call for issue ${count}"
    aiOut="(OPENAI_KEY missing; no AI output)"
  else
    # call OpenAI -- capture HTTP response and errors
    http_resp=$(curl -sS -w "\n%{http_code}" -X POST "https://api.openai.com/v1/chat/completions" \
       -H "Authorization: Bearer ${OPENAI_KEY}" \
       -H "Content-Type: application/json" \
       --data-binary @"${REPORT_DIR}/payload-${count}.json" ) || true

    # split body and code
    http_code=$(echo "${http_resp}" | tail -n1)
    http_body=$(echo "${http_resp}" | sed '$d' || true)

    if [ "${http_code}" = "200" ] || [ "${http_code}" = "201" ]; then
      aiOut=$(echo "${http_body}" | jq -r '.choices[0].message.content // "(no AI response)"' 2>/dev/null || echo "(parse error)")
    else
      echo "OpenAI call failed (HTTP ${http_code}) for issue ${count}; body follows:"
      echo "${http_body}"
      aiOut="(OpenAI call failed HTTP ${http_code})"
      # do not exit on OpenAI failure; record and continue
    fi
  fi

  # append to markdown
  cat >> "${MD_FILE}" <<EOF

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

# final notes
cat >> "${MD_FILE}" <<'MD'

---

## Insights & Next steps
- This report contains AI suggestions where available. If AI calls are skipped, run with OPENAI_KEY configured.
- To speed up runs: reduce MAX_ISSUES_PROCESS env var or run full AI nightly instead of on every commit.
MD

echo "AI stage finished; artifacts: ${MD_FILE} and ${LOG_FILE}"
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
