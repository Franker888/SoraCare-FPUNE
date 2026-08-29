<%@ page contentType="text/html;charset=UTF-8"
         pageEncoding="UTF-8"
         language="java" %>

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
    String mensajeError =
            (String) request.getAttribute("error");

    String identificador =
            (String) request.getAttribute("identificador");

    String googleClientId =
            (String) request.getAttribute("googleClientId");

    String googleLoginUri =
            (String) request.getAttribute("googleLoginUri");

    String turnstileSiteKey =
            (String) request.getAttribute("turnstileSiteKey");

    boolean googleHabilitado = googleClientId != null
            && !googleClientId.isBlank()
            && googleLoginUri != null
            && !googleLoginUri.isBlank();

    boolean turnstileHabilitado = turnstileSiteKey != null
            && !turnstileSiteKey.isBlank();

    boolean registroExitoso =
            "exitoso".equals(request.getParameter("registro"));

    boolean cuentaEliminada =
            "eliminada".equals(request.getParameter("cuenta"));

    if (identificador == null) {
        identificador = "";
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

    <title>Iniciar sesión | SoraCare-FPUNE</title>

    <link
            href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css"
            rel="stylesheet">

    <link rel="icon"
          type="image/png"
          href="${pageContext.request.contextPath}/assets/home/img/logo/SoraCare1.png">

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260815">
</head>

<body class="soracare-auth">

<main class="auth-page">

    <section class="auth-visual" aria-label="Presentación de SoraCare">

        <a class="auth-brand"
           href="${pageContext.request.contextPath}/">

            <img src="${pageContext.request.contextPath}/assets/home/img/logo/SoraCare1.png"
                 alt="Logo SoraCare">

            <span>SoraCare-FPUNE</span>

        </a>

        <div class="auth-visual-content">

            <span class="auth-kicker">
                Centro médico integral
            </span>

            <h2>
                Tu atención médica, organizada en un solo lugar.
            </h2>

            <p>
                Ingresá para administrar citas, consultar pagos y acceder
                a las funciones habilitadas para tu perfil.
            </p>

        </div>

    </section>

    <section class="auth-panel">

        <div class="auth-card">

            <a class="auth-mobile-brand"
               href="${pageContext.request.contextPath}/">

                <img src="${pageContext.request.contextPath}/assets/home/img/logo/SoraCare1.png"
                     alt="Logo SoraCare">

                <span>SoraCare-FPUNE</span>

            </a>

            <span class="badge rounded-pill text-bg-primary mb-3">
                Acceso seguro
            </span>

            <h1>Bienvenido de nuevo</h1>

            <p class="auth-subtitle">
                Iniciá sesión con tu usuario o correo electrónico
                para continuar a tu panel.
            </p>

            <% if (registroExitoso) { %>

            <div class="alert alert-success" role="alert">
                Tu cuenta de paciente fue creada. Ya podés iniciar sesión.
            </div>

            <% } %>

            <% if (cuentaEliminada) { %>

            <div class="alert alert-success" role="alert">
                Tu cuenta fue eliminada y la sesión se cerró correctamente.
            </div>

            <% } %>

            <% if (mensajeError != null
                    && !mensajeError.isBlank()) { %>

            <div class="alert alert-danger" role="alert">
                <%= escaparHtml(mensajeError) %>
            </div>

            <% } %>

            <% if (googleHabilitado) { %>

            <div class="auth-google-provider">
                <p class="auth-google-title">Acceso rápido para pacientes</p>

                <div id="g_id_onload"
                     data-client_id="<%= escaparHtml(googleClientId) %>"
                     data-login_uri="<%= escaparHtml(googleLoginUri) %>"
                     data-auto_prompt="false"
                     data-itp_support="true">
                </div>

                <div class="g_id_signin"
                     data-type="standard"
                     data-shape="rectangular"
                     data-theme="outline"
                     data-text="continue_with"
                     data-size="large"
                     data-logo_alignment="left"
                     data-width="300">
                </div>

                <p class="auth-google-help">
                    Ingresá o creá tu perfil de paciente con Google.
                </p>
            </div>

            <div class="auth-provider-divider" role="separator">
                <span>o ingresá con tu cuenta de SoraCare</span>
            </div>

            <% } %>

            <form method="post"
                  action="${pageContext.request.contextPath}/login"
                  autocomplete="on">

                <div class="mb-3">

                    <label for="identificador" class="form-label">
                        Usuario o correo electrónico
                    </label>

                    <input type="text"
                           class="form-control"
                           id="identificador"
                           name="identificador"
                           value="<%= escaparHtml(identificador) %>"
                           maxlength="150"
                           autocomplete="username"
                           placeholder="Ej.: nombre.usuario"
                           required
                           autofocus>

                </div>

                <div class="mb-3">

                    <label for="contrasena" class="form-label">
                        Contraseña
                    </label>

                    <input type="password"
                           class="form-control"
                           id="contrasena"
                           name="contrasena"
                           maxlength="72"
                           autocomplete="current-password"
                           placeholder="Ingresá tu contraseña"
                           required>

                </div>

                <% if (turnstileHabilitado) { %>

                <div class="turnstile-security mb-4">
                    <div class="cf-turnstile"
                         data-sitekey="<%= escaparHtml(turnstileSiteKey) %>"
                         data-action="login"
                         data-size="flexible"
                         data-theme="auto"
                         data-language="es">
                    </div>
                    <p>
                        <i class="bi bi-shield-check" aria-hidden="true"></i>
                        Acceso protegido contra intentos automatizados.
                    </p>
                </div>

                <% } %>

                <div class="d-grid">

                    <button type="submit" class="btn btn-primary">
                        Ingresar
                        <i class="bi bi-arrow-right ms-2"></i>
                    </button>

                </div>

            </form>

            <div class="auth-card-footer">

                <p class="mb-2">
                    ¿Todavía no tenés cuenta?
                    <a href="${pageContext.request.contextPath}/registro">
                        Crear cuenta de paciente
                    </a>
                </p>

                <a href="${pageContext.request.contextPath}/">
                    <i class="bi bi-arrow-left me-1"></i>
                    Volver a la página principal
                </a>

            </div>

        </div>

    </section>

</main>

<script
        src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js">
</script>

<% if (googleHabilitado) { %>
<script src="https://accounts.google.com/gsi/client"
        async
        defer>
</script>
<% } %>

<% if (turnstileHabilitado) { %>
<script src="https://challenges.cloudflare.com/turnstile/v0/api.js"
        async
        defer>
</script>
<% } %>

</body>
</html>
