# Revocación de sesiones — 13/09/2026

Estado: corregido, verificado y desplegado en Docker local.

## Invariante

Una cuenta inactiva, bloqueada o eliminada no puede reutilizar su sesión para
acceder a paneles o API. Reactivar la cuenta exige un inicio de sesión nuevo.
Se valida al comenzar cada petición; no se cancelan operaciones ya admitidas.

## Cambio acotado

- `V039__revocar_sesiones_por_estado.sql`: versión persistente por cuenta; un
  trigger incrementa la versión al cambiar estado, incluso por otra instancia.
- `UsuarioDAO`, `Usuario` y `UsuarioAutenticadoDTO`: transportan esa versión.
- `AutenticacionService`: captura la versión durante autenticación local/Google.
- `SesionVigenteDAO` y `FiltroVigenciaSesion`: comprobación de estado y versión
  antes de todos los controladores; registro global y prioritario en `web.xml`.
  Rechazo HTML con redirección y API con JSON 401. Fallo de DB: 503 sin ejecutar
  la operación. Visitantes anónimos no consultan DB ni crean una sesión.
- `CambioContrasenaService`, `CambioContrasenaServlet` y `UsuarioDAO`: mantienen
  la versión de origen y la exigen en el UPDATE de contraseña. Una revocación y
  reactivación durante HIBP/BCrypt no pueden renovar la sesión antigua.
- `scripts/iniciar-local-etapa8.ps1`: incluye la migración para Tomcat local.

No modifica usuarios, contraseñas, citas ni datos clínicos existentes durante
la implementación. La migración agrega metadatos; no borra ni reemplaza datos.

## Verificación

- `git diff --check`: sin errores.
- `mvn package` después de cerrar el caso concurrente: BUILD SUCCESS,
  173 pruebas aprobadas, cero fallos/errores/omitidas; WAR generado.
- `mvn -Dtest=FiltroVigenciaSesionTest,FiltroSeguridadApiRestTest test`:
  23 pruebas aprobadas en la primera versión del cambio.
- El primer `mvn -Pintegracion-postgresql verify` se bloqueó porque Docker no
  estaba ejecutándose. Tras iniciarlo: BUILD SUCCESS, 173 pruebas unitarias y
  5 pruebas de integración aprobadas, cero fallos/errores/omitidas.
- Verificación conjunta con el cierre de diagnósticos públicos:
  `mvn -Pintegracion-postgresql clean verify`: BUILD SUCCESS, 176 unitarias y
  5 de integración aprobadas. WAR limpio: filtro y migración presentes,
  `PersonaDAOServlet.class` ausente.
- Se agregó a `PostgreSqlFlywayIT` la comprobación real de migración 039,
  desactivación, bloqueo, reactivación sin rehabilitar cookies previas, búsqueda
  por ID/identificador y cambio de contraseña con versión revocada frente a una
  versión vigente. Siempre usa PostgreSQL temporal y revierte datos sintéticos.
- `FiltroVigenciaSesionTest`: administrador no llega al POST de reactivación;
  paciente revocado no llega a API con CSRF válido; sesiones activas, anónimos y
  cambio obligatorio conservan su comportamiento; fallo DB y DTO inválido se
  rechazan. Una revisión independiente identificó el caso concurrente de cambio
  de contraseña, incluido después en el control SQL y la prueba de integración.

## Despliegue

Respaldo previo `respaldos/20260913-125305-76a89afd` creado y restaurado con
verificación correcta en contenedor aislado (esquema previo 038). Aplicar
migración y WAR juntos; no desplegar solo el WAR contra el esquema anterior.

`powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/iniciar-docker.ps1`:
correcto. Imagen reconstruida con 176 pruebas aprobadas; contenedor saludable;
Flyway aplicado hasta 039. HTTP local: login 200, administración anónima 302,
API de sesión anónima 401. No se desactivaron cuentas reales para probar: el caso
revocado se verifica en el filtro y en PostgreSQL temporal con datos sintéticos.

Esta corrección no constituye una auditoría completa de toda la aplicación.
La protección CSRF de todos los formularios, integraciones externas y condiciones
de producción siguen requiriendo revisión separada.
