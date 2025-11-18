#!/bin/sh
set -e

echo 'Waiting for Kafka broker to be ready...'
until kafka-topics --bootstrap-server broker:9093 --list > /dev/null 2>&1; do
  echo 'Kafka not ready yet, retrying in 2s...'
  sleep 2
done

echo 'Creating topics...'
TOPICS="new-orders"

for topic in $TOPICS; do
  kafka-topics --bootstrap-server broker:9093 \
    --create --if-not-exists \
    --topic "$topic" \
    --partitions 3 \
    --replication-factor 1
done

echo 'Topic creation done.'
