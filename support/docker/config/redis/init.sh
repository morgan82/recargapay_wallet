#!/bin/sh

echo "Waiting for Redis to be ready..."
until redis-cli -h redis -p 6379 ping | grep -q PONG; do
  sleep 2
done

echo "Redis is up. Initializing keys..."

redis-cli -h redis -p 6379 set CVBU_BY_ALIAS::test.1ars.rp "7010576316851312771347"
redis-cli -h redis -p 6379 set CVBU_USED::7010576316851312771347 "LOCKED"
redis-cli -h redis -p 6379 set ALIAS_USED::test.1ars.rp "LOCKED"

redis-cli -h redis -p 6379 set CVBU_BY_ALIAS::test.1usd.rp "7991612183721723346815"
redis-cli -h redis -p 6379 set CVBU_USED::7991612183721723346815 "LOCKED"
redis-cli -h redis -p 6379 set ALIAS_USED::test.1usd.rp "LOCKED"

echo "Redis initialization complete."
