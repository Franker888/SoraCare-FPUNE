# Cierre de diagnósticos públicos — 13/09/2026

Estado: corregido, verificado y desplegado en Docker local.

Corrección del hallazgo de divulgación: se retiró `PersonaDAOServlet.java`, que
permitía consultar la existencia de cédulas sin autenticación. El archivo puede
recuperarse desde Git; no se eliminó el DAO ni datos de personas. La validación
de cédulas para registrar pacientes y médicos sigue usando `PersonaDAO`.

`ConexionServlet.java` conserva `/conexion-bd`, pero únicamente prueba `SELECT 1`
y devuelve `OK` (200) o `NO_DISPONIBLE` (503), como texto sin caché. No consulta
nombres de base/usuario ni refleja mensajes SQL. Docker y el script de arranque
consumen el estado HTTP, de modo que mantienen su contrato.

## Evidencia

- `ConexionServletTest`: tres pruebas aprobadas para éxito, excepción SQL con
  marcador sensible sintético y resultado inesperado. Los cuerpos son constantes.
- `mvn -Pintegracion-postgresql clean verify`: 176 pruebas unitarias y cinco
  de integración PostgreSQL aprobadas. Compilación limpia obligatoria para no
  conservar la clase eliminada en un WAR generado antes del cambio.
- Inspección del WAR: `PersonaDAOServlet.class` ausente.
- Tras reconstruir y actualizar Docker: contenedor saludable, `/conexion-bd`
  devuelve exactamente `OK` y HTTP 200; `/prueba-persona-dao?cedula=0` devuelve
  404. No se consultaron cédulas reales durante la comprobación.
- Investigación de compatibilidad independiente completada. Revisión final
  independiente interrumpida por límite de uso; comprobación final realizada
  por el agente principal sobre servlet, pruebas, rutas y consumidores.

El cierre cubre estas dos rutas, no certifica todas las rutas públicas del sistema.
