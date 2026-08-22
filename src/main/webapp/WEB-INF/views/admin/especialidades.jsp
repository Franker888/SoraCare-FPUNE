<%@ page contentType="text/html;charset=UTF-8"
         pageEncoding="UTF-8"
         language="java" %>

<%@ page import="py.fpune.soracare.mvc.model.Especialidad" %>
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
%>

<%
    Object atributoEspecialidades =
            request.getAttribute(
                    "especialidades"
            );

    List<Especialidad> especialidades;

    if (atributoEspecialidades instanceof List<?>) {
        especialidades =
                (List<Especialidad>) atributoEspecialidades;
    } else {
        especialidades =
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

    String nombre =
            (String) request.getAttribute(
                    "nombre"
            );

    String descripcion =
            (String) request.getAttribute(
                    "descripcion"
            );

    String duracionConsultaMinutos =
            (String) request.getAttribute(
                    "duracionConsultaMinutos"
            );

    if (nombre == null) {
        nombre = "";
    }

    if (descripcion == null) {
        descripcion = "";
    }

    if (duracionConsultaMinutos == null
            || duracionConsultaMinutos.isBlank()) {

        duracionConsultaMinutos =
                "30";
    }
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
        Especialidades | SoraCare-FPUNE
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

        .tabla-especialidades th {
            white-space: nowrap;
        }

        .descripcion-especialidad {
            max-width: 360px;
        }

        .estado-activo {
            background-color: #ccfbf1;
            color: #115e59;
        }

        .estado-inactivo {
            background-color: #e2e8f0;
            color: #475569;
        }

        .contador-especialidades {
            background-color: #ccfbf1;
            color: #115e59;
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
                        Gestión médica
                    </p>

                    <h1 class="display-6 fw-bold mb-3">

                        Especialidades médicas

                    </h1>

                    <p class="mb-0">

                        Registrá y consultá las especialidades
                        disponibles para organizar la atención,
                        los profesionales y las futuras agendas.

                    </p>

                </div>

                <div class="col-12 col-lg-4 mt-4 mt-lg-0 text-lg-end">

                    <span class="badge contador-especialidades fs-6">

                        <%= especialidades.size() %>
                        especialidad<%= especialidades.size() == 1
                            ? ""
                            : "es" %>

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
                Administración de especialidades
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

        <div class="col-12 col-xl-4">

            <div class="card tarjeta-contenido shadow-sm">

                <div class="card-body p-4">

                    <h2 class="h5 mb-1">
                        Nueva especialidad
                    </h2>

                    <p class="text-secondary mb-4">

                        Completá los datos para habilitar
                        una nueva especialidad médica.

                    </p>

                    <form
                            method="post"
                            action="${pageContext.request.contextPath}/admin/especialidades">

                        <div class="mb-3">

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
                                    placeholder="Ejemplo: Cardiología">

                            <div class="form-text">
                                Debe ser único dentro del sistema.
                            </div>

                        </div>

                        <div class="mb-3">

                            <label
                                    for="descripcion"
                                    class="form-label fw-semibold">

                                Descripción

                            </label>

                            <textarea
                                    id="descripcion"
                                    name="descripcion"
                                    class="form-control"
                                    rows="5"
                                    maxlength="500"
                                    placeholder="Descripción general de la especialidad"><%= escaparHtml(descripcion) %></textarea>

                            <div class="form-text">
                                Campo opcional. Máximo 500 caracteres.
                            </div>

                        </div>

                        <div class="mb-4">

                            <label
                                    for="duracionConsultaMinutos"
                                    class="form-label fw-semibold">

                                Duración de consulta

                            </label>

                            <div class="input-group">

                                <input
                                        type="number"
                                        id="duracionConsultaMinutos"
                                        name="duracionConsultaMinutos"
                                        class="form-control"
                                        value="<%= escaparHtml(
                                                duracionConsultaMinutos
                                        ) %>"
                                        min="10"
                                        max="240"
                                        step="1"
                                        required>

                                <span class="input-group-text">
                                    minutos
                                </span>

                            </div>

                            <div class="form-text">
                                Valor permitido: entre 10 y 240 minutos.
                            </div>

                        </div>

                        <button
                                type="submit"
                                class="btn btn-primary boton-principal w-100">

                            Registrar especialidad

                        </button>

                    </form>

                </div>

            </div>

        </div>

        <div class="col-12 col-xl-8">

            <div class="card tarjeta-contenido shadow-sm">

                <div class="card-body p-4">

                    <div class="d-flex flex-column
                                flex-md-row
                                justify-content-between
                                align-items-md-center
                                gap-2 mb-4">

                        <div>

                            <h2 class="h5 mb-1">
                                Especialidades registradas
                            </h2>

                            <p class="text-secondary mb-0">
                                Listado general ordenado por nombre
                            </p>

                        </div>

                        <span class="badge contador-especialidades">

                            Total: <%= especialidades.size() %>

                        </span>

                    </div>

                    <% if (especialidades.isEmpty()) { %>

                    <div class="text-center py-5">

                        <h3 class="h5">
                            Todavía no existen especialidades
                        </h3>

                        <p class="text-secondary mb-0">

                            Utilizá el formulario para registrar
                            la primera especialidad médica.

                        </p>

                    </div>

                    <% } else { %>

                    <div class="table-responsive">

                        <table
                                class="table table-hover
                                       align-middle
                                       tabla-especialidades">

                            <thead>

                            <tr>

                                <th scope="col">
                                    ID
                                </th>

                                <th scope="col">
                                    Especialidad
                                </th>

                                <th scope="col">
                                    Descripción
                                </th>

                                <th scope="col">
                                    Duración
                                </th>

                                <th scope="col">
                                    Estado
                                </th>

                            </tr>

                            </thead>

                            <tbody>

                            <% for (
                                    Especialidad especialidad
                                    : especialidades
                            ) { %>

                            <tr>

                                <td>

                                    <%= especialidad
                                            .getIdEspecialidad() %>

                                </td>

                                <td>

                                    <strong>

                                        <%= escaparHtml(
                                                especialidad.getNombre()
                                        ) %>

                                    </strong>

                                </td>

                                <td class="descripcion-especialidad">

                                    <% if (
                                            especialidad.getDescripcion()
                                                    == null
                                                    || especialidad
                                                    .getDescripcion()
                                                    .isBlank()
                                    ) { %>

                                    <span class="text-secondary">
                                        Sin descripción
                                    </span>

                                    <% } else { %>

                                    <%= escaparHtml(
                                            especialidad.getDescripcion()
                                    ) %>

                                    <% } %>

                                </td>

                                <td>

                                    <%= especialidad
                                            .getDuracionConsultaMinutos() %>
                                    min

                                </td>

                                <td>

                                    <% if (especialidad.isActivo()) { %>

                                    <span class="badge estado-activo">
                                        Activa
                                    </span>

                                    <% } else { %>

                                    <span class="badge estado-inactivo">
                                        Inactiva
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
