pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'mvn'
    }

    environment {
        SONAR_HOST = "http://localhost:9000"
        SONAR_TOKEN = credentials('sonar-token')
    }

    stages {

        stage('Checkout') {
            steps {
                echo "========== Checking out code =========="
                checkout scm
                sh "ls -lah"
            }
        }

        stage('Build') {
            steps {
                echo "========== Building application =========="
                sh "mvn clean package -DskipTests"
            }
        }

        stage('Sonar Scan') {
            steps {
                echo "========== Running SonarQube Scan =========="
                sh """
                    mvn sonar:sonar \
                        -Dsonar.login=${SONAR_TOKEN} \
                        -Dsonar.host.url=${SONAR_HOST} \
                        -Dsonar.projectKey=ai-code-assistant \
                        -Dsonar.projectName="AI Code Assistant" \
                        -Dsonar.sources=src/main/java \
                        -Dsonar.java.binaries=target/classes
                """
            }
        }

        stage('Quality Gate') {
            steps {
                script {
                    echo "========== Checking CE Task Status =========="

                    // Extract ceTaskId from sonar-report.txt
                    def ceTaskId = sh(
                        script: "grep -o 'ceTaskId=[A-Za-z0-9_-]*' **/report-task.txt | cut -d= -f2",
                        returnStdout: true
                    ).trim()

                    echo "📌 CE Task ID = ${ceTaskId}"

                    // Poll CE API until SUCCESS
                    def analysisId = ""
                    timeout(time: 2, unit: 'MINUTES') {
                        waitUntil {
                            def ce = sh(
                                script: "curl -s -u ${SONAR_TOKEN}: ${SONAR_HOST}/api/ce/task?id=${ceTaskId}",
                                returnStdout: true
                            ).trim()

                            echo "CE Response: ${ce}"

                            def status = sh(
                                script: "echo '${ce}' | jq -r '.task.status'",
                                returnStdout: true
                            ).trim()

                            echo "CE Status = ${status}"

                            if (status == "SUCCESS") {
                                analysisId = sh(
                                    script: "echo '${ce}' | jq -r '.task.analysisId'",
                                    returnStdout: true
                                ).trim()
                                echo "📌 Analysis ID = ${analysisId}"
                                return true
                            }
                            return false
                        }
                    }

                    echo "========== Fetching Quality Gate =========="

                    // Retrieve Quality Gate Status
                    def qg = sh(
                        script: "curl -s -u ${SONAR_TOKEN}: ${SONAR_HOST}/api/qualitygates/project_status?analysisId=${analysisId}",
                        returnStdout: true
                    ).trim()

                    echo "Quality Gate Response: ${qg}"

                    def qgStatus = sh(
                        script: "echo '${qg}' | jq -r '.projectStatus.status'",
                        returnStdout: true
                    ).trim()

                    echo "📌 Quality Gate Status = ${qgStatus}"

                    if (qgStatus != "OK") {
                        error "❌ Quality Gate FAILED: ${qgStatus}"
                    } else {
                        echo "✅ Quality Gate PASSED"
                    }
                }
            }
        }
    }

    post {
        always {
            echo "========== Archiving artifacts =========="
            archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true
        }
        success {
            echo "🎉 BUILD SUCCESS"
        }
        failure {
            echo "❌ BUILD FAILED"
        }
    }
}
