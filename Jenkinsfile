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
                    ./mvnw clean verify
                '''
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

        stage('Update Kustomize Image') {
            steps {
                sh '''
                    set -eux
                    echo "📝 Updating image in kustomization..."
                    kustomize edit set image ${IMAGE}=${IMAGE}:${TAG}
                '''
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh '''
                    set -eux

                    echo "📦 Applying Kustomize..."
                    kubectl apply -k .

                    echo "🧹 Cleanup old pods (fix PVC ReadWriteOnce)..."
                    kubectl delete pod -l app=stage-service -n ${NAMESPACE} --ignore-not-found=true

                    echo "⏳ Waiting for rollout to complete..."
                    kubectl rollout status deployment/stage-deployment \
                        -n ${NAMESPACE} \
                        --timeout=5m
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
                echo "📦 Pods:"
                kubectl get pods -n ${NAMESPACE} || true

                echo "📄 Describe:"
                kubectl describe pods -n ${NAMESPACE} || true

                echo "📜 Logs:"
                kubectl logs -l app=stage-service -n ${NAMESPACE} --tail=100 || true

                echo "📢 Events:"
                kubectl get events -n ${NAMESPACE} || true
            '''
        }

        always {
            cleanWs()
        }
    }
}