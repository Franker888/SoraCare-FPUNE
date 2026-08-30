# Contexto maestro de SoraCare-FPUNE

**Actualizado:** 30 de agosto de 2026

**Repositorio:** `TobiBug/SoraCare-FPUNE`

**Rama de este avance:** `codex/etapa8-reportes-administracion`

## 1. Qué es el proyecto

SoraCare-FPUNE es una aplicación web académica de gestión hospitalaria desarrollada para Ingeniería de Software II de la FPUNE. Centraliza la administración del centro médico, los pacientes, la agenda, los pagos simulados, la atención clínica, las indicaciones médicas, los documentos, las notificaciones y los informes.

El sistema ya funciona como una plataforma integral para cuatro perfiles: **administrador, recepcionista, médico y paciente**. Incluye una página pública responsive, autenticación tradicional, acceso de pacientes con Google y varias integraciones opcionales. Sigue siendo un proyecto académico: los pagos no procesan dinero real, los comprobantes no tienen validez fiscal y todavía falta preparar una infraestructura de producción permanente.

## 2. Tecnología y arquitectura

- Java 21.
- Jakarta Servlet 6 y JSP.
- Maven con empaquetado WAR.
- JUnit 5 y Maven Surefire para las validaciones automatizadas críticas.
- Apache Tomcat 10.1.57 para el entorno local.
- PostgreSQL como base de datos.
- JDBC y una separación pedagógica en controladores, servicios, DAO, DTO y modelos.
- Jakarta Persistence 3.1 con Hibernate ORM 6.6 para la migración incremental de los DAO; la unidad `SoraCarePU` administra horarios, excepciones, citas, estados de citas y medicamentos.
- Spring MVC 6.2 para la API REST incremental, integrado en el mismo WAR y compatible con Jakarta EE 10 y Tomcat 10.1.
- BCrypt para las contraseñas locales.
- HTML, CSS, JavaScript y Bootstrap 5.3.3 en la interfaz.
- PDF generado desde Java para comprobantes e indicaciones.
- Integraciones HTTP realizadas del lado del servidor con `HttpClient` cuando corresponde.

Estructura principal:

```text
src/main/java/py/fpune/soracare/
  api/          configuración, controladores, DTO y seguridad de la API REST v1
  config/       conexión y ciclo de vida de recursos
  dao/          acceso transaccional a PostgreSQL
  dto/          objetos de transferencia
  mvc/          controladores, modelos y utilidades web
  security/     filtros de autenticación, autorización y auditoría
  service/      reglas de negocio e integraciones

src/main/resources/db/migration/
  V001 ... V032 migraciones SQL acumulativas

src/main/resources/META-INF/
  persistence.xml unidad de persistencia SoraCarePU

src/main/webapp/
  WEB-INF/views/ vistas JSP privadas
  assets/        CSS, JavaScript, imágenes y librerías web
  index.jsp      página pública principal
```

El alcance de Jakarta Persistence queda cerrado deliberadamente en cinco entidades: horarios recurrentes, excepciones, citas, estados de citas y medicamentos. JPA se usa en consultas y operaciones independientes donde elimina SQL y administración manual de recursos; JDBC se mantiene en informes, consultas especializadas y procesos que deben compartir una sola transacción con varias tablas. No se busca convertir automáticamente los 32 DAO.

### API REST v1

Spring MVC convive con los Servlet y JSP existentes y atiende únicamente el prefijo `/api/v1/*`. Los recursos públicos permitidos son `GET /api/v1/especialidades`, con el catálogo activo; `GET /api/v1/medicos`, con el perfil profesional público y sus especialidades activas; y `GET /api/v1/disponibilidad?fecha=AAAA-MM-DD`, con filtros opcionales `idMedico` e `idEspecialidad`. Los tres reutilizan los servicios existentes. El DTO de médicos excluye cédula, correo, teléfono, dirección, fecha de nacimiento y demás datos internos. Disponibilidad aplica las reglas vigentes de horarios, bloqueos, excepciones, citas ocupadas y zona horaria de Asunción.

