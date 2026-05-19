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
        TAG        = "${BUILD_NUMBER}"
        KUBECONFIG = "/var/lib/jenkins/.kube/config"
        NAMESPACE  = "gestion-projet"

        SONAR_PROJECT_KEY = "rouissinour464_micro-service-stage"
        SONAR_ORG = "rouissinour464"
    }

    stages {

        /* ======================= */
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        /* ✅ TESTS UNIQUEMENT */
        stage('Tests') {
            steps {
                sh '''
                    set -eux
                    chmod +x mvnw
                    ./mvnw test
                '''
            }
        }

        /* ✅ SONAR */
        stage('SonarCloud') {
            steps {
                withSonarQubeEnv('SonarCloud') {
                    withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                        sh '''
                            set -eux

                            ./mvnw sonar:sonar \
                              -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                              -Dsonar.organization=${SONAR_ORG} \
                              -Dsonar.host.url=https://sonarcloud.io \
                              -Dsonar.token=${SONAR_TOKEN}
                        '''
                    }
                }
            }
        }

        /* ✅ QUALITY */
        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        /* ✅ DOCKER BUILD (REAL BUILD) */
        stage('Docker Build & Push') {
            steps {
                withCredentials([string(credentialsId: 'dockerhub-pass', variable: 'DOCKER_PASSWORD')]) {
                    sh '''
                        set -eux

                        echo "$DOCKER_PASSWORD" | docker login -u ${REGISTRY} --password-stdin

                        docker build -t ${IMAGE}:${TAG} .
                        docker tag ${IMAGE}:${TAG} ${IMAGE}:latest

                        docker push ${IMAGE}:${TAG}
                        docker push ${IMAGE}:latest

                        docker logout
                    '''
                }
            }
        }

        /* ✅ CHECK NODES */
        stage('Check Cluster Nodes') {
            steps {
                sh '''
                    set -eux

                    kubectl get nodes
                '''
            }
        }

        /* ✅ DEPLOY */
        stage('Deploy to K3s') {
            steps {
                sh '''
                    set -eux
                    kubectl apply -k k8s/app
                '''
            }
        }

        /* ✅ RESTART */
        stage('Restart Stage Service') {
            steps {
                sh '''
                    set -eux

                    kubectl rollout restart deployment stage-deployment -n ${NAMESPACE}

                    kubectl rollout status deployment stage-deployment \
                        -n ${NAMESPACE} --timeout=660s
                '''
            }
        }

        /* ✅ CHECK */
        stage('Check Cluster') {
            steps {
                sh '''
                    kubectl get pods -n ${NAMESPACE}
                '''
            }
        }
    }

    post {
        success {
            echo "✅ STAGE PIPELINE SUCCESS 🚀"
        }

        failure {
            echo "❌ PIPELINE FAILED"

            sh '''
                kubectl describe pods -n ${NAMESPACE} || true
                kubectl logs -l app=stage-service -n ${NAMESPACE} --tail=80 || true
            '''
        }

        always {
            cleanWs()
        }
    }
}
