#!/usr/bin/env bash
# Runs once, at first container start, via Postgres's docker-entrypoint-initdb.d hook.
# One physical Postgres instance for local/dev convenience, but one database per service —
# each service only ever connects to its own, so nothing here breaks the per-service
# ownership a real microservices deployment (separate DB servers) would give you.
set -e

for db in identity_db exam_db attempt_db proctoring_db review_db result_db payment_db notification_db config_db; do
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    SELECT 'CREATE DATABASE $db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db')\gexec
EOSQL
done
