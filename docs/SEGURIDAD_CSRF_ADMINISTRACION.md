# Protección CSRF de administración — 14/09/2026

## Alcance y resultado

Resultado: **fixed** para el control compartido de los formularios `/admin/*`, verificado y desplegado en Docker local. No se modificaron base de datos, permisos ni integraciones. No representa el cierre de todos los formularios del sistema.

Antes, un POST con una sesión administrativa podía activar, inactivar o desbloquear una cuenta sin un token que demostrara la intención del administrador. La explotación requiere que el navegador envíe su cookie; SameSite y el despliegue pueden limitar esa condición, pero no sustituyen el control de la aplicación.

El filtro administrativo existente ahora exige exactamente un `csrfToken` válido de la sesión antes de cualquier método distinto de GET, HEAD u OPTIONS. Mantiene primero las comprobaciones de identidad, cambio obligatorio de contraseña y rol. Reutiliza `ProteccionCsrf` (32 bytes aleatorios y comparación constante), prepara UTF-8 antes de leer parámetros y entrega el token a las vistas también cuando un POST legítimo vuelve a mostrar un error de validación.

Se añadieron campos ocultos a 18 formularios en 12 vistas: usuarios, configuración, médicos, especialidades, sedes, consultorios, horarios, excepciones, medicamentos y pruebas administrativas de correo, SMS y Sentry. No se cambió su diseño ni se enviaron mensajes externos durante las pruebas.

## Archivos

- `src/main/java/py/fpune/soracare/security/FiltroAutorizacionPorRol.java`: control central de `/admin/*`.
- `src/main/webapp/WEB-INF/views/admin/{usuarios,configuracion,medicos,especialidades,sedes,consultorios,horarios,excepciones-horarios,medicamentos,integracion-correo,integracion-sms,integracion-sentry}.jsp`: token oculto.
- `src/main/webapp/WEB-INF/views/errores/403.jsp`: mensaje específico de recuperación para CSRF; no refleja errores arbitrarios ni datos de entrada.
- `src/test/java/py/fpune/soracare/security/CsrfAdministrativoTest.java`: seis pruebas de regresión.

## Evidencia y pruebas

Maven se ejecutó con Java 21 y el repositorio local de dependencias existente.

1. Reproducción previa: `mvn -B -ntp -Dtest=CsrfAdministrativoTest test`. Dos casos: el legítimo pasó; el caso sin token falló como se esperaba (`expected 403 but was 200`). Solo mocks de sesión/filtro: ningún usuario real fue alterado.
2. Parche: `git diff --check -- src` sin errores; compilación y `mvn -B -ntp -Dtest=CsrfAdministrativoTest,FiltroVigenciaSesionTest,FiltroSeguridadApiRestTest test`: **29 pruebas aprobadas**.
3. Verificación completa: `mvn -B -ntp -Pintegracion-postgresql clean verify`: **182 pruebas unitarias y 5 de PostgreSQL/Flyway aprobadas**, sin fallos ni omisiones. El contenedor de pruebas es aislado de la base principal.
4. Revisión independiente del candidato: no encontró un bypass concreto. Detectó que la página 403 ocultaba el mensaje de recuperación; se confirmó en `web.xml`/JSP y se corrigió mediante un atributo booleano interno, manteniendo el mensaje de acceso restringido para errores de rol.
5. Tras ese ajuste final, `powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/iniciar-docker.ps1` reconstruyó desde limpio y repitió **182 pruebas unitarias aprobadas**. Docker quedó `healthy`. Comprobación HTTP sin credenciales: `/conexion-bd` 200 con cuerpo `OK`, `/login` 200, `/admin/usuarios` 302 al acceso y `/api/v1/sesion` 401. Estas comprobaciones no se presentan como una sesión administrativa interactiva.

Los casos cubren token ausente, vacío, incorrecto, de otra sesión y duplicado; POST/PUT/PATCH/DELETE en doce rutas; token válido; estabilidad en recarga; ausencia de caché; permisos y cambio obligatorio intactos. Un guard de mantenimiento verifica que los 18 formularios incluyan el campo. El POST sin token ya no alcanza la cadena de destino; el válido sí.

No se probaron cambios administrativos reales sobre la base principal ni un ataque desde otro sitio en el navegador. Las pruebas de filtro no equivalen a recorrer visualmente cada JSP autenticado. Los formularios que estuvieran abiertos antes de actualizar deben abrirse nuevamente para obtener el token.

## Operación y respaldo

El respaldo autorizado `respaldos/20260914-003121-d85526a0` terminó correctamente. Se restauró en un contenedor aislado, se comprobó Flyway 039 y se extrajeron y compararon archivos; el contenedor temporal se eliminó y SoraCare recuperó su salud. No se eliminaron respaldos anteriores.

## Pendiente: no es el cierre de toda la seguridad

- Extender la revisión y corrección a formularios no administrativos: paciente, recepción, atención médica, perfil, registro y cierre de sesión.
- Verificar especialmente uploads multipart y sus límites antes de introducir validación compartida allí.
- Mantener los contratos independientes de REST (`X-CSRF-Token`), Google Identity (`g_csrf_token`) y Calendar (`state`); no habilitar Calendar ni crear excepciones generales por prefijo.
- Comprobar TLS, cookies, proxy y demás condiciones de producción en el entorno final.

La revisión siguió Codex Security. Hubo auditor y arquitectura independientes; el investigador adicional previo al parche se interrumpió por límite de uso y su comprobación de compatibilidad se realizó en el agente principal. El revisor posterior sí terminó. Cobertura del escaneo: parcial de `src`, enfocada en CSRF; no auditoría completa de los 540 archivos.

Escaneo inicial: `785b2c82-ca96-49b7-a577-388e248d9516`, sobre `e050c5a`, antes del parche. El informe sellado conserva el hallazgo original y no se reescribe como si fuera una auditoría del código corregido.

- [Informe original](C:/Users/adria/AppData/Local/Temp/codex-security-scans-A0YvzA/SoraCare-FPUNE-GIT/e050c5acad2ed91af525405d8c4f61b4147bfacb_20260913T223750Z_tql46ljx/report.md)
- [Hallazgos](C:/Users/adria/AppData/Local/Temp/codex-security-scans-A0YvzA/SoraCare-FPUNE-GIT/e050c5acad2ed91af525405d8c4f61b4147bfacb_20260913T223750Z_tql46ljx/findings.json)
- [Cobertura](C:/Users/adria/AppData/Local/Temp/codex-security-scans-A0YvzA/SoraCare-FPUNE-GIT/e050c5acad2ed91af525405d8c4f61b4147bfacb_20260913T223750Z_tql46ljx/coverage.json)
- [Manifiesto](C:/Users/adria/AppData/Local/Temp/codex-security-scans-A0YvzA/SoraCare-FPUNE-GIT/e050c5acad2ed91af525405d8c4f61b4147bfacb_20260913T223750Z_tql46ljx/scan-manifest.json)

El plugin informó 4.496.729 tokens agregados para el escaneo, incluidos 4.163.968 de entrada en caché, en cinco tareas; es su medición del registro, no una estimación de facturación. Acceso Daybreak: `not_granted`, sin programas; algunos artefactos protegidos pueden no mostrarse. El informe se generó e indexó correctamente mediante el plugin.
