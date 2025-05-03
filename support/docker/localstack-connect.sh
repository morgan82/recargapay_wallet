#!/bin/bash
CONTAINER_NAME=$(docker compose -f compose.yaml ps --services | grep localstack)

docker compose -f compose.yaml exec "$CONTAINER_NAME" bash