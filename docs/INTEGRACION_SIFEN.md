# Integración e-Kuatia / SIFEN

## Estado actual

SoraCare emite facturas internas comerciales en PDF. Esos documentos no tienen
validez tributaria, no usan timbrado ni CDC y permanecen separados del flujo
fiscal.

La primera base de integración SIFEN ya está preparada:

- configuración por variables de entorno, sin secretos en Git ni en la base;
- validación visible de requisitos faltantes;
- ambientes `DESHABILITADO`, `PRUEBA` y `PRODUCCION`;
- trazabilidad en `solicitudes_sifen`, sin declarar aprobación tributaria;
- bloqueo conceptual: configurar variables no equivale a estar homologado.

## Requisitos externos

Antes de transmitir un Documento Electrónico se necesita, como mínimo:

1. habilitación de la entidad como facturador electrónico ante la DNIT;
2. timbrado electrónico, establecimiento y punto de expedición;
3. certificado cualificado de firma y su clave privada;
4. IdCSC y CSC para la generación del QR;
5. datos fiscales completos del emisor;
6. aprobación de las pruebas exigidas por SIFEN.

Nunca se deben pegar la clave privada, la contraseña del certificado ni el CSC
en código, commits, capturas o mensajes. El certificado debe montarse como un
archivo de solo lectura fuera del repositorio.

## Próximas etapas técnicas

1. modelar el Documento Electrónico según los XSD y catálogos oficiales;
2. generar el CDC y el XML canónico;
3. firmar el XML con el certificado cualificado;
4. validar localmente contra XSD;
5. transmitir únicamente al ambiente de pruebas mediante SOAP 1.2;
6. registrar códigos, protocolos y estados sin datos secretos;
7. generar el KuDE y comprobar eventos, consultas y contingencias;
8. habilitar producción sólo después de la homologación formal.

Documentación oficial:

- https://www.dnit.gov.py/web/e-kuatia/documentacion-tecnica
- https://www.dnit.gov.py/web/e-kuatia/desarrollo-de-software
- https://www.dnit.gov.py/web/e-kuatia/tablas-y-codificaciones
