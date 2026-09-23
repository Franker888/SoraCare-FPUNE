# Guía de presentación en Railway

## Dirección pública

Compartir solamente esta dirección:

<https://soracare-production.up.railway.app/SoraCare-FPUNE/>

La aplicación y PostgreSQL están alojados en Railway. La computadora personal no necesita mantener Docker abierto para que otras personas accedan, siempre que el proyecto de Railway siga activo y con recursos disponibles.

## Cuentas demostrativas

Los nombres de usuario preparados son:

- Paciente: `adrian.paciente`
- Recepción: `prueba.recepcion`
- Administración: `prueba.seguridad`
- Médicos: los 16 usuarios creados para las ocho especialidades comparten una contraseña exclusivamente durante la demostración.

Las contraseñas no se guardan en Git. Están cifradas para el usuario actual de Windows en `.env.demo-railway.clixml`.

Para copiar una contraseña al portapapeles sin mostrarla en la terminal:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\copiar-credencial-demo.ps1" -Cuenta paciente
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\copiar-credencial-demo.ps1" -Cuenta recepcion
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\copiar-credencial-demo.ps1" -Cuenta medicos
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\copiar-credencial-demo.ps1" -Cuenta administracion
```

El script muestra el usuario elegido y copia únicamente su contraseña al portapapeles.

## Ensayo previo

La comprobación de solo lectura valida Google Identity, los cuatro roles y sus rutas principales:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\verificar-presentacion-railway.ps1"
```

Para realizar además una reserva ficticia, confirmar el pago en efectivo y comprobar el PDF autenticado:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\verificar-presentacion-railway.ps1" -CrearFlujoDemostrativo
```

La segunda variante modifica datos demostrativos y debe ejecutarse solo cuando sea necesario.

## Respaldo de la base pública

Con Docker Desktop iniciado y la sesión de Railway CLI vigente:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\respaldar-railway.ps1"
```

El respaldo se guarda en `respaldos\railway\<fecha-hora>` y se valida con `pg_restore --list`. La carpeta de respaldos está excluida de Git.

## Plan de exposición

1. Abrir la dirección pública en una ventana privada del navegador.
2. Confirmar que aparece el botón de Google y que cargan las especialidades y los médicos.
3. Ingresar como paciente y mostrar citas, recetas, documentos y comprobantes PDF.
4. Ingresar como recepción y mostrar agenda y confirmación de pagos.
5. Ingresar como médico y mostrar agenda y facturación.
6. Ingresar como administración y mostrar informes y estado del sistema.
7. Mantener el Docker local como contingencia, pero utilizar Railway como acceso principal para teléfonos y equipos conectados a cualquier red.

## Alcance pendiente fuera de la presentación

- La homologación fiscal real con SIFEN requiere credenciales y certificado del contribuyente.
- Google Calendar permanece deshabilitado por decisión del proyecto.
- Twilio no está configurado.
- No utilizar datos clínicos reales durante la exposición.
