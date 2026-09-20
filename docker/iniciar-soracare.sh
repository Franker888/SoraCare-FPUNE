#!/bin/sh
set -eu

directorio_persistente=/var/lib/soracare

if [ "$(id -u)" -eq 0 ]; then
    mkdir -p \
        "${directorio_persistente}/documentos-clinicos" \
        "${directorio_persistente}/fotos-perfil"
    chown -R soracare:soracare "${directorio_persistente}"
    exec gosu soracare "$0" "$@"
fi

puerto="${PORT:-8080}"
case "${puerto}" in
    ''|*[!0-9]*)
        echo "PORT debe ser un número entre 1024 y 65535." >&2
        exit 1
        ;;
esac

if [ "${puerto}" -lt 1024 ] || [ "${puerto}" -gt 65535 ]; then
    echo "PORT debe ser un número entre 1024 y 65535." >&2
    exit 1
fi

if [ "${puerto}" != "8080" ]; then
    sed -i \
        "s/port=\"8080\" protocol=\"HTTP\/1.1\"/port=\"${puerto}\" protocol=\"HTTP\/1.1\"/" \
        "${CATALINA_HOME}/conf/server.xml"
fi

exec "$@"
