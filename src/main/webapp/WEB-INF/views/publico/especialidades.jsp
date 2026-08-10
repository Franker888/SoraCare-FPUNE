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

    String error =
            (String) request.getAttribute(
                    "error"
            );
%>

<!DOCTYPE html>
<html lang="es">

<head>
    <meta charset="UTF-8">

    <meta name="viewport"
          content="width=device-width, initial-scale=1">

    <meta name="description"
          content="Catálogo público de especialidades médicas de SoraCare-FPUNE.">

    <title>
        Especialidades médicas | SoraCare-FPUNE
    </title>

    <link
            href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
            rel="stylesheet">

    <style>
        body {
            min-height: 100vh;
            background-color: #f8fafc;
            color: #0f172a;
        }

        .barra-publica {
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

        .encabezado-catalogo {
            border: none;
            border-radius: 1.25rem;
            background:
                    linear-gradient(
                            135deg,
                            #0f766e,
                            #0e7490
                    );
            color: #ffffff;
        }

        .tarjeta-especialidad {
            border: none;
            border-radius: 1rem;
            transition:
                    transform 0.2s ease,
                    box-shadow 0.2s ease;
        }

        .tarjeta-especialidad:hover {
            transform: translateY(-4px);
            box-shadow:
                    0 1rem 2rem
                    rgba(15, 23, 42, 0.10);
        }

        .icono-especialidad {
            width: 54px;
            height: 54px;
            border-radius: 1rem;
            display: flex;
            align-items: center;
            justify-content: center;
            background-color: #ccfbf1;
            color: #0f766e;
            font-size: 1.35rem;
            font-weight: 700;
        }

        .etiqueta-duracion {
            background-color: #e0f2fe;
            color: #075985;
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

        .pie-publico {
            background-color: #0f172a;
            color: #cbd5e1;
        }
    </style>
</head>

<body>

<nav class="navbar navbar-expand-lg barra-publica shadow-sm">

    <div class="container">

        <a
                class="navbar-brand marca-soracare"
                href="${pageContext.request.contextPath}/">

            SoraCare-FPUNE

        </a>

        <button
                class="navbar-toggler border-light"
                type="button"
                data-bs-toggle="collapse"
                data-bs-target="#navegacionPublica"
                aria-controls="navegacionPublica"
                aria-expanded="false"
                aria-label="Abrir navegación">

            <span class="navbar-toggler-icon">
            </span>

        </button>

        <div
                class="collapse navbar-collapse"
                id="navegacionPublica">

            <div class="navbar-nav ms-auto align-items-lg-center gap-lg-2">

                <a
                        class="nav-link text-white"
                        href="${pageContext.request.contextPath}/">

                    Inicio

                </a>

                <a
                        class="nav-link text-white fw-semibold"
                        href="${pageContext.request.contextPath}/especialidades">

                    Especialidades

                </a>

                <a
                        class="nav-link text-white"
                        href="${pageContext.request.contextPath}/medicos">

                    Médicos

                </a>

                <a
                        class="btn btn-outline-light ms-lg-2"
                        href="${pageContext.request.contextPath}/login">

                    Iniciar sesión

                </a>

            </div>

        </div>

    </div>

</nav>

<main class="container py-4 py-md-5">

    <section class="card encabezado-catalogo shadow-sm mb-5">

        <div class="card-body p-4 p-md-5">

            <div class="row align-items-center">

                <div class="col-12 col-lg-8">

                    <p class="mb-2 opacity-75">
                        Atención médica
                    </p>

                    <h1 class="display-5 fw-bold mb-3">

                        Nuestras especialidades

                    </h1>

                    <p class="lead mb-0">

                        Conocé las áreas médicas disponibles
                        en SoraCare-FPUNE y la duración estimada
                        de sus consultas.

                    </p>

                </div>

                <div class="col-12 col-lg-4 mt-4 mt-lg-0 text-lg-end">

                    <span class="badge text-bg-light fs-6">

                        <%= especialidades.size() %>
                        especialidad<%= especialidades.size() == 1
                            ? ""
                            : "es" %>
                        disponible<%= especialidades.size() == 1
                            ? ""
                            : "s" %>

                    </span>

                </div>

            </div>

        </div>

    </section>

    <div class="mb-4">

        <h2 class="h3 mb-2">
            Catálogo médico
        </h2>

        <p class="text-secondary mb-0">

            Seleccioná el área de atención que mejor
            corresponda a tu necesidad.

        </p>

    </div>

    <% if (error != null
            && !error.isBlank()) { %>

    <div
            class="alert alert-danger"
            role="alert">

        <strong>No pudimos cargar el catálogo.</strong>

        <div class="mt-1">

            <%= escaparHtml(error) %>

        </div>

    </div>

    <% } %>

    <% if (especialidades.isEmpty()) { %>

    <section class="card border-0 shadow-sm">

        <div class="card-body text-center p-5">

            <h2 class="h4">
                No existen especialidades disponibles
            </h2>

            <p class="text-secondary mb-0">

                El catálogo médico todavía no posee
                especialidades activas para mostrar.

            </p>

        </div>

    </section>

    <% } else { %>

    <section class="row g-4">

        <% for (
                Especialidad especialidad
                : especialidades
        ) { %>

        <div class="col-12 col-md-6 col-xl-4">

            <article class="card tarjeta-especialidad shadow-sm h-100">

                <div class="card-body p-4 d-flex flex-column">

                    <div class="d-flex
                                justify-content-between
                                align-items-start
                                gap-3 mb-3">

                        <div class="icono-especialidad">

                            <%= escaparHtml(
                                    especialidad
                                            .getNombre()
                                            .substring(0, 1)
                                            .toUpperCase()
                            ) %>

                        </div>

                        <span class="badge etiqueta-duracion">

                            <%= especialidad
                                    .getDuracionConsultaMinutos() %>
                            minutos

                        </span>

                    </div>

                    <h2 class="h5 mb-3">

                        <%= escaparHtml(
                                especialidad.getNombre()
                        ) %>

                    </h2>

                    <% if (
                            especialidad.getDescripcion() == null
                                    || especialidad
                                    .getDescripcion()
                                    .isBlank()
                    ) { %>

                    <p class="text-secondary flex-grow-1">

                        Consultá próximamente la información
                        completa de esta especialidad
                        y sus profesionales disponibles.

                    </p>

                    <% } else { %>

                    <p class="text-secondary flex-grow-1">

                        <%= escaparHtml(
                                especialidad.getDescripcion()
                        ) %>

                    </p>

                    <% } %>

                    <a
                            href="${pageContext.request.contextPath}/medicos"
                            class="btn btn-outline-success w-100">

                        Ver médicos

                    </a>

                </div>

            </article>

        </div>

        <% } %>

    </section>

    <% } %>

    <section class="card border-0 shadow-sm mt-5">

        <div class="card-body p-4 p-md-5">

            <div class="row align-items-center">

                <div class="col-12 col-lg-8">

                    <h2 class="h4 mb-2">
                        ¿Ya tenés una cuenta?
                    </h2>

                    <p class="text-secondary mb-lg-0">

                        Iniciá sesión para acceder a las funciones
                        disponibles según tu perfil dentro del sistema.

                    </p>

                </div>

                <div class="col-12 col-lg-4 mt-3 mt-lg-0 text-lg-end">

                    <a
                            href="${pageContext.request.contextPath}/login"
                            class="btn btn-primary boton-principal">

                        Iniciar sesión

                    </a>

                </div>

            </div>

        </div>

    </section>

</main>

<footer class="pie-publico mt-5">

    <div class="container py-4">

        <div class="d-flex flex-column
                    flex-md-row
                    justify-content-between
                    gap-2">

            <span>
                SoraCare-FPUNE
            </span>

            <span>
                Sistema de gestión hospitalaria
            </span>

        </div>

    </div>

</footer>

<script
        src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js">
</script>

</body>
</html>