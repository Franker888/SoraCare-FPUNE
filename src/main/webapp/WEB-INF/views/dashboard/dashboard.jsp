<%@ page contentType="text/html;charset=UTF-8"
         pageEncoding="UTF-8"
         language="java" %>

<%@ page import="py.fpune.soracare.dto.UsuarioAutenticadoDTO" %>
<%@ page import="jakarta.servlet.http.HttpServletResponse" %>
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
    UsuarioAutenticadoDTO usuarioAutenticado =
            (UsuarioAutenticadoDTO) request.getAttribute(
                    "usuarioAutenticado"
            );

    if (usuarioAutenticado == null) {
        response.sendError(
                HttpServletResponse.SC_UNAUTHORIZED,
                "No existe un usuario autenticado."
        );

        return;
    }

    Set<String> roles =
            usuarioAutenticado.getRoles();

    String inicialUsuario =
            usuarioAutenticado
                    .getNombreUsuario()
                    .substring(0, 1)
                    .toUpperCase();

    boolean esAdministrador =
            usuarioAutenticado.tieneRol(
                    "ADMINISTRADOR"
            );

    boolean puedeGestionarAgenda =
            esAdministrador
                    || usuarioAutenticado.tieneRol(
                    "RECEPCIONISTA"
            );

    boolean esPaciente =
            usuarioAutenticado.tieneRol(
                    "PACIENTE"
            );

    String nombrePanel = esAdministrador
            ? "Panel de administración"
            : puedeGestionarAgenda
                    ? "Panel de recepción"
                    : esPaciente
                            ? "Portal del paciente"
                            : "Panel principal";

    String descripcionPanel = esAdministrador
            ? "Control general de la operación de SoraCare"
            : puedeGestionarAgenda
                    ? "Citas, pacientes y cobros de recepción"
                    : esPaciente
                            ? "Tus citas, pagos y servicios médicos"
                            : "Accesos disponibles para tu cuenta";
%>

<!DOCTYPE html>
<html lang="es">

<head>
    <meta charset="UTF-8">

    <meta name="viewport"
          content="width=device-width, initial-scale=1">

    <meta name="robots"
          content="noindex, nofollow">

    <title>Panel principal | SoraCare-FPUNE</title>

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

        .tarjeta-bienvenida {
            border: none;
            border-radius: 1rem;
        }

        .icono-usuario {
            width: 64px;
            height: 64px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            background-color: #ccfbf1;
            color: #0f766e;
            font-size: 1.5rem;
            font-weight: 700;
        }

        .tarjeta-modulo {
            border: none;
            border-radius: 1rem;
            transition:
                    transform 0.2s ease,
                    box-shadow 0.2s ease;
        }

        .tarjeta-modulo:hover {
            transform: translateY(-3px);
            box-shadow:
                    0 0.75rem 1.5rem
                    rgba(15, 23, 42, 0.10);
        }

        .etiqueta-rol {
            background-color: #ccfbf1;
            color: #115e59;
        }

        .boton-cerrar-sesion {
            min-width: 120px;
        }
    </style>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css">
</head>

<body class="app-shell app-dashboard">

