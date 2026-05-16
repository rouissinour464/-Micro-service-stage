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
                    ./mvnw test -Dspring.profiles.active=test
                '''
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: '**/target/surefire-reports/*.xml'
                }
                failure {
                    echo "❌ Unit Tests échoués — voir les rapports Surefire"
                }
            }
        }

        /* =======================
           INTEGRATION TESTS
        ======================= */
        stage('Integration Tests') {
            steps {
                sh '''
                    set -eux
                    ./mvnw verify -Dspring.profiles.active=test
                '''
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: '**/target/failsafe-reports/*.xml'
                }
                failure {
                    echo "❌ Integration Tests échoués"
                }
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
           PREPARE NODE
           Crée le dossier uploads sur le nœud
           avec les bonnes permissions
        ======================= */
        stage('Prepare Node') {
            steps {
                sh '''
                    set -eux
                    # ✅ Crée le dossier hostPath avant le déploiement
                    # pour éviter AccessDeniedException au démarrage
                    sudo mkdir -p /data/uploads/stage
                    sudo chmod 777 /data/uploads/stage
                '''
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

                    # ✅ 5min au lieu de 3min
                    # Spring Boot + Neon DB (cloud) = démarrage lent
                    kubectl rollout status deployment stage-deployment \
                        -n ${NAMESPACE} --timeout=300s
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

                    # ✅ Afficher les logs du pod pour confirmer démarrage OK
                    POD=$(kubectl get pod -n ${NAMESPACE} \
                          -l app=stage-service \
                          -o jsonpath="{.items[0].metadata.name}")

                    echo "=== Logs du pod : $POD ==="
                    kubectl logs -n ${NAMESPACE} ${POD} --tail=50
                '''
            }
        }
    }

    post {
        success {
            echo "✅ STAGE-SERVICE PIPELINE SUCCESS 🎉"
        }
        failure {
            // ✅ Affiche les logs du pod en cas d'échec
            sh '''
                echo "=== Diagnostic pod en échec ==="
                kubectl get pods -n gestion-projet -l app=stage-service || true
                POD=$(kubectl get pod -n gestion-projet \
                      -l app=stage-service \
                      -o jsonpath="{.items[0].metadata.name}" 2>/dev/null || echo "")
                if [ -n "$POD" ]; then
                    kubectl logs -n gestion-projet ${POD} --tail=80 || true
                    kubectl logs -n gestion-projet ${POD} --previous --tail=80 || true
                    kubectl describe pod -n gestion-projet ${POD} | tail -30 || true
                fi
            '''
            echo "❌ STAGE-SERVICE PIPELINE FAILED ❌"
        }
        always {
            cleanWs()
        }
    }
}