# SoraCare-FPUNE

## Resumen de la evolución y estado actual del sistema

**Carrera y asignatura:** Ingeniería de Software II - FPUNE

**Equipo:** Gerardo Adrián González Brizuela, Tobías Fabián Sánchez Florentín,
María Victoria Fleitas Sosa y César Bacilis Olmedo Maidana

**Fecha de corte:** 31 de agosto de 2026

**Versión revisada:** commit `a5206db`

## 1. Resumen ejecutivo

SoraCare-FPUNE evolucionó desde una aplicación académica de administración
hospitalaria hasta una plataforma web integral para cuatro perfiles:
administrador, recepcionista, médico y paciente. El sistema centraliza la
agenda, la disponibilidad profesional, los pagos simulados, la atención y la
historia clínica, las indicaciones médicas, los documentos, las notificaciones
y los informes operativos.

La versión actual se ejecuta de forma reproducible con Docker Compose sobre
Java 21, Tomcat 10.1.57 y PostgreSQL 18.6. Flyway aplica 32 migraciones de base
de datos y Maven ejecuta 86 pruebas JUnit sin fallos. La solución combina
Jakarta Servlet y JSP para la aplicación web, Jakarta Persistence con Hibernate
para una migración JPA controlada y Spring MVC para una primera API REST.

El producto está funcional para demostración, evaluación académica y
continuación del desarrollo. Aún no debe presentarse como una instalación de
producción que procese dinero o emita documentos tributarios válidos: los
pagos actuales son simulados y sus comprobantes PDF no tienen validez fiscal.

## 2. Evolución del proyecto

### Etapa 1 - Base institucional y seguridad de acceso

- estructura por controladores, servicios, DAO, DTO y modelos;
- conexión con PostgreSQL y evolución del esquema mediante Flyway;
- autenticación local, contraseñas BCrypt, sesiones y autorización por roles;
- panel administrativo, usuarios, roles, sedes, consultorios y auditoría.

### Etapa 2 - Profesionales, especialidades y agenda

- gestión de médicos, especialidades, horarios y consultorios;
- búsqueda de disponibilidad por profesional, especialidad y fecha;
- prevención de reservas dobles y control de estados de citas;
- agenda operativa para recepción y autogestión inicial del paciente.

### Etapa 3 - Citas, pagos y comprobantes

- reserva, cancelación y reprogramación de citas;
- métodos académicos de pago: efectivo, QR, transferencia y tarjeta de prueba;
- confirmación administrativa de pagos y generación de comprobantes PDF;
- envío real de comprobantes por correo mediante Resend cuando está configurado.

### Etapa 4 - Atención e historia clínica

- agenda privada y flujo de atención del médico;
- guardado parcial y finalización de consultas;
- síntomas, antecedentes, examen físico, diagnóstico y tratamiento;
- signos vitales, evaluación del área afectada e historial clínico autorizado.

### Etapa 5 - Indicaciones, documentos y notificaciones

- varias indicaciones farmacológicas y estudios en una misma atención;
- generación y descarga de PDF de indicaciones médicas;
- almacenamiento externo de documentos clínicos y fotos de perfil;
- centro de notificaciones, avisos no leídos y suscripciones web preparadas;
- informes administrativos, métricas y análisis de demanda.

### Etapa 6 - Integraciones y endurecimiento

- acceso de pacientes con Google Identity; la sincronización con Google Calendar quedó aplazada;
- Cloudflare Turnstile para formularios públicos;
- Have I Been Pwned para rechazar contraseñas conocidas en filtraciones;
- Sentry preparado para monitoreo técnico sin datos personales ni clínicos;
- Sora con respuestas locales y conexión opcional a OpenAI desde el servidor;
- interfaz responsive revisada en teléfonos, tabletas y escritorio.

### Etapa 7 - Recomendaciones del profesor

- el médico puede administrar bloqueos totales o parciales de disponibilidad;
- las citas afectadas deben cancelarse con motivo o reprogramarse antes del bloqueo;
- Jakarta Persistence se aplicó de forma incremental mediante `SoraCarePU`;
- `JpaContexto` centraliza `EntityManager`, commit, rollback y liberación de recursos;
- Spring MVC expone una API REST v1 sin reemplazar la aplicación Servlet/JSP;
- la API protege recursos privados mediante sesión, roles y token CSRF.

### Etapa 8 - Ejecución reproducible y cierre actual

