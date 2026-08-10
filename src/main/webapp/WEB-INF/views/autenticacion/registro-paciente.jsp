<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%!
    private String h(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Crear cuenta de paciente | SoraCare-FPUNE</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body { background: #f2f7f5; }
        .cabecera { background: linear-gradient(135deg, #0b5f49, #15956e); color: white; }
        .card { border: 0; border-radius: 1rem; }
    </style>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css">
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
        <form method="post" action="${pageContext.request.contextPath}/registro" class="card shadow-sm">
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
                <p class="text-secondary">Usarás estas credenciales para ingresar y gestionar tus citas.</p>
                <div class="row g-3">
                    <div class="col-md-6"><label class="form-label" for="nombreUsuario">Nombre de usuario</label><input class="form-control" id="nombreUsuario" name="nombreUsuario" minlength="4" maxlength="50" pattern="[A-Za-z0-9._-]+" required autocomplete="username" value="<%= h(request.getParameter("nombreUsuario")) %>"></div>
                    <div class="col-md-6"></div>
                    <div class="col-md-6"><label class="form-label" for="contrasena">Contraseña</label><input class="form-control" type="password" id="contrasena" name="contrasena" minlength="10" maxlength="72" required autocomplete="new-password"><div class="form-text">Mínimo 10 caracteres, con mayúscula, minúscula, número y símbolo.</div></div>
                    <div class="col-md-6"><label class="form-label" for="confirmacionContrasena">Confirmar contraseña</label><input class="form-control" type="password" id="confirmacionContrasena" name="confirmacionContrasena" minlength="10" maxlength="72" required autocomplete="new-password"></div>
                </div>

                <div class="d-flex flex-wrap gap-2 mt-4"><button class="btn btn-success px-4">Crear cuenta de paciente</button><a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/login">Cancelar</a></div>
            </div>
        </form>
    </div></div>
</main>
</body>
</html>
