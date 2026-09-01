# Integración continua de SoraCare

El workflow `.github/workflows/calidad.yml` verifica automáticamente el
proyecto en GitHub Actions. Se ejecuta con cada `push`, con cada pull request y
también puede iniciarse manualmente desde la pestaña **Actions**.

## Controles realizados

### Pruebas y WAR

1. descarga el código sin conservar credenciales Git;
2. configura Eclipse Temurin 21 y la caché de dependencias Maven;
3. ejecuta las pruebas unitarias de JUnit;
4. levanta un PostgreSQL temporal con Testcontainers;
5. aplica y valida las migraciones `V001` a `V033` mediante Flyway;
6. comprueba el esquema y una operación real de `EspecialidadDAO`;
7. elimina automáticamente el contenedor y sus datos de prueba;
8. exige que exista `target/SoraCare-FPUNE.war`;
9. conserva los reportes JUnit y Failsafe durante 14 días;
10. conserva el WAR verificado durante 7 días.

La base temporal se llama `soracare_pruebas`, utiliza un puerto aleatorio y una
contraseña generada en memoria. No se conecta a `soracare_db`, no reutiliza su
volumen y no puede modificar los datos locales del proyecto.

### Imagen Docker

Este trabajo comienza únicamente si Maven fue exitoso. Construye la imagen a
partir del `Dockerfile` y comprueba que:

- la construcción finaliza correctamente;
- Tomcat se ejecutará con el usuario no privilegiado `soracare`;
- la imagen contiene un `HEALTHCHECK`.

El Dockerfile compila el WAR y vuelve a ejecutar las pruebas dentro de una
imagen limpia. Esto confirma que el proyecto no depende accidentalmente de la
configuración de una computadora particular.

## Seguridad del workflow

- El token automático posee únicamente permiso de lectura sobre el contenido.
- `checkout` no conserva credenciales en el repositorio de trabajo.
- No se utiliza ninguna contraseña ni clave de integración.
- Las acciones externas están fijadas a hashes completos verificados y no a
  ramas mutables.
- El workflow no publica imágenes ni realiza despliegues.
- Una ejecución anterior de la misma rama se cancela cuando llega otra más
  reciente.

## Resultado visible en GitHub

Cada ejecución muestra dos controles:

- `Pruebas y WAR`;
- `Imagen Docker`.

Cuando ambos están en verde, GitHub ofrece el WAR y los reportes JUnit en la
sección **Artifacts** de la ejecución. Si un control falla, el log indica el
paso exacto y Maven conserva los reportes que haya alcanzado a generar.

Después de publicar el workflow conviene configurar esos dos controles como
obligatorios antes de fusionar cambios a la rama principal.

## Equivalencia local

Las órdenes principales pueden repetirse localmente:

```powershell
mvn --batch-mode --no-transfer-progress -Pintegracion-postgresql clean verify
docker build --tag soracare-fpune:ci .
```

Docker Desktop debe estar iniciado para ejecutar el perfil
`integracion-postgresql`. Sin ese perfil, `mvn clean verify` conserva el ciclo
rápido de pruebas que no necesitan infraestructura externa.
