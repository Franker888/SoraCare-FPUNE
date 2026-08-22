<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.UsuarioAutenticadoDTO" %>
<%!
    private String h(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
%>
<%
    UsuarioAutenticadoDTO usuario =
            (UsuarioAutenticadoDTO) request.getAttribute("usuario");
    String token = (String) request.getAttribute("tokenEliminarCuenta");
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex,nofollow">
    <title>Eliminar mi cuenta | SoraCare-FPUNE</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260821">
</head>
<body class="app-shell app-profile">
<nav class="navbar profile-navbar">
    <div class="container">
        <a class="navbar-brand" href="${pageContext.request.contextPath}/paciente/inicio">
            SoraCare-FPUNE
        </a>
        <a class="btn btn-outline-primary btn-sm" href="${pageContext.request.contextPath}/perfil">
            Volver a mi perfil
        </a>
    </div>
</nav>

<main class="container py-4 py-lg-5">
    <section class="profile-hero mb-4">
        <div class="profile-current">
            <div class="profile-avatar profile-avatar-xl avatar-coral">!</div>
        </div>
        <div>
            <div class="profile-eyebrow">Privacidad y control de la cuenta</div>
            <h1>Eliminar mi cuenta</h1>
            <p>Revisá con calma lo que ocurrirá antes de confirmar la baja.</p>
            <% if (usuario != null) { %>
                <span class="profile-user-badge">@<%= h(usuario.getNombreUsuario()) %></span>
            <% } %>
        </div>
    </section>

    <% if (error != null && !error.isBlank()) { %>
        <div class="alert alert-danger" role="alert"><%= h(error) %></div>
    <% } %>

    <div class="row g-4 align-items-stretch">
        <div class="col-12 col-lg-7">
            <section class="card profile-card h-100">
                <div class="profile-card-heading">
                    <span class="profile-card-icon"><i class="bi bi-shield-exclamation"></i></span>
                    <div>
                        <h2>¿Qué se elimina?</h2>
                        <p>La baja es definitiva para el acceso digital.</p>
                    </div>
                </div>
                <ul class="list-group list-group-flush">
                    <li class="list-group-item px-0">Tu usuario quedará inactivo y ya no podrás iniciar sesión.</li>
                    <li class="list-group-item px-0">Se desvinculará el acceso mediante Google.</li>
                    <li class="list-group-item px-0">La sesión actual se cerrará inmediatamente.</li>
                    <li class="list-group-item px-0">
                        Tus atenciones, historia clínica y comprobantes se conservarán de forma segura
                        para mantener la trazabilidad médica.
                    </li>
                </ul>
                <div class="alert alert-warning mt-4 mb-0">
                    Si tenés citas futuras activas, primero debés cancelarlas desde <strong>Mis citas</strong>.
                </div>
            </section>
        </div>

        <div class="col-12 col-lg-5">
            <section class="card profile-card h-100 border-danger-subtle">
                <div class="profile-card-heading">
                    <span class="profile-card-icon upload-icon"><i class="bi bi-person-x"></i></span>
                    <div>
                        <h2>Confirmación final</h2>
                        <p>Esta acción no puede deshacerse desde el portal.</p>
                    </div>
                </div>
                <button class="btn btn-danger w-100 mt-3" type="button"
                        data-bs-toggle="modal" data-bs-target="#confirmarEliminacion">
                        Eliminar mi cuenta definitivamente
                </button>
                <a class="btn btn-outline-secondary w-100 mt-2"
                   href="${pageContext.request.contextPath}/perfil">
                    Conservar mi cuenta
                </a>
            </section>
        </div>
    </div>
</main>

<div class="modal fade" id="confirmarEliminacion" tabindex="-1"
     aria-labelledby="tituloConfirmacion" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow-lg">
            <div class="modal-header border-0 pb-0">
                <h2 class="modal-title fs-4" id="tituloConfirmacion">
                    ¿Estás seguro de eliminar tu cuenta?
                </h2>
                <button type="button" class="btn-close" data-bs-dismiss="modal"
                        aria-label="Cerrar"></button>
            </div>
            <div class="modal-body py-4">
                <p class="mb-2">
                    Ya no podrás iniciar sesión con tu usuario ni con Google.
                </p>
                <p class="text-secondary mb-0">
                    Tus registros médicos permanecerán protegidos dentro de SoraCare.
                </p>
            </div>
            <div class="modal-footer border-0 pt-0">
                <button type="button" class="btn btn-outline-secondary"
                        data-bs-dismiss="modal">
                    No, volver
                </button>
                <form method="post" action="${pageContext.request.contextPath}/cuenta/eliminar">
                    <input type="hidden" name="token" value="<%= h(token) %>">
                    <button class="btn btn-danger" type="submit">
                        Sí, eliminar mi cuenta
                    </button>
                </form>
            </div>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js"></script>
</body>
</html>
