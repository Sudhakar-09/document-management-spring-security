pipeline {

    agent any

    tools {
        jdk 'JDK17'              // MUST match Global Tool Configuration name
        maven 'Maven-3.8'        // MUST match Maven name in Jenkins
    }

    environment {
        SONARQUBE = credentials('SONAR_TOKEN') // Optional if using Jenkins credential
        SCANNER_HOME = tool 'SonarScanner'     // Must match tool name in Jenkins
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build (Java 17)') {
            steps {
                echo "Using Java version"
                sh "java -version"

                echo "Running Maven Build"
                sh "mvn clean package -DskipTests"
            }
        }

        stage('SonarQube Scan') {
            steps {
                withSonarQubeEnv('MySonar') {   // Must match SonarQube server name
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
            archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
        }

        success {
            echo "Build success!"
        }

        failure {
            echo "Pipeline failed!"
        }
    }
}
