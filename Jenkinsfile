pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'mvn'
    }

    parameters {
        string(name: 'SONAR_PROJECT_KEY', defaultValue: 'ai-code-assistant', description: 'Sonar project key')
    }

    environment {
        SONAR_HOST = "http://localhost:9000"
        SONAR_TOKEN = credentials('sonar-token')
    }

    stages {

        stage('Checkout') {
            steps {
                echo "[INFO] Checking out source code"
                checkout scm
                sh "ls -lah"
            }
        }

        stage('Build') {
            steps {
                echo "[INFO] Building project with Maven"
                sh "mvn clean package -DskipTests"
                echo "[INFO] Build step completed"
            }
        }

        stage('Sonar Scan') {
            steps {
                echo "[INFO] Running SonarQube analysis"
                sh """
                    mvn sonar:sonar \
                        -Dsonar.login=${SONAR_TOKEN} \
                        -Dsonar.host.url=${SONAR_HOST} \
                        -Dsonar.projectKey=${params.SONAR_PROJECT_KEY} \
                        -Dsonar.sources=src/main/java \
                        -Dsonar.java.binaries=target/classes
                """
                echo "[INFO] Sonar scan triggered successfully"
            }
        }

        stage('Quality Gate') {
            steps {
                script {
                    echo "[INFO] Searching for Sonar report-task.txt"

                    // Verify file exists
                    def report = sh(script: "ls -1 **/report-task.txt 2>/dev/null || true", returnStdout: true).trim()
                    echo "[DEBUG] report-task.txt located at: ${report}"

                    if (!report) {
                        error "[ERROR] report-task.txt not found. Sonar scan may have failed."
                    }

                    // Extract ceTaskId
                    def ceTaskId = sh(
                        script: "grep ceTaskId ${report} | cut -d= -f2",
                        returnStdout: true
                    ).trim()
                    echo "[INFO] ceTaskId extracted: ${ceTaskId}"

                    if (!ceTaskId) {
                        error "[ERROR] ceTaskId not found in report-task.txt"
                    }

                    echo "[INFO] Polling SonarQube CE task status..."

                    def analysisId = ""
                    timeout(time: 5, unit: 'MINUTES') {
                        waitUntil {
                            def ceResponse = sh(
                                script: "curl -s -u ${SONAR_TOKEN}: ${SONAR_HOST}/api/ce/task?id=${ceTaskId}",
                                returnStdout: true
                            ).trim()

                            echo "[DEBUG] CE API response: ${ceResponse}"

                            def status = sh(
                                script: "echo '${ceResponse}' | jq -r '.task.status'",
                                returnStdout: true
                            ).trim()

                            echo "[INFO] CE status = ${status}"

                            if (status == "SUCCESS") {
                                analysisId = sh(
                                    script: "echo '${ceResponse}' | jq -r '.task.analysisId'",
                                    returnStdout: true
                                ).trim()

                                echo "[INFO] analysisId retrieved: ${analysisId}"
                                return true
                            }

                            if (status == "FAILED" || status == "CANCELED") {
                                error "[ERROR] Sonar CE task ended with status: ${status}"
                            }

                            echo "[INFO] CE task still running, retrying in 5 seconds..."
                            sleep 5
                            return false
                        }
                    }

                    echo "[INFO] CE task completed. Fetching Quality Gate result..."

                    def qgResponse = sh(
                        script: "curl -s -u ${SONAR_TOKEN}: ${SONAR_HOST}/api/qualitygates/project_status?analysisId=${analysisId}",
                        returnStdout: true
                    ).trim()

                    echo "[DEBUG] Quality Gate API response: ${qgResponse}"

                    def qgStatus = sh(
                        script: "echo '${qgResponse}' | jq -r '.projectStatus.status'",
                        returnStdout: true
                    ).trim()

                    echo "[INFO] Quality Gate status = ${qgStatus}"

                    if (qgStatus != "OK") {
                        error "[ERROR] Quality Gate failed with status: ${qgStatus}"
                    }

                    echo "[INFO] Quality Gate PASSED"
                }
            }
        }
    }

    post {
        always {
            echo "[INFO] Archiving build artifacts"
            archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true
            echo "[INFO] Post-build cleanup completed"
        }
    }
}