La seguridad REST reutiliza la sesión local `HttpSession`; no crea otro login, claves API ni JWT. `GET /api/v1/sesion` exige un usuario autenticado sin cambio de contraseña pendiente y devuelve solamente su nombre de usuario, roles y un token CSRF. Las rutas presentes o futuras bajo `/api/v1/citas` exigen además el rol `PACIENTE`; los métodos de escritura deben enviar el token en la cabecera `X-CSRF-Token`. El filtro Servlet usa una lista explícita de rutas y métodos, por lo que bloquea por defecto recursos no declarados y escrituras sobre los catálogos públicos. La API usa errores JSON sin detalles técnicos y monitoreo mediante Sentry cuando corresponde. No se incorporó Spring Boot ni se reemplazó el despliegue WAR en Tomcat.

## 3. Perfiles y funciones

### Administrador

- Centro administrativo con métricas operativas.
- Gestión de sedes, consultorios, especialidades, médicos, horarios y excepciones.
- Catálogo de medicamentos.
- Administración de usuarios, estados de acceso, roles y permisos visibles.
- Auditoría con filtros por fecha, evento, resultado, módulo y búsqueda.
- Configuración institucional e historial de cambios.
- Estado técnico del sistema y la base de datos.
- Informes de demanda, citas, pagos, pacientes nuevos, especialidades y médicos.
- Curva diaria y mapa de concentración por día y hora.
- Paneles de integración para correo y SMS.

### Recepcionista

- Centro de recepción con indicadores del día.
- Registro y búsqueda de pacientes por cédula.
- Consulta interna de disponibilidad de médicos.
- Agenda operativa y reserva de citas para pacientes.
- Consulta de citas por fecha.
- Gestión de pagos y comprobantes, con filtros por cédula y método.
- Confirmación de pagos en efectivo.

### Médico

- Panel profesional con resumen de agenda.
- Agenda del día automática y consulta de otras fechas.
- Consulta de horarios libres propios.
- Gestión de bloqueos totales o parciales de su propia disponibilidad.
- Revisión y cancelación explícita de citas afectadas antes de guardar un bloqueo, con notificación al paciente.
- Acceso únicamente a pacientes vinculados con sus citas.
- Inicio, guardado parcial y finalización de atención.
- Historia clínica previa y documentos autorizados del paciente.
- Registro de síntomas, antecedentes, examen físico, diagnóstico, tratamiento, indicaciones y próximo control.
- Signos vitales: temperatura, frecuencia cardíaca, frecuencia respiratoria, presión arterial y saturación de oxígeno.
- Evaluación del área afectada.
- Antecedentes patológicos personales, fármacos utilizados y alergias.
- Varias indicaciones farmacológicas en una misma atención.
- Solicitud de estudios clínicos.
- Generación de PDF de indicaciones médicas.

### Paciente

- Registro de cuenta y acceso tradicional.
- Acceso o alta mediante una cuenta Google verificada.
- Panel personalizado con foto o avatar.
- Búsqueda por especialidad, médico y fecha realmente disponible.
- Autogestión de citas y prevención de doble reserva del mismo turno.
- Historial de citas activas, confirmadas y atendidas.
- Cancelación y reprogramación según las reglas del sistema.
- Selección de forma de pago y consulta de estado.
- Acceso al comprobante PDF.
- Consulta de indicaciones médicas y descarga en PDF.
- Carga y consulta de documentos clínicos PDF, JPG, JPEG o PNG.
- Centro de notificaciones con contador de avisos no leídos.
- Incorporación o retiro de una cita en Google Calendar.
- Eliminación de la cuenta con confirmación explícita.

## 4. Agenda y disponibilidad

La agenda se construye a partir de horarios recurrentes por médico, especialidad y consultorio. Las excepciones permiten bloqueos totales, bloqueos parciales o disponibilidad extraordinaria. La lista de fechas que ve el paciente se limita a días atendidos por el profesional seleccionado. El médico administra sus propios bloqueos desde `/medico/mi-disponibilidad`; si el período contiene citas futuras, primero debe cancelarlas con un motivo o reprogramarlas y el sistema nunca las elimina silenciosamente.

