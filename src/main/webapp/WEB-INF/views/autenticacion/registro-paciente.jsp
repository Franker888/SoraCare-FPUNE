<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%!
    private String h(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    String googleClientId = (String) request.getAttribute("googleClientId");
    String googleLoginUri = (String) request.getAttribute("googleLoginUri");
    String turnstileSiteKey = (String) request.getAttribute("turnstileSiteKey");
    boolean googleHabilitado = googleClientId != null
            && !googleClientId.isBlank()
            && googleLoginUri != null
            && !googleLoginUri.isBlank();
    boolean turnstileHabilitado = turnstileSiteKey != null
            && !turnstileSiteKey.isBlank();
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Crear cuenta de paciente | SoraCare-FPUNE</title>
    <link href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css" rel="stylesheet">
    <style>
        body { background: #f2f7f5; }
        .cabecera { background: linear-gradient(135deg, #0b5f49, #15956e); color: white; }
        .card { border: 0; border-radius: 1rem; }
    </style>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260901">
<% if (googleHabilitado) { %>
<script src="https://accounts.google.com/gsi/client?hl=es" async></script>
<% } %>
</head>
<body class="app-shell app-registration">
<nav class="navbar bg-white border-bottom"><div class="container py-2">
    <a class="navbar-brand fw-bold text-success" href="${pageContext.request.contextPath}/">SoraCare-FPUNE</a>
    <a class="btn btn-outline-success btn-sm" href="${pageContext.request.contextPath}/login">Ya tengo cuenta</a>
</div></nav>
<header class="cabecera py-4"><div class="container">
    <p class="small text-uppercase mb-1">Portal del paciente</p>
    <h1 class="h2 mb-1">Crear mi cuenta</h1>
    <p class="mb-0">Registrate para consultar disponibilidad y solicitar tus propias citas.</p>
</div></header>
<main class="container py-4 py-md-5">
    <div class="row justify-content-center"><div class="col-xl-10">
        <% if (request.getAttribute("error") != null) { %>
        <div class="alert alert-danger"><%= h((String) request.getAttribute("error")) %></div>
        <% } %>

        <% if (googleHabilitado) { %>
        <section class="google-registration-entry shadow-sm" aria-label="Registro con Google"
                 data-google-signin-provider>
            <div>
                <span class="google-registration-kicker">Acceso simplificado</span>
                <h2>Crear la cuenta con Google</h2>
                <p>
                    Google verificará tu correo. Después completarás únicamente
                    los datos necesarios para tu ficha de paciente.
                </p>
            </div>
            <div class="google-registration-button">
                <div id="g_id_onload"
                     data-client_id="<%= h(googleClientId) %>"
                     data-login_uri="<%= h(googleLoginUri) %>"
                     data-auto_prompt="false"
                     data-itp_support="true">
                </div>
                <div class="g_id_signin"
                     data-type="standard"
                     data-shape="rectangular"
                     data-theme="outline"
                     data-text="signup_with"
                     data-size="large"
                     data-logo_alignment="left">
                </div>
                <p class="auth-google-status" data-google-signin-status role="status">
                    Cargando acceso con Google…
                </p>
                <button class="btn btn-outline-primary btn-sm" type="button"
                        data-google-signin-retry hidden>
                    Reintentar carga
                </button>
            </div>
        </section>

        <div class="registration-divider"><span>o completá el registro tradicional</span></div>
        <% } %>

        <form method="post" action="${pageContext.request.contextPath}/registro" class="card shadow-sm"><input type="hidden" name="csrfToken" value="${requestScope.csrfToken}">
            <div class="card-body p-4 p-md-5">
                <h2 class="h4">Datos personales</h2>
                <p class="text-secondary">Estos datos formarán tu ficha de paciente.</p>
                <div class="row g-3">
                    <div class="col-md-6"><label class="form-label" for="nombres">Nombres</label><input class="form-control" id="nombres" name="nombres" maxlength="100" required value="<%= h(request.getParameter("nombres")) %>"></div>
                    <div class="col-md-6"><label class="form-label" for="apellidos">Apellidos</label><input class="form-control" id="apellidos" name="apellidos" maxlength="100" required value="<%= h(request.getParameter("apellidos")) %>"></div>
                    <div class="col-md-4"><label class="form-label" for="cedula">Cédula</label><input class="form-control" id="cedula" name="cedula" inputmode="numeric" required value="<%= h(request.getParameter("cedula")) %>"></div>
                    <div class="col-md-4"><label class="form-label" for="fechaNacimiento">Fecha de nacimiento</label><input class="form-control" type="date" id="fechaNacimiento" name="fechaNacimiento" value="<%= h(request.getParameter("fechaNacimiento")) %>"></div>
                    <div class="col-md-4"><label class="form-label" for="telefono">Teléfono</label><input class="form-control" id="telefono" name="telefono" maxlength="30" value="<%= h(request.getParameter("telefono")) %>"></div>
                    <div class="col-md-6"><label class="form-label" for="correo">Correo electrónico</label><input class="form-control" type="email" id="correo" name="correo" maxlength="150" required value="<%= h(request.getParameter("correo")) %>"></div>
                    <div class="col-md-6"><label class="form-label" for="grupoSanguineo">Grupo sanguíneo (opcional)</label><select class="form-select" id="grupoSanguineo" name="grupoSanguineo"><option value="">No informado</option><option>A+</option><option>A-</option><option>B+</option><option>B-</option><option>AB+</option><option>AB-</option><option>O+</option><option>O-</option></select></div>
                    <div class="col-12"><label class="form-label" for="direccion">Dirección</label><textarea class="form-control" id="direccion" name="direccion" maxlength="255" rows="2"><%= h(request.getParameter("direccion")) %></textarea></div>
                    <div class="col-md-6"><label class="form-label" for="contactoEmergencia">Contacto de emergencia</label><input class="form-control" id="contactoEmergencia" name="contactoEmergencia" maxlength="150" value="<%= h(request.getParameter("contactoEmergencia")) %>"></div>
                    <div class="col-md-6"><label class="form-label" for="telefonoEmergencia">Teléfono de emergencia</label><input class="form-control" id="telefonoEmergencia" name="telefonoEmergencia" maxlength="30" value="<%= h(request.getParameter("telefonoEmergencia")) %>"></div>
                </div>

                <hr class="my-4">
                <h2 class="h4">Datos de acceso</h2>
                <p class="text-secondary">Usarás estas credenciales para ingresar y gestionar tus citas. La contraseña que elijas quedará activa y no tendrás que cambiarla al ingresar.</p>
                <div class="row g-3">
                    <div class="col-md-6"><label class="form-label" for="nombreUsuario">Nombre de usuario</label><input class="form-control" id="nombreUsuario" name="nombreUsuario" minlength="4" maxlength="50" pattern="[A-Za-z0-9._-]+" required autocomplete="username" value="<%= h(request.getParameter("nombreUsuario")) %>"></div>
                    <div class="col-md-6"></div>
                    <div class="col-md-6"><label class="form-label" for="contrasena">Contraseña</label><input class="form-control" type="password" id="contrasena" name="contrasena" minlength="10" maxlength="72" required autocomplete="new-password"><div class="form-text">Mínimo 10 caracteres, con mayúscula, minúscula, número y símbolo. También rechazamos contraseñas expuestas en filtraciones conocidas.</div></div>
                    <div class="col-md-6"><label class="form-label" for="confirmacionContrasena">Confirmar contraseña</label><input class="form-control" type="password" id="confirmacionContrasena" name="confirmacionContrasena" minlength="10" maxlength="72" required autocomplete="new-password"></div>
                </div>

                <% if (turnstileHabilitado) { %>
                <div class="turnstile-security mt-4">
                    <div class="cf-turnstile"
                         data-sitekey="<%= h(turnstileSiteKey) %>"
                         data-action="registro"
                         data-size="flexible"
                         data-theme="auto"
                         data-language="es">
                    </div>
                    <p>
                        <i class="bi bi-shield-check" aria-hidden="true"></i>
                        Registro protegido contra cuentas automatizadas.
                    </p>
                </div>
                <% } %>

                <div class="d-flex flex-wrap gap-2 mt-4"><button class="btn btn-success px-4">Crear cuenta de paciente</button><a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/login">Cancelar</a></div>
            </div>
        </form>
    </div></div>
</main>
<% if (googleHabilitado) { %>
<script src="${pageContext.request.contextPath}/assets/app/js/google-signin-estado.js?v=20260901"></script>
<% } %>
<% if (turnstileHabilitado) { %>
<script src="https://challenges.cloudflare.com/turnstile/v0/api.js" async defer></script>
<% } %>
</body>
</html>
