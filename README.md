# SoraCare-FPUNE 🏥

Sistema web de gestión hospitalaria para **Ingeniería de Software II** (FPUNE - 2026).

## Ejecución con Docker

La instalación Docker incluye PostgreSQL, Flyway, Java 21 y Tomcat. En Windows
requiere Docker Desktop con WSL 2 activo.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\iniciar-docker.ps1"
```

Luego se abre `http://localhost:8081/SoraCare-FPUNE/`. La guía completa está
en [docs/DOCKER.md](docs/DOCKER.md).

El cierre técnico de las mejoras solicitadas por el profesor está resumido en
[docs/MEJORAS_PROFESOR.md](docs/MEJORAS_PROFESOR.md).

## Calidad automatizada

GitHub Actions compila el proyecto con Java 21, ejecuta las pruebas JUnit,
publica el WAR verificable y construye la imagen Docker. El funcionamiento y
los controles de seguridad están explicados en
[docs/INTEGRACION_CONTINUA.md](docs/INTEGRACION_CONTINUA.md).

## 👥 Equipo
* Gerardo Adrián González Brizuela
* Tobías Fabián Sánchez Florentín
* María Victoria Fleitas Sosa
* César Bacilis Olmedo Maidana

---

## 🌿 Reglas de Git

* **`main`**: Solo código estable para entrega.
* **`develop`**: Rama donde unimos los avances.
* **`feature/nombre-tarea`**: Rama individual para cada funcionalidad.

### Instrucciones para el equipo:
1. Clonar el repositorio.
2. Posicionarse en `develop`: `git checkout develop`
3. Crear rama propia: `git checkout -b feature/mi-tarea`
4. Guardar cambios y subir: `git push origin feature/mi-tarea`
