<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.UsuarioAutenticadoDTO" %>
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
    UsuarioAutenticadoDTO usuario =
            (UsuarioAutenticadoDTO) request.getAttribute("usuarioAutenticado");
    PerfilVisualDTO perfil = request.getAttribute("perfilVisual") instanceof PerfilVisualDTO
            ? (PerfilVisualDTO) request.getAttribute("perfilVisual")
            : new PerfilVisualDTO("AZUL", false);
    String error = (String) request.getAttribute("error");
    String resultado = request.getParameter("resultado");
    boolean esMedico = usuario != null && usuario.tieneRol("MEDICO");
    boolean esPaciente = usuario != null && usuario.tieneRol("PACIENTE");
    String volver = esMedico
            ? "/medico/inicio"
            : esPaciente ? "/paciente/inicio" : "/dashboard";
    String inicial = usuario == null || usuario.getNombreUsuario().isBlank()
            ? "S" : usuario.getNombreUsuario().substring(0, 1).toUpperCase();
    String[] codigos = {"AZUL", "TURQUESA", "VIOLETA", "CORAL", "VERDE", "DORADO"};
    String[] nombres = {"Azul", "Turquesa", "Violeta", "Coral", "Verde", "Dorado"};
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex,nofollow">
    <title>Mi perfil | SoraCare-FPUNE</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260815e">
</head>
<body class="app-shell app-profile">
<nav class="navbar profile-navbar">
    <div class="container">
        <a class="navbar-brand" href="${pageContext.request.contextPath}<%= volver %>">SoraCare-FPUNE</a>
        <a class="btn btn-outline-primary btn-sm" href="${pageContext.request.contextPath}<%= volver %>">
            <i class="bi bi-arrow-left me-1"></i>Volver al panel
        </a>
    </div>
</nav>
<main class="container py-4 py-lg-5">
    <% if ("avatar".equals(resultado)) { %>
        <div class="alert alert-success">El avatar fue actualizado correctamente.</div>
    <% } else if ("foto".equals(resultado)) { %>
        <div class="alert alert-success">Tu fotograf&iacute;a fue guardada correctamente.</div>
    <% } %>
    <% if (error != null && !error.isBlank()) { %>
        <div class="alert alert-danger"><%= h(error) %></div>
    <% } %>

    <section class="profile-hero mb-4">
        <div class="profile-current">
            <% if (perfil.fotoPersonalizada()) { %>
                <img class="profile-avatar profile-avatar-xl profile-photo"
                     src="${pageContext.request.contextPath}/perfil/foto"
                     alt="Fotografía de perfil actual">
            <% } else { %>
                <div class="profile-avatar profile-avatar-xl avatar-<%= claseAvatar(perfil.avatarCodigo()) %>">
                    <%= h(inicial) %>
                </div>
            <% } %>
        </div>
        <div>
            <div class="profile-eyebrow">Identidad visual de la cuenta</div>
            <h1>Mi foto de perfil</h1>
            <p>Eleg&iacute; un avatar de SoraCare o carg&aacute; una fotograf&iacute;a personal.</p>
            <% if (usuario != null) { %>
                <span class="profile-user-badge">@<%= h(usuario.getNombreUsuario()) %></span>
            <% } %>
        </div>
    </section>

    <div class="row g-4">
        <div class="col-12 col-xl-7">
            <section class="card profile-card h-100">
                <div class="profile-card-heading">
                    <span class="profile-card-icon"><i class="bi bi-person-circle"></i></span>
                    <div><h2>Elegir un avatar</h2><p>Pod&eacute;s cambiarlo cuantas veces quieras.</p></div>
                </div>
                <form method="post" action="${pageContext.request.contextPath}/perfil">
                    <input type="hidden" name="accion" value="avatar">
                    <div class="avatar-grid">
                        <% for (int i = 0; i < codigos.length; i++) { %>
                            <label class="avatar-option">
                                <input type="radio" name="avatar" value="<%= codigos[i] %>"
                                       <%= codigos[i].equals(perfil.avatarCodigo()) && !perfil.fotoPersonalizada()
                                               ? "checked" : "" %>>
                                <span class="profile-avatar avatar-<%= claseAvatar(codigos[i]) %>">
                                    <i class="bi bi-person-fill"></i>
                                </span>
                                <span><%= nombres[i] %></span>
                            </label>
                        <% } %>
                    </div>
                    <button class="btn btn-outline-primary mt-4" type="submit">
                        Usar avatar seleccionado
                    </button>
                </form>
            </section>
        </div>

        <div class="col-12 col-xl-5">
            <section class="card profile-card h-100">
                <div class="profile-card-heading">
                    <span class="profile-card-icon upload-icon"><i class="bi bi-cloud-arrow-up"></i></span>
                    <div><h2>Subir mi fotograf&iacute;a</h2><p>Se mostrar&aacute; solamente dentro de tu cuenta.</p></div>
                </div>
                <form method="post" action="${pageContext.request.contextPath}/perfil"
                      enctype="multipart/form-data">
                    <input type="hidden" name="accion" value="foto">
                    <label class="profile-upload-zone" for="foto-perfil">
                        <i class="bi bi-image"></i>
                        <strong>Seleccionar imagen</strong>
                        <span>JPG, JPEG o PNG · m&aacute;ximo 5 MB</span>
                        <input id="foto-perfil" class="form-control" type="file" name="foto"
                               accept="image/jpeg,image/png" required>
                    </label>
                    <button class="btn btn-primary w-100 mt-4" type="submit">
                        <i class="bi bi-camera me-2"></i>Guardar fotograf&iacute;a
                    </button>
                </form>
                <div class="profile-security-note mt-4">
                    <i class="bi bi-shield-lock"></i>
                    <span>El archivo se valida y se guarda fuera de la aplicaci&oacute;n p&uacute;blica.</span>
                </div>
            </section>
        </div>
    </div>

    <section class="card profile-card mt-4">
        <div class="profile-card-heading">
            <span class="profile-card-icon"><i class="bi bi-shield-lock"></i></span>
            <div>
                <h2>Seguridad de la cuenta</h2>
                <p>Tu contraseña actual continúa vigente. Cambiala solamente cuando vos quieras.</p>
            </div>
        </div>
        <div>
            <a class="btn btn-outline-primary"
               href="${pageContext.request.contextPath}/cambiar-contrasena">
                <i class="bi bi-key me-2"></i>Cambiar mi contraseña
            </a>
        </div>
    </section>

    <% if (esPaciente) { %>
    <section class="card profile-card mt-4 border-danger-subtle">
        <div class="profile-card-heading">
            <span class="profile-card-icon upload-icon"><i class="bi bi-person-x"></i></span>
            <div>
                <h2>Eliminar mi cuenta</h2>
                <p>Inactivá definitivamente tu acceso y desvinculá tu cuenta de Google.</p>
            </div>
        </div>
        <div class="d-flex flex-column flex-lg-row gap-3 align-items-lg-center justify-content-between">
            <p class="text-secondary mb-0">
                Tu historia clínica se conservará protegida para mantener la continuidad médica.
            </p>
            <a class="btn btn-outline-danger flex-shrink-0"
               href="${pageContext.request.contextPath}/cuenta/eliminar">
                Revisar eliminación de cuenta
            </a>
        </div>
    </section>
    <% } %>
</main>
<script src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js"></script>
</body>
</html>
