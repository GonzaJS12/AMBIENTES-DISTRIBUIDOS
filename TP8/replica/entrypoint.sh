#!/bin/bash
set -euo pipefail

DATA=/var/lib/postgresql/data

if [ ! -s "$DATA/PG_VERSION" ]; then
  echo "TP8 replica: esperando al primario..."
  until pg_isready -h primario -p 5432 -U app; do
    sleep 2
  done

  echo "TP8 replica: copiando el primario (pg_basebackup)..."
  rm -rf "${DATA:?}"/*
  export PGPASSWORD=replicator
  pg_basebackup -h primario -p 5432 -D "$DATA" -U replicator -Fp -Xs -P -R

  # -R no guarda la contraseña. La dejamos en primary_conninfo.
  sed -i '/primary_conninfo/d' "$DATA/postgresql.auto.conf"
  printf "%s\n" "primary_conninfo = 'host=primario port=5432 user=replicator password=replicator application_name=tp8_replica'" >> "$DATA/postgresql.auto.conf"
  touch "$DATA/standby.signal"

  chown -R postgres:postgres "$DATA"
  chmod 700 "$DATA"
fi

exec docker-entrypoint.sh postgres -c hot_standby=on
