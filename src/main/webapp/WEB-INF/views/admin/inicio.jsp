<%@ page contentType="text/html;charset=UTF-8"
         pageEncoding="UTF-8"
         language="java" %>

<%@ page import="py.fpune.soracare.dto.UsuarioAutenticadoDTO" %>
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
        Administración | SoraCare-FPUNE
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

        .encabezado-panel {
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

        .tarjeta-opcion {
            border: none;
            border-radius: 1rem;
            transition:
                    transform 0.2s ease,
                    box-shadow 0.2s ease;
        }

        .tarjeta-opcion:hover {
            transform: translateY(-3px);
            box-shadow: 0 0.75rem 1.5rem
            rgba(15, 23, 42, 0.10);
        }

        .etiqueta-rol {
            background-color: #ccfbf1;
            color: #115e59;
        }

        .icono-modulo {
            width: 48px;
            height: 48px;
            border-radius: 0.75rem;
            display: flex;
            align-items: center;
            justify-content: center;
            background-color: #ccfbf1;
            color: #0f766e;
            font-size: 1.25rem;
            font-weight: 700;
        }

        .boton-cerrar-sesion {
            min-width: 120px;
        }
    </style>
</head>

<body>

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

    <section class="card encabezado-panel shadow-sm mb-4">

        <div class="card-body p-4 p-md-5">

            <div class="row align-items-center">

                <div class="col-12 col-lg-8">

                    <p class="mb-2 opacity-75">
                        Panel protegido por rol
                    </p>

                    <h1 class="display-6 fw-bold mb-3">

                        Administración de SoraCare

                    </h1>

                    <p class="mb-0">

                        Desde esta sección se gestionarán los usuarios,
                        roles, configuraciones, reportes y registros
                        de auditoría del sistema.

                    </p>

                </div>

                <div class="col-12 col-lg-4 mt-4 mt-lg-0 text-lg-end">

                    <p class="mb-1 opacity-75">
                        Usuario autenticado
                    </p>

                    <strong class="fs-5">

                        <%= escaparHtml(
                                usuarioAutenticado
                                        .getNombreUsuario()
                        ) %>

                    </strong>

                    <div class="d-flex flex-wrap
                                justify-content-lg-end
                                gap-2 mt-2">

                        <% for (String rol : roles) { %>

                        <span class="badge etiqueta-rol">

                                <%= escaparHtml(rol) %>

                            </span>

                        <% } %>

                    </div>

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
                Funciones administrativas
            </h2>

            <p class="text-secondary mb-0">
                Accesos exclusivos para administradores autorizados
            </p>

        </div>

        <a
                href="${pageContext.request.contextPath}/dashboard"
                class="btn btn-outline-secondary">

            Volver al panel principal

        </a>

    </div>

    <section class="row g-4">
        <div class="col-12 col-md-6 col-xl-4">

            <div class="card tarjeta-opcion shadow-sm h-100">

                <div class="card-body p-4">

                    <div class="icono-modulo mb-3">
                        E
                    </div>

                    <h3 class="h5">
                        Especialidades médicas
                    </h3>

                    <p class="text-secondary">

                        Registrar y consultar las especialidades
                        disponibles para médicos y futuras agendas.
                    </p>

                    <a
                            href="${pageContext.request.contextPath}/admin/especialidades"
                            class="btn btn-outline-success">

                        Administrar especialidades
                    </a>
                </div>
            </div>
        </div>

        <div class="col-12 col-md-6 col-xl-4">

            <div class="card tarjeta-opcion shadow-sm h-100">

                <div class="card-body p-4">

                    <div class="icono-modulo mb-3">
                        U
                    </div>

                    <h3 class="h5">
                        Usuarios
                    </h3>

                    <p class="text-secondary">

                        Crear cuentas, consultar usuarios,
                        activar, bloquear o inactivar accesos.

                    </p>

                    <button
                            type="button"
                            class="btn btn-outline-secondary"
                            disabled>

                        Próximamente

                    </button>

                </div>

            </div>

        </div>

        <div class="col-12 col-md-6 col-xl-4">

            <div class="card tarjeta-opcion shadow-sm h-100">

                <div class="card-body p-4">

                    <div class="icono-modulo mb-3">
                        R
                    </div>

                    <h3 class="h5">
                        Roles y permisos
                    </h3>

                    <p class="text-secondary">

                        Administrar roles y controlar
                        qué funciones puede utilizar cada cuenta.

                    </p>

                    <button
                            type="button"
                            class="btn btn-outline-secondary"
                            disabled>

                        Próximamente

                    </button>

                </div>

            </div>

        </div>

        <div class="col-12 col-md-6 col-xl-4">

            <div class="card tarjeta-opcion shadow-sm h-100">

                <div class="card-body p-4">

                    <div class="icono-modulo mb-3">
                        A
                    </div>

                    <h3 class="h5">
                        Auditoría
                    </h3>

                    <p class="text-secondary">

                        Consultar las acciones realizadas
                        por los usuarios dentro del sistema.

                    </p>

                    <button
                            type="button"
                            class="btn btn-outline-secondary"
                            disabled>

                        Próximamente

                    </button>

                </div>

            </div>

        </div>

        <div class="col-12 col-md-6 col-xl-4">

            <div class="card tarjeta-opcion shadow-sm h-100">

                <div class="card-body p-4">

                    <div class="icono-modulo mb-3">
                        C
                    </div>

                    <h3 class="h5">
                        Configuración
                    </h3>

                    <p class="text-secondary">

                        Ajustar parámetros generales,
                        políticas y datos institucionales.

                    </p>

                    <button
                            type="button"
                            class="btn btn-outline-secondary"
                            disabled>

                        Próximamente

                    </button>

                </div>

            </div>

        </div>

        <div class="col-12 col-md-6 col-xl-4">

            <div class="card tarjeta-opcion shadow-sm h-100">

                <div class="card-body p-4">

                    <div class="icono-modulo mb-3">
                        I
                    </div>

                    <h3 class="h5">
                        Informes
                    </h3>

                    <p class="text-secondary">

                        Consultar estadísticas, actividad
                        y reportes administrativos.

                    </p>

                    <button
                            type="button"
                            class="btn btn-outline-secondary"
                            disabled>

                        Próximamente

                    </button>

                </div>

            </div>

        </div>

        <div class="col-12 col-md-6 col-xl-4">

            <div class="card tarjeta-opcion shadow-sm h-100">

                <div class="card-body p-4">

                    <div class="icono-modulo mb-3">
                        S
                    </div>

                    <h3 class="h5">
                        Estado del sistema
                    </h3>

                    <p class="text-secondary">

                        Revisar el funcionamiento de servicios,
                        base de datos y componentes internos.

                    </p>

                    <button
                            type="button"
                            class="btn btn-outline-secondary"
                            disabled>

                        Próximamente

                    </button>

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