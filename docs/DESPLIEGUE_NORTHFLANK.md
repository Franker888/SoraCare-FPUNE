# Despliegue de demostración en Northflank

Esta guía publica SoraCare con HTTPS para que pueda abrirse desde computadoras,
teléfonos y redes diferentes. El destino es **Developer Sandbox**, pensado para
demostraciones y proyectos educativos, no para operar una clínica real ni
almacenar datos clínicos verdaderos.

Northflank exige un medio de pago para verificar la identidad incluso al elegir
el plan gratuito. Se debe seleccionar **Developer Sandbox** y no habilitar
recursos de pago. La cuenta permite, según la documentación vigente, dos
servicios, dos jobs y un addon.

## Arquitectura

- un servicio combinado conectado al repositorio de GitHub y construido con el
  `Dockerfile` de la raíz;
- un addon PostgreSQL privado dentro del mismo proyecto;
- un grupo de secretos que entrega la conexión de PostgreSQL y las integraciones
  al contenedor sin escribir credenciales en Git;
- un puerto HTTP público `8080`, publicado por Northflank mediante HTTPS;
- Flyway como única fuente del esquema.

La migración `V040` guarda los documentos clínicos y las fotos nuevas dentro de
PostgreSQL. Así, estos archivos no dependen del disco temporal del contenedor.
Las instalaciones anteriores que todavía tengan rutas locales continúan siendo
compatibles.

## 1. Crear la cuenta y el proyecto

1. Iniciar sesión en Northflank con GitHub.
2. Elegir el plan **Developer Sandbox**.
3. Completar personalmente la verificación que solicite Northflank.
4. Crear un proyecto llamado `soracare-demo` en `US East` o en la región más
   cercana disponible.

No se deben introducir contraseñas, códigos ni datos de tarjeta a través de
otra persona. La cuenta y el método de verificación quedan bajo control del
propietario.

## 2. Crear PostgreSQL

Dentro del proyecto:

1. elegir **Create new > Addon > PostgreSQL**;
2. usar el nombre `soracare-postgres`;
3. elegir PostgreSQL 18 o la versión estable predeterminada;
4. dejarlo privado, sin acceso público;
5. usar el recurso gratuito disponible en Developer Sandbox;
6. no activar redundancia, almacenamiento adicional ni opciones de pago.

Flyway creará el esquema `soracare` cuando la aplicación arranque por primera
vez. No hace falta importar la base de Railway para la exposición; se recomienda
trabajar con datos ficticios nuevos.

## 3. Conectar los secretos de PostgreSQL

Crear un grupo de secretos de ejecución llamado `soracare-runtime`, vincular el
addon y asignar estos alias:

| Secreto del addon | Alias para SoraCare |
| --- | --- |
| `JDBC_POSTGRES_URI` | `SORACARE_DB_URL` |
| `USERNAME` | `SORACARE_DB_USER` |
| `PASSWORD` | `SORACARE_DB_PASSWORD` |

Los nombres exactos se seleccionan desde **Show addons > Configure**. Si el
panel muestra variantes con prefijo, se debe elegir el URI JDBC y las
credenciales normales, no las de administración.

El grupo se aplica únicamente al servicio SoraCare. PostgreSQL no necesita un
dominio público.

## 4. Crear el servicio de aplicación

1. elegir **Create new > Combined service**;
2. usar el nombre `soracare`;
3. seleccionar el repositorio `Franker888/SoraCare-FPUNE`;
4. seleccionar la rama estable que contiene este despliegue;
5. elegir construcción mediante el `Dockerfile` de la raíz;
6. aplicar el grupo de secretos `soracare-runtime`;
7. seleccionar el recurso gratuito más pequeño disponible;
8. mantener una sola instancia.

El contenedor limita Java a 256 MB de heap, 128 MB de metaspace y 32 MB de
memoria directa para funcionar dentro del recurso gratuito sin sacrificar el
arranque seguro de Tomcat.

## 5. Publicar el puerto y configurar salud

Northflank detectará el `EXPOSE 8080` del `Dockerfile`. Verificar en
**Configure > Networking**:

```text
Nombre: http
Puerto: 8080
Protocolo: HTTP
Accesibilidad: Public
```

Northflank asignará un dominio `*.code.run` con TLS automático. El tráfico HTTP
se redirige a HTTPS.

