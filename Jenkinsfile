pipeline {
    agent any

    environment {
        RABBITMQ_CONTAINER = 'file-storage-rabbitmq-ci'
        RABBITMQ_NETWORK   = 'file-storage-ci-network'
        RABBITMQ_HOST      = 'rabbitmq'
        RABBITMQ_PORT      = '5672'
        RABBITMQ_USER      = 'ciuser'
        RABBITMQ_PASSWORD  = 'cipassword'
    }

    stages {

        stage('Start RabbitMQ') {
            steps {
                sh '''
                    set -e

                    echo "=== Preparing RabbitMQ CI network ==="

                    docker network inspect "$RABBITMQ_NETWORK" >/dev/null 2>&1 || \
                        docker network create "$RABBITMQ_NETWORK"

                    echo "=== Removing old RabbitMQ container if present ==="

                    docker rm -f "$RABBITMQ_CONTAINER" >/dev/null 2>&1 || true

                    echo "=== Starting RabbitMQ ==="

                    docker run -d \
                        --name "$RABBITMQ_CONTAINER" \
                        --network "$RABBITMQ_NETWORK" \
                        --network-alias rabbitmq \
                        -e RABBITMQ_DEFAULT_USER="$RABBITMQ_USER" \
                        -e RABBITMQ_DEFAULT_PASS="$RABBITMQ_PASSWORD" \
                        rabbitmq:3-management

                    echo "=== Waiting for RabbitMQ ==="

                    for i in $(seq 1 30); do
                        if docker exec "$RABBITMQ_CONTAINER" \
                            rabbitmq-diagnostics -q ping >/dev/null 2>&1; then

                            echo "RabbitMQ is ready."
                            break
                        fi

                        echo "Waiting for RabbitMQ... ($i/30)"
                        sleep 2
                    done

                    docker exec "$RABBITMQ_CONTAINER" \
                        rabbitmq-diagnostics -q ping
                '''
            }
        }

        stage('Verify RabbitMQ Connection') {
            steps {
                sh '''
                    set -e

                    echo "=== Resolving RabbitMQ ==="

                    docker exec jenkins getent hosts "$RABBITMQ_HOST"

                    echo "=== Testing RabbitMQ TCP connection ==="

                    docker exec jenkins bash -c \
                        'timeout 5 bash -c "</dev/tcp/rabbitmq/5672"'

                    echo "RabbitMQ TCP connection OK."
                '''
            }
        }

        stage('Build') {
            steps {
                sh './mvnw clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh '''
                    SPRING_RABBITMQ_HOST="$RABBITMQ_HOST" \
                    SPRING_RABBITMQ_PORT="$RABBITMQ_PORT" \
                    SPRING_RABBITMQ_USERNAME="$RABBITMQ_USER" \
                    SPRING_RABBITMQ_PASSWORD="$RABBITMQ_PASSWORD" \
                    ./mvnw test
                '''
            }
        }

        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
    }

    post {
        always {
            sh '''
                echo "=== Cleaning up RabbitMQ ==="
                docker rm -f "$RABBITMQ_CONTAINER" >/dev/null 2>&1 || true
            '''
        }
    }
}
