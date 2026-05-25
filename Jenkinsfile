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
        SONAR_ORG         = "rouissinour464"

        GIT_CREDENTIALS_ID = "github-creds"
        GIT_USER_EMAIL     = "jenkins@ci.local"
        GIT_USER_NAME      = "Jenkins CI"
    }

    stages {

        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Build + Test + Sonar') {
            steps {
                withSonarQubeEnv('SonarCloud') {
                    withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                        sh '''
                            set -eux
                            chmod +x mvnw
                            ./mvnw clean verify sonar:sonar \
                              -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                              -Dsonar.organization=${SONAR_ORG} \
                              -Dsonar.host.url=https://sonarcloud.io \
                              -Dsonar.token=${SONAR_TOKEN}
                        '''
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: false
                }
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
                withCredentials([string(credentialsId: 'dockerhub-pass', variable: 'DOCKER_PASSWORD')]) {
                    sh '''
                        set -eux
                        echo "$DOCKER_PASSWORD" | docker login -u ${REGISTRY} --password-stdin
                        docker push ${IMAGE}:${TAG}
                        docker tag  ${IMAGE}:${TAG} ${IMAGE}:latest
                        docker push ${IMAGE}:latest
                        docker logout
                    '''
                }
            }
        }

        stage('Check Cluster Nodes') {
            steps {
                sh '''
                    set -eux
                    kubectl get nodes
                    NOT_READY=$(kubectl get nodes --no-headers | grep -v " Ready" || true)
                    if [ ! -z "$NOT_READY" ]; then
                        echo "❌ Some nodes NOT READY"
                        exit 1
                    fi
                    echo "✅ ALL NODES READY"
                '''
            }
        }

        stage('Update Image Tag') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: "${GIT_CREDENTIALS_ID}",
                    usernameVariable: 'GIT_USER',
                    passwordVariable: 'GIT_TOKEN'
                )]) {
                    sh '''
                        set -eux
                        git config user.email "${GIT_USER_EMAIL}"
                        git config user.name  "${GIT_USER_NAME}"

                        git checkout -B v1

                        sed -i "s|newTag:.*|newTag: \\"${TAG}\\"|g" k8s/app/kustomization.yaml

                        git add k8s/app/kustomization.yaml
                        git commit -m "ci: update stage-service image tag to ${TAG} [skip ci]"

                        REMOTE=$(git remote get-url origin \
                            | sed "s|https://|https://${GIT_USER}:${GIT_TOKEN}@|")
                        git push "$REMOTE" HEAD:v1
                    '''
                }
            }
        }

        stage('Wait ArgoCD Sync') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    sh '''
                        set -eux
                        argocd app wait stage-service \
                            --sync --health --timeout 240 --grpc-web || true
                        argocd app get stage-service --grpc-web || true
                    '''
                }
            }
        }

        stage('Check Cluster') {
            steps {
                sh '''
                    kubectl get pods -n ${NAMESPACE}
                    kubectl get svc -n ${NAMESPACE}
                    kubectl get applications -n argocd || true
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
                kubectl get pods -n ${NAMESPACE} || true
                kubectl describe pods -n ${NAMESPACE} || true
                kubectl logs -l app=stage-service -n ${NAMESPACE} --tail=80 || true
                kubectl get events -n ${NAMESPACE} || true
                argocd app get stage-service --grpc-web || true
            '''
        }
        always {
            cleanWs()
        }
    }
}