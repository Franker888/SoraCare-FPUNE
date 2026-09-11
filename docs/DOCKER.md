# SoraCare con Docker

Esta configuración levanta una instalación local aislada de SoraCare. No
reutiliza ni modifica el PostgreSQL o el Tomcat instalados directamente en
Windows.

## Servicios

| Servicio | Función |
| --- | --- |
| `postgres` | PostgreSQL 18 con volumen persistente y usuario de aplicación sin privilegios administrativos. |
| `migraciones` | Flyway valida y aplica en orden las migraciones `V001` a `V036`. |
| `aplicacion` | Compila con Maven y Java 21, y ejecuta el WAR en Tomcat 10.1 como usuario no administrador. |

Los documentos clínicos, las fotografías y PostgreSQL usan volúmenes Docker
separados. Detener los contenedores no elimina esos datos.

## Requisitos

- Docker Desktop con el motor WSL 2 activo en Windows.
- Al menos 4 GB de memoria disponibles para Docker.
- Conexión a Internet durante la primera construcción para descargar las
  imágenes y dependencias.

No es necesario iniciar sesión en Docker Desktop.

## Iniciar

Desde PowerShell, en la raíz del proyecto:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\iniciar-docker.ps1"
```

La primera ejecución crea `.env.docker` con dos contraseñas aleatorias. Ese
archivo está excluido de Git. El script reutiliza en memoria las integraciones
opcionales configuradas en el perfil de Windows, incluida OpenAI, pero no copia
sus claves al repositorio ni a la imagen.

La aplicación queda disponible en:

```text
http://localhost:8081/SoraCare-FPUNE/
```

El puerto `8081` permite mantener temporalmente el Tomcat tradicional en
`8080`. Puede cambiarse en `.env.docker`.

## Detener

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\iniciar-docker.ps1" -Detener
```

Este comando conserva los volúmenes y los datos. La eliminación de volúmenes
debe ser una decisión explícita porque borra la base, documentos y fotografías
de la instalación Docker.

## Comprobaciones manuales

```powershell
docker compose --env-file .env.docker ps
docker compose --env-file .env.docker logs --tail 100 aplicacion
docker compose --env-file .env.docker logs migraciones
```

PostgreSQL no publica su puerto hacia Windows: sólo `aplicacion` y
`migraciones` pueden acceder a la red interna de base de datos. La aplicación
se publica exclusivamente en `127.0.0.1` para el entorno local.

## Validación realizada

El 31 de agosto de 2026 se comprobó la instalación completa en Windows con
WSL 2:

- construcción reproducible del WAR con 128 pruebas aprobadas y cero fallos;
- PostgreSQL y SoraCare en estado saludable;
- Flyway finalizado correctamente, con 32 versiones aplicadas (`V001` a
  `V033`);
- portada, conexión a base de datos y API REST respondiendo correctamente;
- Sora conectada con OpenAI y consultando el catálogo público real;
- apagado y recreación de los contenedores sin pérdida del historial de la
  base de datos.

## Validación de la API clínica (10 de septiembre de 2026)

- Imagen reconstruida con `mvn clean package`: 142 pruebas, cero fallos,
  errores o pruebas omitidas.
- Aplicación y PostgreSQL saludables, usando los volúmenes existentes.
- Flyway validó el esquema en versión `036`, sin migraciones pendientes.
- Portada, conexión a PostgreSQL y catálogo REST: HTTP 200.
- Las cinco rutas clínicas privadas sin sesión: HTTP 401 en JSON, incluso
  cuando se solicita PDF; escritura sobre documentos: HTTP 405.
- Las respuestas REST comprobadas incluyen `Cache-Control: no-store`.

La consulta autorizada, el control de rol, la generación/entrega de archivos
y los rechazos por propiedad se verifican con MockMvc y servicios de prueba.
La comprobación HTTP del despliegue no usa cuentas ni datos clínicos reales.

## Recuperación local del arranque (10 de septiembre de 2026)

Docker Desktop no podía iniciar por sockets AF_UNIX bloqueados en Windows:
`Docker/run/sailor-ingest.sock` y `docker-secrets-engine/engine.sock`, bajo
`%LOCALAPPDATA%`. Con el motor detenido se verificó que ambas carpetas solo
contenían sockets vacíos y se conservaron como respaldos antes de recrearlas.
El motor volvió a iniciar. No se restableció Docker a valores de fábrica ni
se eliminaron contenedores, volúmenes, base de datos o credenciales.

Los respaldos locales son `Docker/run.soracare-respaldo-20260907`,
`Docker/run.soracare-respaldo-20260910` y
`docker-secrets-engine.soracare-respaldo-20260910`, bajo `%LOCALAPPDATA%`.
La incidencia coincide con el [reporte de sockets bloqueados de Docker Desktop](https://github.com/docker/desktop-feedback/issues/554).
Si vuelve a ocurrir, comprobar primero el error y el contenido de las carpetas;
esta reparación no debe aplicarse a directorios de datos ni con el motor activo.

## Alcance de producción

La composición local es una base reproducible, no reemplaza por sí sola una
plataforma hospitalaria de producción. Un despliegue real requiere HTTPS,
gestión externa de secretos, copias de seguridad verificadas, monitoreo,
actualizaciones, alta disponibilidad y revisión legal y de seguridad.
