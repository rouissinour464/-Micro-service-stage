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

    environment {
        REGISTRY   = "nour292"
        IMAGE      = "${REGISTRY}/stage-service"
        TAG        = "${BUILD_NUMBER}"
        KUBECONFIG = "/var/lib/jenkins/.kube/config"
        NAMESPACE  = "gestion-projet"

        GIT_CREDENTIALS_ID = "github-creds"
        GIT_USER_EMAIL     = "jenkins@ci.local"
        GIT_USER_NAME      = "Jenkins CI"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build + Test') {
            steps {
                sh '''
                    set -eux
                    chmod +x mvnw
                    ./mvnw verify
                '''
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    set -eux
                    docker build -t ${IMAGE}:${TAG} .
                    docker tag ${IMAGE}:${TAG} ${IMAGE}:latest
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
                        docker push ${IMAGE}:latest
                        docker logout

                        echo "🧹 Cleanup images locales..."
                        docker rmi ${IMAGE}:${TAG} ${IMAGE}:latest || true
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
                    if [ -n "$NOT_READY" ]; then
                        echo "❌ Some nodes NOT READY"
                        exit 1
                    fi

                    echo "✅ ALL NODES READY"
                '''
            }
        }

        stage('Update Kustomize Image') {
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

                        git checkout -B v2

                        echo "📝 Updating image in kustomization.yaml..."
                        sed -i "s|newTag:.*|newTag: \\"${TAG}\\"|g" k8s/app/kustomization.yaml

                        git add k8s/app/kustomization.yaml
                        git diff --cached --quiet && echo "⏭️ Pas de changement — skip commit" && exit 0

                        git commit -m "ci: update stage-service image tag to ${TAG} [skip ci]"

                        REMOTE=$(git remote get-url origin \
                            | sed "s|https://|https://${GIT_USER}:${GIT_TOKEN}@|")
                        git push "$REMOTE" HEAD:v2

                        echo "✅ Tag ${TAG} pushé sur branche v2"
                    '''
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh '''
                    set -eux

                    echo "📂 Contenu de k8s/app :"
                    ls -la k8s/app/

                    echo "🔍 Rendu Kustomize :"
                    kubectl kustomize k8s/app

                    echo "🏗️ Namespace..."
                    kubectl create namespace ${NAMESPACE} \
                        --dry-run=client -o yaml | kubectl apply -f -

                    echo "📦 Applying Kustomize..."
                    kubectl apply -k k8s/app

                    echo "⏳ Waiting rollout..."
                    kubectl rollout status deployment/stage-deployment \
                        -n ${NAMESPACE} \
                        --timeout=120s

                    echo "🔄 Restart forcé pour prendre la nouvelle image..."
                    kubectl rollout restart deployment/stage-deployment \
                        -n ${NAMESPACE}

                    kubectl rollout status deployment/stage-deployment \
                        -n ${NAMESPACE} --timeout=120s

                    echo "✅ stage-service déployé"
                '''
            }
        }

        stage('ArgoCD Sync') {
            steps {
                sh '''
                    set -eux

                    echo "📋 Apply ArgoCD Application..."
                    kubectl apply -f k8s/argocd/ -n argocd

                    echo "🔄 Refresh ArgoCD repo server..."
                    kubectl rollout restart deployment argocd-repo-server -n argocd
                    kubectl rollout status deployment argocd-repo-server \
                        -n argocd --timeout=60s

                    echo "🔁 Force Sync ArgoCD..."
                    argocd app sync stage-service --grpc-web || true

                    echo "⏳ Attente sync + health..."
                    argocd app wait stage-service \
                        --sync --health --timeout 240 --grpc-web || true

                    echo "📊 Status ArgoCD..."
                    argocd app get stage-service --grpc-web || true
                '''
            }
        }

        stage('Check Cluster') {
            steps {
                sh '''
                    echo "📦 Pods:"
                    kubectl get pods -n ${NAMESPACE} -o wide

                    echo "🌐 Services:"
                    kubectl get svc -n ${NAMESPACE}

                    echo "🚀 Deployments:"
                    kubectl get deployments -n ${NAMESPACE}
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
                echo "=== Pods ==="
                kubectl get pods -n ${NAMESPACE} || true

                echo "=== Describe Pods ==="
                kubectl describe pods -n ${NAMESPACE} || true

                echo "=== Logs stage-service ==="
                kubectl logs -l app=stage-service \
                    -n ${NAMESPACE} --tail=50 || true

                echo "=== Events ==="
                kubectl get events -n ${NAMESPACE} \
                    --sort-by='.lastTimestamp' || true

                echo "=== ArgoCD status ==="
                argocd app get stage-service --grpc-web || true
            '''
        }

        always {
            cleanWs()
        }
    }
}