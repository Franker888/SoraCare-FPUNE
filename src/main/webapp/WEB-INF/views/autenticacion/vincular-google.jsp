<%@ page contentType="text/html;charset=UTF-8"
         pageEncoding="UTF-8"
         language="java" %>
<%@ page import="py.fpune.soracare.dto.PerfilGoogleDTO" %>
<%!
    private String h(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
%>
<%
    PerfilGoogleDTO perfil =
            (PerfilGoogleDTO) request.getAttribute("perfilGoogle");
    String error = (String) request.getAttribute("error");
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex, nofollow">
    <title>Vincular Google | SoraCare-FPUNE</title>
    <link href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css"
          rel="stylesheet">
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260821">
</head>
<body class="app-shell google-flow-page">
<main class="google-flow-shell">
    <section class="google-flow-visual">
        <a href="${pageContext.request.contextPath}/" class="google-flow-brand">
            <img src="${pageContext.request.contextPath}/assets/home/img/logo/SoraCare1.png"
                 alt="Logo SoraCare">
            <span>SoraCare-FPUNE</span>
        </a>
        <span class="google-flow-kicker">Protección de tu cuenta</span>
        <h1>Confirmemos que esta cuenta ya es tuya</h1>
        <p>
            Encontramos un paciente registrado con el mismo correo. Ingresá su
            contraseña una sola vez para vincular Google de manera segura.
        </p>
    </section>

    <section class="google-flow-card">
        <div class="google-account-summary">
            <% if (perfil != null && perfil.fotoUrl() != null) { %>
            <img src="<%= h(perfil.fotoUrl()) %>"
                 alt="Foto de la cuenta de Google"
                 referrerpolicy="no-referrer">
            <% } else { %>
            <span class="google-account-fallback">G</span>
            <% } %>
            <div>
                <small>Cuenta de Google verificada</small>
                <strong><%= h(perfil == null ? null : perfil.nombreCompleto()) %></strong>
                <span><%= h(perfil == null ? null : perfil.correo()) %></span>
            </div>
        </div>

        <% if (error != null && !error.isBlank()) { %>
        <div class="alert alert-danger" role="alert"><%= h(error) %></div>
        <% } %>

        <form method="post"
              action="${pageContext.request.contextPath}/google/vincular">
            <label for="contrasena" class="form-label">
                Contraseña actual de SoraCare
            </label>
            <input type="password"
                   class="form-control form-control-lg"
                   id="contrasena"
                   name="contrasena"
                   maxlength="72"
                   autocomplete="current-password"
                   required
                   autofocus>
            <p class="form-text mt-2">
                SoraCare no comparte esta contraseña con Google.
            </p>
            <button class="btn btn-primary btn-lg w-100 mt-3" type="submit">
                Verificar y vincular Google
            </button>
        </form>

        <a class="google-flow-cancel" href="${pageContext.request.contextPath}/login">
            Cancelar y volver al inicio de sesión
        </a>
    </section>
</main>
</body>
</html>
