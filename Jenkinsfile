pipeline {
    agent any

    environment {
        RABBITMQ_CONTAINER = 'file-storage-rabbitmq-ci'
        RABBITMQ_VOLUME    = 'file-storage-rabbitmq-ci-volume'
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

                    echo "=== Removing old RabbitMQ container ==="

                    docker rm -f "$RABBITMQ_CONTAINER" >/dev/null 2>&1 || true

                    echo "=== Removing old RabbitMQ volume ==="

                    docker volume rm "$RABBITMQ_VOLUME" >/dev/null 2>&1 || true

                    echo "=== Creating fresh RabbitMQ volume ==="

                    docker volume create "$RABBITMQ_VOLUME"

                    echo "=== Starting RabbitMQ ==="

                    docker run -d \
                        --name "$RABBITMQ_CONTAINER" \
                        --network "$RABBITMQ_NETWORK" \
                        --network-alias rabbitmq \
                        --mount "source=$RABBITMQ_VOLUME,target=/var/lib/rabbitmq" \
                        -e RABBITMQ_DEFAULT_USER="$RABBITMQ_USER" \
                        -e RABBITMQ_DEFAULT_PASS="$RABBITMQ_PASSWORD" \
                        rabbitmq:3-management

                    echo "=== Waiting for RabbitMQ to become ready ==="

                    READY=false

                    for i in $(seq 1 60); do

                        if docker exec "$RABBITMQ_CONTAINER" \
                            rabbitmq-diagnostics -q ping >/dev/null 2>&1; then

                            echo "RabbitMQ is ready."
                            READY=true
                            break
                        fi

                        if ! docker ps --format '{{.Names}}' | \
                            grep -q "^${RABBITMQ_CONTAINER}$"; then

                            echo "ERROR: RabbitMQ container stopped unexpectedly."
                            echo "=== RabbitMQ logs ==="
                            docker logs "$RABBITMQ_CONTAINER" || true
                            exit 1
                        fi

                        echo "Waiting for RabbitMQ... ($i/60)"
                        sleep 2
                    done

                    if [ "$READY" != "true" ]; then
                        echo "ERROR: RabbitMQ did not become ready within 120 seconds."
                        echo "=== RabbitMQ logs ==="
                        docker logs "$RABBITMQ_CONTAINER" || true
                        exit 1
                    fi

                    echo "=== RabbitMQ health check ==="

                    docker exec "$RABBITMQ_CONTAINER" \
                        rabbitmq-diagnostics -q ping
                '''
            }
        }

        stage('Verify RabbitMQ Connection') {
            steps {
                sh '''
                    set -e

                    echo "=== RabbitMQ DNS ==="

                    docker exec jenkins \
                        getent hosts "$RABBITMQ_HOST"

                    echo "=== RabbitMQ TCP connection ==="

                    docker exec jenkins \
                        bash -c 'timeout 5 bash -c "</dev/tcp/rabbitmq/5672"'

                    echo "RabbitMQ TCP connection OK."

                    echo "=== RabbitMQ authentication ==="

                    docker exec "$RABBITMQ_CONTAINER" \
                        rabbitmqctl authenticate_user \
                        "$RABBITMQ_USER" \
                        "$RABBITMQ_PASSWORD"

                    echo "RabbitMQ authentication OK."
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
                    set -e

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
                echo "=== Cleaning up RabbitMQ container ==="

                docker rm -f "$RABBITMQ_CONTAINER" \
                    >/dev/null 2>&1 || true

                echo "=== Cleaning up RabbitMQ volume ==="

                docker volume rm "$RABBITMQ_VOLUME" \
                    >/dev/null 2>&1 || true
            '''
        }
    }
}
