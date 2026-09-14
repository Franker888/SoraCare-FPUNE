# Protección CSRF de formularios — 14/09/2026

## Alcance

SoraCare seguirá en esta PC con Docker, según confirmación del usuario. Este cierre es del control CSRF de navegador; no es una certificación de seguridad integral ni una autorización para publicar datos clínicos en Internet.

Antes, distintas operaciones fuera de administración aceptaban POST sin demostrar que el usuario había abierto el formulario de su sesión. Un sitio externo podía intentar enviar una operación con la autoridad de una sesión del navegador. La explotación depende de que el navegador acompañe esa solicitud con la cookie de sesión.

## Cambio

- `FiltroCsrfFormularios`, registrado después de la vigencia de sesión, exige un token ligado a la sesión antes de encadenar solicitudes de escritura. No reemplaza permisos, identidad ni controles de propiedad.
- Se agregaron campos ocultos a los formularios POST que faltaban: citas, cobros, atención clínica, registros, perfil y cierre de sesión, entre otros. El catálogo JSP se verifica automáticamente.
- Se conservan los nombres existentes `csrfToken` y `tokenCsrf`; se rechazan parámetros duplicados o ambiguos y tokens de otra sesión.
- Sora envía `X-CSRF-Token` sin cambiar el contrato JSON ni consumir su cuerpo en el filtro. Se actualizó la versión del recurso JavaScript para evitar una copia anterior en caché.
- Los archivos multipart siguen sujetos a los límites del servlet. Se consulta explícitamente el parser para devolver 413 ante exceso de tamaño.
- `/api/v1/*` conserva su filtro propio y `/auth/google` su comprobación cookie/formulario de Google. Sus rutas vecinas no se eximen.
- Las vistas dinámicas entregan token con `Cache-Control: no-store`; recursos estáticos y salud no crean sesiones por este filtro. Un rechazo 403 muestra cómo volver a abrir el formulario.

## Verificación

1. Revisión del diff y `git diff --check`: sin errores de espacios. Los avisos LF/CRLF son del entorno Windows.
2. Pruebas enfocadas iniciales: 36 correctas (CSRF administrativo y general, API y vigencia de sesión).
3. `mvn -B -ntp -Dmaven.repo.local=C:\Users\adria\.m2\repository -Pintegracion-postgresql verify`: **BUILD SUCCESS**, 189 pruebas unitarias y 6 de integración, sin fallos ni omitidas.

La primera ejecución completa detectó que el límite de archivo de la prueba aislada (32 bytes) era menor que el token (43 caracteres). Se corrigió el fixture a 128 bytes y se mantuvo una carga de 256 bytes para comprobar el rechazo. No se aumentaron los límites de archivos de SoraCare.

`FiltroCsrfFormulariosTest` prueba solicitudes sin sesión/token, valores incorrectos, ajenos y duplicados, JSON, compatibilidad de nombres, excepciones de protocolo y presencia de un único campo en más de 50 formularios JSP.

`CsrfTomcatIT` monta un WAR mínimo con el filtro y la página 403 reales en Tomcat 10.1.57: formulario válido 200; sin token 403 con página recuperable; multipart válido con texto Unicode y archivo 200; token incorrecto 403; exceso de tamaño 413; formulario válido después del error 200. No utiliza cuentas, archivos clínicos ni integraciones reales. Las cinco pruebas PostgreSQL/Flyway usan contenedores aislados.

La investigación delegada previa no estuvo disponible por límite de uso y se sustituyó por una revisión separada del agente principal. La revisión independiente posterior del candidato sí terminó: no encontró bypass ni regresión concreta; no constituye una auditoría exhaustiva.

Despliegue local: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\iniciar-docker.ps1` terminó correctamente, recompilando y ejecutando las pruebas unitarias en Docker. Comprobaciones HTTP posteriores: salud 200, login 200 con token, página principal 200 con token para Sora, administración sin identidad 302, sesión REST sin identidad 401, POST a login sin token 403. Puerto publicado únicamente en `127.0.0.1:8081`; la base conservó su contenedor y datos. No se inició sesión con una cuenta real en estas comprobaciones.

## Límites y siguiente revisión

No se ejercitó en este cambio cada operación de negocio con cuentas reales ni se enviaron mensajes a OpenAI, Google o pacientes. Las pruebas demuestran el control compartido y sus formas de entrada, no la ausencia de cualquier vulnerabilidad en todo el proyecto.

Antes de dar por cerrado todo el bloque local queda consolidar la revisión de cookies/cabeceras, dependencias y configuración efectiva. Para un futuro despliegue público quedan dominio, TLS, secretos del servidor, respaldo fuera de esta PC y monitoreo del entorno elegido. No se modificó el destino de despliegue ni se habilitó acceso público.
