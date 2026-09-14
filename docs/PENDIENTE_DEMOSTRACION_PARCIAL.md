# Pendiente para el final: demostración desde otros dispositivos

Pedido del usuario — 14/09/2026. No implementado ni habilitado todavía.

Objetivo: iniciar SoraCare desde Docker en esta PC y permitir que compañeros y profesor ingresen desde otras computadoras y teléfonos durante el segundo parcial.

- Definir si todos estarán en la misma red del aula o también accederán desde otras redes. No son el mismo despliegue.
- Preparar un modo de demostración separado del modo local actual, con una dirección compartible y QR.
- Para una red local: verificar que la red permita comunicación entre equipos y limitar la regla de firewall al entorno acordado. No desactivar el firewall completo.
- Si se necesita Internet: elegir un acceso temporal HTTPS controlado; no publicar PostgreSQL ni abrir puertos del router sin evaluación y autorización.
- Usar datos ficticios y cuentas de demostración con permisos por rol. Evitar exponer historias clínicas reales, secretos y cuentas administrativas compartidas.
- Revisar Google Identity y las demás integraciones para la dirección efectiva; no asumir que las URL de localhost sirven en teléfonos.
- Probar concurrencia, inicio/cierre de sesión, permisos, citas y vista móvil. Definir cómo cerrar el acceso al terminar.
- La PC debe permanecer encendida y Docker activo durante la exposición.

Por ahora se conserva `127.0.0.1:8081`: este documento no abre acceso a la red ni a Internet. Implementar después del bloque de seguridad y de las tareas previas al cierre.
