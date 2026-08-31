# Cierre de las mejoras solicitadas por el profesor

Estado comprobado el 31 de agosto de 2026.

## 1. Disponibilidad administrada por el médico

El médico posee el módulo `Mi disponibilidad`, desde el cual puede registrar
bloqueos totales o parciales para una fecha. Un período bloqueado deja de
ofrecerse al reservar citas.

Si existen citas futuras dentro del período, SoraCare no las elimina en
silencio: exige cancelarlas o reprogramarlas antes de guardar el bloqueo. La
cancelación valida que la cita pertenezca al médico y genera la notificación
correspondiente para el paciente.

La agenda distingue fecha y estado de cada excepción. Por tanto, un bloqueo
pasado, de otra fecha o desactivado no puede ocultar turnos actuales. Una cita
cancelada vuelve a liberar su intervalo.

## 2. Mejor aprovechamiento de Jakarta y JPA

SoraCare utiliza Jakarta Servlet para la capa web y Jakarta Persistence 3.1
con Hibernate para el núcleo de agenda clínica. La unidad de persistencia
`SoraCarePU` administra cinco entidades relacionadas:

- horarios médicos;
- excepciones y bloqueos;
- citas;
- estados de citas;
- medicamentos.

`JpaContexto` concentra la creación y el cierre de `EntityManager`, además de
la confirmación y reversión de transacciones. Esto quitó código repetido de
conexiones, sentencias, resultados, `commit` y `rollback` en operaciones que
JPA puede resolver de forma segura.

La reducción es concreta: `HorarioMedicoDAO` pasó de 736 a 189 líneas durante
la migración. No se convirtieron automáticamente los 32 DAO. JDBC permanece en
informes y procesos que necesitan una única transacción con varias tablas,
como citas, pagos, historial y notificaciones.

Flyway sigue siendo la única fuente del esquema. JPA tiene deshabilitada la
creación automática de tablas para evitar diferencias entre instalaciones.

## 3. API REST con Spring MVC

Spring MVC 6.2 se incorporó dentro del mismo WAR, sin reemplazar Tomcat, los
Servlet ni las páginas JSP. La versión actual expone:

- `GET /api/v1/especialidades`;
- `GET /api/v1/medicos`;
- `GET /api/v1/disponibilidad?fecha=AAAA-MM-DD`;
- `GET /api/v1/sesion`, para una sesión autenticada;
- `GET /api/v1/citas`, limitado a las citas del paciente autenticado.

Los controladores reutilizan los servicios existentes y responden mediante
DTO que excluyen datos internos. La seguridad bloquea escrituras en catálogos,
recursos no declarados y accesos a citas sin el rol `PACIENTE`. Las futuras
operaciones de escritura privadas deben presentar el token CSRF de la sesión.

Esta API prepara a SoraCare para una aplicación móvil, otro frontend o una
integración institucional sin duplicar las reglas del sistema.

## 4. Evidencia de cierre

- WAR compilado con Java 21 y Maven.
- 86 pruebas JUnit aprobadas, sin fallos ni errores.
- Pruebas directas de bloqueos totales, parciales, fecha, estado y citas
  canceladas.
- Pruebas de configuración de `SoraCarePU`, mapeos JPA y transacciones.
- Pruebas de controladores Spring, permisos, aislamiento por paciente y CSRF.
- PostgreSQL, Flyway y SoraCare ejecutándose en Docker con estado saludable.

Con este alcance quedan implementados y comprobados los puntos indicados por
el profesor. Las ampliaciones futuras de JPA o REST deben responder a una
necesidad funcional concreta y no a convertir código estable de forma masiva.
