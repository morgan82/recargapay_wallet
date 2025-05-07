#!/bin/sh

echo "Waiting for Redis to be ready..."
until redis-cli -h redis -p 6379 ping | grep -q PONG; do
  sleep 2
done

echo "Redis is up. Initializing keys..."

redis-cli -h redis -p 6379 set ALIAS::test.1ars.rp "{\"cvbu\":\"7010576316851312771347\",\"alias\":\"test.1ars.rp\",\"bank_account_type\":\"CVU\",\"status\":\"OK\",\"is_rp_user\":true}"
redis-cli -h redis -p 6379 set CVBU::7010576316851312771347 "test.1ars.rp"

#redis-cli -h redis -p 6379 set ALIAS::test.1usd.rp "{\"cvbu\":\"7991612183721723346815\",\"alias\":\"test.1usd.rp\",\"bank_account_type\":\"CVU\",\"status\":\"OK\",\"is_rp_user\":true}"
#redis-cli -h redis -p 6379 set CVBU::7991612183721723346815 "test.1usd.rp"


echo "Redis initialization complete."
