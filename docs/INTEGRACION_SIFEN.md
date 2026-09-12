# Integración e-Kuatia / SIFEN

## Alcance implementado

SoraCare conserva la factura interna comercial y el Documento Electrónico (DE)
como conceptos separados. La factura `FI-*` sigue sin validez tributaria. El DE
solo pasa a ser Documento Tributario Electrónico (DTE) cuando SIFEN responde que
fue aprobado.

El flujo técnico implementado permite:

1. tomar una factura interna emitida y perteneciente al médico autenticado;
2. seleccionar explícitamente IVA 10 %, IVA 5 % o exento;
3. seleccionar efectivo o transferencia, sin deducir información fiscal que el
   pago interno no contiene;
4. reservar de forma transaccional la numeración fiscal por ambiente,
   establecimiento y punto de expedición;
5. generar el CDC de 44 dígitos y su dígito verificador módulo 11;
6. calcular base gravada e IVA incluido;
7. generar el XML v150, firmarlo con RSA-SHA256 y agregar el QR con CSC;
8. validar el XML final contra los XSD oficiales guardados localmente;
9. conservar XML, hash, CDC, QR, importes y estados en PostgreSQL;
10. transmitir manualmente el DE mediante SOAP 1.2 al ambiente oficial de
    pruebas, usando autenticación TLS mutua con PKCS#12;
11. registrar el identificador secuencial del envío, código HTTP, protocolo,
    código, mensaje y XML de respuesta;
12. distinguir en la interfaz `VALIDADO`, `ENVIADO`, `APROBADO`, `RECHAZADO` y
    `ERROR`, sin presentar una validación local como aprobación tributaria.

La generación es idempotente: una factura interna no consume otra numeración
si ya posee una solicitud SIFEN. Los reintentos de transporte reutilizan el
mismo DE y CDC, y generan un nuevo identificador secuencial de envío.

## Protección de producción

La pantalla médica solo genera y transmite en el ambiente `PRUEBA`. Aunque las
variables indiquen `PRODUCCION`, ambas acciones permanecen bloqueadas. Además,
la configuración de producción no se considera completa sin:

```text
SORACARE_SIFEN_HOMOLOGACION_CONFIRMADA=SI
```

Esta variable es una barrera técnica adicional; no sustituye ninguna
habilitación de la DNIT.

## Requisitos externos pendientes

El software no puede inventar ni reemplazar estos requisitos:

1. habilitación de la entidad como facturador electrónico ante la DNIT;
2. RUC, actividad económica y domicilio fiscal reales del emisor;
3. timbrado electrónico, establecimiento y punto de expedición vigentes;
4. certificado cualificado con clave privada y permiso `clientAuth`;
5. IdCSC y CSC proporcionados para el ambiente correspondiente;
6. ejecución y aprobación de los escenarios exigidos por la guía de pruebas;
7. autorización formal antes de habilitar producción.

Nunca se deben pegar la clave privada, la contraseña del certificado ni el CSC
en código, commits, capturas o mensajes.

## Configuración

Las variables están enumeradas en `.env.docker.example`. Cuando se usa Docker,
el certificado debe montarse como archivo de solo lectura y la ruta visible para
Java debe ser la ruta interna del contenedor. Ejemplo para un
`compose.override.yaml` local y no versionado:

```yaml
services:
  aplicacion:
    volumes:
      - C:/ruta/privada/certificado.p12:/run/secrets/sifen.p12:ro
    environment:
      SORACARE_SIFEN_CERTIFICADO_PATH: /run/secrets/sifen.p12
```

## Verificación automatizada

La suite cubre el vector de CDC, cálculo fiscal, firma criptográfica, QR,
validación XSD, sobre SOAP 1.2, interpretación de aprobación/rechazo/error,
configuración sin exposición de secretos y las 36 migraciones sobre PostgreSQL
real mediante Testcontainers.

```powershell
mvn -P integracion-postgresql verify
```

## Trabajo futuro del módulo fiscal

- completar los escenarios formales de pruebas y homologación con credenciales
  reales de la entidad;
- habilitar el KuDE fiscal y su entrega al paciente después de la validación formal;
- completar conciliación persistente y análisis de eventos a partir de la consulta
  por CDC; implementar eventos de cancelación e inutilización;
