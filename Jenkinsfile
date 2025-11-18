pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'mvn'
    }

    environment {
        SONAR_HOST = "http://localhost:9000"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                sh "ls -lah"
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Sonar Scan') {
            steps {
                withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
                    echo "[INFO] Using Sonar Token (length=${SONAR_TOKEN.length()})"

                    sh """
                        mvn sonar:sonar \
                            -Dsonar.login=${SONAR_TOKEN} \
                            -Dsonar.host.url=${SONAR_HOST} \
                            -Dsonar.projectKey=ai-code-assistant \
                            -Dsonar.sources=src/main/java \
                            -Dsonar.java.binaries=target/classes
                    """
                }
            }
        }

        stage('Quality Gate') {
            steps {
                script {

                    withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {

                        echo "[INFO] Reading CE Task ID"
                        def ceTaskId = sh(
                            script: "grep ceTaskId **/report-task.txt | cut -d= -f2",
                            returnStdout: true
                        ).trim()

                        echo "[INFO] CE Task ID = ${ceTaskId}"

                        def analysisId = ""
                        waitUntil {
                            def ceJson = sh(
                                script: "curl -s -u ${SONAR_TOKEN}: ${SONAR_HOST}/api/ce/task?id=${ceTaskId}",
                                returnStdout: true
                            ).trim()

                            def status = sh(
                                script: "echo '${ceJson}' | jq -r '.task.status'",
                                returnStdout: true
                            ).trim()

                            echo "[INFO] CE Status: ${status}"

                            if (status == 'SUCCESS') {
                                analysisId = sh(
                                    script: "echo '${ceJson}' | jq -r '.task.analysisId'",
                                    returnStdout: true
                                ).trim()
                                return true
                            }
                            return false
                        }

                        echo "[INFO] Analysis ID = ${analysisId}"

                        def qgJson = sh(
                            script: "curl -s -u ${SONAR_TOKEN}: ${SONAR_HOST}/api/qualitygates/project_status?analysisId=${analysisId}",
                            returnStdout: true
                        ).trim()

                        def qgStatus = sh(
                            script: "echo '${qgJson}' | jq -r '.projectStatus.status'",
                            returnStdout: true
                        ).trim()

                        echo "[INFO] Quality Gate Status = ${qgStatus}"

                        if (qgStatus != "OK") {
                            error "❌ Quality Gate FAILED (${qgStatus})"
                        } else {
                            echo "✅ Quality Gate PASSED"
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            echo "[INFO] Archiving artifacts"
            archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true
        }
    }
}