El sistema evita que dos pacientes reserven el mismo médico, fecha y turno. La recepcionista puede consultar disponibilidad interna y el médico puede consultar sus espacios libres. Las búsquedas de disponibilidad se registran de forma analítica y sin almacenar el texto clínico del paciente. Los DAO de horarios recurrentes y excepciones usan entidades y JPQL; `JpaContexto` centraliza la apertura, cierre, confirmación y reversión de las transacciones locales de `SoraCarePU`, reutilizando el conjunto de conexiones seguro existente. Las lecturas independientes de citas y estados también usan JPA y la relación `Cita` a `EstadoCita`; las escrituras de citas permanecen en JDBC para conservar su transacción conjunta con pagos, notificaciones, historial y bloqueos. Flyway continúa siendo la única fuente del esquema y JPA tiene deshabilitada la generación de tablas.

## 5. Pagos y comprobantes

Los métodos implementados son:

- Efectivo.
- Código QR.
- Transferencia bancaria.
- Tarjeta de prueba.

Todo el circuito es una **simulación académica**. No existe conexión con bancos, procesadores o tarjetas reales. El pago en efectivo mantiene la cita pendiente hasta la confirmación de recepción. Los medios digitales muestran flujos simulados y, al confirmarse, generan un comprobante PDF. Los reembolsos y anulaciones también quedan reflejados en los estados administrativos.

## 6. Historia clínica, documentos e indicaciones

La atención médica cerrada queda incorporada al historial del paciente. Una atención no puede iniciarse antes de la fecha de la cita y los campos clínicos obligatorios deben completarse para finalizarla.

Los documentos clínicos se guardan fuera del WAR en un directorio configurable. Sólo el paciente propietario y los profesionales autorizados pueden consultarlos. Las fotos de perfil siguen el mismo principio de almacenamiento externo.

Las antiguas “recetas” se presentan al usuario como **indicaciones médicas**. Una atención puede incluir varios medicamentos con dosis, frecuencia, duración, vía y observaciones, además de estudios clínicos solicitados. El paciente puede abrir o descargar el PDF consolidado.

El catálogo de medicamentos usa una entidad JPA para altas, listados y cambios de estado. La consulta por conexión se conserva en JDBC únicamente dentro de la emisión de recetas, de modo que la validación del medicamento, la receta y su notificación sigan confirmándose o revirtiéndose como una sola transacción clínica.

## 7. Paneles, diseño y compatibilidad

- Página pública con médicos, especialidades, disponibilidad, registro e inicio de sesión.
- Paneles diferenciados para administrador, recepción, médico y paciente.
- Diseño visual coherente con SoraCare, tarjetas, estados, iconografía y navegación contextual.
- Imágenes optimizadas para reducir el tiempo de carga.
- Reglas responsive compartidas para teléfonos, tabletas y escritorio.
- Formularios, calendarios, tablas y controles ajustados para pantallas pequeñas.
- Tablas administrativas anchas dentro de contenedores con desplazamiento controlado.
- Auditoría visual realizada en anchos de 320, 390, 768 y 1366 píxeles.

Las rutas públicas `/`, `/medicos`, `/especialidades`, `/login` y `/registro` fueron comprobadas sin desbordamiento horizontal del cuerpo de la página. También se revisaron paneles protegidos en resolución móvil.

## 8. Integraciones externas

### Resend: operativo y probado

- Envío real de correo desde el servidor.
- Comprobante de pago adjunto en PDF.
- Soporte para envío de indicaciones médicas en PDF.
- La clave nunca se guarda en el repositorio.
- Variables: `SORACARE_RESEND_API_KEY` y `SORACARE_EMAIL_FROM`.

La prueba real de correo y la recepción de un comprobante PDF fueron verificadas.

### Google Identity: operativo para pacientes

- Inicio de sesión con Google para pacientes existentes.
- Registro complementario para una identidad Google nueva: Google aporta la identidad y el usuario completa sus datos hospitalarios.
- Vinculación segura de una cuenta local preexistente cuando corresponde.
- El token de identidad se valida en el servidor y debe corresponder al Client ID configurado.
- Variable: `SORACARE_GOOGLE_CLIENT_ID`.