- agregar recepción asíncrona por lotes si el volumen real lo justifica;
- habilitar producción solo mediante una decisión operativa auditada.

## KuDE de prueba (11 de septiembre de 2026)

El médico propietario puede abrir `/facturas/kude?id=ID_SOLICITUD` desde
**KuDE de prueba** en su listado de facturas. Solo admite solicitudes de ambiente
`PRUEBA`, estado `VALIDADO` o `APROBADO`, con XSD validado. Incluso cuando una
solicitud fue aprobada en pruebas, el PDF conserva **SIN VALIDEZ FISCAL**.
No se habilita ninguna operación de producción ni se envía correo al descargar.

El PDF se construye desde el XML firmado almacenado, no desde perfiles o tarifas
actuales. Comprueba SHA-256, XSD, CDC y correspondencia del destino QR con el
ambiente de pruebas. Conserva datos de emisor/receptor, timbrado, fecha, detalle,
afectación IVA, importes, totales y CDC agrupado; el QR se repite en cada página.
Los totales se mantienen juntos al final y los textos se ajustan al ancho.

Alcance: factura electrónica de servicios, contado, PYG; IVA gravado, exento o
exonerado. No admite crédito, otras monedas ni afectación mixta. La tipografía
admite caracteres latinos habituales; rechaza caracteres no representables sin
sustituir silenciosamente nombres fiscales. No se revalida aquí la cadena de
confianza del certificado: la firma pertenece al flujo de emisión existente.

Esta es una representación **para validar el módulo en pruebas**, no una
certificación de cumplimiento normativo. Antes de habilitar su uso fiscal deben
revisarse las notas técnicas/reglamentaciones vigentes y casos de homologación,
completar la entrega autorizada al paciente y los eventos/consultas pendientes.

Se usan PDFBox 3.0.8 para maquetación y ZXing 3.5.4 para el QR. Los ejemplos
sintéticos generados por las pruebas están en `target/kude-qa/`, fuera de Git y
del WAR. La base y los archivos originales no se modifican.

Validación local: 151 pruebas unitarias y 3 de integración PostgreSQL/Flyway
aprobadas. Incluye nueve pruebas nuevas de KuDE: permiso y propiedad, sesión,
cabeceras de descarga, integridad, estados/ambientes, lectura del QR y paginación.
Se revisaron visualmente un ejemplo de una página y otro de cuatro páginas.
La autorización de propiedad se prueba con DAO simulado; sigue pendiente el
recorrido autenticado completo con un DE real del ambiente oficial de pruebas.
Docker reconstruido con `clean package`: 151 pruebas aprobadas nuevamente.
Aplicación y PostgreSQL saludables; `/facturas/kude?id=1` sin sesión redirige al
login (302), mientras la API clínica privada devuelve 401. Sin migraciones nuevas.

Referencias del KuDE:

