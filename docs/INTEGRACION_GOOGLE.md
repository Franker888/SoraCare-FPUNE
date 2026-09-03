# Google Identity y Google Calendar

## Estado implementado

SoraCare separa correctamente dos permisos diferentes:

- Google Identity confirma quién es el paciente y entrega un ID token.
- Google Calendar requiere autorización OAuth adicional para crear o retirar eventos.

El acceso con Google continúa usando `SORACARE_GOOGLE_CLIENT_ID`. Para Calendar
se agregó un flujo OAuth 2.0 de servidor con acceso offline:

1. el paciente vincula Calendar una sola vez;
2. Google devuelve un authorization code;
3. el servidor intercambia el código por un refresh token;
4. el refresh token se cifra con AES-256-GCM antes de guardarlo;
5. las siguientes operaciones renuevan el access token desde el servidor;
6. cada evento queda relacionado con su cita y su paciente en PostgreSQL.

No se guardan access tokens en el navegador ni se escriben secretos en Git.
Las peticiones de guardar y retirar utilizan sesión, rol PACIENTE, CSRF y una
consulta de pertenencia de la cita.

Si solo existe el Client ID, SoraCare conserva temporalmente el flujo anterior
en el navegador. Ese modo puede pedir acceso nuevamente después de recargar la
página y se muestra explícitamente como modo local temporal.

## Configuración local necesaria

En Google Cloud debe utilizarse una credencial OAuth de tipo Web application,
con Google Calendar API habilitada.

Origen JavaScript local:

```text
http://localhost:8081
```

Redirect URI local exacta:

```text
http://localhost:8081/SoraCare-FPUNE/google/calendar/callback
```

Variables requeridas para el flujo de servidor:

```text
SORACARE_GOOGLE_CLIENT_ID
SORACARE_GOOGLE_CLIENT_SECRET
SORACARE_GOOGLE_CALENDAR_REDIRECT_URI
SORACARE_GOOGLE_TOKEN_ENCRYPTION_KEY
```

El Client Secret y la clave de cifrado deben configurarse fuera del repositorio.
La clave de cifrado es Base64 de 32 bytes aleatorios. Perder esa clave impide
descifrar autorizaciones ya guardadas y obliga a volver a vincular Calendar.

## Datos enviados a Calendar

El evento contiene solamente:

- especialidad;
- nombre del profesional;
- consultorio y sede;
- fecha y hora;
- identificador técnico de la cita.

No se envían el motivo de consulta, observaciones clínicas, documentos ni
información de facturación.

## Referencias oficiales

- OAuth 2.0 para aplicaciones de servidor:
  https://developers.google.com/identity/protocols/oauth2/web-server
- Crear eventos en Google Calendar:
  https://developers.google.com/workspace/calendar/api/guides/create-events
- Prácticas recomendadas de OAuth:
  https://developers.google.com/identity/protocols/oauth2/resources/best-practices
