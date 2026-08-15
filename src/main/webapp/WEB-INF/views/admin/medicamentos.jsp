<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.mvc.model.Medicamento" %>
<%@ page import="py.fpune.soracare.mvc.model.ViaAdministracion" %>
<%@ page import="java.util.List" %>

<%!
    private String escaparHtml(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
%>

<%
    List<Medicamento> medicamentos = request.getAttribute("medicamentos") instanceof List<?>
            ? (List<Medicamento>) request.getAttribute("medicamentos") : List.of();
    Object viasAtributo = request.getAttribute("viasAdministracion");
    ViaAdministracion[] vias = viasAtributo instanceof ViaAdministracion[]
            ? (ViaAdministracion[]) viasAtributo
            : ViaAdministracion.values();
    String mensajeExito = (String) request.getAttribute("mensajeExito");
    String error = (String) request.getAttribute("error");
    String nombreComercial = (String) request.getAttribute("nombreComercial");
    String principioActivo = (String) request.getAttribute("principioActivo");
    String presentacion = (String) request.getAttribute("presentacion");
    String concentracion = (String) request.getAttribute("concentracion");
    String viaSeleccionada = (String) request.getAttribute("viaAdministracion");
    boolean requiereReceta = !"POST".equalsIgnoreCase(request.getMethod())
            || request.getAttribute("requiereReceta") != null;
%>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex, nofollow">
    <title>Medicamentos | SoraCare-FPUNE</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css">
    <style>
        body { background: #f4f7fb; }
        .catalogo-hero {
            border: 0; border-radius: 1.25rem; color: #fff;
            background: linear-gradient(135deg, #1746b3 0%, #2563eb 55%, #0ea5a8 100%);
        }
        .panel { border: 1px solid #dce5f1; border-radius: 1.1rem; }
        .icono { width: 48px; height: 48px; display: grid; place-items: center;
            border-radius: 14px; color: #1d4ed8; background: #eaf1ff; font-size: 1.4rem; }
        .form-control, .form-select { border-radius: .75rem; min-height: 46px; }
        .buscador { max-width: 390px; }
        .medicamento-inactivo { opacity: .68; }
        .detalle { color: #64748b; font-size: .92rem; }
    </style>
</head>
<body class="app-shell app-admin">

<main class="container py-4 py-lg-5">
    <section class="card catalogo-hero shadow-sm mb-4">
        <div class="card-body p-4 p-lg-5 d-flex flex-column flex-lg-row justify-content-between gap-4">
            <div>
                <p class="mb-2 opacity-75">Etapa 7 · Prescripción segura</p>
                <h1 class="display-6 fw-bold mb-2">Catálogo de medicamentos</h1>
                <p class="mb-0">Centralizá los medicamentos disponibles para que las indicaciones médicas sean claras y consistentes.</p>
            </div>
            <div class="align-self-lg-center">
                <span class="badge bg-white text-primary fs-6 px-3 py-2">
                    <%= medicamentos.size() %> registrado<%= medicamentos.size() == 1 ? "" : "s" %>
                </span>
            </div>
        </div>
    </section>

    <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div>
            <h2 class="h4 mb-1">Administración farmacológica</h2>
            <p class="text-secondary mb-0">Acceso exclusivo para administradores.</p>
        </div>
        <a class="btn btn-outline-primary" href="${pageContext.request.contextPath}/admin/inicio">
            <i class="bi bi-arrow-left me-1"></i> Volver a administración
        </a>
    </div>

    <% if (mensajeExito != null && !mensajeExito.isBlank()) { %>
    <div class="alert alert-success"><i class="bi bi-check-circle me-2"></i><%= escaparHtml(mensajeExito) %></div>
    <% } %>
    <% if (error != null && !error.isBlank()) { %>
    <div class="alert alert-danger"><strong>No se pudo completar la operación.</strong>
        <div><%= escaparHtml(error) %></div>
    </div>
    <% } %>

    <section class="row g-4">
        <div class="col-12 col-xl-4">
            <div class="card panel shadow-sm h-100">
                <div class="card-body p-4">
                    <div class="icono mb-3"><i class="bi bi-capsule-pill"></i></div>
                    <h2 class="h5 mb-1">Registrar medicamento</h2>
                    <p class="text-secondary mb-4">Completá su identificación y presentación clínica.</p>
                    <form method="post" action="${pageContext.request.contextPath}/admin/medicamentos">
                        <input type="hidden" name="accion" value="registrar">
                        <div class="mb-3">
                            <label for="nombreComercial" class="form-label fw-semibold">Nombre comercial</label>
                            <input id="nombreComercial" name="nombreComercial" class="form-control"
                                   maxlength="120" required autocomplete="off"
                                   value="<%= escaparHtml(nombreComercial) %>" placeholder="Ej.: Paracetamol">
                        </div>
                        <div class="mb-3">
                            <label for="principioActivo" class="form-label fw-semibold">Principio activo</label>
                            <input id="principioActivo" name="principioActivo" class="form-control"
                                   maxlength="120" required autocomplete="off"
                                   value="<%= escaparHtml(principioActivo) %>" placeholder="Ej.: Acetaminofén">
                        </div>
                        <div class="row g-3 mb-3">
                            <div class="col-sm-7">
                                <label for="presentacion" class="form-label fw-semibold">Presentación</label>
                                <input id="presentacion" name="presentacion" class="form-control"
                                       maxlength="100" required value="<%= escaparHtml(presentacion) %>"
                                       placeholder="Ej.: Comprimidos">
                            </div>
                            <div class="col-sm-5">
                                <label for="concentracion" class="form-label fw-semibold">Concentración</label>
                                <input id="concentracion" name="concentracion" class="form-control"
                                       maxlength="60" value="<%= escaparHtml(concentracion) %>"
                                       placeholder="500 mg">
                            </div>
                        </div>
                        <div class="mb-3">
                            <label for="viaAdministracion" class="form-label fw-semibold">Vía de administración</label>
                            <select id="viaAdministracion" name="viaAdministracion" class="form-select" required>
                                <option value="">Seleccionar</option>
                                <% for (ViaAdministracion via : vias) { %>
                                <option value="<%= via.name() %>"
                                        <%= via.name().equals(viaSeleccionada) ? "selected" : "" %>>
                                    <%= escaparHtml(via.getNombreVisible()) %>
                                </option>
                                <% } %>
                            </select>
                        </div>
                        <div class="form-check mb-4">
                            <input class="form-check-input" type="checkbox" id="requiereReceta"
                                   name="requiereReceta" value="true" <%= requiereReceta ? "checked" : "" %>>
                            <label class="form-check-label" for="requiereReceta">Requiere receta médica</label>
                        </div>
                        <button class="btn btn-primary w-100" type="submit">
                            <i class="bi bi-plus-circle me-1"></i> Registrar medicamento
                        </button>
                    </form>
                </div>
            </div>
        </div>

        <div class="col-12 col-xl-8">
            <div class="card panel shadow-sm">
                <div class="card-body p-4">
                    <div class="d-flex flex-column flex-md-row justify-content-between gap-3 mb-4">
                        <div>
                            <h2 class="h5 mb-1">Medicamentos registrados</h2>
                            <p class="text-secondary mb-0">Los inactivos no aparecerán al registrar indicaciones nuevas.</p>
                        </div>
                        <div class="input-group buscador">
                            <span class="input-group-text bg-white"><i class="bi bi-search"></i></span>
                            <input id="buscarMedicamento" class="form-control" type="search"
                                   placeholder="Buscar nombre o principio activo">
                        </div>
                    </div>

                    <% if (medicamentos.isEmpty()) { %>
                    <div class="text-center py-5">
                        <div class="icono mx-auto mb-3"><i class="bi bi-capsule"></i></div>
                        <h3 class="h5">Todavía no existen medicamentos</h3>
                        <p class="text-secondary mb-0">Registrá el primero desde el formulario.</p>
                    </div>
                    <% } else { %>
                    <div class="table-responsive">
                        <table class="table table-hover align-middle" id="tablaMedicamentos">
                            <thead class="table-light">
                            <tr><th>Medicamento</th><th>Presentación</th><th>Vía</th><th>Estado</th><th class="text-end">Acción</th></tr>
                            </thead>
                            <tbody>
                            <% for (Medicamento medicamento : medicamentos) { %>
                            <tr class="<%= medicamento.isActivo() ? "" : "medicamento-inactivo" %>"
                                data-busqueda="<%= escaparHtml((medicamento.getNombreComercial() + " "
                                        + medicamento.getPrincipioActivo()).toLowerCase()) %>">
                                <td>
                                    <strong><%= escaparHtml(medicamento.getNombreComercial()) %></strong>
                                    <div class="detalle"><%= escaparHtml(medicamento.getPrincipioActivo()) %></div>
                                </td>
                                <td><%= escaparHtml(medicamento.getPresentacion()) %>
                                    <% if (medicamento.getConcentracion() != null) { %>
                                    <div class="detalle"><%= escaparHtml(medicamento.getConcentracion()) %></div>
                                    <% } %>
                                </td>
                                <td><%= escaparHtml(medicamento.getViaAdministracion().getNombreVisible()) %></td>
                                <td>
                                    <span class="badge <%= medicamento.isActivo() ? "text-bg-success" : "text-bg-secondary" %>">
                                        <%= medicamento.isActivo() ? "Activo" : "Inactivo" %>
                                    </span>
                                    <% if (!medicamento.isRequiereReceta()) { %><div class="detalle">Venta libre</div><% } %>
                                </td>
                                <td class="text-end">
                                    <form method="post" action="${pageContext.request.contextPath}/admin/medicamentos">
                                        <input type="hidden" name="accion" value="estado">
                                        <input type="hidden" name="idMedicamento" value="<%= medicamento.getIdMedicamento() %>">
                                        <input type="hidden" name="activo" value="<%= !medicamento.isActivo() %>">
                                        <button class="btn btn-sm <%= medicamento.isActivo() ? "btn-outline-danger" : "btn-outline-success" %>" type="submit">
                                            <%= medicamento.isActivo() ? "Inactivar" : "Activar" %>
                                        </button>
                                    </form>
                                </td>
                            </tr>
                            <% } %>
                            </tbody>
                        </table>
                        <p id="sinResultados" class="text-center text-secondary py-4 d-none mb-0">
                            No hay medicamentos que coincidan con la búsqueda.
                        </p>
                    </div>
                    <% } %>
                </div>
            </div>
        </div>
    </section>
</main>

<script>
    const buscador = document.getElementById('buscarMedicamento');
    if (buscador) {
        buscador.addEventListener('input', () => {
            const termino = buscador.value.trim().toLocaleLowerCase('es');
            const filas = [...document.querySelectorAll('#tablaMedicamentos tbody tr')];
            let visibles = 0;
            filas.forEach(fila => {
                const mostrar = fila.dataset.busqueda.includes(termino);
                fila.classList.toggle('d-none', !mostrar);
                if (mostrar) visibles++;
            });
            document.getElementById('sinResultados').classList.toggle('d-none', visibles !== 0);
        });
    }
</script>
</body>
</html>
