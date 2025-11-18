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

        stage('Quality Gate') {
            steps {
                script {
                    // Read CE task id from Sonar's report-task.txt
                    def ceTaskId = sh(
                        script: "grep ceTaskId **/report-task.txt | cut -d= -f2",
                        returnStdout: true
                    ).trim()

                    // Poll CE task until it finishes
                    def analysisId = ""
                    waitUntil {
                        def ceResponse = sh(
                            script: "curl -s -u ${SONAR_TOKEN}: ${SONAR_HOST}/api/ce/task?id=${ceTaskId}",
                            returnStdout: true
                        ).trim()

                        def status = sh(
                            script: "echo '${ceResponse}' | jq -r '.task.status'",
                            returnStdout: true
                        ).trim()

                        if (status == "SUCCESS") {
                            analysisId = sh(
                                script: "echo '${ceResponse}' | jq -r '.task.analysisId'",
                                returnStdout: true
                            ).trim()
                            return true
                        }
                        return false
                    }

                    // Get Quality Gate result
                    def qgResponse = sh(
                        script: "curl -s -u ${SONAR_TOKEN}: ${SONAR_HOST}/api/qualitygates/project_status?analysisId=${analysisId}",
                        returnStdout: true
                    ).trim()

                    def qgStatus = sh(
                        script: "echo '${qgResponse}' | jq -r '.projectStatus.status'",
                        returnStdout: true
                    ).trim()

                    if (qgStatus != "OK") {
                        error "Quality Gate failed: ${qgStatus}"
                    }
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true
        }
    }
}
