# Integración continua de SoraCare

El workflow `.github/workflows/calidad.yml` verifica automáticamente el
proyecto en GitHub Actions. Se ejecuta con cada `push`, con cada pull request y
también puede iniciarse manualmente desde la pestaña **Actions**.

## Controles realizados

### Pruebas y WAR

1. descarga el código sin conservar credenciales Git;
2. configura Eclipse Temurin 21 y la caché de dependencias Maven;
3. ejecuta `mvn --batch-mode --no-transfer-progress clean verify`;
4. exige que exista `target/SoraCare-FPUNE.war`;
5. conserva los reportes JUnit durante 14 días;
6. conserva el WAR verificado durante 7 días.

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
mvn --batch-mode --no-transfer-progress clean verify
docker build --tag soracare-fpune:ci .
```

La ejecución de PostgreSQL, Flyway y las pruebas de integración con una base
temporal formarán el siguiente frente. No se agregaron contraseñas ficticias ni
servicios de base de datos a este primer workflow.
