# SoraCare FPUNE

<p align="center">
  <img src="src/main/webapp/assets/home/img/logo/SoraCare1.png" alt="Logo de SoraCare" width="170">
</p>

SoraCare es una aplicación web de gestión para centros médicos desarrollada como proyecto académico en la Facultad Politécnica de la Universidad Nacional del Este. Reúne agenda, atención clínica, pagos simulados, documentos, notificaciones y administración en una sola aplicación con acceso diferenciado para pacientes, médicos, recepción y administración.

**Demostración pública:** [soracare-production.up.railway.app/SoraCare-FPUNE](https://soracare-production.up.railway.app/SoraCare-FPUNE/)

> La demostración no debe utilizarse con datos clínicos reales. Los pagos son simulados y los comprobantes o documentos SIFEN no tienen validez fiscal.

## Funciones principales

- Registro e inicio de sesión tradicional, además de Google Identity para pacientes.
- Agenda por médico, especialidad, consultorio y disponibilidad real.
- Bloqueos totales o parciales de horarios y control de citas afectadas.
- Reserva, cancelación y reprogramación de citas sin doble asignación de turnos.
- Atención médica, historia clínica, indicaciones, medicamentos y estudios solicitados.
- Generación y descarga privada de comprobantes e indicaciones en PDF.
- Gestión de pagos simulados y facturación comercial interna.
- Notificaciones dentro de la aplicación y envío de correos transaccionales.
- Informes administrativos de citas, pagos, pacientes, médicos y especialidades.
- Asistente Sora con respuestas locales y conexión opcional a OpenAI desde el servidor.
- API REST para catálogos públicos y operaciones privadas del paciente y del médico.

## Perfiles del sistema

| Perfil | Operaciones principales |
| --- | --- |
| Paciente | Consultar médicos, reservar citas, revisar pagos, documentos e indicaciones |
| Médico | Administrar disponibilidad, atender citas y emitir indicaciones médicas |
| Recepción | Gestionar agenda, pacientes y confirmaciones de cobro |
| Administración | Mantener usuarios, catálogos, reportes y configuración operativa |

## Tecnologías

- Java 21, Jakarta Servlet 6 y JSP
- Jakarta Persistence 3.1 e Hibernate ORM 6.6
- Spring MVC 6.2 para la API REST
- PostgreSQL y migraciones Flyway
- Maven, JUnit 5 y Testcontainers
- Tomcat 10.1
- Docker y Docker Compose
- HTML, CSS, JavaScript y Bootstrap
- GitHub Actions para compilación, pruebas, WAR e imagen Docker
- Railway para la demostración pública

## Arquitectura

```text
Navegador
   |
   | HTTPS / JSON
   v
Tomcat 10.1
   |-- Servlets y JSP
   |-- Servicios de aplicación
   |-- API REST con Spring MVC
   |-- JPA Hibernate y JDBC
   v
PostgreSQL
   |-- Esquema administrado por Flyway
```

La aplicación conserva una arquitectura WAR tradicional. JPA se utiliza donde reduce consultas y manejo manual de recursos; JDBC se mantiene en operaciones que deben confirmar varias tablas dentro de una misma transacción. Flyway es la única fuente de cambios del esquema.

## Calidad y seguridad

- 198 pruebas JUnit verificadas localmente y pruebas de integración aisladas con PostgreSQL y Flyway.
- Integración continua con Java 21, Maven, Testcontainers y construcción de imagen Docker.
- Contraseñas protegidas con BCrypt y credenciales administradas fuera de Git.
- Protección CSRF para formularios y operaciones REST de escritura.
- Autorización por rol y aislamiento de citas, comprobantes y documentos por propietario.
- Cookies de sesión seguras, cabeceras HTTP defensivas y revocación de sesiones.
- Límites y validaciones específicas para servicios externos y el asistente Sora.

## Ejecución con Docker

### Requisitos

- Docker Desktop
- WSL 2 en Windows

Desde PowerShell, en la raíz del proyecto:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\iniciar-docker.ps1"
```

La aplicación queda disponible en:

```text
http://localhost:8081/SoraCare-FPUNE/
```

La guía de instalación, diagnóstico y cierre está en [docs/DOCKER.md](docs/DOCKER.md).

## Pruebas

Pruebas unitarias:

```powershell
mvn clean test
```

Validación completa con PostgreSQL temporal, Flyway y generación del WAR:

```powershell
mvn -Pintegracion-postgresql clean verify
```

## Documentación

- [Contexto técnico y estado actual](docs/CONTEXTO_MAESTRO_SORACARE.md)
- [API REST](docs/API_REST.md)
- [Ejecución con Docker](docs/DOCKER.md)
- [Despliegue en Railway](docs/DESPLIEGUE_RAILWAY.md)
- [Despliegue en Northflank](docs/DESPLIEGUE_NORTHFLANK.md)
- [Integración continua](docs/INTEGRACION_CONTINUA.md)
- [Facturación interna](docs/FACTURACION_INTERNA.md)
- [Integración SIFEN y limitaciones](docs/INTEGRACION_SIFEN.md)
- [Seguridad y operación](docs/SEGURIDAD_DIAGNOSTICOS.md)

## Equipo

- Gerardo Adrián González Brizuela
- Tobías Fabián Sánchez Florentín
- María Victoria Fleitas Sosa
- César Bacilis Olmedo Maidana

Proyecto académico de Ingeniería de Sistemas, Universidad Nacional del Este, 2026.
