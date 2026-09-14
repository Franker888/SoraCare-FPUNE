# Cabeceras y cookies — revisión del 14/09/2026

## Alcance y evidencia

Entorno autorizado: Docker local en esta PC. No se habilitó acceso por red, no se cambió el puerto publicado ni la base. La demostración del segundo parcial queda en [su pendiente separado](PENDIENTE_DEMOSTRACION_PARCIAL.md).

La respuesta real de `/login` carecía de X-Frame-Options, CSP y X-Content-Type-Options. JSESSIONID ya tenía HttpOnly por defecto del contenedor: no se atribuye al parche una protección que ya existía. `web.xml` no explicitaba la política de sesión.

## Implementación

- Nuevo `FiltroCabecerasSeguridad`, antes de los controles de sesión/CSRF y aplicado en REQUEST y ERROR: `nosniff`, `SAMEORIGIN`, CSP limitada a `frame-ancestors 'self'` y Referrer-Policy `no-referrer` por defecto.
- `web.xml` fija HttpOnly para JSESSIONID y seguimiento exclusivamente por COOKIE, evitando adoptar una sesión mediante `;jsessionid` en la URL.
- Se conservan las excepciones existentes de Referrer-Policy del login y registro para Google Identity en HTTP localhost. No se reescriben cookies de Google ni se agrega COOP, una política restrictiva de scripts, HSTS o Secure obligatorio sobre HTTP local.
- Se mantienen MIME y cuerpo de respuestas, incluidos PDF; los marcos del mismo origen siguen permitidos. Esto no equivale a una CSP completa contra XSS.

Se eligió un filtro Jakarta pequeño para cubrir el control compartido, sin agregar dependencias ni modificar cada servlet.

## Verificación y límites

`FiltroCabecerasSeguridadTest` verifica cabeceras, conservación del cuerpo/MIME PDF, ausencia de sesiones/cookies nuevas y compatibilidad del override de Google. La revisión independiente detectó que el registro POST fallido no reponía la excepción de Google: se corrigió en `RegistroCuentaPacienteServlet` y una prueba ejecuta su ruta real de error con servicios simulados, sin red ni base. `CsrfTomcatIT` usa la sección session-config real del proyecto: verifica HttpOnly, sesión funcional por HTTP, rechazo de un cliente que intenta adoptar la sesión por URL, cabeceras en 200/403 y conservación del flujo multipart.

No se ha repetido el acceso real con Google ni el recorrido autenticado de cada PDF. La respuesta del navegador a frame-ancestors se valida mediante la política entregada; no se hizo una prueba visual de clickjacking. No se certifica la ausencia de toda vulnerabilidad.

Verificación final de código: `git diff --check` sin errores; `mvn -B -ntp -Dmaven.repo.local=C:\Users\adria\.m2\repository -Pintegracion-postgresql verify dependency:list -DoutputFile=target/dependencias-seguridad.txt -DappendOutput=false` terminó **BUILD SUCCESS**, con 192 pruebas unitarias y 6 de integración (198 en total), sin fallos ni omitidas. Incluye la firma XML SIFEN con la biblioteca actualizada.

Despliegue final: `scripts/iniciar-docker.ps1` reconstruyó la imagen con pruebas unitarias y terminó correctamente. Aplicación y PostgreSQL saludables; puerto `127.0.0.1:8081`, sin exposición de PostgreSQL. HTTP real: `/`, `/login`, `/registro`, salud y JavaScript 200 con las nuevas cabeceras; administración anónima 302; POST login sin token 403 con CSP. La primera respuesta crea una cookie HttpOnly y las siguientes reutilizan esa sesión. Login/registro mantienen su Referrer-Policy especial. No se alteraron cuentas ni registros clínicos.

## Dependencias

`mvn dependency:list -DoutputFile=target/dependencias-seguridad.txt -DappendOutput=false` generó correctamente el inventario resuelto: 93 coordenadas Maven, incluyendo pruebas y dependencias transitivas.

La consulta inicial fue interrumpida por el límite de uso de aprobaciones. Tras restablecerse la autorización, OSV respondió para las 93 coordenadas sin paginación pendiente. Identificó tres avisos en dos artefactos de alcance **test**: `bcprov-jdk18on:1.80` (GHSA-574f-3g2m-x479 y GHSA-c3fc-8qff-9hwx) y `bcpkix-jdk18on:1.80` (GHSA-wg6q-6289-32hp). No hubo coincidencias en los artefactos de ejecución consultados. Estos resultados son temporales y no demuestran ausencia de vulnerabilidades desconocidas.

Se actualizó la propiedad Bouncy Castle a 1.84, versión corregida indicada por los avisos. Su uso en este proyecto es generar certificados RSA/SHA256 ficticios para `FlujoXmlSifenTest`; no se constató explotación de los algoritmos afectados, no se usa en `src/main` y permanece fuera del WAR por su alcance test. No se modificó el motor fiscal ni se emitieron documentos reales.

La segunda consulta OSV sobre el inventario resuelto final devolvió **93 respuestas, cero paquetes con avisos y cero páginas pendientes** el 14/09/2026. Se inspeccionó el WAR y no contiene bcprov, bcpkix ni bcutil. El resultado abarca solo las coordenadas Maven consultadas y la información disponible en esa fecha.

Queda fuera de esta consulta la base de paquetes del sistema operativo de la imagen Docker, Tomcat como contenedor y bibliotecas JavaScript vendorizadas. Esa cobertura debe revisarse antes de publicar el sistema; esta consulta no es un escaneo integral de la imagen.

## Referencias consultadas

- [Filtros de seguridad HTTP de Tomcat](https://tomcat.apache.org/tomcat-10.1-doc/config/filter.html).
- [Configuración de Google Identity y Referrer-Policy para localhost](https://developers.google.com/identity/gsi/web/guides/get-google-api-clientid?hl=en).
- [Consulta por lotes de OSV](https://google.github.io/osv.dev/post-v1-querybatch/).
- [Aviso GOST Bouncy Castle](https://github.com/advisories/GHSA-574f-3g2m-x479), [aviso LDAP](https://github.com/advisories/GHSA-c3fc-8qff-9hwx) y [aviso PKIX](https://github.com/advisories/GHSA-wg6q-6289-32hp).
