pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'mvn'
    }

    environment {
        SCANNER_HOME = tool 'SonarScanner'
        SONARQUBE = credentials('SONAR_TOKEN')
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build (Java 17)') {
            steps {
                sh "java -version"
                sh "mvn clean package -DskipTests"
            }
        }

        stage('SonarQube Scan') {
            steps {
                withSonarQubeEnv('MySonar') {
                    sh """
                        ${SCANNER_HOME}/bin/sonar-scanner \
                        -Dproject.settings=sonar-project.properties
                    """
                }
            }
        }

        stage("Quality Gate") {
            steps {
                script {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
    }

    post {
        always {
            echo "Archiving JAR files…"
            archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
        }

        success {
            echo "Pipeline success!"
        }

        failure {
            echo "Pipeline failed!"
        }
    }
}
