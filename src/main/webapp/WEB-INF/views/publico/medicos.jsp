<%@ page contentType="text/html;charset=UTF-8"
         pageEncoding="UTF-8"
         language="java" %>

<%@ page import="py.fpune.soracare.mvc.model.Especialidad" %>
<%@ page import="py.fpune.soracare.mvc.model.Medico" %>
<%@ page import="py.fpune.soracare.mvc.model.MedicoEspecialidad" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>

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

    private String obtenerInicial(String texto) {
        if (texto == null
                || texto.isBlank()) {

            return "M";
        }

        return texto
                .strip()
                .substring(0, 1)
                .toUpperCase();
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

    Object atributoEspecialidadesPorMedico =
            request.getAttribute(
                    "especialidadesPorMedico"
            );

    Map<Long, List<MedicoEspecialidad>>
            especialidadesPorMedico;

    if (atributoEspecialidadesPorMedico
            instanceof Map<?, ?>) {

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

    if (atributoEspecialidadesPorId
            instanceof Map<?, ?>) {

        especialidadesPorId =
                (Map<Integer, Especialidad>)
                        atributoEspecialidadesPorId;
    } else {
        especialidadesPorId =
                Map.of();
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
          content="Catálogo público de médicos de SoraCare-FPUNE.">

    <title>
        Médicos | SoraCare-FPUNE
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

        .tarjeta-medico {
            border: none;
            border-radius: 1rem;
            transition:
                    transform 0.2s ease,
                    box-shadow 0.2s ease;
        }

        .tarjeta-medico:hover {
            transform: translateY(-4px);
            box-shadow:
                    0 1rem 2rem
                    rgba(15, 23, 42, 0.10);
        }

        .avatar-medico {
            width: 58px;
            height: 58px;
            border-radius: 1rem;
            display: flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;
            background-color: #ccfbf1;
            color: #0f766e;
            font-size: 1.4rem;
            font-weight: 700;
        }

        .etiqueta-principal {
            background-color: #ccfbf1;
            color: #115e59;
        }

        .etiqueta-secundaria {
            background-color: #e0f2fe;
            color: #075985;
        }

        .matricula-medico {
            color: #475569;
            font-size: 0.925rem;
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
                        class="nav-link text-white"
                        href="${pageContext.request.contextPath}/especialidades">

                    Especialidades

                </a>

                <a
                        class="nav-link text-white fw-semibold"
                        href="${pageContext.request.contextPath}/medicos"
                        aria-current="page">

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
                        Equipo profesional
                    </p>

                    <h1 class="display-5 fw-bold mb-3">

                        Nuestros médicos

                    </h1>

                    <p class="lead mb-0">

                        Conocé a los profesionales activos de
                        SoraCare-FPUNE y las especialidades médicas
                        que ejercen dentro de la institución.

                    </p>

                </div>

                <div class="col-12 col-lg-4 mt-4 mt-lg-0 text-lg-end">

                    <span class="badge text-bg-light fs-6">

                        <%= medicos.size() %>
                        médico<%= medicos.size() == 1
                            ? ""
                            : "s" %>
                        disponible<%= medicos.size() == 1
                            ? ""
                            : "s" %>

                    </span>

                </div>

            </div>

        </div>

    </section>

    <div class="mb-4">

        <h2 class="h3 mb-2">
            Catálogo de profesionales
        </h2>

        <p class="text-secondary mb-0">

            Consultá su matrícula, presentación profesional
            y especialidades activas.

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

    <% if (medicos.isEmpty()) { %>

    <section class="card border-0 shadow-sm">

        <div class="card-body text-center p-5">

            <h2 class="h4">
                No existen médicos disponibles
            </h2>

            <p class="text-secondary mb-0">

                El catálogo todavía no posee profesionales
                activos para mostrar.

            </p>

        </div>

    </section>

    <% } else { %>

    <section class="row g-4">

        <% for (
                Medico medico
                : medicos
        ) {

            List<MedicoEspecialidad> relaciones =
                    especialidadesPorMedico.getOrDefault(
                            medico.getIdMedico(),
                            List.of()
                    );

            MedicoEspecialidad relacionPrincipal =
                    null;

            for (
                    MedicoEspecialidad relacion
                    : relaciones
            ) {

                if (relacion.isPrincipal()) {
                    relacionPrincipal =
                            relacion;

                    break;
                }
            }

            Especialidad especialidadPrincipal =
                    relacionPrincipal == null
                            ? null
                            : especialidadesPorId.get(
                                    relacionPrincipal
                                            .getIdEspecialidad()
                            );
        %>

        <div class="col-12 col-lg-6">

            <article class="card tarjeta-medico shadow-sm h-100">

                <div class="card-body p-4 d-flex flex-column">

                    <div class="d-flex align-items-start gap-3 mb-4">

                        <div class="avatar-medico">

                            <%= escaparHtml(
                                    obtenerInicial(
                                            medico.getNombres()
                                    )
                            ) %><%= escaparHtml(
                                    obtenerInicial(
                                            medico.getApellidos()
                                    )
                            ) %>

                        </div>

                        <div>

                            <h2 class="h4 mb-1">

                                <%= escaparHtml(
                                        medico.getNombres()
                                ) %>

                                <%= escaparHtml(
                                        medico.getApellidos()
                                ) %>

                            </h2>

                            <div class="matricula-medico">

                                Matrícula profesional:

                                <strong>

                                    <%= escaparHtml(
                                            medico
                                                    .getMatriculaProfesional()
                                    ) %>

                                </strong>

                            </div>

                        </div>

                    </div>

                    <div class="mb-4">

                        <h3 class="h6 text-uppercase text-secondary mb-2">
                            Presentación profesional
                        </h3>

                        <% if (medico.getBiografia() == null
                                || medico.getBiografia().isBlank()) { %>

                        <p class="text-secondary mb-0">

                            La presentación profesional de este médico
                            se encuentra pendiente de actualización.

                        </p>

                        <% } else { %>

                        <p class="text-secondary mb-0">

                            <%= escaparHtml(
                                    medico.getBiografia()
                            ) %>

                        </p>

                        <% } %>

                    </div>

                    <div class="mt-auto">

                        <h3 class="h6 text-uppercase text-secondary mb-2">
                            Especialidades
                        </h3>

                        <% if (especialidadPrincipal != null) { %>

                        <div class="mb-2">

                            <span class="badge etiqueta-principal">

                                Principal ·

                                <%= escaparHtml(
                                        especialidadPrincipal
                                                .getNombre()
                                ) %>

                            </span>

                        </div>

                        <% } %>

                        <div class="d-flex flex-wrap gap-2">

                            <% for (
                                    MedicoEspecialidad relacion
                                    : relaciones
                            ) {

                                if (relacion.isPrincipal()) {
                                    continue;
                                }

                                Especialidad especialidad =
                                        especialidadesPorId.get(
                                                relacion
                                                        .getIdEspecialidad()
                                        );

                                if (especialidad == null) {
                                    continue;
                                }
                            %>

                            <span class="badge etiqueta-secundaria">

                                <%= escaparHtml(
                                        especialidad.getNombre()
                                ) %>

                            </span>

                            <% } %>

                            <% if (relaciones.isEmpty()) { %>

                            <span class="text-secondary">
                                Especialidad pendiente de confirmación.
                            </span>

                            <% } %>

                        </div>

                    </div>

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
