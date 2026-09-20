# syntax=docker/dockerfile:1

FROM maven:3.9.16-eclipse-temurin-21-noble AS compilacion

WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp clean package

FROM tomcat:10.1.59-jre21-temurin-noble AS ejecucion

LABEL org.opencontainers.image.title="SoraCare-FPUNE" \
      org.opencontainers.image.description="Sistema de gestión hospitalaria SoraCare" \
      org.opencontainers.image.source="https://github.com/TobiBug/SoraCare-FPUNE"

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        curl \
        libc-bin \
        libc6 \
        libsqlite3-0 \
        locales \
        perl-base \
        gosu \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 soracare \
    && useradd --system --uid 10001 --gid soracare \
        --home-dir /var/lib/soracare --shell /usr/sbin/nologin soracare \
    && rm -rf "${CATALINA_HOME}/webapps/"* \
    && mkdir -p /var/lib/soracare/documentos-clinicos \
        /var/lib/soracare/fotos-perfil \
    && chown -R soracare:soracare "${CATALINA_HOME}" /var/lib/soracare

COPY --from=compilacion --chown=soracare:soracare \
    /workspace/target/SoraCare-FPUNE.war \
    ${CATALINA_HOME}/webapps/SoraCare-FPUNE.war

COPY --chmod=755 docker/iniciar-soracare.sh \
    /usr/local/bin/iniciar-soracare

ENV SORACARE_DOCUMENTOS_DIR=/var/lib/soracare/documentos-clinicos \
    SORACARE_PERFILES_DIR=/var/lib/soracare/fotos-perfil \
    CATALINA_OPTS="-Xms128m -Xmx384m -Dfile.encoding=UTF-8 -Duser.timezone=America/Asuncion"

USER soracare

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=8 \
    CMD curl --fail --silent --show-error \
        http://127.0.0.1:${PORT:-8080}/SoraCare-FPUNE/conexion-bd > /dev/null \
        || exit 1

ENTRYPOINT ["iniciar-soracare"]
CMD ["catalina.sh", "run"]
