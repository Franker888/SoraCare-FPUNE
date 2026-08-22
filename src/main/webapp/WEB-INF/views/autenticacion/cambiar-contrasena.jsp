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
    boolean cambioObligatorio =
            Boolean.TRUE.equals(request.getAttribute("cambioObligatorio"));
    String rutaVolver = (String) request.getAttribute("rutaVolver");
    if (rutaVolver == null || rutaVolver.isBlank()) {
        rutaVolver = "/dashboard";
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

    <title>Cambiar contraseña | SoraCare-FPUNE</title>

    <link
            href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css"
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

        .contenedor-cambio {
            min-height: 100vh;
        }

        .tarjeta-cambio {
            width: 100%;
            max-width: 520px;
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

        .boton-actualizar {
            min-height: 48px;
            background-color: #0f766e;
            border-color: #0f766e;
        }

        .boton-actualizar:hover,
        .boton-actualizar:focus {
            background-color: #115e59;
            border-color: #115e59;
        }

        .lista-requisitos {
            color: #475569;
            font-size: 0.9rem;
        }
    </style>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260815">
</head>

<body class="soracare-auth soracare-auth-simple">

<main class="container">

    <div class="row align-items-center
                justify-content-center
                contenedor-cambio
                py-4">

        <div class="col-12 col-sm-11 col-md-8 col-lg-6">

            <div class="card tarjeta-cambio shadow-lg mx-auto">

                <div class="card-body p-4 p-md-5">

                    <div class="text-center mb-4">

                        <img src="${pageContext.request.contextPath}/assets/home/img/logo/SoraCare1.png"
                             alt="Logo SoraCare"
                             width="82"
                             height="82"
                             class="object-fit-contain mb-3">

                        <h1 class="h3 marca-soracare mb-2">
                            SoraCare-FPUNE
                        </h1>

                        <p class="subtitulo mb-0">
                            Protección de la cuenta
                        </p>

                    </div>

                    <h2 class="h5 text-center mb-3">
                        Cambiar contraseña
                    </h2>

                    <% if (cambioObligatorio) { %>
                        <div class="alert alert-warning" role="alert">
                            Esta cuenta recibió una contraseña temporal.
                            Por seguridad, debés reemplazarla antes de continuar.
                        </div>
                    <% } else { %>
                        <div class="alert alert-info" role="alert">
                            El cambio es opcional. Tu contraseña actual seguirá
                            funcionando mientras no la reemplaces.
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
                            action="${pageContext.request.contextPath}/cambiar-contrasena"
                            autocomplete="on">

                        <div class="mb-3">

                            <label
                                    for="contrasenaActual"
                                    class="form-label">

                                Contraseña actual

                            </label>

                            <input
                                    type="password"
                                    class="form-control"
                                    id="contrasenaActual"
                                    name="contrasenaActual"
                                    maxlength="72"
                                    autocomplete="current-password"
                                    required
                                    autofocus>

                        </div>

                        <div class="mb-3">

                            <label
                                    for="nuevaContrasena"
                                    class="form-label">

                                Nueva contraseña

                            </label>

                            <input
                                    type="password"
                                    class="form-control"
                                    id="nuevaContrasena"
                                    name="nuevaContrasena"
                                    minlength="10"
                                    maxlength="72"
                                    autocomplete="new-password"
                                    required>

                        </div>

                        <div class="mb-3">

                            <label
                                    for="confirmacionNuevaContrasena"
                                    class="form-label">

                                Confirmar nueva contraseña

                            </label>

                            <input
                                    type="password"
                                    class="form-control"
                                    id="confirmacionNuevaContrasena"
                                    name="confirmacionNuevaContrasena"
                                    minlength="10"
                                    maxlength="72"
                                    autocomplete="new-password"
                                    required>

                        </div>

                        <div class="bg-light rounded p-3 mb-4">

                            <p class="fw-semibold mb-2">
                                La nueva contraseña debe contener:
                            </p>

                            <ul class="lista-requisitos mb-0">
                                <li>Al menos 10 caracteres.</li>
                                <li>Una letra mayúscula.</li>
                                <li>Una letra minúscula.</li>
                                <li>Un número.</li>
                                <li>Un carácter especial.</li>
                                <li>Debe ser diferente de la actual.</li>
                            </ul>

                        </div>

                        <div class="d-grid">

                            <button
                                    type="submit"
                                    class="btn btn-primary boton-actualizar">

                                Actualizar contraseña

                            </button>

                        </div>

                    </form>

                    <div class="text-center mt-4">

                        <% if (!cambioObligatorio) { %>
                            <a class="btn btn-link text-decoration-none"
                               href="${pageContext.request.contextPath}<%= escaparHtml(rutaVolver) %>">
                                Volver sin cambiarla
                            </a>
                        <% } else { %>

                        <form
                                method="post"
                                action="${pageContext.request.contextPath}/logout">

                            <button
                                    type="submit"
                                    class="btn btn-link text-decoration-none">

                                Cerrar sesión

                            </button>

                        </form>

                        <% } %>

                    </div>

                </div>

            </div>

            <p class="text-center text-white-50 small mt-3">
                La contraseña se almacenará de forma segura
            </p>

        </div>

    </div>

</main>

<script
        src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js">
</script>

</body>
</html>
