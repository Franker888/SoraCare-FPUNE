<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.ResumenPanelPacienteDTO" %>
<%@ page import="py.fpune.soracare.dto.PerfilVisualDTO" %>
<%@ page import="java.time.ZoneId" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.Locale" %>
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

    private String contadorVisible(long cantidad) {
        return cantidad > 9 ? "9+" : Long.toString(cantidad);
    }
%>
<%
    ResumenPanelPacienteDTO resumen =
            (ResumenPanelPacienteDTO) request.getAttribute("resumenPaciente");
    PerfilVisualDTO perfil = request.getAttribute("perfilVisual") instanceof PerfilVisualDTO
            ? (PerfilVisualDTO) request.getAttribute("perfilVisual")
            : new PerfilVisualDTO("AZUL", false);
    String error = (String) request.getAttribute("error");
    String iniciales = "P";
    if (resumen != null) {
        String primera = resumen.nombres() == null || resumen.nombres().isBlank()
                ? "P" : resumen.nombres().substring(0, 1);
        String segunda = resumen.apellidos() == null || resumen.apellidos().isBlank()
                ? "" : resumen.apellidos().substring(0, 1);
        iniciales = (primera + segunda).toUpperCase();
    }
    DateTimeFormatter formatoProximaCita = DateTimeFormatter
            .ofPattern("EEEE d 'de' MMMM, HH:mm", new Locale("es", "PY"));
    String proximaCitaVisible = resumen != null && resumen.proximaCita() != null
            ? resumen.proximaCita().atZoneSameInstant(ZoneId.of("America/Asuncion"))
                    .format(formatoProximaCita)
            : "Sin citas próximas";
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex,nofollow">
    <title>Portal del paciente | SoraCare-FPUNE</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260815f">
</head>
<body class="app-shell app-patient-home">
<%@ include file="_navbar.jspf" %>

