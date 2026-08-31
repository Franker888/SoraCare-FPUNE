#!/usr/bin/env bash
set -Eeuo pipefail

: "${POSTGRES_USER:?POSTGRES_USER es obligatorio}"
: "${POSTGRES_DB:?POSTGRES_DB es obligatorio}"
: "${SORACARE_DB_PASSWORD:?SORACARE_DB_PASSWORD es obligatorio}"

psql \
    --username "${POSTGRES_USER}" \
    --dbname "${POSTGRES_DB}" \
    --set=app_password="${SORACARE_DB_PASSWORD}" \
    --set=database_name="${POSTGRES_DB}" \
    --set=ON_ERROR_STOP=1 <<'SQL'
SELECT format(
    'CREATE ROLE soracare_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD %L',
    :'app_password'
)
WHERE NOT EXISTS (
    SELECT 1 FROM pg_roles WHERE rolname = 'soracare_app'
)
\gexec

ALTER ROLE soracare_app
    WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;
ALTER ROLE soracare_app PASSWORD :'app_password';
ALTER DATABASE :"database_name" OWNER TO soracare_app;
GRANT CONNECT, TEMPORARY ON DATABASE :"database_name" TO soracare_app;
SQL
