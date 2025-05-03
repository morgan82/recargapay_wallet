#!/bin/bash
CONTAINER_NAME=$(docker compose -f compose.yaml ps --services | grep redis)

docker compose -f compose.yaml exec "$CONTAINER_NAME" redis-cli
