pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'mvn'
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
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Sonar Scan') {
            steps {
                echo "========== Running SonarQube scan =========="
                withSonarQubeEnv('MySonar') {
                    sh '''
                        mvn sonar:sonar \
                            -Dsonar.projectKey=ai-code-assistant \
                            -Dsonar.projectName="AI Code Assistant" \
                            -Dsonar.sources=src/main/java \
                            -Dsonar.java.binaries=target/classes \
                            -Dsonar.exclusions=**/config/**,**/dto/**
                    '''
                }
                echo "✅ SonarQube scan completed"
            }
        }

        stage('Quality Gate') {
            steps {
                echo "========== Waiting for Quality Gate =========="
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        def qg = waitForQualityGate()
                        echo "Quality Gate status: ${qg.status}"

                        if (qg.status == 'OK') {
                            echo "✅ Quality Gate PASSED"
                        } else if (qg.status == 'WARN') {
                            echo "⚠️ Quality Gate WARNING"
                        } else {
                            echo "❌ Quality Gate FAILED"
                            error "Pipeline aborted: Quality Gate failed with status ${qg.status}"
                        }
                    }
                }
            }
        }
    }

    post {

        always {
            echo "========== Archiving artifacts =========="
            archiveArtifacts artifacts: 'target/*.jar',
                             fingerprint: true,
                             allowEmptyArchive: true
        }

        success {
            echo "✅ BUILD SUCCESSFUL"
        }

        failure {
            echo "❌ BUILD FAILED"
        }
    }
}
