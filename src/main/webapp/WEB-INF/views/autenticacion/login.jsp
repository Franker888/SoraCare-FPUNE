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

    <style>
        body {
            min-height: 100vh;
            background: linear-gradient(
                    135deg,
                    #0f766e,
                    #0e7490
            );
        }

        .contenedor-login {
            min-height: 100vh;
        }

        .tarjeta-login {
            width: 100%;
            max-width: 440px;
            border: none;
            border-radius: 1.25rem;
        }

        .marca-soracare {
            color: #0f766e;
            font-weight: 700;
        }

        .subtitulo {
            color: #64748b;
        }

        .form-control {
            min-height: 48px;
        }

        .form-control:focus {
            border-color: #0f766e;
            box-shadow: 0 0 0 0.25rem
            rgba(15, 118, 110, 0.20);
        }

        .boton-ingresar {
            min-height: 48px;
            background-color: #0f766e;
            border-color: #0f766e;
        }

        .boton-ingresar:hover,
        .boton-ingresar:focus {
            background-color: #115e59;
            border-color: #115e59;
        }
    </style>
</head>

<body>

<main class="container">

    <div class="row align-items-center
                justify-content-center
                contenedor-login">

        <div class="col-12 col-sm-10 col-md-7 col-lg-5">

            <div class="card tarjeta-login shadow-lg mx-auto">

                <div class="card-body p-4 p-md-5">

                    <div class="text-center mb-4">

                        <h1 class="h3 marca-soracare mb-2">
                            SoraCare-FPUNE
                        </h1>

                        <p class="subtitulo mb-0">
                            Sistema de gestión hospitalaria
                        </p>

                    </div>

                    <h2 class="h5 text-center mb-4">
                        Iniciar sesión
                    </h2>

                    <% if (registroExitoso) { %>

                    <div class="alert alert-success" role="alert">
                        Tu cuenta de paciente fue creada. Ya podés iniciar sesión.
                    </div>

                    <% } %>

                    <% if (mensajeError != null
                            && !mensajeError.isBlank()) { %>

                    <div class="alert alert-danger"
                         role="alert">

                        <%= escaparHtml(mensajeError) %>

                    </div>

                    <% } %>

                    <form
                            method="post"
                            action="${pageContext.request.contextPath}/login"
                            autocomplete="on">

                        <div class="mb-3">

                            <label
                                    for="identificador"
                                    class="form-label">

                                Usuario o correo electrónico

                            </label>

                            <input
                                    type="text"
                                    class="form-control"
                                    id="identificador"
                                    name="identificador"
                                    value="<%= escaparHtml(identificador) %>"
                                    maxlength="150"
                                    autocomplete="username"
                                    required
                                    autofocus>

                        </div>

                        <div class="mb-4">

                            <label
                                    for="contrasena"
                                    class="form-label">

                                Contraseña

                            </label>

                            <input
                                    type="password"
                                    class="form-control"
                                    id="contrasena"
                                    name="contrasena"
                                    maxlength="72"
                                    autocomplete="current-password"
                                    required>

                        </div>

                        <div class="d-grid">

                            <button
                                    type="submit"
                                    class="btn btn-primary boton-ingresar">

                                Ingresar

                            </button>

                        </div>

                    </form>

                    <div class="text-center mt-4">

                        <p class="mb-2">
                            ¿Todavía no tenés cuenta?
                            <a href="${pageContext.request.contextPath}/registro"
                               class="text-decoration-none fw-semibold">
                                Registrarme como paciente
                            </a>
                        </p>

                        <a
                                href="${pageContext.request.contextPath}/"
                                class="text-decoration-none">

                            Volver a la página principal

                        </a>

                    </div>

                </div>

            </div>

            <p class="text-center text-white-50 small mt-3">
                Acceso exclusivo para usuarios autorizados
            </p>

        </div>

    </div>

</main>

<script
        src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js">
</script>

</body>
</html>
