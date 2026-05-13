pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
        timestamps()
    }

    triggers {
        githubPush()
        cron('H */6 * * *')
    }

    tools {
        jdk 'JDK21'
    }

    environment {
        REGISTRY   = "nour292"
        IMAGE      = "${REGISTRY}/stage-service"
        TAG        = "latest"
        KUBECONFIG = "/var/lib/jenkins/.kube/config"
        NAMESPACE  = "gestion-projet"
    }

    stages {

        /* =======================
           SOURCE CODE
        ======================= */
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        /* =======================
           BUILD
        ======================= */
        stage('Build') {
            steps {
                sh '''
                    set -eux

                    chmod +x mvnw
                    ./mvnw clean compile
                '''
            }
        }

        /* =======================
           UNIT TESTS
        ======================= */
        stage('Unit Tests') {
            steps {
                sh '''
                    set -eux

                    ./mvnw test
                '''
            }
        }

        /* =======================
           INTEGRATION TESTS
        ======================= */
        stage('Integration Tests') {
            steps {
                sh '''
                    set -eux

                    ./mvnw verify
                '''
            }
        }

        /* =======================
           PACKAGE JAR
        ======================= */
        stage('Package') {
            steps {
                sh '''
                    set -eux

                    ./mvnw clean package -DskipTests
                '''
            }
        }

        /* =======================
           DOCKER BUILD
        ======================= */
        stage('Docker Build') {
            steps {
                sh '''
                    set -eux

                    docker build -t ${IMAGE}:${TAG} .
                '''
            }
        }

        /* =======================
           DOCKER PUSH
        ======================= */
        stage('Docker Push') {
            steps {
                withCredentials([
                    string(credentialsId: 'dockerhub-pass', variable: 'DOCKER_PASSWORD')
                ]) {
                    sh '''
                        set -eux

                        echo "$DOCKER_PASSWORD" | docker login -u ${REGISTRY} --password-stdin

                        docker push ${IMAGE}:${TAG}

                        docker logout
                    '''
                }
            }
        }

        /* =======================
           DEPLOY TO K3S
        ======================= */
        stage('Deploy to K3s') {
            steps {
                sh '''
                    set -eux

                    kubectl apply -k k8s/app

                    kubectl get all -n ${NAMESPACE}
                '''
            }
        }

        /* =======================
           RESTART STAGE SERVICE
        ======================= */
        stage('Restart Stage Service') {
            steps {
                sh '''
                    set -eux

                    kubectl rollout restart deployment stage-deployment -n ${NAMESPACE}

                    kubectl rollout status deployment stage-deployment -n ${NAMESPACE} --timeout=180s
                '''
            }
        }

        /* =======================
           CHECK PODS
        ======================= */
        stage('Check Pods') {
            steps {
                sh '''
                    set -eux

                    kubectl get pods -n ${NAMESPACE}

                    kubectl get pvc -n ${NAMESPACE}

                    kubectl get pv
                '''
            }
        }
    }

    post {

        success {
            echo "✅ STAGE-SERVICE PIPELINE SUCCESS 🎉"
        }

        failure {
            echo "❌ STAGE-SERVICE PIPELINE FAILED ❌"
        }

        always {
            cleanWs()
        }
    }
}