El acceso con una cuenta existente fue probado. El alta nueva posee su formulario, aunque conviene incluirla nuevamente en una prueba de aceptación antes de producción.

### Google Calendar: operativo y probado

- El paciente puede agregar, actualizar o retirar su cita de su calendario personal.
- Solicita permiso limitado a eventos de Calendar.
- No copia información clínica sensible al evento.
- Requiere habilitar Google Calendar API y configurar los orígenes autorizados del dominio usado.

### Twilio SMS: preparado, pendiente de activación completa

- Existe un cliente para Twilio Programmable Messaging y una pantalla administrativa de prueba.
- Variables: `SORACARE_TWILIO_ACCOUNT_SID`, `SORACARE_TWILIO_AUTH_TOKEN` y `SORACARE_TWILIO_FROM`.
- La activación completa quedó aplazada por las restricciones de la cuenta de prueba y debe verificarse antes de prometer recordatorios reales.

### Sora y OpenAI: asistente local operativo, IA remota opcional

- Sora aparece en la página pública con avatar y guía al visitante por la plataforma.
- Posee respuestas locales seguras cuando OpenAI no está configurado o no está disponible.
- El conector opcional usa la Responses API desde el servidor, nunca desde el navegador.
- Variables: `SORACARE_OPENAI_API_KEY` y `SORACARE_OPENAI_MODEL`.
- La activación remota quedó pendiente de crédito o facturación en la cuenta de API.
- Sora no diagnostica, no receta, no interpreta estudios y no solicita datos sensibles.

### Cloudflare Turnstile: operativo y probado

- Protege el inicio de sesión tradicional y el registro público de pacientes.
- El navegador muestra el desafío, pero la autorización siempre se valida nuevamente desde el servidor.
- Los tokens se comprueban con la acción esperada (`login` o `registro`) para impedir que un token de un formulario se reutilice en el otro.
- Sin claves, el entorno local continúa funcionando para no bloquear el desarrollo; una configuración parcial se rechaza por seguridad.
- Variables: `SORACARE_TURNSTILE_SITE_KEY` y `SORACARE_TURNSTILE_SECRET_KEY`.
- Configuración local: `scripts\configurar-turnstile-local.ps1`.
- La clave secreta nunca se incorpora al código, al WAR ni a GitHub.
- La validación real fue comprobada en el inicio de sesión y el registro público.

### Have I Been Pwned Passwords: integrado

- Las contraseñas nuevas se comparan con el catálogo público Pwned Passwords antes de guardarse.
- Se aplica al registro público, al cambio de contraseña y a las cuentas locales creadas por administración.
- SoraCare calcula SHA-1 localmente y envía solamente los primeros cinco caracteres del hash mediante el modelo de privacidad `k-anonymity`; nunca envía la contraseña ni el hash completo.
- La consulta utiliza `Add-Padding: true` y no requiere clave API.
- Una contraseña filtrada se rechaza con un mensaje claro. Si el servicio no puede responder, se bloquea únicamente el alta o cambio de esa contraseña; el acceso de las cuentas existentes continúa funcionando.

### Sentry: preparado para monitoreo técnico seguro

- Captura errores no controlados del servidor mediante el SDK oficial de Java.
- El servicio es opcional: si no existe un DSN, SoraCare inicia y funciona normalmente sin monitoreo remoto.
- Antes de enviar un error, reemplaza su mensaje original por una descripción técnica genérica y conserva únicamente la pila de ejecución.
- Sólo agrega el tipo de excepción, el método HTTP y una ruta normalizada sin parámetros ni identificadores numéricos.
- No adjunta usuario, sesión, IP, cookies, encabezados, formularios, nombres, correos, cédulas, contraseñas ni información clínica.
- Las trazas de rendimiento y el envío predeterminado de información personal permanecen desactivados.
- Variables: `SORACARE_SENTRY_DSN` y `SORACARE_SENTRY_ENVIRONMENT`.
- Configuración local: `scripts\configurar-sentry-local.ps1`.
- El administrador dispone de `/admin/integraciones/sentry` para comprobar la conexión mediante un evento controlado.

### Firebase Cloud Messaging: suscripciones web preparadas

