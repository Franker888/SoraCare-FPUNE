# Esquemas oficiales SIFEN v150

Estos archivos se descargaron de los puntos de distribucion oficiales de la
DNIT/SIFEN el 1 de septiembre de 2026 para validar localmente los Documentos
Electronicos sin depender de la red durante la ejecucion.

- `siRecepDE_v150.xsd` y sus dependencias:
  `https://ekuatia.set.gov.py/sifen/xsd/`
- `WS_SiRecepDE_v150.xsd`, `protProcesDE_v150.xsd` y
  `SIFEN_Types_v141.xsd` describen el pedido y la respuesta del servicio
  sincrónico de recepción.
- `xmldsig-core-schema.xsd`:
  `https://www.w3.org/TR/xmldsig-core/xmldsig-core-schema.xsd`

Al esquema XMLDSig se le retiro solamente la declaracion `DOCTYPE`, que no es
necesaria para sus tipos y permitiria acceso externo durante el parseo. El
contenido normativo de elementos y tipos permanece sin cambios.

La documentacion normativa y las notas tecnicas vigentes siempre prevalecen:
`https://www.dnit.gov.py/web/e-kuatia/documentacion-tecnica`.
