#!/bin/bash

set -e
export LOCALSTACK_HOST=localstack
set -x
QUEUES=(
  "sqs-recargapay-local-cvu-created"
  "sqs-recargapay-local-cvu-created-dlq"
  "sqs-recargapay-local-deposit-arrived"
  "sqs-recargapay-local-deposit-arrived-dlq"
  "sqs-recargapay-local-withdrawal-complete"
  "sqs-recargapay-local-withdrawal-complete-dlq"
)

AWS_REGION="eu-west-1"

queue_exists() {
  local queue_name="$1"
  awslocal --region="$AWS_REGION" sqs list-queues| grep "$queue_name" > /dev/null 2>&1
  echo "Checking queue: $queue_name"
}

echo "Waiting for the SQS queues to be available in LocalStack... Nuevoooo 2"

for queue in "${QUEUES[@]}"; do
  echo -n "Waiting for queue '$queue'"
  until queue_exists "$queue"; do
      echo -n "."
      sleep 2
  done
  echo " OK"
done

echo "All SQS queues are ready!"

touch /tmp/sqs_ready
tail -f /dev/null