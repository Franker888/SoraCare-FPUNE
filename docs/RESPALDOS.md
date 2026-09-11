# Respaldos locales y restauracion comprobada

## Ejecutar

Con Docker y SoraCare encendidos, desde la raiz del proyecto:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\respaldar-docker.ps1 -Verificar
```

El script detiene brevemente solo la aplicacion, conserva PostgreSQL encendido,
genera un `pg_dump` completo en formato custom, copia documentos clinicos y fotos,
y arranca de nuevo el mismo contenedor de la aplicacion incluso ante errores.
Durante la copia no deben existir otros procesos escribiendo directamente en la
base o los volumenes. Un mutex impide dos respaldos concurrentes del mismo usuario.

El resultado queda en `respaldos/fecha-identificador/`:

- `base.dump`: base de datos, sin propietarios ni permisos del servidor;
- `archivos.tar.gz`: documentos clinicos y fotos de perfil;
- `manifiesto.json`: fecha UTC, tamanos y SHA-256 de ambos archivos;
- `verificacion.json`: evidencia de la ultima restauracion satisfactoria.

Una carpeta sin manifiesto es una copia incompleta y no debe usarse. Un informe
de verificacion anterior no sustituye ejecutar la verificacion antes de recuperar.
Los scripts no borran respaldos anteriores ni modifican la base original.

## Comprobar una copia existente

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\verificar-respaldo-docker.ps1 -Ruta .\respaldos\FECHA-IDENTIFICADOR
```

Se comprueban tamanos y hashes y se restaura en PostgreSQL 18.6 temporal,
sin red ni puertos publicados. Sus datos y archivos viven en tmpfs; no usa los
volumenes originales. Se exige que `pg_restore` no tenga errores, se consulta
el historial Flyway y se extraen/comparan los archivos. Al terminar se elimina
exclusivamente el contenedor temporal cuya etiqueta coincide con esta ejecucion.
La memoria disponible debe alcanzar para la copia restaurada; para bases grandes
se necesitara un entorno de recuperacion separado con almacenamiento suficiente.

Esto comprueba recuperabilidad tecnica, no todos los flujos funcionales de la
aplicacion restaurada. Solo aceptar respaldos locales de confianza: un dump SQL
o un archivo tar de terceros puede contener instrucciones o rutas peligrosas.

## Programacion local

El 10 de septiembre de 2026 se configuro en Codex una automatizacion diaria a
las **21:00, America/Asuncion**, vinculada a esta tarea: `respaldo-diario-de-soracare`.
Ejecuta el respaldo con verificacion y avisa ante fallos. Requiere disponibilidad
de Codex en este equipo, Docker y SoraCare encendidos y permisos de ejecucion.
No es un servicio de respaldo independiente ni una garantia si la PC esta apagada.
La programacion pertenece a Codex: clonar este repositorio no la instala.

## Proteccion y recuperacion real

La carpeta esta excluida de Git y de la construccion Docker. Cada copia restringe
la herencia de permisos de Windows al usuario que la crea. **No esta cifrada**:
contiene informacion clinica y datos de cuentas. No compartirla ni adjuntarla a
las entregas academicas. Administradores del equipo conservan capacidad de acceso.

La copia no incluye `.env.docker`, claves de cifrado, certificados, claves API ni
configuraciones externas. Deben custodiarse por separado en un gestor de secretos;
sin las claves originales algunas integraciones no podran recuperar sus tokens.
No respalda roles globales de PostgreSQL: el despliegue destino debe crear el
usuario de aplicacion y asignar los permisos antes de abrir el servicio.

Antes de produccion falta definir retencion, cifrado, copia fuera del equipo,
objetivos de perdida/tiempo de recuperacion, alertas independientes y un simulacro
completo de aplicacion en infraestructura destino. No se ofrece restauracion
automatica sobre produccion: requiere seleccionar destino, validar permisos,
resguardar su estado previo y autorizar expresamente la sustitucion de datos.