- El paciente puede autorizar o desactivar avisos para su navegador desde el centro de notificaciones.
- La configuración pública de Firebase y la clave VAPID se cargan mediante variables de entorno; no se guarda una cuenta de servicio en el repositorio.
- Los tokens de dispositivos se almacenan en `suscripciones_push` mediante la migración `V032` y cada cambio exige sesión de paciente más un token anti-CSRF.
- El service worker muestra avisos generales y abre el centro de notificaciones.
- Variables: `SORACARE_FIREBASE_API_KEY`, `SORACARE_FIREBASE_AUTH_DOMAIN`, `SORACARE_FIREBASE_PROJECT_ID`, `SORACARE_FIREBASE_STORAGE_BUCKET`, `SORACARE_FIREBASE_MESSAGING_SENDER_ID`, `SORACARE_FIREBASE_APP_ID` y `SORACARE_FIREBASE_VAPID_KEY`.
- Configuración local: `scripts\configurar-firebase-local.ps1`.
- El registro del dispositivo está implementado; el envío automático desde el servidor todavía no forma parte del flujo. Las pruebas de recepción se realizan manualmente desde Firebase Console.

### Cloudflare Quick Tunnel: sólo demostración temporal

Se utilizó un túnel temporal para que otras personas probaran la aplicación desde teléfonos y computadoras. La dirección cambia al reiniciarlo y puede mostrar el error 1033 cuando el conector se detiene. No debe considerarse hosting permanente. Para una entrega pública estable hace falta desplegar Tomcat y PostgreSQL en infraestructura persistente o contratar un hosting compatible.

## 9. Seguridad aplicada

- Contraseñas locales almacenadas con BCrypt.
- Rechazo de contraseñas nuevas presentes en filtraciones conocidas mediante Pwned Passwords.
- Sesiones y filtros de autenticación.
- Autorización por rol y protección de portales.
- Restricción de historias, documentos, comprobantes y citas al propietario o profesional autorizado.
- Auditoría de operaciones relevantes.
- Transacciones para altas y cambios que modifican varias tablas.
- Consultas parametrizadas.
- Validación del token de Google en el servidor.
- Claves externas sólo mediante variables de entorno.
- Límites de tamaño y formato para documentos, fotos y mensajes.
- La eliminación de una cuenta se realiza mediante un flujo autenticado y confirmado.

Este proyecto no debe publicar contraseñas de prueba, claves API, tokens OAuth ni la contraseña de PostgreSQL. Las credenciales se administran fuera de Git.

## 10. Base de datos

El esquema se llama `soracare` y evoluciona mediante migraciones SQL numeradas de `V001` a `V032`. Entre ellas se encuentran:

- Personas, pacientes, usuarios y roles.
- Auditoría.
- Médicos, especialidades, sedes y consultorios.
- Horarios, excepciones, citas, pagos y comprobantes.
- Historia clínica, atención, recetas/indicaciones, medicamentos y notificaciones.
- Documentos, informes y configuración.
- Perfil clínico, signos vitales y evaluación del área afectada.
- Reprogramaciones, presentación de médicos y estudios solicitados.
- Analítica de disponibilidad, perfiles visuales, estados de correo e identidades Google.

Las nuevas migraciones de este avance son `V027` a `V032`. `V032` crea las suscripciones web de Firebase. No se deben editar migraciones ya aplicadas; los cambios futuros se agregan con el siguiente número disponible.

## 11. Cómo iniciar el proyecto en la notebook actual

Abrir PowerShell o la terminal de IntelliJ en la carpeta del proyecto y ejecutar:

```powershell
Set-Location "C:\Users\adria\Desktop\Hospital\Proyecto\SoraCare-FPUNE-GIT"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\iniciar-local-etapa8.ps1"
```

