#!/bin/bash
docker compose up -d --force-recreate
# Get the LocalStack container name dynamically using compose.yaml
CONTAINER_NAME=$(docker ps --format '{{.Names}}' | grep localstack)

QUEUES=("sqs-recargapay-local-cvu-created"
"sqs-recargapay-local-cvu-created-dlq"
"sqs-recargapay-local-deposit-arrived"
"sqs-recargapay-local-deposit-arrived-dlq"
"sqs-recargapay-local-withdrawal-complete"
"sqs-recargapay-local-withdrawal-complete-dlq"
)

AWS_REGION="eu-west-1"
SQS_ENDPOINT="http://localhost:4566"

queue_exists() {
  local queue_name="$1"
  # List queues and check if the queue exists
  docker exec "$CONTAINER_NAME" awslocal --endpoint-url="$SQS_ENDPOINT" --region="$AWS_REGION" sqs list-queues | grep "$queue_name" > /dev/null 2>&1
}

echo "Waiting for the SQS queues to be available in LocalStack..."

for queue in "${QUEUES[@]}"; do
  echo -n "Waiting for queue '$queue' "
  until queue_exists "$queue"; do
      echo -n "."
      sleep 2
  done
  echo " OK"
done

echo "All SQS queues are ready!"