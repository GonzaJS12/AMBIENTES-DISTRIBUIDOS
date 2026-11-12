#!/bin/bash
set -euo pipefail

# El primario acepta al usuario de replicacion desde la red de Docker.
echo "host replication replicator all scram-sha-256" >> "$PGDATA/pg_hba.conf"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-'EOSQL'
CREATE ROLE replicator WITH REPLICATION LOGIN PASSWORD 'replicator';
SELECT pg_reload_conf();
EOSQL
