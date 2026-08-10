<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">

    <meta name="viewport"
          content="width=device-width, initial-scale=1">

    <title>Registrar paciente | SoraCare-FPUNE</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css"
          rel="stylesheet">
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css">
</head>

<body class="app-shell app-reception">

<main class="container py-5">

    <div class="row justify-content-center">
        <div class="col-lg-8">

            <div class="card shadow-sm">
                <div class="card-body p-4 p-md-5">

                    <span class="badge text-bg-primary mb-3">
                        Registro de pacientes
                    </span>

                    <h1 class="h3 mb-2">
                        Nuevo paciente
                    </h1>

                    <p class="text-secondary">
                        Completá los datos personales del paciente.
                    </p>

                    <p class="text-danger">
                        ${error}
                    </p>

                    <form action="${pageContext.request.contextPath}/pacientes/registro"
                          method="post">

                        <div class="row g-3">

                            <div class="col-md-6">
                                <label for="nombres"
                                       class="form-label">
                                    Nombres
                                </label>

                                <input type="text"
                                       class="form-control"
                                       id="nombres"
                                       name="nombres"
                                       value="${param.nombres}"
                                       required>
                            </div>

                            <div class="col-md-6">
                                <label for="apellidos"
                                       class="form-label">
                                    Apellidos
                                </label>

                                <input type="text"
                                       class="form-control"
                                       id="apellidos"
                                       name="apellidos"
                                       value="${param.apellidos}"
                                       required>
                            </div>

                            <div class="col-md-6">
                                <label for="cedula"
                                       class="form-label">
                                    Cédula
                                </label>

                                <input type="text"
                                       class="form-control"
                                       id="cedula"
                                       name="cedula"
                                       value="${param.cedula}"
                                       required>
                            </div>

                            <div class="col-md-6">
                                <label for="fechaNacimiento"
                                       class="form-label">
                                    Fecha de nacimiento
                                </label>

                                <input type="date"
                                       class="form-control"
                                       id="fechaNacimiento"
                                       name="fechaNacimiento"
                                       value="${param.fechaNacimiento}">
                            </div>

                            <div class="col-md-6">
                                <label for="telefono"
                                       class="form-label">
                                    Teléfono
                                </label>

                                <input type="text"
                                       class="form-control"
                                       id="telefono"
                                       name="telefono"
                                       maxlength="30"
                                       value="${param.telefono}">
                            </div>

                            <div class="col-md-6">
                                <label for="correo"
                                       class="form-label">
                                    Correo electrónico
                                </label>

                                <input type="email"
                                       class="form-control"
                                       id="correo"
                                       name="correo"
                                       maxlength="150"
                                       value="${param.correo}">
                            </div>

                            <div class="col-12">
                                <label for="direccion"
                                       class="form-label">
                                    Dirección
                                </label>

                                <textarea class="form-control"
                                          id="direccion"
                                          name="direccion"
                                          rows="3"
                                          maxlength="255">${param.direccion}</textarea>

                                <div class="form-text">
                                    Ingresá una referencia clara de la dirección.
                                </div>
                            </div>

                            <div class="col-md-6">
                                <label for="grupoSanguineo"
                                       class="form-label">
                                    Grupo sanguíneo
                                </label>

                                <select class="form-select"
                                        id="grupoSanguineo"
                                        name="grupoSanguineo">

                                    <option value="">
                                        Seleccionar
                                    </option>

                                    <option value="A+"
                                    ${param.grupoSanguineo == 'A+' ? 'selected' : ''}>
                                        A+
                                    </option>

                                    <option value="A-"
                                    ${param.grupoSanguineo == 'A-' ? 'selected' : ''}>
                                        A-
                                    </option>

                                    <option value="B+"
                                    ${param.grupoSanguineo == 'B+' ? 'selected' : ''}>
                                        B+
                                    </option>

                                    <option value="B-"
                                    ${param.grupoSanguineo == 'B-' ? 'selected' : ''}>
                                        B-
                                    </option>

                                    <option value="AB+"
                                    ${param.grupoSanguineo == 'AB+' ? 'selected' : ''}>
                                        AB+
                                    </option>

                                    <option value="AB-"
                                    ${param.grupoSanguineo == 'AB-' ? 'selected' : ''}>
                                        AB-
                                    </option>

                                    <option value="O+"
                                    ${param.grupoSanguineo == 'O+' ? 'selected' : ''}>
                                        O+
                                    </option>

                                    <option value="O-"
                                    ${param.grupoSanguineo == 'O-' ? 'selected' : ''}>
                                        O-
                                    </option>

                                </select>

                                <div class="form-text">
                                    Podés dejarlo vacío si no se conoce.
                                </div>
                            </div>

                            <div class="col-md-6">
                                <label for="contactoEmergencia"
                                       class="form-label">
                                    Contacto de emergencia
                                </label>

                                <input type="text"
                                       class="form-control"
                                       id="contactoEmergencia"
                                       name="contactoEmergencia"
                                       maxlength="150"
                                       value="${param.contactoEmergencia}">

                                <div class="form-text">
                                    Nombre de la persona a contactar.
                                </div>
                            </div>

                            <div class="col-md-6">
                                <label for="telefonoEmergencia"
                                       class="form-label">
                                    Teléfono de emergencia
                                </label>

                                <input type="tel"
                                       class="form-control"
                                       id="telefonoEmergencia"
                                       name="telefonoEmergencia"
                                       maxlength="30"
                                       value="${param.telefonoEmergencia}">

                                <div class="form-text">
                                    Número del contacto de emergencia.
                                </div>
                            </div>

                            <div class="col-12 d-flex gap-2 mt-4">

                                <button type="submit"
                                        class="btn btn-primary">
                                    Registrar paciente
                                </button>

                                <a href="${pageContext.request.contextPath}/"
                                   class="btn btn-outline-secondary">
                                    Cancelar
                                </a>

                            </div>

                        </div>

                    </form>

                </div>
            </div>

        </div>
    </div>

</main>

</body>
</html>
