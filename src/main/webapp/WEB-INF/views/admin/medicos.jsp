<%@ page contentType="text/html;charset=UTF-8"
         pageEncoding="UTF-8"
         language="java" %>

<%@ page import="py.fpune.soracare.mvc.model.Medico" %>
<%@ page import="py.fpune.soracare.mvc.model.Especialidad" %>
<%@ page import="py.fpune.soracare.mvc.model.MedicoEspecialidad" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>

<%!
    private String escaparHtml(String texto) {
        if (texto == null) {
            return "";
        }

        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
%>

<%
    Object atributoMedicos =
            request.getAttribute(
                    "medicos"
            );

    List<Medico> medicos;

    if (atributoMedicos instanceof List<?>) {
        medicos =
                (List<Medico>) atributoMedicos;
    } else {
        medicos =
                List.of();
    }

    Object atributoEspecialidades =
            request.getAttribute(
                    "especialidadesActivas"
            );

    List<Especialidad> especialidadesActivas;

    if (atributoEspecialidades instanceof List<?>) {
        especialidadesActivas =
                (List<Especialidad>) atributoEspecialidades;
    } else {
        especialidadesActivas =
                List.of();
    }

    Object atributoEspecialidadesPorMedico =
            request.getAttribute(
                    "especialidadesPorMedico"
            );

    Map<Long, List<MedicoEspecialidad>>
            especialidadesPorMedico;

    if (atributoEspecialidadesPorMedico instanceof Map<?, ?>) {
        especialidadesPorMedico =
                (Map<Long, List<MedicoEspecialidad>>)
                        atributoEspecialidadesPorMedico;
    } else {
        especialidadesPorMedico =
                Map.of();
    }

    Object atributoEspecialidadesPorId =
            request.getAttribute(
                    "especialidadesPorId"
            );

    Map<Integer, Especialidad>
            especialidadesPorId;

    if (atributoEspecialidadesPorId instanceof Map<?, ?>) {
        especialidadesPorId =
                (Map<Integer, Especialidad>)
                        atributoEspecialidadesPorId;
    } else {
        especialidadesPorId =
                Map.of();
    }

    String mensajeExito =
            (String) request.getAttribute(
                    "mensajeExito"
            );

    String error =
            (String) request.getAttribute(
                    "error"
            );

    String nombres =
            (String) request.getAttribute(
                    "nombres"
            );

    String apellidos =
            (String) request.getAttribute(
                    "apellidos"
            );

    String cedula =
            (String) request.getAttribute(
                    "cedula"
            );

    String fechaNacimiento =
            (String) request.getAttribute(
                    "fechaNacimiento"
            );

    String telefono =
            (String) request.getAttribute(
                    "telefono"
            );

    String correo =
            (String) request.getAttribute(
                    "correo"
            );

    String direccion =
            (String) request.getAttribute(
                    "direccion"
            );

    String matriculaProfesional =
            (String) request.getAttribute(
                    "matriculaProfesional"
            );

    String fechaIngreso =
            (String) request.getAttribute(
                    "fechaIngreso"
            );

    String biografia =
            (String) request.getAttribute(
                    "biografia"
            );

    String asignacionIdMedico =
            (String) request.getAttribute(
                    "asignacionIdMedico"
            );

    String asignacionIdEspecialidad =
            (String) request.getAttribute(
                    "asignacionIdEspecialidad"
            );

    String asignacionPrincipal =
            (String) request.getAttribute(
                    "asignacionPrincipal"
            );

    String asignacionFechaDesde =
            (String) request.getAttribute(
                    "asignacionFechaDesde"
            );

    Object atributoIdsPersonasConCuenta =
            request.getAttribute(
                    "idsPersonasConCuenta"
            );

    Set<Long> idsPersonasConCuenta;

    if (atributoIdsPersonasConCuenta instanceof Set<?>) {
        idsPersonasConCuenta =
                (Set<Long>) atributoIdsPersonasConCuenta;
    } else {
        idsPersonasConCuenta =
                Set.of();
    }

    String cuentaIdMedico =
            (String) request.getAttribute(
                    "cuentaIdMedico"
            );

    String cuentaNombreUsuario =
            (String) request.getAttribute(
                    "cuentaNombreUsuario"
            );

    if (nombres == null) {
        nombres = "";
    }

    if (apellidos == null) {
        apellidos = "";
    }

    if (cedula == null) {
        cedula = "";
    }

    if (fechaNacimiento == null) {
        fechaNacimiento = "";
    }

    if (telefono == null) {
        telefono = "";
    }

    if (correo == null) {
        correo = "";
    }

    if (direccion == null) {
        direccion = "";
    }

    if (matriculaProfesional == null) {
        matriculaProfesional = "";
    }

    if (fechaIngreso == null) {
        fechaIngreso = "";
    }

    if (biografia == null) {
        biografia = "";
    }

    if (asignacionIdMedico == null) {
        asignacionIdMedico = "";
    }

    if (asignacionIdEspecialidad == null) {
        asignacionIdEspecialidad = "";
    }

    if (asignacionFechaDesde == null) {
        asignacionFechaDesde = "";
    }

    if (cuentaIdMedico == null) {
        cuentaIdMedico = "";
    }

    if (cuentaNombreUsuario == null) {
        cuentaNombreUsuario = "";
    }

    boolean asignacionEsPrincipal =
            "on".equalsIgnoreCase(
                    asignacionPrincipal
            )
                    || "true".equalsIgnoreCase(
                    asignacionPrincipal
            )
                    || "1".equals(
                    asignacionPrincipal
            )
                    || "si".equalsIgnoreCase(
                    asignacionPrincipal
            )
                    || "sí".equalsIgnoreCase(
                    asignacionPrincipal
            );
%>

<!DOCTYPE html>
<html lang="es">

<head>

    <meta charset="UTF-8">

    <meta
            name="viewport"
            content="width=device-width, initial-scale=1">

    <meta
            name="robots"
            content="noindex, nofollow">

    <title>
        Médicos | SoraCare-FPUNE
    </title>

    <link
            href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
            rel="stylesheet">

    <style>

        body {
            min-height: 100vh;
            background-color: #f1f5f9;
        }

        .barra-superior {
            background-color: #0f766e;
        }

        .marca-soracare {
            color: #ffffff;
            font-weight: 700;
        }

        .marca-soracare:hover,
        .marca-soracare:focus {
            color: #ffffff;
        }

        .encabezado-modulo {
            border: none;
            border-radius: 1rem;
            background:
                    linear-gradient(
                            135deg,
                            #0f766e,
                            #0e7490
                    );
            color: #ffffff;
        }

        .tarjeta-contenido {
            border: none;
            border-radius: 1rem;
        }

        .tarjeta-asignacion {
            border: none;
            border-radius: 1rem;
            border-left: 5px solid #0f766e;
        }

        .form-control,
        .form-select {
            border-radius: 0.75rem;
        }

        .form-control:focus,
        .form-select:focus {
            border-color: #0f766e;
            box-shadow:
                    0 0 0 0.25rem
                    rgba(15, 118, 110, 0.15);
        }

        .boton-principal {
            background-color: #0f766e;
            border-color: #0f766e;
        }

        .boton-principal:hover,
        .boton-principal:focus {
            background-color: #115e59;
            border-color: #115e59;
        }

        .contador-medicos {
            background-color: #ccfbf1;
            color: #115e59;
        }

        .estado-activo {
            background-color: #ccfbf1;
            color: #115e59;
        }

        .estado-licencia {
            background-color: #fef3c7;
            color: #92400e;
        }

        .estado-inactivo {
            background-color: #e2e8f0;
            color: #475569;
        }

        .especialidad-principal {
            background-color: #0f766e;
            color: #ffffff;
        }

        .especialidad-secundaria {
            background-color: #e0f2fe;
            color: #075985;
        }

        .boton-cerrar-sesion {
            min-width: 120px;
        }

        .tabla-medicos th {
            white-space: nowrap;
        }

        .dato-secundario {
            font-size: 0.875rem;
            color: #64748b;
        }

        .formulario-seccion {
            border-bottom: 1px solid #e2e8f0;
            padding-bottom: 0.75rem;
            margin-bottom: 1.25rem;
        }

        .icono-relacion {
            width: 52px;
            height: 52px;
            border-radius: 1rem;
            background-color: #ccfbf1;
            color: #0f766e;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.4rem;
            font-weight: 700;
        }

        .contenedor-especialidades {
            min-width: 190px;
        }

        .badge-especialidad {
            display: inline-block;
            margin-right: 0.25rem;
            margin-bottom: 0.35rem;
            padding: 0.45rem 0.6rem;
            border-radius: 0.6rem;
            font-weight: 600;
        }

        .fecha-especialidad {
            display: block;
            font-size: 0.72rem;
            font-weight: 400;
            margin-top: 0.15rem;
            opacity: 0.85;
        }

    </style>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css">

</head>

<body class="app-shell app-admin">

<nav class="navbar barra-superior shadow-sm">

    <div class="container">

        <a
                class="navbar-brand marca-soracare"
                href="${pageContext.request.contextPath}/dashboard">

            SoraCare-FPUNE

        </a>

        <div class="d-flex align-items-center gap-3">

            <span class="navbar-text text-white d-none d-lg-inline">

                Área administrativa

            </span>

            <form
                    method="post"
                    action="${pageContext.request.contextPath}/logout"
                    class="m-0">

                <button
                        type="submit"
                        class="btn btn-outline-light btn-sm boton-cerrar-sesion">

                    Cerrar sesión

                </button>

            </form>

        </div>

    </div>

</nav>

<main class="container py-4 py-md-5">

    <section class="card encabezado-modulo shadow-sm mb-4">

        <div class="card-body p-4 p-md-5">

            <div class="row align-items-center">

                <div class="col-12 col-lg-8">

                    <p class="mb-2 opacity-75">
                        Gestión médica
                    </p>

                    <h1 class="display-6 fw-bold mb-3">
                        Médicos
                    </h1>

                    <p class="mb-0">

                        Registrá profesionales y administrá
                        las especialidades médicas que
                        pueden ejercer dentro de SoraCare-FPUNE.

                    </p>

                </div>

                <div class="col-12 col-lg-4 mt-4 mt-lg-0 text-lg-end">

                    <span class="badge contador-medicos fs-6">

                        <%= medicos.size() %>
                        médico<%= medicos.size() == 1
                            ? ""
                            : "s" %>

                    </span>

                </div>

            </div>

        </div>

    </section>

    <div class="d-flex flex-column
                flex-md-row
                justify-content-between
                align-items-md-center
                gap-3 mb-4">

        <div>

            <h2 class="h4 mb-1">
                Administración de médicos
            </h2>

            <p class="text-secondary mb-0">
                Acceso exclusivo para administradores autorizados
            </p>

        </div>

        <a
                href="${pageContext.request.contextPath}/admin/inicio"
                class="btn btn-outline-secondary">

            Volver a administración

        </a>

    </div>

    <% if (mensajeExito != null
            && !mensajeExito.isBlank()) { %>

    <div
            class="alert alert-success alert-dismissible fade show"
            role="alert">

        <strong>Operación correcta.</strong>

        <%= escaparHtml(mensajeExito) %>

        <button
                type="button"
                class="btn-close"
                data-bs-dismiss="alert"
                aria-label="Cerrar">
        </button>

    </div>

    <% } %>

    <% if (error != null
            && !error.isBlank()) { %>

    <div
            class="alert alert-danger"
            role="alert">

        <strong>No se pudo completar la operación.</strong>

        <div class="mt-1">

            <%= escaparHtml(error) %>

        </div>

    </div>

    <% } %>

    <section class="card tarjeta-asignacion shadow-sm mb-4">

        <div class="card-body p-4">

            <div class="d-flex align-items-center gap-3 mb-4">

                <div class="icono-relacion">
                    +
                </div>

                <div>

                    <h2 class="h5 mb-1">
                        Asignar especialidad a médico
                    </h2>

                    <p class="text-secondary mb-0">

                        Relacioná un profesional con una
                        especialidad médica activa.

                    </p>

                </div>

            </div>

            <% if (medicos.isEmpty()) { %>

            <div class="alert alert-warning mb-0">

                Primero debe existir al menos un médico
                registrado para poder asignar una especialidad.

            </div>

            <% } else if (especialidadesActivas.isEmpty()) { %>

            <div class="alert alert-warning mb-0">

                No existen especialidades activas disponibles
                para realizar una asignación.

            </div>

            <% } else { %>

            <form
                    method="post"
                    action="${pageContext.request.contextPath}/admin/medicos">

                <input
                        type="hidden"
                        name="accion"
                        value="asignarEspecialidad">

                <div class="row g-3 align-items-end">

                    <div class="col-12 col-lg-4">

                        <label
                                for="idMedico"
                                class="form-label fw-semibold">

                            Médico

                        </label>

                        <select
                                id="idMedico"
                                name="idMedico"
                                class="form-select"
                                required>

                            <option value="">
                                Seleccionar médico
                            </option>

                            <% for (
                                    Medico medico
                                    : medicos
                            ) {

                                String idMedicoActual =
                                        String.valueOf(
                                                medico.getIdMedico()
                                        );

                                boolean medicoSeleccionado =
                                        idMedicoActual.equals(
                                                asignacionIdMedico
                                        );
                            %>

                            <option
                                    value="<%= escaparHtml(
                                            idMedicoActual
                                    ) %>"
                                    <%= medicoSeleccionado
                                            ? "selected"
                                            : "" %>>

                                <%= escaparHtml(
                                        medico.getNombres()
                                ) %>

                                <%= escaparHtml(
                                        medico.getApellidos()
                                ) %>

                                —
                                <%= escaparHtml(
                                        medico.getMatriculaProfesional()
                                ) %>

                            </option>

                            <% } %>

                        </select>

                    </div>

                    <div class="col-12 col-lg-4">

                        <label
                                for="idEspecialidad"
                                class="form-label fw-semibold">

                            Especialidad

                        </label>

                        <select
                                id="idEspecialidad"
                                name="idEspecialidad"
                                class="form-select"
                                required>

                            <option value="">
                                Seleccionar especialidad
                            </option>

                            <% for (
                                    Especialidad especialidad
                                    : especialidadesActivas
                            ) {

                                String idEspecialidadActual =
                                        String.valueOf(
                                                especialidad
                                                        .getIdEspecialidad()
                                        );

                                boolean especialidadSeleccionada =
                                        idEspecialidadActual.equals(
                                                asignacionIdEspecialidad
                                        );
                            %>

                            <option
                                    value="<%= escaparHtml(
                                            idEspecialidadActual
                                    ) %>"
                                    <%= especialidadSeleccionada
                                            ? "selected"
                                            : "" %>>

                                <%= escaparHtml(
                                        especialidad.getNombre()
                                ) %>

                            </option>

                            <% } %>

                        </select>

                    </div>

                    <div class="col-12 col-md-6 col-lg-2">

                        <label
                                for="fechaDesde"
                                class="form-label fw-semibold">

                            Desde

                        </label>

                        <input
                                type="date"
                                id="fechaDesde"
                                name="fechaDesde"
                                class="form-control"
                                value="<%= escaparHtml(
                                        asignacionFechaDesde
                                ) %>">

                    </div>

                    <div class="col-12 col-md-6 col-lg-2">

                        <div class="form-check mb-2">

                            <input
                                    class="form-check-input"
                                    type="checkbox"
                                    id="principal"
                                    name="principal"
                                    value="true"
                                <%= asignacionEsPrincipal
                                            ? "checked"
                                            : "" %>>

                            <label
                                    class="form-check-label fw-semibold"
                                    for="principal">

                                Principal

                            </label>

                        </div>

                    </div>

                    <div class="col-12">

                        <div class="d-flex flex-column
                                    flex-md-row
                                    justify-content-between
                                    align-items-md-center
                                    gap-3">

                            <div class="form-text">

                                Si no indicás una fecha,
                                se utilizará la fecha actual.
                                Cada médico puede tener solamente
                                una especialidad principal activa.

                            </div>

                            <button
                                    type="submit"
                                    class="btn btn-primary boton-principal">

                                Asignar especialidad

                            </button>

                        </div>

                    </div>

                </div>

            </form>

            <% } %>

        </div>

    </section>

    <section class="card tarjeta-asignacion shadow-sm mb-4">

        <div class="card-body p-4">

            <div class="d-flex align-items-center gap-3 mb-4">

                <div class="icono-relacion">@</div>

                <div>

                    <h2 class="h5 mb-1">
                        Crear acceso al portal médico
                    </h2>

                    <p class="text-secondary mb-0">
                        Asigná una cuenta a un profesional registrado
                        para que consulte exclusivamente su propia agenda.
                    </p>

                </div>

            </div>

            <% boolean hayMedicoSinCuenta =
                    medicos.stream().anyMatch(medico ->
                            medico.getId() != null
                                    && medico.getEstado() != null
                                    && "ACTIVO".equals(medico.getEstado().name())
                                    && !idsPersonasConCuenta.contains(medico.getId()));

            if (!hayMedicoSinCuenta) { %>

            <div class="alert alert-light border mb-0">
                Todos los médicos registrados ya poseen una cuenta de acceso.
            </div>

            <% } else { %>

            <form method="post"
                  action="${pageContext.request.contextPath}/admin/medicos">

                <input type="hidden" name="accion" value="crearCuentaMedico">

                <div class="row g-3 align-items-end">

                    <div class="col-12 col-lg-4">

                        <label for="cuentaIdMedico" class="form-label fw-semibold">
                            Médico sin cuenta
                        </label>

                        <select id="cuentaIdMedico" name="cuentaIdMedico"
                                class="form-select" required>

                            <option value="">Seleccionar médico</option>

                            <% for (Medico medico : medicos) {
                                if (medico.getId() == null
                                        || medico.getEstado() == null
                                        || !"ACTIVO".equals(medico.getEstado().name())
                                        || idsPersonasConCuenta.contains(medico.getId())) {
                                    continue;
                                }
                                String idMedicoActual =
                                        String.valueOf(medico.getIdMedico());
                            %>

                            <option value="<%= escaparHtml(idMedicoActual) %>"
                                    <%= idMedicoActual.equals(cuentaIdMedico)
                                            ? "selected" : "" %>>
                                <%= escaparHtml(medico.getNombres()) %>
                                <%= escaparHtml(medico.getApellidos()) %>
                                — <%= escaparHtml(medico.getMatriculaProfesional()) %>
                            </option>

                            <% } %>

                        </select>

                    </div>

                    <div class="col-12 col-lg-3">

                        <label for="cuentaNombreUsuario" class="form-label fw-semibold">
                            Nombre de usuario
                        </label>

                        <input type="text" id="cuentaNombreUsuario"
                               name="cuentaNombreUsuario" class="form-control"
                               minlength="4" maxlength="50"
                               pattern="[A-Za-z0-9._-]+" required
                               autocomplete="off"
                               value="<%= escaparHtml(cuentaNombreUsuario) %>">

                    </div>

                    <div class="col-12 col-md-6 col-lg-2">

                        <label for="cuentaContrasena" class="form-label fw-semibold">
                            Contraseña temporal
                        </label>

                        <input type="password" id="cuentaContrasena"
                               name="cuentaContrasena" class="form-control"
                               minlength="10" maxlength="72" required
                               autocomplete="new-password">

                    </div>

                    <div class="col-12 col-md-6 col-lg-2">

                        <label for="cuentaConfirmacion" class="form-label fw-semibold">
                            Confirmar contraseña
                        </label>

                        <input type="password" id="cuentaConfirmacion"
                               name="cuentaConfirmacion" class="form-control"
                               minlength="10" maxlength="72" required
                               autocomplete="new-password">

                    </div>

                    <div class="col-12 col-lg-1 d-grid">
                        <button type="submit" class="btn btn-primary boton-principal">
                            Crear
                        </button>
                    </div>

                    <div class="col-12">
                        <div class="form-text">
                            La contraseña debe tener mayúscula, minúscula,
                            número y al menos 10 caracteres. El médico deberá
                            reemplazarla al iniciar sesión por primera vez.
                        </div>
                    </div>

                </div>

            </form>

            <% } %>

        </div>

    </section>

    <section class="row g-4">

        <div class="col-12 col-xl-5">

            <div class="card tarjeta-contenido shadow-sm">

                <div class="card-body p-4">

                    <h2 class="h5 mb-1">
                        Nuevo médico
                    </h2>

                    <p class="text-secondary mb-4">

                        Completá los datos personales
                        y profesionales.

                    </p>

                    <form
                            method="post"
                            action="${pageContext.request.contextPath}/admin/medicos">

                        <input
                                type="hidden"
                                name="accion"
                                value="registrarMedico">

                        <h3 class="h6 formulario-seccion">
                            Datos personales
                        </h3>

                        <div class="row">

                            <div class="col-12 col-md-6 mb-3">

                                <label
                                        for="nombres"
                                        class="form-label fw-semibold">

                                    Nombres

                                </label>

                                <input
                                        type="text"
                                        id="nombres"
                                        name="nombres"
                                        class="form-control"
                                        value="<%= escaparHtml(nombres) %>"
                                        maxlength="100"
                                        required
                                        autocomplete="off">

                            </div>

                            <div class="col-12 col-md-6 mb-3">

                                <label
                                        for="apellidos"
                                        class="form-label fw-semibold">

                                    Apellidos

                                </label>

                                <input
                                        type="text"
                                        id="apellidos"
                                        name="apellidos"
                                        class="form-control"
                                        value="<%= escaparHtml(apellidos) %>"
                                        maxlength="100"
                                        required
                                        autocomplete="off">

                            </div>

                        </div>

                        <div class="row">

                            <div class="col-12 col-md-6 mb-3">

                                <label
                                        for="cedula"
                                        class="form-label fw-semibold">

                                    Cédula

                                </label>

                                <input
                                        type="text"
                                        id="cedula"
                                        name="cedula"
                                        class="form-control"
                                        value="<%= escaparHtml(cedula) %>"
                                        inputmode="numeric"
                                        pattern="[0-9]+"
                                        required
                                        autocomplete="off">

                                <div class="form-text">
                                    Ingresá solamente números.
                                </div>

                            </div>

                            <div class="col-12 col-md-6 mb-3">

                                <label
                                        for="fechaNacimiento"
                                        class="form-label fw-semibold">

                                    Fecha de nacimiento

                                </label>

                                <input
                                        type="date"
                                        id="fechaNacimiento"
                                        name="fechaNacimiento"
                                        class="form-control"
                                        value="<%= escaparHtml(
                                                fechaNacimiento
                                        ) %>">

                            </div>

                        </div>

                        <div class="row">

                            <div class="col-12 col-md-6 mb-3">

                                <label
                                        for="telefono"
                                        class="form-label fw-semibold">

                                    Teléfono

                                </label>

                                <input
                                        type="text"
                                        id="telefono"
                                        name="telefono"
                                        class="form-control"
                                        value="<%= escaparHtml(telefono) %>"
                                        maxlength="30"
                                        autocomplete="off">

                            </div>

                            <div class="col-12 col-md-6 mb-3">

                                <label
                                        for="correo"
                                        class="form-label fw-semibold">

                                    Correo electrónico

                                </label>

                                <input
                                        type="email"
                                        id="correo"
                                        name="correo"
                                        class="form-control"
                                        value="<%= escaparHtml(correo) %>"
                                        maxlength="150"
                                        autocomplete="off">

                            </div>

                        </div>

                        <div class="mb-4">

                            <label
                                    for="direccion"
                                    class="form-label fw-semibold">

                                Dirección

                            </label>

                            <input
                                    type="text"
                                    id="direccion"
                                    name="direccion"
                                    class="form-control"
                                    value="<%= escaparHtml(direccion) %>"
                                    maxlength="255"
                                    autocomplete="off">

                        </div>

                        <h3 class="h6 formulario-seccion">
                            Datos profesionales
                        </h3>

                        <div class="row">

                            <div class="col-12 col-md-6 mb-3">

                                <label
                                        for="matriculaProfesional"
                                        class="form-label fw-semibold">

                                    Matrícula profesional

                                </label>

                                <input
                                        type="text"
                                        id="matriculaProfesional"
                                        name="matriculaProfesional"
                                        class="form-control"
                                        value="<%= escaparHtml(
                                                matriculaProfesional
                                        ) %>"
                                        maxlength="50"
                                        required
                                        autocomplete="off">

                            </div>

                            <div class="col-12 col-md-6 mb-3">

                                <label
                                        for="fechaIngreso"
                                        class="form-label fw-semibold">

                                    Fecha de ingreso

                                </label>

                                <input
                                        type="date"
                                        id="fechaIngreso"
                                        name="fechaIngreso"
                                        class="form-control"
                                        value="<%= escaparHtml(
                                                fechaIngreso
                                        ) %>">

                            </div>

                        </div>

                        <div class="mb-4">

                            <label
                                    for="biografia"
                                    class="form-label fw-semibold">

                                Biografía profesional

                            </label>

                            <textarea
                                    id="biografia"
                                    name="biografia"
                                    class="form-control"
                                    rows="4"
                                    maxlength="1000"
                                    placeholder="Experiencia, formación o información profesional del médico"><%= escaparHtml(biografia) %></textarea>

                            <div class="form-text">
                                Campo opcional. Máximo 1000 caracteres.
                            </div>

                        </div>

                        <button
                                type="submit"
                                class="btn btn-primary boton-principal w-100">

                            Registrar médico

                        </button>

                    </form>

                </div>

            </div>

        </div>

        <div class="col-12 col-xl-7">

            <div class="card tarjeta-contenido shadow-sm">

                <div class="card-body p-4">

                    <div class="d-flex flex-column
                                flex-md-row
                                justify-content-between
                                align-items-md-center
                                gap-2 mb-4">

                        <div>

                            <h2 class="h5 mb-1">
                                Médicos registrados
                            </h2>

                            <p class="text-secondary mb-0">
                                Profesionales y especialidades activas
                            </p>

                        </div>

                        <span class="badge contador-medicos">

                            Total: <%= medicos.size() %>

                        </span>

                    </div>

                    <% if (medicos.isEmpty()) { %>

                    <div class="text-center py-5">

                        <h3 class="h5">
                            Todavía no existen médicos
                        </h3>

                        <p class="text-secondary mb-0">

                            Utilizá el formulario para registrar
                            el primer profesional médico.

                        </p>

                    </div>

                    <% } else { %>

                    <div class="table-responsive">

                        <table
                                class="table table-hover
                                       align-middle
                                       tabla-medicos">

                            <thead>

                            <tr>

                                <th scope="col">
                                    ID
                                </th>

                                <th scope="col">
                                    Médico
                                </th>

                                <th scope="col">
                                    Matrícula
                                </th>

                                <th scope="col">
                                    Especialidades
                                </th>

                                <th scope="col">
                                    Contacto
                                </th>

                                <th scope="col">
                                    Estado
                                </th>

                                <th scope="col">
                                    Acceso
                                </th>

                            </tr>

                            </thead>

                            <tbody>

                            <% for (
                                    Medico medico
                                    : medicos
                            ) {

                                List<MedicoEspecialidad>
                                        relacionesMedico =
                                        especialidadesPorMedico
                                                .getOrDefault(
                                                        medico.getIdMedico(),
                                                        List.of()
                                                );
                            %>

                            <tr>

                                <td>
                                    <%= medico.getIdMedico() %>
                                </td>

                                <td>

                                    <strong>

                                        <%= escaparHtml(
                                                medico.getNombres()
                                        ) %>

                                        <%= escaparHtml(
                                                medico.getApellidos()
                                        ) %>

                                    </strong>

                                    <div class="dato-secundario">

                                        CI:
                                        <%= escaparHtml(
                                                medico.getCedula()
                                        ) %>

                                    </div>

                                    <% if (
                                            medico.getFechaIngreso()
                                                    != null
                                    ) { %>

                                    <div class="dato-secundario">

                                        Ingreso:
                                        <%= medico.getFechaIngreso() %>

                                    </div>

                                    <% } %>

                                </td>

                                <td>

                                    <%= escaparHtml(
                                            medico.getMatriculaProfesional()
                                    ) %>

                                </td>

                                <td class="contenedor-especialidades">

                                    <% if (
                                            relacionesMedico.isEmpty()
                                    ) { %>

                                    <span class="text-secondary">
                                        Sin especialidades asignadas
                                    </span>

                                    <% } else {

                                        for (
                                                MedicoEspecialidad relacion
                                                : relacionesMedico
                                        ) {

                                            Especialidad especialidad =
                                                    especialidadesPorId.get(
                                                            relacion
                                                                    .getIdEspecialidad()
                                                    );

                                            String nombreEspecialidad =
                                                    especialidad != null
                                                            ? especialidad
                                                            .getNombre()
                                                            : "Especialidad #"
                                                            + relacion
                                                            .getIdEspecialidad();
                                    %>

                                    <% if (relacion.isPrincipal()) { %>

                                    <span
                                            class="badge-especialidad
                                                   especialidad-principal">

                                        <%= escaparHtml(
                                                nombreEspecialidad
                                        ) %>

                                        · Principal

                                        <span class="fecha-especialidad">

                                            Desde:
                                            <%= relacion.getFechaDesde() %>

                                        </span>

                                    </span>

                                    <% } else { %>

                                    <span
                                            class="badge-especialidad
                                                   especialidad-secundaria">

                                        <%= escaparHtml(
                                                nombreEspecialidad
                                        ) %>

                                        <span class="fecha-especialidad">

                                            Desde:
                                            <%= relacion.getFechaDesde() %>

                                        </span>

                                    </span>

                                    <% } %>

                                    <% } %>

                                    <% } %>

                                </td>

                                <td>

                                    <% if (
                                            medico.getCorreo() != null
                                                    && !medico
                                                    .getCorreo()
                                                    .isBlank()
                                    ) { %>

                                    <div>

                                        <%= escaparHtml(
                                                medico.getCorreo()
                                        ) %>

                                    </div>

                                    <% } %>

                                    <% if (
                                            medico.getTelefono() != null
                                                    && !medico
                                                    .getTelefono()
                                                    .isBlank()
                                    ) { %>

                                    <div class="dato-secundario">

                                        <%= escaparHtml(
                                                medico.getTelefono()
                                        ) %>

                                    </div>

                                    <% } %>

                                    <% if (
                                            (medico.getCorreo() == null
                                                    || medico
                                                    .getCorreo()
                                                    .isBlank())
                                                    &&
                                                    (medico.getTelefono() == null
                                                            || medico
                                                            .getTelefono()
                                                            .isBlank())
                                    ) { %>

                                    <span class="text-secondary">
                                        Sin datos
                                    </span>

                                    <% } %>

                                </td>

                                <td>

                                    <% if (
                                            medico.getEstado() == null
                                    ) { %>

                                    <span class="badge estado-inactivo">
                                        Sin estado
                                    </span>

                                    <% } else if (
                                            "ACTIVO".equals(
                                                    medico
                                                            .getEstado()
                                                            .name()
                                            )
                                    ) { %>

                                    <span class="badge estado-activo">
                                        Activo
                                    </span>

                                    <% } else if (
                                            "LICENCIA".equals(
                                                    medico
                                                            .getEstado()
                                                            .name()
                                            )
                                    ) { %>

                                    <span class="badge estado-licencia">
                                        Licencia
                                    </span>

                                    <% } else { %>

                                    <span class="badge estado-inactivo">
                                        Inactivo
                                    </span>

                                    <% } %>

                                </td>

                                <td>

                                    <% if (medico.getId() != null
                                            && idsPersonasConCuenta.contains(medico.getId())) { %>

                                    <span class="badge estado-activo">
                                        Cuenta creada
                                    </span>

                                    <% } else { %>

                                    <span class="badge estado-inactivo">
                                        Sin cuenta
                                    </span>

                                    <% } %>

                                </td>

                            </tr>

                            <% } %>

                            </tbody>

                        </table>

                    </div>

                    <% } %>

                </div>

            </div>

        </div>

    </section>

</main>

<script
        src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js">
</script>

</body>
</html>
