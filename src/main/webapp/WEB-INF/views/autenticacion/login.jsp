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

    boolean registroExitoso =
            "exitoso".equals(request.getParameter("registro"));

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
            href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
            rel="stylesheet">

    <link rel="icon"
          type="image/png"
          href="${pageContext.request.contextPath}/assets/home/img/logo/SoraCare1.png">

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css">
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

            <% if (mensajeError != null
                    && !mensajeError.isBlank()) { %>

            <div class="alert alert-danger" role="alert">
                <%= escaparHtml(mensajeError) %>
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

                <div class="mb-4">

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

                <div class="d-grid">

                    <button type="submit" class="btn btn-primary">
                        Ingresar a mi panel
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
        src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js">
</script>

</body>
</html>