<nav class="navbar barra-superior shadow-sm">

    <div class="container">

        <a
                class="navbar-brand marca-soracare"
                href="${pageContext.request.contextPath}/dashboard">

            SoraCare

        </a>

        <div class="d-flex align-items-center gap-3">

            <span class="navbar-text text-white d-none d-md-inline">
                <%= escaparHtml(nombrePanel) %>
            </span>

            <a href="${pageContext.request.contextPath}/"
               class="btn btn-outline-light btn-sm d-none d-sm-inline-flex">
                Página principal
            </a>

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

    <% if (usuarioAutenticado.isDebeCambiarContrasena()) { %>

    <div
            class="alert alert-warning"
            role="alert">

        <strong>Cambio de contraseña pendiente.</strong>

        Por seguridad, esta cuenta deberá establecer
        una nueva contraseña antes de utilizar
        las funciones privadas del sistema.

    </div>

    <% } %>

    <section class="card tarjeta-bienvenida shadow-sm mb-4">

        <div class="card-body p-4">

            <div class="d-flex flex-column
                        flex-md-row
                        align-items-md-center
                        gap-3">

                <div class="icono-usuario">

                    <%= escaparHtml(inicialUsuario) %>

                </div>

                <div class="flex-grow-1">

                    <p class="text-secondary mb-1">
                        <%= escaparHtml(nombrePanel) %>
                    </p>

                    <h1 class="h3 mb-2">

                        <%= escaparHtml(
                                usuarioAutenticado
                                        .getNombreUsuario()
                        ) %>

                    </h1>

                    <div class="d-flex flex-wrap gap-2">

                        <% for (String rol : roles) { %>

                        <span class="badge etiqueta-rol">

                                <%= escaparHtml(rol) %>

                            </span>

                        <% } %>

                    </div>

                </div>

                <div class="text-md-end">

                    <small class="text-secondary d-block">
                        ID de usuario
                    </small>

                    <strong>
                        <%= usuarioAutenticado.getIdUsuario() %>
                    </strong>

                </div>

            </div>

        </div>

    </section>

    <section>

        <div class="mb-3">

            <h2 class="h4 mb-1">
                <%= escaparHtml(nombrePanel) %>
            </h2>

            <p class="text-secondary mb-0">
                <%= escaparHtml(descripcionPanel) %>
            </p>

        </div>

        <div class="row g-4">

            <% if (puedeGestionarAgenda) { %>

            <div class="col-12 col-md-6 col-xl-4">

                <div class="card tarjeta-modulo shadow-sm h-100">

                    <div class="card-body p-4">

                        <div class="module-icon">
                            <i class="bi bi-people"></i>
                        </div>

                        <h3 class="h5">
                            Pacientes
                        </h3>

                        <p class="text-secondary">
                            Registrar pacientes y consultar
                            la información disponible.
                        </p>

                        <a
                                href="${pageContext.request.contextPath}/pacientes/registro"
                                class="btn btn-outline-success">

                            Ir a pacientes

                        </a>

                    </div>

                </div>

            </div>

            <% } %>

            <div class="col-12 col-md-6 col-xl-4">

                <div class="card tarjeta-modulo shadow-sm h-100">

                    <div class="card-body p-4">

                        <div class="module-icon">
                            <i class="bi bi-calendar2-check"></i>
                        </div>

                        <h3 class="h5">
                            <%= esPaciente
                                    ? "Mis citas"
                                    : "Agenda médica" %>
                        </h3>

                        <p class="text-secondary">
                            <%= esPaciente
                                    ? "Revisar tus citas pendientes y confirmadas, o agendar una nueva consulta."
                                    : "Consultar turnos disponibles, solicitar citas y administrar la agenda diaria." %>
                        </p>

                        <% if (puedeGestionarAgenda) { %>

                        <a
                                href="${pageContext.request.contextPath}/agenda/citas"
                                class="btn btn-outline-success">

                            Ir a agenda

                        </a>

                        <% } else if (esPaciente) { %>

                        <a
                                href="${pageContext.request.contextPath}/mis-citas"
                                class="btn btn-outline-success">

                            Ir a mis citas

                        </a>

                        <% } else { %>

                        <a
                                href="${pageContext.request.contextPath}/disponibilidad"
                                class="btn btn-outline-success">

                            Ver disponibilidad

                        </a>

                        <% } %>

                    </div>

                </div>

            </div>

            <% if (esPaciente) { %>

            <div class="col-12 col-md-6 col-xl-4">

                <div class="card tarjeta-modulo shadow-sm h-100">

                    <div class="card-body p-4">

                        <div class="module-icon">
                            <i class="bi bi-person-badge"></i>
                        </div>

                        <h3 class="h5">Médicos</h3>

                        <p class="text-secondary">
                            Conocé a los profesionales activos y revisá
                            la disponibilidad de atención.
                        </p>

                        <a href="${pageContext.request.contextPath}/medicos"
                           class="btn btn-outline-success">
                            Ver médicos
                        </a>

                    </div>

                </div>

            </div>

            <div class="col-12 col-md-6 col-xl-4">

                <div class="card tarjeta-modulo shadow-sm h-100">

                    <div class="card-body p-4">

                        <div class="module-icon">
                            <i class="bi bi-heart-pulse"></i>
                        </div>

                        <h3 class="h5">Especialidades</h3>

                        <p class="text-secondary">
                            Explorá las áreas médicas disponibles
                            para encontrar la atención adecuada.
                        </p>

                        <a href="${pageContext.request.contextPath}/especialidades"
                           class="btn btn-outline-success">
                            Ver especialidades
                        </a>

                    </div>

                </div>

            </div>

            <% } %>

            <% if (puedeGestionarAgenda) { %>

            <div class="col-12 col-md-6 col-xl-4">

                <div class="card tarjeta-modulo shadow-sm h-100">

                    <div class="card-body p-4">

                        <div class="module-icon">
                            <i class="bi bi-credit-card"></i>
                        </div>

                        <h3 class="h5">Pagos y comprobantes</h3>

                        <p class="text-secondary">
                            Confirmar pagos en efectivo y consultar
                            las operaciones simuladas de las citas.
                        </p>

                        <a href="${pageContext.request.contextPath}/agenda/pagos"
                           class="btn btn-outline-success">
                            Ir a pagos
                        </a>

                    </div>

                </div>

            </div>

            <% } %>

            <% if (esAdministrador) { %>

            <div class="col-12 col-md-6 col-xl-4">

                <div class="card tarjeta-modulo shadow-sm h-100">

                    <div class="card-body p-4">

                        <div class="module-icon">
                            <i class="bi bi-sliders"></i>
                        </div>

                        <h3 class="h5">
                            Administración
                        </h3>

                        <p class="text-secondary">
                            Gestión de usuarios, roles,
                            reportes y configuración.
                        </p>

                        <a
                                href="${pageContext.request.contextPath}/admin/inicio"
                                class="btn btn-outline-success">

                            Ir a administración

                        </a>

                    </div>

                </div>

            </div>

            <% } %>

        </div>

    </section>

</main>

<script
        src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js">
</script>

</body>
</html>
