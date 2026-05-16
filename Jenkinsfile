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

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -eux
                    chmod +x mvnw
                    ./mvnw clean compile
                '''
            }
        }

        stage('Unit Tests') {
            steps {
                sh '''
                    set -eux
                    ./mvnw test
                '''
            }
        }

        stage('Integration Tests') {
            steps {
                sh '''
                    set -eux
                    ./mvnw verify
                '''
            }
        }

        stage('Package') {
            steps {
                sh '''
                    set -eux
                    ./mvnw clean package -DskipTests
                '''
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    set -eux
                    docker build -t ${IMAGE}:${TAG} .
                '''
            }
        }

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

        stage('Deploy to K3s') {
            steps {
                sh '''
                    set -eux
                    kubectl apply -k k8s/app
                    kubectl get all -n ${NAMESPACE}
                '''
            }
        }

        stage('Restart Stage Service') {
            steps {
                sh '''
                    set -eux

                    kubectl rollout restart deployment stage-deployment -n ${NAMESPACE}

                    # ✅ CORRECTION : 660s > 600s (fenêtre max startupProbe)
                    kubectl rollout status deployment stage-deployment \
                        -n ${NAMESPACE} --timeout=660s
                '''
            }
        }

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
            // ✅ AJOUT : logs de debug automatiques en cas d'échec
            sh '''
                echo "=== DESCRIBE POD ==="
                kubectl describe pod -l app=stage-service \
                    -n gestion-projet || true
                echo "=== LOGS DU CONTENEUR ==="
                kubectl logs -l app=stage-service \
                    -n gestion-projet --tail=80 || true
                echo "=== ETAT DES PVC ==="
                kubectl get pvc -n gestion-projet || true
            '''
        }

        always {
            cleanWs()
        }
    }
}