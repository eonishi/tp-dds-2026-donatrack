#!/bin/sh
#borra los esquemas donaciones y logística, queda pelado
set -eu

ARCHIVO_ENV=${1:-.env}
if [ ! -f "$ARCHIVO_ENV" ]; then
    echo "No existe $ARCHIVO_ENV" >&2
    exit 1
fi

docker run --rm --env-file "$ARCHIVO_ENV" ${RED:+--network "$RED"} \
    docker.io/library/postgres:18.6-alpine sh -euc '
    resto=${DB_URL#jdbc:postgresql://}
    resto=${resto%%\?*}
    servidor=${resto%%/*}
    base=${resto#*/}
    host=${servidor%%:*}
    puerto=5432
    case "$servidor" in *:*) puerto=${servidor#*:} ;; esac

    echo "Borrando esquemas donaciones y logística en $host/$base"
    PGPASSWORD="$DB_PASSWORD" PGSSLMODE=prefer psql -h "$host" -p "$puerto" -U "$DB_USER" \
        -d "$base" -v ON_ERROR_STOP=1 -q \
        -c "DROP SCHEMA IF EXISTS donaciones CASCADE" \
        -c "DROP SCHEMA IF EXISTS logistica CASCADE"
    echo "Listo" 
    '