<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.DocumentoClinicoDTO" %>
<%@ page import="py.fpune.soracare.dto.TipoDocumentoClinicoDTO" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%!
    private String h(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String tamano(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024L * 1024L) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
%>
<%
    List<DocumentoClinicoDTO> documentos = request.getAttribute("documentos") instanceof List<?>
            ? (List<DocumentoClinicoDTO>) request.getAttribute("documentos") : List.of();
    List<TipoDocumentoClinicoDTO> tipos = request.getAttribute("tiposDocumento") instanceof List<?>
            ? (List<TipoDocumentoClinicoDTO>) request.getAttribute("tiposDocumento") : List.of();
    LocalDate hoy = request.getAttribute("hoy") instanceof LocalDate
            ? (LocalDate) request.getAttribute("hoy") : LocalDate.now();
    DateTimeFormatter fecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex, nofollow">
    <title>Mis documentos clínicos | SoraCare-FPUNE</title>
    <link href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260815">
    <style>
        .document-hero { border-radius: 1.25rem; }
        .upload-card, .document-card { border-radius: 1.1rem; }
        .document-icon {
            display: grid; width: 3.25rem; height: 3.25rem; place-items: center;
            border-radius: .9rem; color: #2563eb; background: #eff6ff; font-size: 1.5rem;
        }
        .document-preview {
            width: 100%; height: 11rem; object-fit: cover; border-radius: .85rem;
            border: 1px solid #e2e8f0; background: #f8fafc;
        }
    </style>
</head>
<body class="app-shell app-patient">
<nav class="navbar">
    <div class="container py-2">
        <a class="navbar-brand" href="${pageContext.request.contextPath}/paciente/inicio">SoraCare-FPUNE</a>
        <div class="d-flex gap-2">
            <a class="btn btn-outline-primary btn-sm" href="${pageContext.request.contextPath}/paciente/inicio">Mi panel</a>
            <a class="btn btn-outline-primary btn-sm" href="${pageContext.request.contextPath}/mis-citas">Mis citas</a>
        </div>
    </div>
</nav>

<main class="container py-4 py-md-5">
    <header class="cabecera document-hero shadow-sm p-4 p-md-5 mb-4">
        <div class="row align-items-center g-3">
            <div class="col-lg-8">
                <p class="mb-2 opacity-75">Portal del paciente · Historia clínica</p>
                <h1 class="display-6 fw-bold mb-2">Mis documentos clínicos</h1>
                <p class="mb-0">Conservá laboratorios, estudios e informes junto a tu historial médico.</p>
            </div>
            <div class="col-lg-4 text-lg-end">
                <span class="badge bg-white text-primary fs-6 px-3 py-2"><%= documentos.size() %> archivo<%= documentos.size() == 1 ? "" : "s" %></span>
            </div>
        </div>
    </header>

    <% if (request.getAttribute("mensajeExito") != null) { %>
    <div class="alert alert-success"><%= h((String) request.getAttribute("mensajeExito")) %></div>
    <% } %>
    <% if (request.getAttribute("error") != null) { %>
    <div class="alert alert-danger"><%= h((String) request.getAttribute("error")) %></div>
    <% } %>

    <section class="card upload-card shadow-sm mb-4">
        <div class="card-body p-4">
            <div class="d-flex gap-3 align-items-start mb-4">
                <div class="document-icon"><i class="bi bi-cloud-arrow-up"></i></div>
                <div>
                    <h2 class="h4 mb-1">Agregar un estudio</h2>
                    <p class="text-secondary mb-0">Admite PDF, JPG, JPEG y PNG de hasta 10 MB.</p>
                </div>
            </div>
            <form method="post" action="${pageContext.request.contextPath}/mis-documentos"
                  enctype="multipart/form-data" class="row g-3"><input type="hidden" name="csrfToken" value="${requestScope.csrfToken}">
                <div class="col-md-5">
                    <label class="form-label" for="idTipoDocumento">Tipo de documento</label>
                    <select class="form-select" id="idTipoDocumento" name="idTipoDocumento" required>
                        <option value="">Seleccionar</option>
                        <% for (TipoDocumentoClinicoDTO tipo : tipos) { %>
                        <option value="<%= tipo.idTipoDocumento() %>"><%= h(tipo.nombre()) %></option>
                        <% } %>
                    </select>
                </div>
                <div class="col-md-3">
                    <label class="form-label" for="fechaDocumento">Fecha del estudio</label>
                    <input class="form-control" type="date" id="fechaDocumento" name="fechaDocumento"
                           value="<%= h(request.getParameter("fechaDocumento") == null ? hoy.toString() : request.getParameter("fechaDocumento")) %>"
                           max="<%= hoy %>" required>
                </div>
                <div class="col-md-4">
                    <label class="form-label" for="archivo">Archivo</label>
                    <input class="form-control" type="file" id="archivo" name="archivo"
                           accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png" required>
                </div>
                <div class="col-12">
                    <label class="form-label" for="descripcion">Descripción (opcional)</label>
                    <textarea class="form-control" id="descripcion" name="descripcion" maxlength="500"
                              rows="2" placeholder="Ej.: Hemograma solicitado durante el control anual."><%= h(request.getParameter("descripcion")) %></textarea>
                </div>
                <div class="col-12 d-flex align-items-center justify-content-between flex-wrap gap-2">
                    <small class="text-secondary"><i class="bi bi-shield-lock me-1"></i>El archivo sólo será visible para vos y profesionales autorizados.</small>
                    <button class="btn btn-primary"><i class="bi bi-upload me-2"></i>Cargar documento</button>
                </div>
            </form>
        </div>
    </section>

    <section>
        <div class="mb-3">
            <h2 class="h4 mb-1">Archivos de mi historia clínica</h2>
            <p class="text-secondary mb-0">Los documentos más recientes aparecen primero.</p>
        </div>
        <% if (documentos.isEmpty()) { %>
        <div class="card shadow-sm"><div class="card-body text-center py-5">
            <div class="document-icon mx-auto mb-3"><i class="bi bi-file-earmark-medical"></i></div>
            <h3 class="h5">Todavía no cargaste documentos</h3>
            <p class="text-secondary mb-0">Cuando agregues un laboratorio o estudio aparecerá en este espacio.</p>
        </div></div>
        <% } else { %>
        <div class="row g-4">
            <% for (DocumentoClinicoDTO documento : documentos) { %>
            <div class="col-md-6 col-xl-4">
                <article class="card document-card shadow-sm h-100"><div class="card-body p-4 d-flex flex-column">
                    <% if (documento.esImagen()) { %>
                    <img class="document-preview mb-3" loading="lazy" alt="Vista previa de <%= h(documento.nombreOriginal()) %>"
                         src="${pageContext.request.contextPath}/documentos/clinicos/archivo?id=<%= documento.idDocumento() %>">
                    <% } else { %>
                    <div class="document-icon mb-3"><i class="bi bi-file-earmark-pdf"></i></div>
                    <% } %>
                    <div class="d-flex justify-content-between gap-2 mb-2">
                        <span class="badge text-bg-primary"><%= h(documento.nombreTipo()) %></span>
                        <small class="text-secondary"><%= tamano(documento.tamanoBytes()) %></small>
                    </div>
                    <h3 class="h6 text-break"><%= h(documento.nombreOriginal()) %></h3>
                    <p class="small text-secondary mb-2">Estudio: <%= documento.fechaDocumento() == null ? "Sin fecha" : documento.fechaDocumento().format(fecha) %></p>
                    <% if (documento.descripcion() != null) { %><p class="small"><%= h(documento.descripcion()) %></p><% } %>
                    <small class="text-secondary mt-auto mb-3">Cargado por <%= h(documento.cargadoPor()) %></small>
                    <div class="d-flex gap-2">
                        <a class="btn btn-primary btn-sm flex-fill" target="_blank"
                           href="${pageContext.request.contextPath}/documentos/clinicos/archivo?id=<%= documento.idDocumento() %>">Ver</a>
                        <a class="btn btn-outline-primary btn-sm" href="${pageContext.request.contextPath}/documentos/clinicos/archivo?id=<%= documento.idDocumento() %>&descargar=1"><i class="bi bi-download"></i></a>
                    </div>
                </div></article>
            </div>
            <% } %>
        </div>
        <% } %>
    </section>
</main>
</body>
</html>
