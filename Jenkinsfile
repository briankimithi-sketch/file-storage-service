pipeline {
    agent any

    environment {
        // ==========================================
        // RabbitMQ CI Configuration
        // ==========================================
        RABBITMQ_CONTAINER = 'file-storage-rabbitmq-ci'
        RABBITMQ_VOLUME    = 'file-storage-rabbitmq-ci-volume'
        RABBITMQ_NETWORK   = 'file-storage-ci-network'
        RABBITMQ_HOST      = 'rabbitmq'
        RABBITMQ_PORT      = '5672'
        RABBITMQ_USER      = 'ciuser'
        RABBITMQ_PASSWORD  = 'cipassword'

        // ==========================================
        // PostgreSQL CI Configuration
        // ==========================================
        POSTGRES_CONTAINER = 'file-storage-postgres-ci'
        POSTGRES_VOLUME    = 'file-storage-postgres-ci-volume'
        POSTGRES_HOST      = 'postgres'
        POSTGRES_PORT      = '5432'
        POSTGRES_DB        = 'file_storage_db'
        POSTGRES_USER      = 'postgres'
        POSTGRES_PASSWORD  = 'cipassword'
    }

    stages {

        // =========================================================
        // START RABBITMQ
        // =========================================================
        stage('Start RabbitMQ') {
            steps {
                sh '''
                    set -e

                    echo "=========================================="
                    echo "Preparing RabbitMQ CI network"
                    echo "=========================================="

                    docker network inspect "$RABBITMQ_NETWORK" >/dev/null 2>&1 || \
                        docker network create "$RABBITMQ_NETWORK"

                    echo "=== Removing old RabbitMQ container ==="

                    docker rm -f "$RABBITMQ_CONTAINER" >/dev/null 2>&1 || true

                    echo "=== Removing old RabbitMQ volume ==="

                    docker volume rm "$RABBITMQ_VOLUME" >/dev/null 2>&1 || true

                    echo "=== Creating fresh RabbitMQ volume ==="

                    docker volume create "$RABBITMQ_VOLUME"

                    echo "=== Initializing RabbitMQ volume permissions ==="
                    docker run --rm \
                        --user root \
                        --mount "source=$RABBITMQ_VOLUME,target=/var/lib/rabbitmq" \
                        rabbitmq:3-management \
                        bash -c 'mkdir -p /var/lib/rabbitmq && chown -R rabbitmq:rabbitmq /var/lib/rabbitmq'

                    echo "=== Verifying RabbitMQ volume permissions ==="
                    docker run --rm \
                        --user root \
                        --mount "source=$RABBITMQ_VOLUME,target=/var/lib/rabbitmq" \
                        rabbitmq:3-management \
                        bash -c 'ls -lan /var/lib/rabbitmq'

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

                    echo "RabbitMQ startup completed successfully."
                '''
            }
        }

        // =========================================================
        // START POSTGRESQL
        // =========================================================
        stage('Start PostgreSQL') {
            steps {
                sh '''
                    set -e

                    echo "=========================================="
                    echo "Preparing PostgreSQL CI"
                    echo "=========================================="

                    echo "=== Removing old PostgreSQL container ==="

                    docker rm -f "$POSTGRES_CONTAINER" >/dev/null 2>&1 || true

                    echo "=== Removing old PostgreSQL volume ==="

                    docker volume rm "$POSTGRES_VOLUME" >/dev/null 2>&1 || true

                    echo "=== Creating fresh PostgreSQL volume ==="

                    docker volume create "$POSTGRES_VOLUME"

                    echo "=== Starting PostgreSQL ==="

                    docker run -d \
                        --name "$POSTGRES_CONTAINER" \
                        --network "$RABBITMQ_NETWORK" \
                        --network-alias postgres \
                        --mount "source=$POSTGRES_VOLUME,target=/var/lib/postgresql/data" \
                        -e POSTGRES_DB="$POSTGRES_DB" \
                        -e POSTGRES_USER="$POSTGRES_USER" \
                        -e POSTGRES_PASSWORD="$POSTGRES_PASSWORD" \
                        postgres:18

                    echo "=== Waiting for PostgreSQL to become ready ==="

                    READY=false

                    for i in $(seq 1 60); do

                        if docker exec "$POSTGRES_CONTAINER" \
                            pg_isready \
                            -U "$POSTGRES_USER" \
                            -d "$POSTGRES_DB" >/dev/null 2>&1; then

                            echo "PostgreSQL is ready."
                            READY=true
                            break
                        fi

                        if ! docker ps --format '{{.Names}}' | \
                            grep -q "^${POSTGRES_CONTAINER}$"; then

                            echo "ERROR: PostgreSQL container stopped unexpectedly."
                            echo "=== PostgreSQL logs ==="
                            docker logs "$POSTGRES_CONTAINER" || true
                            exit 1
                        fi

                        echo "Waiting for PostgreSQL... ($i/60)"
                        sleep 2
                    done

                    if [ "$READY" != "true" ]; then
                        echo "ERROR: PostgreSQL did not become ready within 120 seconds."
                        echo "=== PostgreSQL logs ==="
                        docker logs "$POSTGRES_CONTAINER" || true
                        exit 1
                    fi

                    echo "=== PostgreSQL health check ==="

                    docker exec "$POSTGRES_CONTAINER" \
                        pg_isready \
                        -U "$POSTGRES_USER" \
                        -d "$POSTGRES_DB"

                    echo "=== PostgreSQL database check ==="

                    docker exec "$POSTGRES_CONTAINER" \
                        psql \
                        -U "$POSTGRES_USER" \
                        -d "$POSTGRES_DB" \
                        -c "SELECT version();"

                    echo "PostgreSQL startup completed successfully."
                '''
            }
        }

        // =========================================================
        // VERIFY RABBITMQ
        // =========================================================
        stage('Verify RabbitMQ Connection') {
            steps {
                sh '''
                    set -e

                    echo "=========================================="
                    echo "Verifying RabbitMQ"
                    echo "=========================================="

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

        // =========================================================
        // VERIFY POSTGRESQL
        // =========================================================
        stage('Verify PostgreSQL Connection') {
            steps {
                sh '''
                    set -e

                    echo "=========================================="
                    echo "Verifying PostgreSQL"
                    echo "=========================================="

                    echo "=== PostgreSQL DNS ==="

                    docker exec jenkins \
                        getent hosts "$POSTGRES_HOST"

                    echo "=== PostgreSQL TCP connection ==="

                    docker exec jenkins \
                        bash -c 'timeout 5 bash -c "</dev/tcp/postgres/5432"'

                    echo "PostgreSQL TCP connection OK."

                    echo "=== PostgreSQL authentication ==="

                    docker exec "$POSTGRES_CONTAINER" \
                        psql \
                        -U "$POSTGRES_USER" \
                        -d "$POSTGRES_DB" \
                        -c "SELECT current_database(), current_user;"

                    echo "PostgreSQL authentication OK."
                '''
            }
        }

        // =========================================================
        // BUILD
        // =========================================================
        stage('Build') {
            steps {
                sh '''
                    set -e

                    echo "=========================================="
                    echo "Building application"
                    echo "=========================================="

                    ./mvnw clean package -DskipTests
                '''
            }
        }

        // =========================================================
        // TEST
        // =========================================================
        stage('Test') {
            steps {
                sh '''
                    set -e

                    echo "=========================================="
                    echo "Running tests"
                    echo "=========================================="

                    SPRING_RABBITMQ_HOST="$RABBITMQ_HOST" \
                    SPRING_RABBITMQ_PORT="$RABBITMQ_PORT" \
                    SPRING_RABBITMQ_USERNAME="$RABBITMQ_USER" \
                    SPRING_RABBITMQ_PASSWORD="$RABBITMQ_PASSWORD" \
                    SPRING_DATASOURCE_HOST="$POSTGRES_HOST" \
                    SPRING_DATASOURCE_PORT="$POSTGRES_PORT" \
                    SPRING_DATASOURCE_USERNAME="$POSTGRES_USER" \
                    SPRING_DATASOURCE_PASSWORD="$POSTGRES_PASSWORD" \
                    ./mvnw test
                '''
            }
        }

        // =========================================================
        // ARCHIVE
        // =========================================================
        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
    }

    // =========================================================
    // CLEANUP
    // =========================================================
    post {
        always {
            sh '''
                echo "=========================================="
                echo "Cleaning up CI containers"
                echo "=========================================="

                echo "=== Cleaning RabbitMQ container ==="

                docker rm -f "$RABBITMQ_CONTAINER" \
                    >/dev/null 2>&1 || true

                echo "=== Cleaning RabbitMQ volume ==="

                docker volume rm "$RABBITMQ_VOLUME" \
                    >/dev/null 2>&1 || true

                echo "=== Cleaning PostgreSQL container ==="

                docker rm -f "$POSTGRES_CONTAINER" \
                    >/dev/null 2>&1 || true

                echo "=== Cleaning PostgreSQL volume ==="

                docker volume rm "$POSTGRES_VOLUME" \
                    >/dev/null 2>&1 || true

                echo "CI cleanup completed."
            '''
        }
    }
}
