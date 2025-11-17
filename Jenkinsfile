pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'mvn'
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
                withSonarQubeEnv('MySonar') {
                    sh """
                        /opt/sonar-scanner/bin/sonar-scanner \
                        -Dproject.settings=sonar-project.properties
                    """
                }
            }
        }

        stage('Quality Gate') {
            steps {
                script {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
        }
    }
}
