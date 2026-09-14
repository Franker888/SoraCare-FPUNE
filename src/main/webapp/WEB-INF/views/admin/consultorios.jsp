<%@ page contentType="text/html;charset=UTF-8"
         pageEncoding="UTF-8"
         language="java" %>

<%@ page import="py.fpune.soracare.mvc.model.Consultorio" %>
<%@ page import="py.fpune.soracare.mvc.model.Sede" %>
<%@ page import="java.util.List" %>

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

    private String obtenerNombreSede(
            List<Sede> sedes,
            Integer idSede
    ) {
        if (sedes == null || idSede == null) {
            return "Sede no disponible";
        }

        for (Sede sede : sedes) {
            if (idSede.equals(
                    sede.getIdSede()
            )) {
                return sede.getNombre();
            }
        }

        return "Sede ID " + idSede;
    }

    private boolean estaMarcado(
            String valor
    ) {
        if (valor == null) {
            return false;
        }

        String normalizado =
                valor.trim()
                        .toLowerCase();

        return normalizado.equals("true")
                || normalizado.equals("on")
                || normalizado.equals("1")
                || normalizado.equals("si")
                || normalizado.equals("sí");
    }
%>

<%
    Object atributoConsultorios =
            request.getAttribute(
                    "consultorios"
            );

    List<Consultorio> consultorios;

    if (atributoConsultorios instanceof List<?>) {
        consultorios =
                (List<Consultorio>) atributoConsultorios;
    } else {
        consultorios =
                List.of();
    }

    Object atributoSedes =
            request.getAttribute(
                    "sedes"
            );

    List<Sede> sedes;

    if (atributoSedes instanceof List<?>) {
        sedes =
                (List<Sede>) atributoSedes;
    } else {
        sedes =
                List.of();
    }

    String mensajeExito =
            (String) request.getAttribute(
                    "mensajeExito"
            );

    String error =
            (String) request.getAttribute(
                    "error"
            );

    String idSede =
            (String) request.getAttribute(
                    "idSede"
            );

    String codigo =
            (String) request.getAttribute(
                    "codigo"
            );

    String nombre =
            (String) request.getAttribute(
                    "nombre"
            );

    String piso =
            (String) request.getAttribute(
                    "piso"
            );

    String ubicacionReferencia =
            (String) request.getAttribute(
                    "ubicacionReferencia"
            );

    String tipo =
            (String) request.getAttribute(
                    "tipo"
            );

    String capacidad =
            (String) request.getAttribute(
                    "capacidad"
            );

    String accesible =
            (String) request.getAttribute(
                    "accesible"
            );

    if (idSede == null) {
        idSede = "";
    }

    if (codigo == null) {
        codigo = "";
    }

    if (nombre == null) {
        nombre = "";
    }

    if (piso == null) {
        piso = "";
    }

    if (ubicacionReferencia == null) {
        ubicacionReferencia = "";
    }

    if (tipo == null || tipo.isBlank()) {
        tipo = "CONSULTA";
    }

    if (capacidad == null || capacidad.isBlank()) {
        capacidad = "1";
    }

    boolean accesibleMarcado =
            estaMarcado(
                    accesible
            );
%>

<!DOCTYPE html>
<html lang="es">

<head>
    <meta charset="UTF-8">

    <meta name="viewport"
          content="width=device-width, initial-scale=1">

    <meta name="robots"
          content="noindex, nofollow">

    <title>
        Consultorios | SoraCare-FPUNE
    </title>

    <link
            href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css"
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

        .contador-consultorios {
            background-color: #ccfbf1;
            color: #115e59;
        }

        .estado-disponible {
            background-color: #dcfce7;
            color: #166534;
        }

        .estado-mantenimiento {
            background-color: #fef3c7;
            color: #92400e;
        }

        .estado-inactivo {
            background-color: #e2e8f0;
            color: #475569;
        }

        .tipo-consultorio {
            background-color: #e0f2fe;
            color: #075985;
        }

        .accesible-si {
            background-color: #ccfbf1;
            color: #115e59;
        }

        .accesible-no {
            background-color: #f1f5f9;
            color: #64748b;
        }

        .tabla-consultorios th {
            white-space: nowrap;
        }

        .dato-secundario {
            color: #64748b;
            font-size: 0.9rem;
        }

        .boton-cerrar-sesion {
            min-width: 120px;
        }
    </style>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260815c">