Configurar una prueba HTTP de salud:

```text
Ruta: /SoraCare-FPUNE/conexion-bd
Puerto: 8080
Tiempo inicial recomendado: 300 segundos
```

La página pública será:

```text
https://DOMINIO.code.run/SoraCare-FPUNE/
```

## 6. Variables seguras de la aplicación

Después de conocer el dominio público, cargar en `soracare-runtime`:

```text
SORACARE_PUBLIC_ORIGIN=https://DOMINIO.code.run
SORACARE_SENTRY_ENVIRONMENT=northflank-demo
SORACARE_SIFEN_AMBIENTE=DESHABILITADO
SORACARE_SIFEN_HOMOLOGACION_CONFIRMADA=NO
```

Las integraciones disponibles se agregan como secretos de ejecución, nunca
como argumentos de construcción ni valores dentro del repositorio:

- `SORACARE_OPENAI_API_KEY` y, opcionalmente, `SORACARE_OPENAI_MODEL`;
- `SORACARE_RESEND_API_KEY` y `SORACARE_EMAIL_FROM`;
- `SORACARE_GOOGLE_CLIENT_ID`;
- `SORACARE_TURNSTILE_SITE_KEY` y `SORACARE_TURNSTILE_SECRET_KEY`;
- `SORACARE_SENTRY_DSN`;
- las variables públicas de Firebase usadas por notificaciones web.

Google Calendar permanece fuera del alcance de esta demostración. SIFEN sigue
deshabilitado hasta obtener certificado, RUC y homologación reales.

## 7. Ajustes externos por dominio

Cuando el dominio definitivo esté activo:

1. Google Cloud: añadir el origen HTTPS a los **Orígenes de JavaScript
   autorizados** del cliente OAuth web.
2. Cloudflare Turnstile: añadir solamente el hostname `*.code.run` concreto de
   SoraCare a la lista permitida.
3. Sentry: no requiere cambiar el DSN; usar el ambiente `northflank-demo`.
4. Resend: conservar el remitente verificado. Cada paciente recibe en su correo
   registrado; no se configura una dirección destinataria fija.

## 8. Verificación antes de compartir

La base debe contener solo datos ficticios. Validar desde una computadora y un
teléfono con datos móviles:

1. portada, especialidades y médicos sin iniciar sesión;
2. registro tradicional y acceso con Google;
3. sesión conservada al navegar por páginas públicas;
4. reserva, reprogramación y cancelación de una cita;
5. confirmación del pago simulado desde recepción;
6. descarga del comprobante PDF por el paciente;
7. agenda y atención del médico;
8. panel administrativo y revocación de una cuenta;
9. carga y descarga de un documento clínico y de una foto de perfil;
10. respuesta de Sora y correo transaccional cuando estén habilitados;
11. estado saludable en `/SoraCare-FPUNE/conexion-bd`;
12. reconstrucción manual del servicio seguida de una segunda descarga del
    archivo cargado, para confirmar su persistencia en PostgreSQL.

## 9. Encendido, apagado y respaldo

El servicio queda en línea mientras el proyecto y el plan Developer Sandbox
estén activos. No depende de que la notebook esté encendida. Para detenerlo se
puede pausar el servicio desde Northflank; para volver a publicarlo, se reanuda
el mismo servicio sin cambiar el enlace.

Antes de la exposición:

- comprobar que Northflank no muestra recursos facturables;
- exportar PostgreSQL o conservar una carga de datos demostrativos repetible;
- mantener Docker local como alternativa;
- no reutilizar contraseñas personales ni datos reales.

## Referencias oficiales

- [Precios y Developer Sandbox](https://northflank.com/docs/v1/application/billing/pricing-on-northflank)
- [Desplegar PostgreSQL](https://northflank.com/docs/v1/application/databases-and-persistence/deploy-databases-on-northflank/deploy-postgresql-on-northflank)
- [Conectar secretos de una base](https://northflank.com/docs/v1/application/databases-and-persistence/connect-database-secrets-to-workloads)
- [Construir y desplegar código](https://northflank.com/docs/v1/application/getting-started/build-and-deploy-your-code)
- [Configurar puertos públicos](https://northflank.com/docs/v1/application/network/configure-ports)
