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

    private String valor(HttpServletRequest request, String nombre, String inicial) {
        String recibido = request.getParameter(nombre);
        return recibido == null ? inicial : recibido;
    }
%>
<%
    PerfilGoogleDTO perfil =
            (PerfilGoogleDTO) request.getAttribute("perfilGoogle");
    String error = (String) request.getAttribute("error");
    String nombresGoogle = perfil == null ? "" : perfil.nombres();
    String apellidosGoogle = perfil == null ? "" : perfil.apellidos();
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex, nofollow">
    <title>Completar perfil | SoraCare-FPUNE</title>
    <link href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css"
          rel="stylesheet">
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260821">
</head>
<body class="app-shell google-registration-page">
<nav class="navbar bg-white border-bottom">
    <div class="container py-2">
        <a class="navbar-brand fw-bold text-primary"
           href="${pageContext.request.contextPath}/">SoraCare-FPUNE</a>
        <a class="btn btn-outline-secondary btn-sm"
           href="${pageContext.request.contextPath}/login">Cancelar</a>
    </div>
</nav>

<header class="google-registration-hero">
    <div class="container">
        <span>Registro protegido con Google</span>
        <h1>Completá tu ficha de paciente</h1>
        <p>
            Tu correo ya fue verificado. Solicitamos únicamente los datos
            necesarios para gestionar citas e historia clínica.
        </p>
    </div>
</header>

<main class="container py-4 py-md-5">
    <div class="row justify-content-center">
        <div class="col-xl-10">
            <div class="verified-google-email">
                <span class="verified-google-icon">G</span>
                <div>
                    <small>Correo verificado por Google</small>
                    <strong><%= h(perfil == null ? null : perfil.correo()) %></strong>
                </div>
            </div>

            <% if (error != null && !error.isBlank()) { %>
            <div class="alert alert-danger" role="alert"><%= h(error) %></div>
            <% } %>

            <form method="post"
                  action="${pageContext.request.contextPath}/google/registro"
                  class="card border-0 shadow-sm google-registration-form">
                <div class="card-body p-4 p-md-5">
                    <div class="section-heading-medical">
                        <span>01</span>
                        <div>
                            <h2>Identificación personal</h2>
                            <p>Datos obligatorios para crear tu historia clínica.</p>
                        </div>
                    </div>

                    <div class="row g-3">
                        <div class="col-md-6">
                            <label class="form-label" for="nombres">Nombres</label>
                            <input class="form-control" id="nombres" name="nombres"
                                   maxlength="100" required
                                   value="<%= h(valor(request, "nombres", nombresGoogle)) %>">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="apellidos">Apellidos</label>
                            <input class="form-control" id="apellidos" name="apellidos"
                                   maxlength="100" required
                                   value="<%= h(valor(request, "apellidos", apellidosGoogle)) %>">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label" for="cedula">Cédula</label>
                            <input class="form-control" id="cedula" name="cedula"
                                   inputmode="numeric" pattern="[0-9]+" required
                                   value="<%= h(valor(request, "cedula", "")) %>">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label" for="fechaNacimiento">Fecha de nacimiento</label>
                            <input class="form-control" type="date"
                                   id="fechaNacimiento" name="fechaNacimiento"
                                   value="<%= h(valor(request, "fechaNacimiento", "")) %>">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label" for="telefono">Teléfono</label>
                            <input class="form-control" id="telefono" name="telefono"
                                   maxlength="30"
                                   value="<%= h(valor(request, "telefono", "")) %>">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="grupoSanguineo">Grupo sanguíneo</label>
                            <select class="form-select" id="grupoSanguineo" name="grupoSanguineo">
                                <option value="">No informado</option>
                                <option>A+</option><option>A-</option>
                                <option>B+</option><option>B-</option>
                                <option>AB+</option><option>AB-</option>
                                <option>O+</option><option>O-</option>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="nombreUsuario">Nombre de usuario</label>
                            <input class="form-control" id="nombreUsuario"
                                   name="nombreUsuario" minlength="4" maxlength="50"
                                   pattern="[A-Za-z0-9._-]+" required
                                   autocomplete="username"
                                   value="<%= h(valor(request, "nombreUsuario", "")) %>">
                            <div class="form-text">Podrás usar Google o este nombre para identificar tu cuenta.</div>
                        </div>
                        <div class="col-12">
                            <label class="form-label" for="direccion">Dirección</label>
                            <textarea class="form-control" id="direccion" name="direccion"
                                      maxlength="255" rows="2"><%= h(valor(request, "direccion", "")) %></textarea>
                        </div>
                    </div>

                    <div class="section-heading-medical mt-5">
                        <span>02</span>
                        <div>
                            <h2>Contacto de emergencia</h2>
                            <p>Información opcional que podrás completar o actualizar posteriormente.</p>
                        </div>
                    </div>

                    <div class="row g-3">
                        <div class="col-md-6">
                            <label class="form-label" for="contactoEmergencia">Nombre del contacto</label>
                            <input class="form-control" id="contactoEmergencia"
                                   name="contactoEmergencia" maxlength="150"
                                   value="<%= h(valor(request, "contactoEmergencia", "")) %>">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="telefonoEmergencia">Teléfono del contacto</label>
                            <input class="form-control" id="telefonoEmergencia"
                                   name="telefonoEmergencia" maxlength="30"
                                   value="<%= h(valor(request, "telefonoEmergencia", "")) %>">
                        </div>
                    </div>

                    <div class="google-registration-actions">
                        <p>
                            Al continuar se creará exclusivamente un perfil de paciente.
                        </p>
                        <button class="btn btn-primary btn-lg" type="submit">
                            Crear mi cuenta y continuar
                        </button>
                    </div>
                </div>
            </form>
        </div>
    </div>
</main>
</body>
</html>
