<%@ page contentType="text/html;charset=UTF-8"
         pageEncoding="UTF-8"
         language="java" %>

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
%>

<%
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

    String codigo =
            (String) request.getAttribute(
                    "codigo"
            );

    String nombre =
            (String) request.getAttribute(
                    "nombre"
            );

    String direccion =
            (String) request.getAttribute(
                    "direccion"
            );

    String ciudad =
            (String) request.getAttribute(
                    "ciudad"
            );

    String departamento =
            (String) request.getAttribute(
                    "departamento"
            );

    String telefono =
            (String) request.getAttribute(
                    "telefono"
            );

    String correo =
            (String) request.getAttribute(
                    "correo"
            );

    if (codigo == null) {
        codigo = "";
    }

    if (nombre == null) {
        nombre = "";
    }

    if (direccion == null) {
        direccion = "";
    }

    if (ciudad == null) {
        ciudad = "";
    }

    if (departamento == null) {
        departamento = "";
    }

    if (telefono == null) {
        telefono = "";
    }

    if (correo == null) {
        correo = "";
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
        Sedes | SoraCare-FPUNE
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

        .form-control {
            border-radius: 0.75rem;
        }

        .form-control:focus {
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

        .contador-sedes {
            background-color: #ccfbf1;
            color: #115e59;
        }

        .estado-activo {
            background-color: #ccfbf1;
            color: #115e59;
        }

        .estado-inactivo {
            background-color: #e2e8f0;
            color: #475569;
        }

        .tabla-sedes th {
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
                        Infraestructura hospitalaria
                    </p>

                    <h1 class="display-6 fw-bold mb-3">

                        Sedes hospitalarias

                    </h1>

                    <p class="mb-0">

                        Registrá y consultá las ubicaciones físicas
                        donde funcionarán los consultorios,
                        profesionales y servicios de SoraCare.

                    </p>

                </div>

                <div class="col-12 col-lg-4 mt-4 mt-lg-0 text-lg-end">

                    <span class="badge contador-sedes fs-6">

                        <%= sedes.size() %>
                        sede<%= sedes.size() == 1
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
                Administración de sedes
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
                        Nueva sede
                    </h2>

                    <p class="text-secondary mb-4">

                        Completá los datos de la ubicación
                        hospitalaria.

                    </p>

                    <form
                            method="post"
                            action="${pageContext.request.contextPath}/admin/sedes"><input type="hidden" name="csrfToken" value="${requestScope.csrfToken}">

                        <div class="row g-3">

                            <div class="col-12 col-md-4">

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
                                        maxlength="20"
                                        required
                                        autocomplete="off"
                                        placeholder="CDE-01">

                            </div>

                            <div class="col-12 col-md-8">

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
                                        maxlength="120"
                                        required
                                        autocomplete="off"
                                        placeholder="Sede Central">

                            </div>

                            <div class="col-12">

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
                                        required
                                        autocomplete="street-address"
                                        placeholder="Avenida y número">

                            </div>

                            <div class="col-12 col-md-6">

                                <label
                                        for="ciudad"
                                        class="form-label fw-semibold">

                                    Ciudad

                                </label>

                                <input
                                        type="text"
                                        id="ciudad"
                                        name="ciudad"
                                        class="form-control"
                                        value="<%= escaparHtml(ciudad) %>"
                                        maxlength="100"
                                        required
                                        autocomplete="address-level2"
                                        placeholder="Ciudad del Este">

                            </div>

                            <div class="col-12 col-md-6">

                                <label
                                        for="departamento"
                                        class="form-label fw-semibold">

                                    Departamento

                                </label>

                                <input
                                        type="text"
                                        id="departamento"
                                        name="departamento"
                                        class="form-control"
                                        value="<%= escaparHtml(departamento) %>"
                                        maxlength="100"
                                        autocomplete="address-level1"
                                        placeholder="Alto Paraná">

                            </div>

                            <div class="col-12 col-md-6">

                                <label
                                        for="telefono"
                                        class="form-label fw-semibold">

                                    Teléfono

                                </label>

                                <input
                                        type="tel"
                                        id="telefono"
                                        name="telefono"
                                        class="form-control"
                                        value="<%= escaparHtml(telefono) %>"
                                        maxlength="30"
                                        autocomplete="tel"
                                        placeholder="061 500 000">

                            </div>

                            <div class="col-12 col-md-6">

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
                                        autocomplete="email"
                                        placeholder="sede@soracare.test">

                            </div>

                            <div class="col-12">

                                <div class="form-text mb-3">

                                    El código y el nombre deben ser
                                    únicos dentro del sistema.

                                </div>

                                <button
                                        type="submit"
                                        class="btn btn-primary boton-principal w-100">

                                    Registrar sede

                                </button>

                            </div>

                        </div>

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
                                Sedes registradas
                            </h2>

                            <p class="text-secondary mb-0">
                                Listado general ordenado por nombre
                            </p>

                        </div>

                        <span class="badge contador-sedes">

                            Total: <%= sedes.size() %>

                        </span>

                    </div>

                    <% if (sedes.isEmpty()) { %>

                    <div class="text-center py-5">

                        <h3 class="h5">
                            Todavía no existen sedes
                        </h3>

                        <p class="text-secondary mb-0">

                            Utilizá el formulario para registrar
                            la primera sede hospitalaria.

                        </p>

                    </div>

                    <% } else { %>

                    <div class="table-responsive">

                        <table
                                class="table table-hover
                                       align-middle
                                       tabla-sedes">

                            <thead>

                            <tr>

                                <th scope="col">
                                    Código
                                </th>

                                <th scope="col">
                                    Sede
                                </th>

                                <th scope="col">
                                    Ubicación
                                </th>

                                <th scope="col">
                                    Contacto
                                </th>

                                <th scope="col">
                                    Estado
                                </th>

                            </tr>

                            </thead>

                            <tbody>

                            <% for (Sede sede : sedes) { %>

                            <tr>

                                <td>

                                    <span class="badge text-bg-light">

                                        <%= escaparHtml(
                                                sede.getCodigo()
                                        ) %>

                                    </span>

                                </td>

                                <td>

                                    <strong>

                                        <%= escaparHtml(
                                                sede.getNombre()
                                        ) %>

                                    </strong>

                                    <div class="dato-secundario">

                                        ID:
                                        <%= sede.getIdSede() %>

                                    </div>

                                </td>

                                <td>

                                    <div>

                                        <%= escaparHtml(
                                                sede.getDireccion()
                                        ) %>

                                    </div>

                                    <div class="dato-secundario">

                                        <%= escaparHtml(
                                                sede.getCiudad()
                                        ) %>

                                        <% if (
                                                sede.getDepartamento()
                                                        != null
                                                        && !sede
                                                        .getDepartamento()
                                                        .isBlank()
                                        ) { %>

                                        ·
                                        <%= escaparHtml(
                                                sede.getDepartamento()
                                        ) %>

                                        <% } %>

                                    </div>

                                </td>

                                <td>

                                    <% if (
                                            sede.getTelefono() == null
                                                    && sede.getCorreo()
                                                    == null
                                    ) { %>

                                    <span class="text-secondary">
                                        Sin contacto
                                    </span>

                                    <% } else { %>

                                    <% if (
                                            sede.getTelefono() != null
                                    ) { %>

                                    <div>

                                        <%= escaparHtml(
                                                sede.getTelefono()
                                        ) %>

                                    </div>

                                    <% } %>

                                    <% if (
                                            sede.getCorreo() != null
                                    ) { %>

                                    <div class="dato-secundario">

                                        <%= escaparHtml(
                                                sede.getCorreo()
                                        ) %>

                                    </div>

                                    <% } %>

                                    <% } %>

                                </td>

                                <td>

                                    <% if (sede.isActivo()) { %>

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
