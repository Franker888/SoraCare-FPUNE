# Runtime y frontend — revisión del 19/09/2026

## Alcance

Esta revisión completa la cobertura que había quedado fuera del inventario Maven: el runtime Tomcat de la imagen Docker y las bibliotecas frontend de terceros. El entorno continúa siendo local en esta PC; no se abrió el puerto a la red ni se publicaron PostgreSQL, credenciales o datos clínicos.

## Tomcat

La imagen de ejecución y la prueba de integración usan ahora `tomcat:10.1.59-jre21-temurin-noble`, en lugar de `10.1.57`. La página oficial de seguridad de Apache publica 10.1.59 como versión corregida para avisos que alcanzan a 10.1.57. La actualización es preventiva: la revisión del proyecto no encontró configuración propia de Realm, RewriteValve, HTTP/2, WebSocket ni autenticación declarativa de Tomcat que demuestre una ruta de explotación concreta.

Los dos puntos de versión se mantienen sincronizados:

- `Dockerfile`: runtime que se entrega.
- `CsrfTomcatIT`: contenedor real utilizado para probar sesión, cookies, cabeceras y CSRF.

No se modificaron los scripts heredados que apuntan al Tomcat 10.1.57 instalado físicamente en Windows. El cambio de este bloque se limita a Docker y a su prueba automatizada.

La imagen final también actualiza de forma explícita `libc-bin`, `libc6`, `libsqlite3-0`, `locales` y `perl-base`. Eran los cinco paquetes para los que el escaneo de la imagen encontró una versión corregida disponible en los repositorios de Ubuntu.

## Cookie de sesión

La aplicación incorpora `META-INF/context.xml` con `sameSiteCookies="lax"`. La cookie `JSESSIONID` conserva `HttpOnly` y ahora declara `SameSite=Lax`: el navegador puede usarla en la navegación normal y en retornos GET de autenticación, pero no la adjunta a solicitudes cruzadas comunes. La prueba real `CsrfTomcatIT` comprueba ambos atributos y también conserva el comportamiento necesario del entorno local HTTP, donde `Secure` no puede activarse hasta disponer de HTTPS.

## Aplicación y bibliotecas frontend

Trivy 0.74.0, descargado desde la publicación oficial y verificado por SHA-256, analizó el directorio desplegado `target/SoraCare-FPUNE`, incluido `WEB-INF/lib`: **1 objetivo, 0 vulnerabilidades detectadas** el 19/09/2026. El resultado solo representa la base pública disponible en esa fecha y no prueba la ausencia de vulnerabilidades desconocidas.

Se inventariaron las bibliotecas frontend que tienen una versión fija:

- Bootstrap 5.3.3, servido localmente.
- Popper 2.11.8, incluido en `bootstrap.bundle.min.js`.
- Bootstrap Icons 1.13.1, servido localmente.
- Firebase compat 10.12.5, cargado desde `gstatic.com` únicamente en notificaciones.

La consulta individual a OSV devolvió **0 avisos** para esas cuatro coordenadas el 19/09/2026. No se actualizaron por novedad solamente: sin un aviso confirmado, conservar las versiones ya probadas evita introducir una regresión visual o funcional innecesaria. Google Identity y Cloudflare Turnstile se cargan desde sus URL administradas y no exponen una versión npm fija que pueda consultarse del mismo modo.

Los archivos locales quedaron identificados por estos hashes SHA-256:

- `bootstrap.min.css`: `3C8F27E6009CCFD710A905E6DCF12D0EE3C6F2AC7DA05B0572D3E0D12E736FC8`.
- `bootstrap.bundle.min.js`: `0833B2E9C3A26C258476C46266E6877FC75218625162E0460BE9A3A098A61C6C`.
- `bootstrap-icons.min.css`: `A5D6387A32CA3BAEC4D02336B5B3EDAB50C9DD518355576A011EA3DD9C1D884E`.

## Verificación

- `mvn -Pintegracion-postgresql clean verify`: **BUILD SUCCESS**, 192 pruebas unitarias y 6 pruebas de integración con Tomcat, PostgreSQL y Flyway; 0 fallos, 0 errores y 0 omitidas.
- Verificación enfocada posterior de la cookie: 192 pruebas unitarias y `CsrfTomcatIT`; 0 fallos.
- Reconstrucción de `soracare-fpune:local`: **correcta**; Tomcat 10.1.59, ejecución como UID/GID 10001 `soracare`, solo SoraCare desplegado y estado saludable.
- Publicación local: únicamente `127.0.0.1:8081`; PostgreSQL no publica puerto al equipo anfitrión.
- Recorrido HTTP: inicio, login, registro y conexión responden; el acceso administrativo anónimo redirige al login; un POST de login sin token CSRF devuelve 403; CSP, `X-Frame-Options`, `X-Content-Type-Options` y `Referrer-Policy` están presentes.
- Cookie observada en la imagen final: `HttpOnly` y `SameSite=Lax` activos. `Secure` queda pendiente de HTTPS, no debe activarse en el HTTP local porque impediría conservar la sesión.
- Trivy sobre la aplicación desplegada: 0 vulnerabilidades detectadas.
- Trivy sobre la imagen Docker final: 0 críticas, 0 altas y 0 vulnerabilidades con corrección disponible; permanecen 41 medias y 4 bajas sin versión corregida publicada para los paquetes instalados. Este resultado es una fotografía de la base de datos del 19/09/2026 y debe repetirse antes de publicar.
- OSV sobre Bootstrap, Popper, Bootstrap Icons y Firebase: 0 avisos.
- `scripts/respaldar-docker.ps1 -Verificar`: respaldo completo generado y restauración aislada correcta hasta Flyway 039; archivos extraídos y comparados; contenedor temporal eliminado. Aplicación y base regresaron a estado saludable.

## Incidencia externa de Docker Desktop

Docker Desktop 4.89.0 no iniciaba en Windows build 26200 porque sus sockets AF_UNIX habían quedado como puntos de análisis inaccesibles (`ERROR_CANT_ACCESS_FILE`). Los registros locales mostraban el fallo en `sailor-ingest.sock` y `docker-secrets-engine/engine.sock`. Coincide con un error abierto de Docker Desktop para Windows; no proviene de SoraCare ni implicó pérdida de imágenes, volúmenes o PostgreSQL.

Después del reinicio se preservaron las carpetas temporales dañadas mediante renombrado a `run-sockets-corruptos-20260919-185817` y `docker-secrets-engine-sockets-corruptos-20260919-185817`, sin borrar datos. Docker volvió a iniciar y permitió completar construcción, pruebas, respaldo y escaneos. Si el error reaparece, se debe repetir el diagnóstico antes de tocar esos directorios; no se debe usar **Reset to factory defaults**, porque sí eliminaría el estado local de Docker.

## Referencias

- [Avisos de seguridad de Apache Tomcat 10](https://tomcat.apache.org/security-10.html).
- [Publicaciones oficiales de Trivy](https://github.com/aquasecurity/trivy/releases).
- [Consulta de vulnerabilidades de OSV](https://google.github.io/osv.dev/post-v1-query/).
- [Incidencia de sockets AF_UNIX en Docker Desktop para Windows](https://github.com/docker/desktop-feedback/issues/554).