- Docker Compose levanta PostgreSQL, Flyway y SoraCare sin guardar claves en Git;
- el WAR se construye dentro de la imagen con las pruebas automatizadas;
- se corrigió que un bloqueo de otra fecha o desactivado ocultara turnos;
- la validación de disponibilidad pasó de una prueba manual a siete pruebas JUnit;
- se comprobó el sistema completo con 86 pruebas, migración V032 y rutas HTTP reales.

## 3. Estado funcional actual

### Administrador

Dispone de gestión de estructura institucional, médicos, especialidades,
horarios, medicamentos, usuarios, roles, configuración, auditoría, informes y
estado técnico de integraciones.

### Recepcionista

Puede registrar y buscar pacientes, consultar disponibilidad, reservar citas,
operar la agenda, revisar pagos y confirmar cobros en efectivo.

### Médico

Puede consultar su agenda, revisar espacios libres, bloquear su disponibilidad,
resolver citas afectadas, atender pacientes autorizados, mantener la historia
clínica y emitir indicaciones médicas en PDF.

### Paciente

Puede registrarse o ingresar con Google, buscar disponibilidad real, administrar
sus citas, elegir un medio de pago simulado, consultar comprobantes,
indicaciones, documentos y notificaciones. La sincronización con Google Calendar
se retiró del flujo activo y quedó anotada como mejora futura.

## 4. Arquitectura y persistencia

La aplicación mantiene un único WAR desplegado en Tomcat. Jakarta Servlet y
JSP atienden los portales; Spring MVC atiende exclusivamente `/api/v1/*`; los
servicios concentran las reglas de negocio y la persistencia usa JPA o JDBC
según el tipo de operación.

La unidad `SoraCarePU` administra cinco entidades: horarios médicos,
excepciones de horario, citas, estados de citas y medicamentos. JPA eliminó
SQL repetitivo y administración manual de recursos en operaciones independientes.
Por ejemplo, `HorarioMedicoDAO` se redujo de 736 a 189 líneas. JDBC se conserva
en informes y procesos clínicos o financieros que deben confirmar varias tablas
en una sola transacción. Flyway continúa siendo la única fuente del esquema y
JPA no crea tablas automáticamente.

## 5. API REST disponible

La versión actual ofrece:

- `GET /api/v1/especialidades`;
- `GET /api/v1/medicos`;
- `GET /api/v1/disponibilidad?fecha=AAAA-MM-DD`;
- `GET /api/v1/sesion`, para una sesión autenticada;
- `GET /api/v1/citas`, para el paciente autenticado.

Los datos personales internos no forman parte de los DTO públicos. Las citas
se obtienen desde la identidad de la sesión y no desde un identificador de
paciente enviado por el navegador. Las rutas desconocidas y los métodos no
permitidos se bloquean por defecto.

## 6. Integraciones: situación real

| Integración | Estado actual |
| --- | --- |
| Resend | Correo real y PDF adjunto probados; falta dominio institucional para producción. |
| Google Identity | Acceso de paciente existente probado; conviene repetir aceptación del alta nueva. |
| Google Calendar | Implementación técnica conservada, retirada de la interfaz y aplazada para una versión futura. |
| OpenAI | Conector servidor operativo y modo local de respaldo disponible. |
| Turnstile | Integrado y probado en inicio de sesión y registro. |
| Have I Been Pwned | Integrado sin enviar la contraseña ni su hash completo. |
| Sentry | Integración segura preparada; depende del DSN del entorno. |
| Firebase | Registro de dispositivos implementado; falta envío automático desde el servidor. |
| Twilio | Cliente y pantalla de prueba preparados; falta activación extremo a extremo. |
| Pagos | Simulación académica; no existe pasarela bancaria real. |

## 7. Seguridad aplicada

- contraseñas locales protegidas con BCrypt;
- autenticación, sesión y autorización por rol;
- protección CSRF en operaciones privadas;
- consultas parametrizadas y transacciones con rollback;
- acceso a historias, documentos y citas limitado al propietario o profesional autorizado;
- claves externas administradas únicamente mediante variables de entorno;
- límites de tamaño y formato en archivos y mensajes;
- errores REST en JSON sin detalles internos;
- auditoría de operaciones relevantes.

## 8. Evidencia de validación

