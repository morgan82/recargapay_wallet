#!/bin/bash
set -e

echo "Waiting for LocalStack to be available..."

# Wait for the service to be ready
until curl -s http://localhost:4566/_localstack/health | grep '"sqs": "running"'; do
  sleep 2
done

echo "Creating SQS queues..."

awslocal sqs create-queue   --queue-name sqs-recargapay-local-cvu-created-dlq   --endpoint-url http://localhost:4566   --attributes VisibilityTimeout=5   --region eu-west-1

awslocal sqs create-queue \
  --queue-name sqs-recargapay-local-cvu-created \
  --attributes file:///etc/localstack/init/ready.d/sqs-attributes.json \
  --endpoint-url http://localhost:4566 \
  --region eu-west-1


echo "LocalStack initialization completed."
