<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.UsuarioAutenticadoDTO" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.util.List" %>
<%!
    private String h(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
%>
<%
    Object usuarioSesion = session == null
            ? null
            : session.getAttribute("usuarioAutenticado");
    boolean esRecepcion = usuarioSesion instanceof UsuarioAutenticadoDTO
            && ((UsuarioAutenticadoDTO) usuarioSesion).tieneRol("RECEPCIONISTA");
    String rutaPanel = esRecepcion ? "/recepcion/inicio" : "/admin/inicio";
    String error = (String) request.getAttribute("error");
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex,nofollow">
    <title>Registrar paciente | SoraCare-FPUNE</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260820d">
</head>
<body class="app-shell app-reception patient-registration-page">
<nav class="navbar reception-navbar">
    <div class="container">
        <a class="navbar-brand" href="${pageContext.request.contextPath}<%= rutaPanel %>">SoraCare-FPUNE</a>
        <div class="d-flex align-items-center gap-2 gap-md-3">
            <a class="btn btn-outline-primary btn-sm" href="${pageContext.request.contextPath}<%= rutaPanel %>">
                <i class="bi bi-arrow-left"></i> Volver al panel
            </a>
            <form method="post" action="${pageContext.request.contextPath}/logout" class="m-0">
                <button type="submit" class="btn btn-outline-secondary btn-sm">Cerrar sesi&oacute;n</button>
            </form>
        </div>
    </div>
</nav>

<main class="container py-4 py-lg-5">
    <section class="patient-registration-hero mb-4">
        <div>
            <span class="patient-registration-eyebrow"><i class="bi bi-person-plus"></i> Admisión de pacientes</span>
            <h1>Registrar nuevo paciente</h1>
            <p>Creá su ficha personal e historia clínica para poder gestionar citas y atención médica.</p>
        </div>
        <span class="patient-registration-hero-icon"><i class="bi bi-person-vcard"></i></span>
    </section>

    <% if (error != null && !error.isBlank()) { %>
    <div class="alert alert-danger" role="alert">
        <i class="bi bi-exclamation-circle me-2"></i><%= h(error) %>
    </div>
    <% } %>

    <div class="row g-4 align-items-start">
        <div class="col-lg-4">
            <aside class="patient-registration-guide">
                <span class="patient-registration-guide-icon"><i class="bi bi-clipboard2-pulse"></i></span>
                <h2>Ficha inicial del paciente</h2>
                <p>Los datos ingresados formarán parte de su identificación dentro de SoraCare.</p>
                <ul>
                    <li><i class="bi bi-check-circle"></i><span><strong>Identificación</strong> para evitar registros duplicados.</span></li>
                    <li><i class="bi bi-check-circle"></i><span><strong>Contacto</strong> para avisos relacionados con sus citas.</span></li>
                    <li><i class="bi bi-check-circle"></i><span><strong>Datos asistenciales</strong> útiles ante una atención médica.</span></li>
                </ul>
                <div class="patient-registration-privacy">
                    <i class="bi bi-shield-lock"></i>
                    <span>La información se utiliza exclusivamente para la gestión asistencial autorizada.</span>
                </div>
            </aside>
        </div>

        <div class="col-lg-8">
            <section class="card patient-registration-card">
                <div class="card-body p-4 p-lg-5">
                    <form action="${pageContext.request.contextPath}/pacientes/registro" method="post">
                        <div class="registration-section-heading">
                            <span>1</span>
                            <div><h2>Datos personales</h2><p>Información básica para identificar al paciente.</p></div>
                        </div>
                        <div class="row g-4">
                            <div class="col-md-6">
                                <div class="registration-field">
                                    <label for="nombres"><i class="bi bi-person"></i> Nombres</label>
                                    <div><i class="bi bi-person"></i><input type="text" class="form-control" id="nombres" name="nombres" maxlength="100" autocomplete="given-name" value="<%= h(request.getParameter("nombres")) %>" required></div>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="registration-field">
                                    <label for="apellidos"><i class="bi bi-person"></i> Apellidos</label>
                                    <div><i class="bi bi-person"></i><input type="text" class="form-control" id="apellidos" name="apellidos" maxlength="100" autocomplete="family-name" value="<%= h(request.getParameter("apellidos")) %>" required></div>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="registration-field">
                                    <label for="cedula"><i class="bi bi-person-vcard"></i> Cédula</label>
                                    <div><i class="bi bi-123"></i><input type="text" class="form-control" id="cedula" name="cedula" inputmode="numeric" maxlength="30" autocomplete="off" value="<%= h(request.getParameter("cedula")) %>" required></div>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="registration-field">
                                    <label for="fechaNacimiento"><i class="bi bi-calendar3"></i> Fecha de nacimiento</label>
                                    <div><i class="bi bi-calendar3"></i><input type="date" class="form-control" id="fechaNacimiento" name="fechaNacimiento" max="<%= LocalDate.now() %>" value="<%= h(request.getParameter("fechaNacimiento")) %>"></div>
                                </div>
                            </div>
                        </div>

                        <div class="registration-section-heading mt-5">
                            <span>2</span>
                            <div><h2>Contacto y ubicación</h2><p>Medios para comunicarse con el paciente cuando sea necesario.</p></div>
                        </div>
                        <div class="row g-4">
                            <div class="col-md-6">
                                <div class="registration-field">
                                    <label for="telefono"><i class="bi bi-telephone"></i> Teléfono</label>
                                    <div><i class="bi bi-telephone"></i><input type="tel" class="form-control" id="telefono" name="telefono" maxlength="30" autocomplete="tel" value="<%= h(request.getParameter("telefono")) %>" placeholder="Ej.: 0981 123 456"></div>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="registration-field">
                                    <label for="correo"><i class="bi bi-envelope"></i> Correo electrónico</label>
                                    <div><i class="bi bi-envelope"></i><input type="email" class="form-control" id="correo" name="correo" maxlength="150" autocomplete="email" value="<%= h(request.getParameter("correo")) %>" placeholder="paciente@correo.com"></div>
                                </div>
                            </div>
                            <div class="col-12">
                                <div class="registration-field registration-field-textarea">
                                    <label for="direccion"><i class="bi bi-geo-alt"></i> Dirección</label>
                                    <div><i class="bi bi-house-door"></i><textarea class="form-control" id="direccion" name="direccion" rows="3" maxlength="255" autocomplete="street-address" placeholder="Barrio, calle y referencia clara"><%= h(request.getParameter("direccion")) %></textarea></div>
                                    <small>Ingresá una referencia que permita identificar correctamente la ubicación.</small>
                                </div>
                            </div>
                        </div>

                        <div class="registration-section-heading mt-5">
                            <span>3</span>
                            <div><h2>Información asistencial</h2><p>Datos opcionales que pueden ser útiles durante la atención.</p></div>
                        </div>
                        <div class="row g-4">
                            <div class="col-md-6">
                                <div class="registration-field">
                                    <label for="grupoSanguineo"><i class="bi bi-droplet"></i> Grupo sanguíneo</label>
                                    <div><i class="bi bi-droplet"></i><select class="form-select" id="grupoSanguineo" name="grupoSanguineo">
                                        <option value="">No informado</option>
                                        <% for (String grupo : List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")) { %>
                                        <option value="<%= grupo %>" <%= grupo.equals(request.getParameter("grupoSanguineo")) ? "selected" : "" %>><%= grupo %></option>
                                        <% } %>
                                    </select></div>
                                    <small>Puede dejarse vacío si el paciente no lo conoce.</small>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="registration-field">
                                    <label for="contactoEmergencia"><i class="bi bi-person-exclamation"></i> Contacto de emergencia</label>
                                    <div><i class="bi bi-person"></i><input type="text" class="form-control" id="contactoEmergencia" name="contactoEmergencia" maxlength="150" value="<%= h(request.getParameter("contactoEmergencia")) %>" placeholder="Nombre y apellido"></div>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="registration-field">
                                    <label for="telefonoEmergencia"><i class="bi bi-telephone-outbound"></i> Teléfono de emergencia</label>
                                    <div><i class="bi bi-telephone"></i><input type="tel" class="form-control" id="telefonoEmergencia" name="telefonoEmergencia" maxlength="30" value="<%= h(request.getParameter("telefonoEmergencia")) %>" placeholder="Ej.: 0971 555 555"></div>
                                </div>
                            </div>
                        </div>

                        <div class="patient-registration-actions mt-5">
                            <div><i class="bi bi-info-circle"></i><span>Al guardar se generará automáticamente el número de historia clínica.</span></div>
                            <div class="d-flex flex-wrap gap-2">
                                <a href="${pageContext.request.contextPath}<%= rutaPanel %>" class="btn btn-outline-secondary">Cancelar</a>
                                <button type="submit" class="btn btn-primary"><i class="bi bi-person-check"></i> Registrar paciente</button>
                            </div>
                        </div>
                    </form>
                </div>
            </section>
        </div>
    </div>
</main>
<script src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js"></script>
</body>
</html>
