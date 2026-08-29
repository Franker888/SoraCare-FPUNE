<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.ResumenPanelMedicoDTO" %>
<%@ page import="py.fpune.soracare.dto.PerfilVisualDTO" %>
<%!
    private String h(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String claseAvatar(String codigo) {
        if ("TURQUESA".equals(codigo)) return "turquesa";
        if ("VIOLETA".equals(codigo)) return "violeta";
        if ("CORAL".equals(codigo)) return "coral";
        if ("VERDE".equals(codigo)) return "verde";
        if ("DORADO".equals(codigo)) return "dorado";
        return "azul";
    }
%>
<%
    ResumenPanelMedicoDTO resumen =
            (ResumenPanelMedicoDTO) request.getAttribute("resumenMedico");
    PerfilVisualDTO perfil = request.getAttribute("perfilVisual") instanceof PerfilVisualDTO
            ? (PerfilVisualDTO) request.getAttribute("perfilVisual")
            : new PerfilVisualDTO("AZUL", false);
    String error = (String) request.getAttribute("error");
    String iniciales = "M";
    if (resumen != null) {
        String primera = resumen.nombres() == null || resumen.nombres().isBlank()
                ? "M" : resumen.nombres().substring(0, 1);
        String segunda = resumen.apellidos() == null || resumen.apellidos().isBlank()
                ? "" : resumen.apellidos().substring(0, 1);
        iniciales = (primera + segunda).toUpperCase();
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex,nofollow">
    <title>Portal m&eacute;dico | SoraCare-FPUNE</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260815e">
</head>
<body class="app-shell app-doctor-home">
<%@ include file="_navbar.jspf" %>
<main class="container py-4 py-lg-5">
    <% if (error != null && !error.isBlank()) { %>
        <div class="alert alert-danger" role="alert"><%= h(error) %></div>
    <% } %>

    <section class="doctor-hero mb-4">
        <div class="row align-items-center g-4">
            <div class="col-md-auto">
                <div class="doctor-profile-wrap">
                    <% if (perfil.fotoPersonalizada()) { %>
                        <img class="profile-avatar profile-avatar-xl profile-photo"
                             src="${pageContext.request.contextPath}/perfil/foto"
                             alt="Fotografía del perfil médico">
                    <% } else { %>
                        <div class="profile-avatar profile-avatar-xl avatar-<%= claseAvatar(perfil.avatarCodigo()) %>">
                            <%= h(iniciales) %>
                        </div>
                    <% } %>
                    <a class="doctor-profile-edit" href="${pageContext.request.contextPath}/perfil"
                       aria-label="Cambiar fotografía de perfil">
                        <i class="bi bi-camera"></i>
                    </a>
                </div>
            </div>
            <div class="col">
                <div class="doctor-eyebrow">Portal profesional</div>
                <% if (resumen != null) { %>
                    <h1><%= h(resumen.tratamientoVisible()) %> <%= h(resumen.nombreCompleto()) %></h1>
                    <p><%= h(resumen.especialidadPrincipal()) %> · Matr&iacute;cula <%= h(resumen.matriculaProfesional()) %></p>
                <% } else { %>
                    <h1>Portal m&eacute;dico</h1>
                    <p>Agenda, pacientes y seguimiento de la atenci&oacute;n cl&iacute;nica.</p>
                <% } %>
                <a class="btn btn-light mt-3" href="${pageContext.request.contextPath}/perfil">
                    <i class="bi bi-person-circle me-2"></i>Editar mi perfil
                </a>
            </div>
            <div class="col-lg-auto text-lg-end d-none d-lg-block">
                <span class="doctor-hero-icon"><i class="bi bi-clipboard2-pulse"></i></span>
            </div>
        </div>
    </section>

    <% if (resumen != null) { %>
    <section class="row g-3 mb-5" aria-label="Resumen de la actividad médica">
        <div class="col-6 col-lg-3">
            <article class="card doctor-stat-card">
                <span class="doctor-stat-icon"><i class="bi bi-calendar2-day"></i></span>
                <div class="doctor-stat-value"><%= resumen.citasHoy() %></div>
                <div class="doctor-stat-label">Citas de hoy</div>
            </article>
        </div>
        <div class="col-6 col-lg-3">
            <article class="card doctor-stat-card">
                <span class="doctor-stat-icon stat-confirmed"><i class="bi bi-person-check"></i></span>
                <div class="doctor-stat-value"><%= resumen.confirmadasHoy() %></div>
                <div class="doctor-stat-label">Confirmadas hoy</div>
            </article>
        </div>
        <div class="col-6 col-lg-3">
            <article class="card doctor-stat-card">
                <span class="doctor-stat-icon stat-attention"><i class="bi bi-activity"></i></span>
                <div class="doctor-stat-value"><%= resumen.enAtencionHoy() %></div>
                <div class="doctor-stat-label">En atenci&oacute;n</div>
            </article>
        </div>
        <div class="col-6 col-lg-3">
            <article class="card doctor-stat-card">
                <span class="doctor-stat-icon stat-finished"><i class="bi bi-clipboard2-check"></i></span>
                <div class="doctor-stat-value"><%= resumen.atendidasMes() %></div>
                <div class="doctor-stat-label">Atendidas este mes</div>
            </article>
        </div>
    </section>
    <% } %>

    <section>
        <div class="doctor-section-heading">
            <div>
                <h2 class="h4">Espacio de trabajo cl&iacute;nico</h2>
                <p class="text-secondary mb-0">Tu agenda concentra el acceso seguro a cada paciente y su historia.</p>
            </div>
            <span class="doctor-status-badge"><i class="bi bi-shield-check"></i> Acceso profesional</span>
        </div>

        <div class="row g-4">
            <div class="col-12 col-lg-6">
                <article class="card doctor-work-card doctor-agenda-card h-100">
                    <div class="row align-items-center g-4">
                        <div class="col-md">
                            <span class="doctor-work-icon"><i class="bi bi-calendar2-week"></i></span>
                            <h3>Mi agenda m&eacute;dica</h3>
                            <p>Consult&aacute las citas del d&iacute;a, abr&iacute; el panel cl&iacute;nico del paciente e inici&aacute; o finaliz&aacute; cada atenci&oacute;n.</p>
                            <a class="btn btn-primary" href="${pageContext.request.contextPath}/medico/agenda">
                                Abrir mi agenda <i class="bi bi-arrow-right ms-1"></i>
                            </a>
                        </div>
                        <div class="col-md-auto d-none d-md-block">
                            <div class="doctor-agenda-illustration">
                                <i class="bi bi-person-vcard"></i>
                                <span>Agenda e historia cl&iacute;nica</span>
                            </div>
                        </div>
                    </div>
                </article>
            </div>
            <div class="col-12 col-md-6 col-lg-3">
                <article class="card doctor-work-card h-100 d-flex flex-column">
                    <span class="doctor-work-icon availability-icon"><i class="bi bi-calendar2-range"></i></span>
                    <h3>Mi disponibilidad</h3>
                    <p>Bloque&aacute; un d&iacute;a o un intervalo cuando no puedas atender y revis&aacute; las citas afectadas.</p>
                    <a class="btn btn-outline-primary align-self-start mt-auto"
                       href="${pageContext.request.contextPath}/medico/mi-disponibilidad">
                        Gestionar disponibilidad <i class="bi bi-arrow-right ms-1"></i>
                    </a>
                </article>
            </div>
            <div class="col-12 col-md-6 col-lg-3">
                <article class="card doctor-work-card h-100 d-flex flex-column">
                    <span class="doctor-work-icon profile-icon"><i class="bi bi-person-bounding-box"></i></span>
                    <h3>Mi perfil</h3>
                    <p>Eleg&iacute; un avatar o carg&aacute; tu propia fotograf&iacute;a para identificar tu cuenta.</p>
                    <a class="btn btn-outline-primary align-self-start mt-auto"
                       href="${pageContext.request.contextPath}/perfil">
                        Personalizar perfil <i class="bi bi-arrow-right ms-1"></i>
                    </a>
                </article>
            </div>
        </div>

        <div class="doctor-privacy-note mt-4">
            <i class="bi bi-lock"></i>
            <div>
                <strong>Privacidad cl&iacute;nica</strong>
                <span>Los historiales, estudios e indicaciones se abren desde la cita correspondiente del paciente.</span>
            </div>
        </div>
    </section>
</main>
<script src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js"></script>
</body>
</html>