- compilación y empaquetado WAR con Java 21 y Maven;
- 86 pruebas JUnit, sin fallos, errores ni pruebas omitidas;
- pruebas específicas de disponibilidad total, parcial, fecha, estado y citas canceladas;
- pruebas de persistencia JPA, configuración, transacciones y mapeos;
- pruebas de autenticación, permisos, aislamiento por paciente y CSRF de la API REST;
- PostgreSQL y SoraCare saludables en Docker;
- Flyway finalizado correctamente con las versiones V001 a V032;
- respuestas HTTP verificadas: recursos públicos 200, privados sin sesión 401 y escritura no permitida 405.

## 9. Trabajo pendiente

### Prioridad de producción

1. desplegar en infraestructura permanente con HTTPS y PostgreSQL administrado;
2. establecer copias de seguridad, recuperación, privacidad y términos de uso;
3. completar una revisión final de seguridad y accesibilidad antes de usar datos reales;
4. agregar integración continua y pruebas de integración contra PostgreSQL;
5. configurar dominios definitivos para Google, Resend y demás servicios externos.

### Integraciones pendientes

1. activar y probar Twilio SMS de extremo a extremo;
2. implementar el envío automático de Firebase Cloud Messaging desde el servidor;
3. decidir si el proyecto académico incorporará una pasarela de pagos real;
4. ampliar las pruebas de aceptación en dispositivos y navegadores reales.

### Ampliaciones previstas de la API REST

1. crear, cancelar y reprogramar citas mediante operaciones privadas seguras;
2. administrar disponibilidad del médico mediante recursos autenticados;
3. consultar indicaciones y documentos propios desde otro frontend o aplicación móvil;
4. exponer estados operativos estrictamente necesarios sin publicar datos internos.

Estas ampliaciones reutilizarán los servicios actuales, validarán roles y CSRF,
y evitarán duplicar las reglas de negocio en los controladores REST.

## 10. Módulo futuro de facturación electrónica

El nombre correcto del sistema paraguayo es **e-Kuatia**, integrado con el
Sistema Integrado de Facturación Electrónica Nacional (SIFEN) de la DNIT. Este
módulo todavía no está implementado. El comprobante PDF actual de SoraCare es
académico y debe mantenerse claramente separado de una factura tributaria.

Una implementación responsable requiere:

1. definir quién será el contribuyente emisor y confirmar su habilitación;
2. modelar establecimientos, puntos de expedición, timbrado, impuestos, ítems y eventos;
3. generar el Documento Electrónico en XML conforme a los XSD y tablas oficiales;
4. incorporar certificado cualificado, firma electrónica y autenticación requerida;
5. integrar los servicios de prueba de SIFEN, validar respuestas y manejar reintentos;
6. registrar aprobación, rechazo, consulta y eventos sin duplicar documentos;
7. generar el KuDE PDF del DTE aprobado y enviarlo al paciente por correo;
8. auditar el proceso y proteger certificados, claves y datos tributarios.

Antes de programarlo debe decidirse si corresponde e-Kuatia, orientado a
software propio, o e-Kuatia'i, el sistema gratuito para contribuyentes de menor
volumen. La decisión depende de la situación tributaria y operativa del
consultorio u organización destinataria.

Fuentes oficiales consultadas:

- DNIT, documentación técnica para desarrolladores: https://ekuatia.set.gov.py/web/e-kuatia/documentacion-tecnica
- DNIT, recomendaciones para desarrollo de software: https://www.dnit.gov.py/web/e-kuatia/desarrollo-de-software

## 11. Contenido de la entrega del código

El archivo ZIP contiene el núcleo funcional validado en el commit `a5206db`,
junto con la documentación de entrega, e incluye únicamente archivos
versionados del proyecto. No incluye base de datos, contraseñas, claves API,
`.env`, configuración local, dependencias descargadas ni la carpeta `target`.

Para iniciar el proyecto con Docker en Windows:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\iniciar-docker.ps1"
```

La aplicación queda disponible en:
`http://localhost:8081/SoraCare-FPUNE/`.

## 12. Conclusión

SoraCare cuenta con un núcleo hospitalario funcional, seguridad por roles,
agenda clínica, atención médica, documentación, informes, integraciones y un
entorno Docker reproducible. Las recomendaciones del profesor sobre
disponibilidad, Jakarta Persistence y API REST quedaron implementadas y
comprobadas. El siguiente salto no consiste en agregar funciones aisladas,
sino en completar la preparación para producción y diseñar la facturación
electrónica contra SIFEN como un módulo formal, probado y auditable.