</head>

<body class="app-shell app-admin">

<%@ include file="_navbar.jspf" %>

<main class="container py-4 py-md-5">

    <section class="card encabezado-modulo shadow-sm mb-4">

        <div class="card-body p-4 p-md-5">

            <div class="row align-items-center">

                <div class="col-12 col-lg-8">

                    <p class="mb-2 opacity-75">
                        Infraestructura asistencial
                    </p>

                    <h1 class="display-6 fw-bold mb-3">

                        Consultorios

                    </h1>

                    <p class="mb-0">

                        Administrá los espacios físicos donde
                        los profesionales de SoraCare realizarán
                        consultas, procedimientos, diagnósticos
                        y atención de emergencia.

                    </p>

                </div>

                <div class="col-12 col-lg-4 mt-4 mt-lg-0 text-lg-end">

                    <span class="badge contador-consultorios fs-6">

                        <%= consultorios.size() %>
                        consultorio<%= consultorios.size() == 1
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
                Administración de consultorios
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

        <strong>Registro correcto.</strong>

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

    <section class="row g-4">

        <div class="col-12 col-xl-5">

            <div class="card tarjeta-contenido shadow-sm">

                <div class="card-body p-4">

                    <h2 class="h5 mb-1">
                        Nuevo consultorio
                    </h2>

                    <p class="text-secondary mb-4">

                        Completá los datos del espacio
                        hospitalario.

                    </p>

                    <% if (sedes.isEmpty()) { %>

                    <div
                            class="alert alert-warning"
                            role="alert">

                        <strong>No existen sedes activas.</strong>

                        <div class="mt-1">

                            Para registrar un consultorio
                            primero debe existir una sede activa.

                        </div>

                    </div>

                    <a
                            href="${pageContext.request.contextPath}/admin/sedes"
                            class="btn btn-outline-primary w-100">

                        Ir a administración de sedes

                    </a>

                    <% } else { %>

                    <form
                            method="post"
                            action="${pageContext.request.contextPath}/admin/consultorios"><input type="hidden" name="csrfToken" value="${requestScope.csrfToken}">

                        <div class="row g-3">

                            <div class="col-12">

                                <label
                                        for="idSede"
                                        class="form-label fw-semibold">

                                    Sede

                                </label>

                                <select
                                        id="idSede"
                                        name="idSede"
                                        class="form-select"
                                        required>

                                    <option value="">
                                        Seleccioná una sede
                                    </option>

                                    <% for (Sede sede : sedes) { %>

                                    <option
                                            value="<%= sede.getIdSede() %>"
                                            <%= String.valueOf(
                                                    sede.getIdSede()
                                            ).equals(idSede)
                                                    ? "selected"
                                                    : "" %>>

                                        <%= escaparHtml(
                                                sede.getNombre()
                                        ) %>

                                        -
                                        <%= escaparHtml(
                                                sede.getCodigo()
                                        ) %>

                                    </option>

                                    <% } %>

                                </select>

                            </div>

                            <div class="col-12 col-md-5">

                                <label
                                        for="codigo"
                                        class="form-label fw-semibold">

                                    Código

                                </label>

                                <input
                                        type="text"
                                        id="codigo"
                                        name="codigo"
                                        class="form-control"
                                        value="<%= escaparHtml(codigo) %>"
                                        maxlength="30"
                                        required
                                        autocomplete="off"
                                        placeholder="CONS-101">

                            </div>

                            <div class="col-12 col-md-7">

                                <label
                                        for="nombre"
                                        class="form-label fw-semibold">

                                    Nombre

                                </label>

                                <input
                                        type="text"
                                        id="nombre"
                                        name="nombre"
                                        class="form-control"
                                        value="<%= escaparHtml(nombre) %>"
                                        maxlength="100"
                                        required
                                        autocomplete="off"
                                        placeholder="Consultorio 101">

                            </div>

                            <div class="col-12 col-md-5">

                                <label
                                        for="piso"
                                        class="form-label fw-semibold">

                                    Piso

                                </label>

                                <input
                                        type="text"
                                        id="piso"
                                        name="piso"
                                        class="form-control"
                                        value="<%= escaparHtml(piso) %>"
                                        maxlength="20"
                                        autocomplete="off"
                                        placeholder="2">

                            </div>

                            <div class="col-12 col-md-7">

                                <label
                                        for="tipo"
                                        class="form-label fw-semibold">

                                    Tipo

                                </label>

                                <select
                                        id="tipo"
                                        name="tipo"
                                        class="form-select"
                                        required>

                                    <option
                                            value="CONSULTA"
                                            <%= "CONSULTA".equalsIgnoreCase(tipo)
                                                    ? "selected"
                                                    : "" %>>

                                        Consulta

                                    </option>

                                    <option
                                            value="PROCEDIMIENTO"
                                            <%= "PROCEDIMIENTO".equalsIgnoreCase(tipo)
                                                    ? "selected"
                                                    : "" %>>

                                        Procedimiento

                                    </option>

                                    <option
                                            value="DIAGNOSTICO"
                                            <%= "DIAGNOSTICO".equalsIgnoreCase(tipo)
                                                    ? "selected"
                                                    : "" %>>

                                        Diagnóstico

                                    </option>

                                    <option
                                            value="EMERGENCIA"
                                            <%= "EMERGENCIA".equalsIgnoreCase(tipo)
                                                    ? "selected"
                                                    : "" %>>

                                        Emergencia

                                    </option>

                                </select>

                            </div>

                            <div class="col-12">

                                <label
                                        for="ubicacionReferencia"
                                        class="form-label fw-semibold">

                                    Referencia de ubicación

                                </label>

                                <input
                                        type="text"
                                        id="ubicacionReferencia"
                                        name="ubicacionReferencia"
                                        class="form-control"
                                        value="<%= escaparHtml(
                                                ubicacionReferencia
                                        ) %>"
                                        maxlength="255"
                                        autocomplete="off"
                                        placeholder="Segundo piso, ala norte">

                            </div>

                            <div class="col-12 col-md-6">

                                <label
                                        for="capacidad"
                                        class="form-label fw-semibold">

                                    Capacidad

                                </label>

                                <input
                                        type="number"
                                        id="capacidad"
                                        name="capacidad"
                                        class="form-control"
                                        value="<%= escaparHtml(capacidad) %>"
                                        min="1"
                                        max="50"
                                        required>

                                <div class="form-text">
                                    Entre 1 y 50 personas.
                                </div>

                            </div>

                            <div class="col-12 col-md-6">

                                <label
                                        class="form-label fw-semibold">

                                    Accesibilidad

                                </label>

                                <div
                                        class="form-check
                                               border
                                               rounded-3
                                               px-5 py-2">

                                    <input
                                            class="form-check-input"
                                            type="checkbox"
                                            id="accesible"
                                            name="accesible"
                                            value="true"
                                        <%= accesibleMarcado
                                                    ? "checked"
                                                    : "" %>>

                                    <label
                                            class="form-check-label"
                                            for="accesible">

                                        Espacio accesible

                                    </label>

                                </div>

                            </div>

                            <div class="col-12">

                                <div class="form-text mb-3">

                                    El código debe ser único dentro
                                    de la sede seleccionada.

                                </div>

                                <button
                                        type="submit"
                                        class="btn btn-primary boton-principal w-100">

                                    Registrar consultorio

                                </button>

                            </div>

                        </div>

                    </form>

                    <% } %>

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
                                Consultorios registrados
                            </h2>

                            <p class="text-secondary mb-0">
                                Listado general de espacios hospitalarios
                            </p>

                        </div>

                        <span class="badge contador-consultorios">

                            Total: <%= consultorios.size() %>

                        </span>

                    </div>

                    <% if (consultorios.isEmpty()) { %>

                    <div class="text-center py-5">

                        <h3 class="h5">
                            Todavía no existen consultorios
                        </h3>

                        <p class="text-secondary mb-0">

                            Utilizá el formulario para registrar
                            el primer consultorio.

                        </p>

                    </div>

                    <% } else { %>

                    <div class="table-responsive">

                        <table
                                class="table table-hover
                                       align-middle
                                       tabla-consultorios">

                            <thead>

                            <tr>

                                <th scope="col">
                                    Código
                                </th>

                                <th scope="col">
                                    Consultorio
                                </th>

                                <th scope="col">
                                    Sede
                                </th>

                                <th scope="col">
                                    Tipo
                                </th>

                                <th scope="col">
                                    Capacidad
                                </th>

                                <th scope="col">
                                    Accesibilidad
                                </th>

                                <th scope="col">
                                    Estado
                                </th>

                            </tr>

                            </thead>

                            <tbody>

                            <% for (
                                    Consultorio consultorio
                                    : consultorios
                            ) { %>

                            <tr>

                                <td>

                                    <span class="badge text-bg-light">

                                        <%= escaparHtml(
                                                consultorio.getCodigo()
                                        ) %>

                                    </span>

                                </td>

                                <td>

                                    <strong>

                                        <%= escaparHtml(
                                                consultorio.getNombre()
                                        ) %>

                                    </strong>

                                    <div class="dato-secundario">

                                        ID:
                                        <%= consultorio
                                                .getIdConsultorio() %>

                                    </div>

                                    <% if (
                                            consultorio.getPiso() != null
                                                    && !consultorio
                                                    .getPiso()
                                                    .isBlank()
                                    ) { %>

                                    <div class="dato-secundario">

                                        Piso:
                                        <%= escaparHtml(
                                                consultorio.getPiso()
                                        ) %>

                                    </div>

                                    <% } %>

                                    <% if (
                                            consultorio
                                                    .getUbicacionReferencia()
                                                    != null
                                                    && !consultorio
                                                    .getUbicacionReferencia()
                                                    .isBlank()
                                    ) { %>

                                    <div class="dato-secundario">

                                        <%= escaparHtml(
                                                consultorio
                                                        .getUbicacionReferencia()
                                        ) %>

                                    </div>

                                    <% } %>

                                </td>

                                <td>

                                    <strong>

                                        <%= escaparHtml(
                                                obtenerNombreSede(
                                                        sedes,
                                                        consultorio
                                                                .getIdSede()
                                                )
                                        ) %>

                                    </strong>

                                    <div class="dato-secundario">

                                        ID sede:
                                        <%= consultorio.getIdSede() %>

                                    </div>

                                </td>

                                <td>

                                    <span class="badge tipo-consultorio">

                                        <%= escaparHtml(
                                                consultorio
                                                        .getTipo()
                                                        .name()
                                        ) %>

                                    </span>

                                </td>

                                <td>

                                    <%= consultorio.getCapacidad() %>

                                </td>

                                <td>

                                    <% if (
                                            consultorio.isAccesible()
                                    ) { %>

                                    <span class="badge accesible-si">

                                        Sí

                                    </span>

                                    <% } else { %>

                                    <span class="badge accesible-no">

                                        No

                                    </span>

                                    <% } %>

                                </td>

                                <td>

                                    <% if (
                                            "DISPONIBLE".equals(
                                                    consultorio
                                                            .getEstado()
                                                            .name()
                                            )
                                    ) { %>

                                    <span class="badge estado-disponible">

                                        Disponible

                                    </span>

                                    <% } else if (
                                            "MANTENIMIENTO".equals(
                                                    consultorio
                                                            .getEstado()
                                                            .name()
                                            )
                                    ) { %>

                                    <span class="badge estado-mantenimiento">

                                        Mantenimiento

                                    </span>

                                    <% } else { %>

                                    <span class="badge estado-inactivo">

                                        Inactivo

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
        src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js">
</script>

</body>
</html>
