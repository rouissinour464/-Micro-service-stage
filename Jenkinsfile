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
        REGISTRY           = "nour292"
        IMAGE              = "${REGISTRY}/stage-service"
        TAG                = "${BUILD_NUMBER}"
        KUBECONFIG         = "/var/lib/jenkins/.kube/config"
        NAMESPACE          = "gestion-projet"
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

        // ─────────────────────────────────────────
        // Tests unitaires
        // ─────────────────────────────────────────
        stage('Unit Tests') {
            steps {
                sh '''
                    set -eux
                    chmod +x mvnw
                    ./mvnw test
                '''
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        // ─────────────────────────────────────────
        // Tests d'intégration
        // ─────────────────────────────────────────
        stage('Integration Tests') {
            steps {
                sh '''
                    set -eux
                    ./mvnw verify -DskipUnitTests
                '''
            }
        }

        // ─────────────────────────────────────────
        // Docker Build & Push
        // ─────────────────────────────────────────
        stage('Docker Build & Push') {
            steps {
                withCredentials([string(
                    credentialsId: 'dockerhub-pass',
                    variable: 'DOCKER_PASSWORD'
                )]) {
                    sh '''
                        set -eux
                        docker build -t ${IMAGE}:${TAG} .
                        docker tag  ${IMAGE}:${TAG} ${IMAGE}:latest

                        echo "$DOCKER_PASSWORD" | \
                            docker login -u ${REGISTRY} --password-stdin
                        docker push ${IMAGE}:${TAG}
                        docker push ${IMAGE}:latest
                        docker logout

                        docker rmi ${IMAGE}:${TAG} ${IMAGE}:latest || true
                    '''
                }
            }
        }

        // ─────────────────────────────────────────
        // Vérification cluster
        // ─────────────────────────────────────────
        stage('Check Cluster') {
            steps {
                sh '''
                    set -eux
                    kubectl get nodes

                    NOT_READY=$(kubectl get nodes \
                        --no-headers | grep -v " Ready" || true)
                    if [ -n "$NOT_READY" ]; then
                        echo "Nodes NOT READY"
                        exit 1
                    fi
                    echo "ALL NODES READY"
                '''
            }
        }

        // ─────────────────────────────────────────
        // Mettre à jour le tag dans Git
        // ─────────────────────────────────────────
        stage('Update Git Tag') {
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

                        git checkout -B main

                        sed -i "s|newTag:.*|newTag: \\"${TAG}\\"|g" \
                            k8s/app/kustomization.yaml

                        git add k8s/app/kustomization.yaml
                        git diff --cached --quiet && \
                            echo "Pas de changement — skip" && exit 0

                        git commit -m "ci: stage-service → ${TAG} [skip ci]"

                        REMOTE=$(git remote get-url origin \
                            | sed "s|https://|https://${GIT_USER}:${GIT_TOKEN}@|")
                        git push "$REMOTE" HEAD:main --force-with-lease
                    '''
                }
            }
        }

        // ─────────────────────────────────────────
        // Deploy direct kubectl
        // ─────────────────────────────────────────
        stage('Deploy to Kubernetes') {
            steps {
                sh '''
                    set -eux

                    echo "Namespace..."
                    kubectl create namespace ${NAMESPACE} \
                        --dry-run=client -o yaml | kubectl apply -f -

                    echo "Contenu k8s/app :"
                    ls -la k8s/app/

                    echo "Manifestes Kustomize :"
                    kubectl kustomize k8s/app

                    echo "Application via Kustomize..."
                    kubectl apply -k k8s/app

                    echo "Cleanup pods PVC..."
                    kubectl delete pod \
                        -l app=stage-service \
                        -n ${NAMESPACE} \
                        --ignore-not-found=true

                    echo "Attente rollout..."
                    kubectl rollout status deployment/stage-deployment \
                        -n ${NAMESPACE} --timeout=5m

                    echo "Deploy stage-service termine"
                '''
            }
        }

        // ─────────────────────────────────────────
        // Vérification finale
        // ─────────────────────────────────────────
        stage('Check Final') {
            steps {
                sh '''
                    echo "=== Pods ==="
                    kubectl get pods -n ${NAMESPACE} -o wide

                    echo "=== Services ==="
                    kubectl get svc -n ${NAMESPACE}

                    echo "=== Deployment ==="
                    kubectl get deployment stage-deployment \
                        -n ${NAMESPACE}
                '''
            }
        }
    }

    post {
        success {
            echo "SUCCES — stage-service:${TAG} deploye"
        }

        failure {
            sh '''
                echo "=== Pods ==="
                kubectl get pods -n ${NAMESPACE} || true

                echo "=== Describe ==="
                kubectl describe pods \
                    -l app=stage-service \
                    -n ${NAMESPACE} || true

                echo "=== Logs ==="
                kubectl logs \
                    -l app=stage-service \
                    -n ${NAMESPACE} \
                    --tail=100 || true

                echo "=== Events ==="
                kubectl get events -n ${NAMESPACE} \
                    --sort-by=.lastTimestamp | tail -20 || true
            '''
        }

        always {
            cleanWs()
        }
    }
}