Para activar Turnstile por primera vez, ejecutar antes:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\configurar-turnstile-local.ps1"
```

El configurador solicita la clave pública y oculta la clave secreta. Después se reinicia SoraCare con el comando habitual de `iniciar-local-etapa8.ps1`.

El script:

1. Solicita de forma oculta la contraseña de `soracare_app`.
2. Comprueba PostgreSQL.
3. Aplica las migraciones pendientes de esta etapa.
4. Carga las integraciones configuradas como variables del usuario de Windows.
5. Compila el WAR con Java 21 y Maven.
6. Detiene el Tomcat anterior si existe.
7. Despliega `SoraCare-FPUNE.war` y vuelve a iniciar Tomcat.
8. Comprueba la aplicación y prepara las vistas públicas más usadas.

Direcciones locales:

- Página principal: `http://localhost:8080/SoraCare-FPUNE/`
- Inicio de sesión: `http://localhost:8080/SoraCare-FPUNE/login`

Directorios externos usados en la notebook actual:

- Documentos: `C:\Users\adria\Desktop\Hospital\Datos\SoraCare\documentos-clinicos`
- Fotos: `C:\Users\adria\Desktop\Hospital\Datos\SoraCare\fotos-perfil`

## 12. Validación realizada

- Compilación Maven y generación del WAR.
- Pruebas manuales por roles.
- Registro e inicio de sesión local.
- Acceso con Google de un paciente existente.
- Reserva, bloqueo de turnos duplicados, cancelación y reprogramación.
- Flujos simulados de pago y comprobante PDF.
- Correo real de comprobante mediante Resend.
- Agenda, atención, guardado parcial y finalización médica.
- Historia clínica, varios medicamentos, estudios solicitados y PDF de indicaciones.
- Carga y apertura de documentos clínicos.
- Notificaciones y permisos 403.
- Informes, auditoría, configuración, usuarios y roles administrativos.
- Google Calendar: agregar y retirar cita.
- Validación aislada de Turnstile: modo local, configuración incompleta, token aceptado, token inválido y acción incorrecta.
- Sesenta pruebas JUnit ejecutadas por Maven para BCrypt, Google, citas, pagos, tarjetas de prueba, HIBP, Sentry, Turnstile, protección anti-CSRF, disponibilidad del médico, validación de horarios recurrentes, uso seguro del contexto JPA, configuración de la unidad de persistencia, mapeo JPA y autenticación, autorización y CSRF de la API REST con Spring MVC.
- Rutas públicas y paneles en resoluciones móviles y de escritorio.

El `pom.xml` incorpora JUnit 5 y Maven Surefire. `mvn clean test` debe informar una cantidad mayor que cero; una compilación sin ese resumen no reemplaza la evidencia de pruebas. Las pruebas manuales, de integración con PostgreSQL y de servicios externos siguen documentándose por separado.

## 13. Pendientes recomendados

1. Hosting permanente con HTTPS y base de datos administrada.
2. Configuración de producción para Google OAuth y Calendar, con dominio definitivo.
3. Dominio verificado para Resend y remitente institucional.
4. Activación y prueba extremo a extremo de Twilio SMS.
5. Crédito de API y elección de un modelo público válido antes de activar OpenAI para Sora.
6. Integración con una pasarela de pago real sólo si el alcance académico cambia.
7. Política de privacidad, términos, copias de seguridad y estrategia de recuperación.
8. Ampliar las pruebas JUnit de integración con PostgreSQL, agregar integración continua y probar con más dispositivos reales.
9. Revisión final de accesibilidad y seguridad antes de exponer datos reales.
10. Implementar la primera operación REST de citas usando exclusivamente la identidad del paciente autenticado y la base de seguridad ya establecida.
11. Dockerizar Tomcat, PostgreSQL y la configuración local después de cerrar las mejoras solicitadas por el profesor.

## 14. Instrucciones para continuar en otro chat

Entregar este archivo al nuevo asistente y usar un mensaje inicial parecido a este:

> Leé completo el archivo `CONTEXTO_MAESTRO_SORACARE.md`. Trabajamos sobre SoraCare-FPUNE. Antes de cambiar código, verificá la rama actual, `git status`, las migraciones y las rutas relacionadas. Conservá la arquitectura Java/JSP/PostgreSQL existente, hacé una modificación coherente por vez, compilá y validá el flujo. No publiques claves ni contraseñas y no hagas commit o push sin mi autorización explícita.

El nuevo chat debe tratar este documento como contexto de orientación y comprobar el estado real del repositorio antes de actuar, porque la rama y los pendientes pueden seguir cambiando.
