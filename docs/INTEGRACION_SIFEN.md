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
configuración sin exposición de secretos y las 35 migraciones sobre PostgreSQL
real mediante Testcontainers.

```powershell
mvn -P integracion-postgresql verify
```

## Trabajo futuro del módulo fiscal

- completar los escenarios formales de pruebas y homologación con credenciales
  reales de la entidad;
- generar la representación gráfica KuDE fiscal y entregarla al paciente;
- implementar consulta por CDC, eventos de cancelación e inutilización;
- agregar recepción asíncrona por lotes si el volumen real lo justifica;
- habilitar producción solo mediante una decisión operativa auditada.

Documentación oficial:

- https://www.dnit.gov.py/web/e-kuatia/documentacion-tecnica
- https://www.dnit.gov.py/web/e-kuatia/desarrollo-de-software
- https://www.dnit.gov.py/web/e-kuatia/tablas-y-codificaciones
