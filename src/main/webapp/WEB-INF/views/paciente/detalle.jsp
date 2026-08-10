<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">

    <meta name="viewport"
          content="width=device-width, initial-scale=1">

    <title>Paciente registrado | SoraCare-FPUNE</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css"
          rel="stylesheet">
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css">
</head>

<body class="app-shell app-reception">

<main class="container py-5">

    <div class="row justify-content-center">
        <div class="col-lg-9">

            <div class="alert alert-success shadow-sm"
                 role="alert">

                <h1 class="h4 mb-2">
                    Paciente registrado correctamente
                </h1>

                <p class="mb-0">
                    Los datos fueron guardados en PostgreSQL.
                </p>

            </div>

            <div class="card shadow-sm">
                <div class="card-body p-4 p-md-5">

                    <span class="badge text-bg-primary mb-3">
                        Historia clínica:
                        ${paciente.numeroHistoriaClinica}
                    </span>

                    <h2 class="h4 mb-4">
                        Información del paciente
                    </h2>

                    <h3 class="h6 text-primary mb-3">
                        Datos personales
                    </h3>

                    <div class="row g-3 mb-4">

                        <div class="col-md-6">
                            <div class="border rounded p-3 h-100">
                                <span class="text-secondary small">
                                    Nombre completo
                                </span>

                                <p class="mb-0 fw-semibold">
                                    ${paciente.nombres}
                                    ${paciente.apellidos}
                                </p>
                            </div>
                        </div>

                        <div class="col-md-6">
                            <div class="border rounded p-3 h-100">
                                <span class="text-secondary small">
                                    Cédula
                                </span>

                                <p class="mb-0 fw-semibold">
                                    ${paciente.cedula}
                                </p>
                            </div>
                        </div>

                        <div class="col-md-6">
                            <div class="border rounded p-3 h-100">
                                <span class="text-secondary small">
                                    Fecha de nacimiento
                                </span>

                                <p class="mb-0 fw-semibold">
                                    ${empty paciente.fechaNacimiento
                                            ? 'No informada'
                                            : paciente.fechaNacimiento}
                                </p>
                            </div>
                        </div>

                        <div class="col-md-6">
                            <div class="border rounded p-3 h-100">
                                <span class="text-secondary small">
                                    Teléfono
                                </span>

                                <p class="mb-0 fw-semibold">
                                    ${empty paciente.telefono
                                            ? 'No informado'
                                            : paciente.telefono}
                                </p>
                            </div>
                        </div>

                        <div class="col-md-6">
                            <div class="border rounded p-3 h-100">
                                <span class="text-secondary small">
                                    Correo electrónico
                                </span>

                                <p class="mb-0 fw-semibold text-break">
                                    ${empty paciente.correo
                                            ? 'No informado'
                                            : paciente.correo}
                                </p>
                            </div>
                        </div>

                        <div class="col-md-6">
                            <div class="border rounded p-3 h-100">
                                <span class="text-secondary small">
                                    Dirección
                                </span>

                                <p class="mb-0 fw-semibold">
                                    ${empty paciente.direccion
                                            ? 'No informada'
                                            : paciente.direccion}
                                </p>
                            </div>
                        </div>

                    </div>

                    <h3 class="h6 text-primary mb-3">
                        Datos clínicos y de emergencia
                    </h3>

                    <div class="row g-3">

                        <div class="col-md-6">
                            <div class="border rounded p-3 h-100">
                                <span class="text-secondary small">
                                    Número de historia clínica
                                </span>

                                <p class="mb-0 fw-semibold">
                                    ${paciente.numeroHistoriaClinica}
                                </p>
                            </div>
                        </div>

                        <div class="col-md-6">
                            <div class="border rounded p-3 h-100">
                                <span class="text-secondary small">
                                    Grupo sanguíneo
                                </span>

                                <p class="mb-0 fw-semibold">
                                    ${empty paciente.grupoSanguineo
                                            ? 'No informado'
                                            : paciente.grupoSanguineo}
                                </p>
                            </div>
                        </div>

                        <div class="col-md-6">
                            <div class="border rounded p-3 h-100">
                                <span class="text-secondary small">
                                    Contacto de emergencia
                                </span>

                                <p class="mb-0 fw-semibold">
                                    ${empty paciente.contactoEmergencia
                                            ? 'No informado'
                                            : paciente.contactoEmergencia}
                                </p>
                            </div>
                        </div>

                        <div class="col-md-6">
                            <div class="border rounded p-3 h-100">
                                <span class="text-secondary small">
                                    Teléfono de emergencia
                                </span>

                                <p class="mb-0 fw-semibold">
                                    ${empty paciente.telefonoEmergencia
                                            ? 'No informado'
                                            : paciente.telefonoEmergencia}
                                </p>
                            </div>
                        </div>

                    </div>

                    <div class="d-flex flex-wrap gap-2 mt-4">

                        <a href="${pageContext.request.contextPath}/pacientes/registro"
                           class="btn btn-primary">
                            Registrar otro paciente
                        </a>

                        <a href="${pageContext.request.contextPath}/"
                           class="btn btn-outline-secondary">
                            Volver al inicio
                        </a>

                    </div>

                </div>
            </div>

        </div>
    </div>

</main>

</body>
</html>