<main class="container py-4 py-lg-5">
    <% if (error != null && !error.isBlank()) { %>
        <div class="alert alert-danger" role="alert"><%= h(error) %></div>
    <% } %>

    <section class="patient-hero mb-4">
        <div class="row align-items-center g-4">
            <div class="col-md-auto">
                <div class="patient-profile-wrap">
                    <% if (perfil.fotoPersonalizada()) { %>
                        <img class="profile-avatar profile-avatar-xl profile-photo"
                             src="${pageContext.request.contextPath}/perfil/foto"
                             alt="Fotograf&iacute;a del perfil del paciente">
                    <% } else { %>
                        <div class="profile-avatar profile-avatar-xl avatar-<%= claseAvatar(perfil.avatarCodigo()) %>">
                            <%= h(iniciales) %>
                        </div>
                    <% } %>
                    <a class="patient-profile-edit" href="${pageContext.request.contextPath}/perfil"
                       aria-label="Cambiar fotograf&iacute;a de perfil">
                        <i class="bi bi-camera"></i>
                    </a>
                </div>
            </div>
            <div class="col">
                <div class="patient-eyebrow">Tu espacio de salud</div>
                <% if (resumen != null) { %>
                    <h1>Hola, <%= h(resumen.nombres()) %></h1>
                    <p><%= h(resumen.nombreCompleto()) %> &middot; Historia cl&iacute;nica <%= h(resumen.numeroHistoriaClinica()) %></p>
                <% } else { %>
                    <h1>Portal del paciente</h1>
                    <p>Organiz&aacute; tus citas y documentos desde un solo lugar.</p>
                <% } %>
                <div class="patient-hero-actions">
                    <a class="btn btn-light" href="${pageContext.request.contextPath}/mis-citas?nueva=1">
                        <i class="bi bi-calendar2-plus me-2"></i>Agendar una cita
                    </a>
                    <a class="btn patient-hero-profile" href="${pageContext.request.contextPath}/perfil">
                        <i class="bi bi-person-circle me-2"></i>Mi perfil
                    </a>
                </div>
            </div>
            <div class="col-lg-auto d-none d-lg-block">
                <span class="patient-hero-icon"><i class="bi bi-heart-pulse"></i></span>
            </div>
        </div>
    </section>

    <% if (resumen != null) { %>
    <section class="row g-3 mb-5" aria-label="Resumen personal">
        <div class="col-6 col-lg-3">
            <article class="card patient-stat-card">
                <span class="patient-stat-icon"><i class="bi bi-calendar2-check"></i></span>
                <div class="patient-stat-value"><%= resumen.proximasCitas() %></div>
                <div class="patient-stat-label">Pr&oacute;ximas citas</div>
            </article>
        </div>
        <div class="col-6 col-lg-3">
            <article class="card patient-stat-card">
                <span class="patient-stat-icon stat-prescription"><i class="bi bi-prescription2"></i></span>
                <div class="patient-stat-value"><%= resumen.indicacionesMedicas() %></div>
                <div class="patient-stat-label">Indicaciones m&eacute;dicas</div>
            </article>
        </div>
        <div class="col-6 col-lg-3">
            <article class="card patient-stat-card">
                <span class="patient-stat-icon stat-document"><i class="bi bi-file-earmark-medical"></i></span>
                <div class="patient-stat-value"><%= resumen.documentosClinicos() %></div>
                <div class="patient-stat-label">Documentos cl&iacute;nicos</div>
            </article>
        </div>
        <div class="col-6 col-lg-3">
            <article class="card patient-stat-card">
                <span class="patient-stat-icon stat-notification"><i class="bi bi-bell"></i></span>
                <div class="patient-stat-value"><%= resumen.notificacionesNoLeidas() %></div>
                <div class="patient-stat-label">Avisos sin leer</div>
            </article>
        </div>
    </section>
    <% } %>

    <section>
        <div class="patient-section-heading">
            <div>
                <h2 class="h4">Mi espacio personal</h2>
                <p class="text-secondary mb-0">Accesos ordenados para cuidar y seguir tu atenci&oacute;n.</p>
            </div>
            <span class="patient-status-badge"><i class="bi bi-shield-check"></i> Informaci&oacute;n privada</span>
        </div>

        <div class="row g-4">
            <div class="col-12 col-lg-7">
                <article class="card patient-feature-card patient-appointment-card h-100">
                    <div class="row align-items-center g-4">
                        <div class="col-md">
                            <span class="patient-feature-icon"><i class="bi bi-calendar2-heart"></i></span>
                            <h3>Mis citas</h3>
                            <p>Consult&aacute; tus reservas, complet&aacute; pagos pendientes, cancel&aacute; o reprogram&aacute; una consulta.</p>
                            <div class="patient-next-appointment">
                                <span>Pr&oacute;xima cita</span>
                                <strong><%= h(proximaCitaVisible) %></strong>
                            </div>
                            <a class="btn btn-primary" href="${pageContext.request.contextPath}/mis-citas">
                                Abrir mis citas <i class="bi bi-arrow-right ms-1"></i>
                            </a>
                        </div>
                        <div class="col-md-auto d-none d-md-block">
                            <div class="patient-appointment-illustration">
                                <i class="bi bi-calendar2-week"></i>
                                <span>Reservas y seguimiento</span>
                            </div>
                        </div>
                    </div>
                </article>
            </div>

            <div class="col-12 col-sm-6 col-lg-5">
                <article class="card patient-feature-card h-100 d-flex flex-column">
                    <span class="patient-feature-icon prescription-icon"><i class="bi bi-capsule-pill"></i></span>
                    <h3>Mis indicaciones m&eacute;dicas</h3>
                    <p>Revis&aacute; medicamentos, cuidados y estudios solicitados, con descarga de la indicaci&oacute;n en PDF.</p>
                    <a class="btn btn-outline-primary align-self-start mt-auto"
                       href="${pageContext.request.contextPath}/mis-recetas">
                        Ver indicaciones <i class="bi bi-arrow-right ms-1"></i>
                    </a>
                </article>
            </div>

            <div class="col-12 col-sm-6 col-lg-4">
                <article class="card patient-feature-card h-100 d-flex flex-column">
                    <span class="patient-feature-icon document-icon"><i class="bi bi-folder2-open"></i></span>
                    <h3>Documentos cl&iacute;nicos</h3>
                    <p>Guard&aacute; laboratorios, im&aacute;genes, estudios e informes junto a tu historial.</p>
                    <a class="btn btn-outline-primary align-self-start mt-auto"
                       href="${pageContext.request.contextPath}/mis-documentos">
                        Ver documentos <i class="bi bi-arrow-right ms-1"></i>
                    </a>
                </article>
            </div>

            <div class="col-12 col-sm-6 col-lg-4">
                <article class="card patient-feature-card h-100 d-flex flex-column">
                    <span class="patient-feature-icon notification-icon"><i class="bi bi-bell"></i></span>
                    <h3>Notificaciones</h3>
                    <p>Encontr&aacute; recordatorios de citas, pagos, indicaciones y nuevos documentos.</p>
                    <a class="btn btn-outline-primary align-self-start mt-auto"
                       href="${pageContext.request.contextPath}/mis-notificaciones">
                        Ver notificaciones
                        <% if (resumen != null && resumen.notificacionesNoLeidas() > 0) { %>
                            <span class="patient-unread-count"><%= contadorVisible(resumen.notificacionesNoLeidas()) %></span>
                        <% } %>
                        <i class="bi bi-arrow-right ms-1"></i>
                    </a>
                </article>
            </div>

            <div class="col-12 col-sm-6 col-lg-4">
                <article class="card patient-feature-card h-100 d-flex flex-column">
                    <span class="patient-feature-icon profile-feature-icon"><i class="bi bi-person-bounding-box"></i></span>
                    <h3>Mi perfil</h3>
                    <p>Personaliz&aacute; tu avatar o fotograf&iacute;a para identificar tu cuenta de forma clara.</p>
                    <a class="btn btn-outline-primary align-self-start mt-auto"
                       href="${pageContext.request.contextPath}/perfil">
                        Personalizar perfil <i class="bi bi-arrow-right ms-1"></i>
                    </a>
                </article>
            </div>
        </div>

        <div class="patient-privacy-note mt-4">
            <i class="bi bi-lock"></i>
            <div>
                <strong>Tu informaci&oacute;n cl&iacute;nica est&aacute; protegida</strong>
                <span>Los documentos e indicaciones s&oacute;lo son visibles para vos y profesionales autorizados.</span>
            </div>
            <% if (resumen != null) { %>
                <span class="patient-blood-badge">Grupo sangu&iacute;neo: <strong><%= h(resumen.grupoSanguineoVisible()) %></strong></span>
            <% } %>
        </div>
    </section>
</main>

<script src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js"></script>
</body>
</html>
