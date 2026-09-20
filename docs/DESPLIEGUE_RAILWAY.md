# Despliegue de demostración en Railway

Esta guía publica SoraCare con HTTPS para que pueda abrirse desde computadoras
y teléfonos conectados a redes diferentes. Es un entorno de demostración; no
debe contener historias clínicas ni datos reales de pacientes.

## Arquitectura

- un servicio de aplicación construido con el `Dockerfile` del repositorio;
- un PostgreSQL administrado dentro de la red privada de Railway;
- un volumen persistente montado en `/var/lib/soracare`;
- un dominio `*.railway.app` con HTTPS automático.

La aplicación usa el puerto asignado por `PORT`, ejecuta Flyway antes de aceptar
tráfico y no inicia si PostgreSQL o las migraciones fallan. Railway puede iniciar
el contenedor como `root` para preparar el volumen, pero el proceso de Tomcat se
ejecuta después como el usuario sin privilegios `soracare` (`uid 10001`).

## 1. Crear el proyecto

1. Iniciar sesión en Railway y crear un proyecto vacío.
2. Agregar un servicio PostgreSQL.
3. Agregar el servicio de SoraCare desde el repositorio GitHub.
4. Seleccionar la rama estable que contiene este cambio.
5. Confirmar que Railway detecte el `Dockerfile` de la raíz.

No se necesita un archivo `railway.toml`. La configuración operativa se realiza
en el panel para evitar depender del formato Config-as-Code que Railway está
retirando.

## 2. Crear almacenamiento persistente

En el servicio SoraCare, agregar un volumen con este punto de montaje:

```text
/var/lib/soracare
```

Ese único volumen conserva los subdirectorios `documentos-clinicos` y
`fotos-perfil`. También se debe definir:

```text
RAILWAY_RUN_UID=0
```

El script de entrada prepara los permisos del volumen y baja a `uid 10001`
antes de iniciar Tomcat.

## 3. Configurar PostgreSQL

Si el servicio conserva el nombre predeterminado `Postgres`, cargar estas
variables en el servicio SoraCare. Si se renombra, reemplazar `Postgres` por el
nombre real en las referencias.

```text
SORACARE_DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}?ApplicationName=SoraCare-Railway
SORACARE_DB_USER=${{Postgres.PGUSER}}
SORACARE_DB_PASSWORD=${{Postgres.PGPASSWORD}}
```

No exponer el puerto de PostgreSQL como dominio público. Flyway creará y
actualizará el esquema `soracare` durante el arranque de la aplicación.

## 4. Generar el dominio HTTPS

En **Networking** del servicio SoraCare:

1. elegir **Generate Domain**;
2. copiar el origen completo, por ejemplo
   `https://soracare-demo.up.railway.app`;
3. guardar exactamente ese origen, sin una ruta final, en:

```text
SORACARE_PUBLIC_ORIGIN=https://dominio-generado.up.railway.app
SORACARE_SENTRY_ENVIRONMENT=railway-demo
```

No se debe definir `PORT`: Railway lo inyecta automáticamente. La variable
`SORACARE_PUBLIC_ORIGIN` solo acepta HTTPS en hosts públicos. Si está mal
escrita, SoraCare rechaza el arranque para no crear sesiones inseguras.

Configurar como healthcheck:

```text
/SoraCare-FPUNE/conexion-bd
```

Se recomienda un tiempo de espera inicial de 300 segundos para el primer build
y una política de reinicio ante fallos.

## 5. Integraciones opcionales

Las claves se cargan en Variables de Railway, nunca en Git. Solo agregar las
integraciones que se probarán en la exposición:

- `SORACARE_OPENAI_API_KEY` y `SORACARE_OPENAI_MODEL` para Sora;
- `SORACARE_RESEND_API_KEY` y `SORACARE_EMAIL_FROM` para correo;
- `SORACARE_GOOGLE_CLIENT_ID` para acceso con Google;
- variables Turnstile, Sentry y Firebase cuando estén activas.

Para Google Identity se debe añadir el nuevo origen HTTPS a **Orígenes de
JavaScript autorizados** en Google Cloud. Google Calendar permanece oculto en
la interfaz y no necesita configurarse para esta exposición.

SIFEN debe continuar con:

```text
SORACARE_SIFEN_AMBIENTE=DESHABILITADO
SORACARE_SIFEN_HOMOLOGACION_CONFIRMADA=NO
```

hasta disponer de credenciales, certificado y homologación reales.

## 6. Datos para la exposición

Usar una base limpia con cuentas y pacientes ficticios. No copiar la base local
si contiene correos, cédulas o información clínica real. Las contraseñas de
demostración deben ser distintas de cualquier contraseña personal y deben
cambiarse o eliminarse después del examen.

## 7. Verificación antes del examen

Realizar esta matriz desde dispositivos y redes diferentes:

1. abrir la portada, especialidades y médicos sin iniciar sesión;
2. registrar o usar un paciente ficticio y comprobar que la sesión se conserva;
3. agendar, reprogramar y cancelar una cita;
4. confirmar un pago simulado desde recepción y abrir su comprobante PDF;
5. iniciar como médico y comprobar agenda, atención e indicaciones;
6. iniciar como administrador y revisar usuarios, auditoría y reportes;
7. probar Sora y el correo si sus variables están configuradas;
8. comprobar que una cuenta desactivada pierda el acceso;
9. probar Chrome y Edge en computadora, además de Android o iPhone con datos
   móviles;
10. verificar `/SoraCare-FPUNE/conexion-bd` y los logs de Railway sin errores.

El día del examen conviene usar un plan con disponibilidad garantizada, hacer
un respaldo previo y conservar Docker local como alternativa. Un despliegue
real para clínicas requiere además dominio propio, revisión legal de datos de
salud, política de privacidad, monitoreo externo y respaldos restaurables.
