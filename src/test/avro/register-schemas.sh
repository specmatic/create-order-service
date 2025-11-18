#!/bin/sh
set -e

SCHEMA_REGISTRY_URL="http://schema-registry:8085"

# Wait for Schema Registry to be up
echo "Waiting for Schema Registry to be ready..."
until curl -s ${SCHEMA_REGISTRY_URL}/subjects > /dev/null; do
  sleep 2
done
echo "Schema Registry is up."

cd /usr/src/app/schemas

curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  --data "{\"schema\":$(jq -Rs . < NewOrders.avsc)}" \
  ${SCHEMA_REGISTRY_URL}/subjects/new-orders-value/versions

echo "Schemas registered."