pipeline {
    agent any

    environment {
        IMAGE = 'codetuscan/calculator'
        TAG = "${env.BUILD_NUMBER}"
        DOCKERHUB = credentials('dockerhub-creds')
        NOTIFY_EMAIL = 'sunhith.reddy@iiitb.ac.in'
    }

    triggers {
        githubPush()
        pollSCM('H/5 * * * *')
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker build -t $IMAGE:$TAG -t $IMAGE:latest .'
            }
        }

        stage('Push') {
            steps {
                sh 'echo "$DOCKERHUB_PSW" | docker login -u "$DOCKERHUB_USR" --password-stdin'
                sh 'docker push $IMAGE:$TAG'
                sh 'docker push $IMAGE:latest'
            }
        }

        stage('Deploy') {
            steps {
                sh 'docker compose up -d'
                sh '''
                    for i in $(seq 1 20); do
                        if curl -sf http://localhost/actuator/health | grep -q UP; then
                            echo "Deployment healthy"
                            exit 0
                        fi
                        sleep 3
                    done
                    echo "Health check failed"
                    docker compose ps
                    exit 1
                '''
            }
        }
    }

    post {
        always {
            sh 'docker logout || true'
        }
        success {
            mail to: env.NOTIFY_EMAIL,
                 subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: "Build succeeded and deployed image ${env.IMAGE}:${env.TAG}.\n${env.BUILD_URL}"
        }
        failure {
            mail to: env.NOTIFY_EMAIL,
                 subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: "Build failed.\n${env.BUILD_URL}console"
        }
    }
}