- [Manual técnico SIFEN v150, capítulo 13](https://www.dnit.gov.py/documents/20123/420592/Manual%2BT%C3%A9cnico%2BVersi%C3%B3n%2B150.pdf/e706f7c7-6d93-21d4-b45b-5d22d07b2d22?t=1687351495907).
- [Apache PDFBox](https://pdfbox.apache.org/download.html).
- [ZXing](https://github.com/zxing/zxing/releases).

## Consulta diagnóstica por CDC (11 de septiembre de 2026)

El botón **Consultar CDC en SIFEN Prueba** realiza `siConsDE` mediante SOAP 1.2
y autenticación mutua. Solo el médico propietario puede consultar su solicitud;
el CDC se toma de PostgreSQL, no de un campo editable ni de la URL del navegador.
Se exige sesión, rol MEDICO, cambio de contraseña completado y token CSRF válido.
Se reutiliza la configuración fiscal existente (que exige los datos completos).
No se habilitan consultas a producción ni se realizan llamadas al entrar al panel.

La respuesta se muestra separadamente del estado fiscal guardado:

- `0422`: encontrado, únicamente si el contenedor incluye el mismo CDC y protocolo;
- `0420`: no encontrado; no implica rechazo ni autorización para reenviar;
- `0421`: certificado sin permiso; no implica rechazo del documento;
- fallos HTTP, SOAP, estructura, transporte o CDC distinto: error de consulta.

La consulta informa cuántos contenedores de eventos llegaron, pero **no interpreta
todavía sus efectos fiscales**. Encontrado no equivale a vigencia confirmada.
No modifica estado, XML, respuesta de emisión, numeración fiscal ni intentos de
envío. Solo consume un identificador técnico de la secuencia existente para la
petición. El resultado se conserva ahora en el historial descrito a continuación;
la conciliación automática todavía no está habilitada.

Las respuestas tienen límite de 2 MB durante su recepción, tiempo máximo de
solicitud de 40 segundos y redirecciones deshabilitadas. El parser desactiva
entidades externas y verifica namespaces, estructura y campos únicos; la vista
escapa los mensajes antes de mostrarlos. No expone el XML devuelto ni secretos.

Referencia: [Manual técnico SIFEN v150, secciones 7.10 y 9.4](https://www.dnit.gov.py/documents/20123/420592/Manual%2BT%C3%A9cnico%2BVersi%C3%B3n%2B150.pdf/e706f7c7-6d93-21d4-b45b-5d22d07b2d22?t=1687351495907).
La prueba real de extremo a extremo requiere certificado y CDC de la entidad en
el ambiente oficial; los tests del cliente usan respuestas sintéticas, sin llamadas
fiscales reales. La habilitación y homologación siguen pendientes.

Verificación local: `mvn -Pintegracion-postgresql clean verify`, con 161 pruebas
unitarias y 3 de integración aprobadas, sin omisiones. Diez pruebas nuevas cubren
sobre SOAP, clasificación, CDC distinto, respuestas ambiguas y excesivas,
contención de entidades XML, permisos, CSRF y borrado de la contraseña temporal.

Despliegue Docker verificado: aplicación y PostgreSQL saludables, inicio y
conexión a BD con HTTP 200; facturas y consulta sin sesión redirigen al login,
y la API privada responde 401. Pendiente prueba autenticada con SIFEN real.

## Historial persistente de consultas

V037 agrega `consultas_sifen` sin modificar documentos ni estados fiscales.
Antes de contactar SIFEN se registra el identificador técnico, solicitud, médico,
CDC, ambiente PRUEBA y fecha de inicio. Si no puede guardarse el inicio, no se
consulta el servicio externo. Después se guarda el resultado, código, mensaje,
protocolo y cantidad de contenedores de eventos. No se almacena el XML ni secretos.

Una interrupción o fallo al guardar el resultado deja `SIN_RESULTADO`: puede estar
en curso o interrumpida, y no debe interpretarse como rechazo. No se reintenta
automáticamente. Un resultado finalizado no se sobrescribe desde este DAO.
Los errores de configuración previos a iniciar la consulta no crean historial.

Facturación muestra las últimas 50 consultas del médico, con hora de Paraguay.
La lectura comprueba también la propiedad actual del documento. Tras una consulta
guardada se redirige a GET para evitar repetirla al refrescar. No hay borrado ni
retención automática. Los registros anteriores a V037 no se reconstruyen.

La interpretación de eventos y la conciliación fiscal siguen pendientes; el
historial no certifica vigencia ni reemplaza la prueba oficial con certificado.

Verificado con Maven: 163 pruebas unitarias y 4 de integración, sin fallos ni
omisiones. PostgreSQL temporal aplica V001–V037; se prueba el registro y cierre,
aislamiento entre médicos, límite de 50, orden, rechazo de sobrescritura y estado
fiscal intacto. Tests de servicio cubren fallos de escritura antes/después de la
llamada y ausencia de reintentos. Ninguna prueba contactó SIFEN real.
Docker actualizado: V037 aplicada correctamente, inicio HTTP 200 y accesos a
facturación/consulta sin sesión redirigidos al login (302). Pendiente revisión
visual autenticada del historial y consulta con certificado oficial.

Enlaces generales:

- https://www.dnit.gov.py/web/e-kuatia/documentacion-tecnica
- https://www.dnit.gov.py/web/e-kuatia/desarrollo-de-software
- https://www.dnit.gov.py/web/e-kuatia/tablas-y-codificaciones
