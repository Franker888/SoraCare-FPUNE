<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.mvc.util.SesionNavegacionPublica" %>
<%@ page import="py.fpune.soracare.mvc.model.Especialidad" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%!
    private String h(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String claseIcono(String nombre) {
        String valor = nombre == null ? "" : nombre.toLowerCase(Locale.ROOT);
        if (valor.contains("cardio")) return "bi-heart-pulse";
        if (valor.contains("clínica") || valor.contains("clinica")) return "bi-clipboard2-pulse";
        if (valor.contains("dermato")) return "bi-bandaid";
        if (valor.contains("endocrino")) return "bi-activity";
        if (valor.contains("gineco") || valor.contains("obstetricia")) return "bi-gender-female";
        if (valor.contains("neuro")) return "bi-diagram-3";
        if (valor.contains("pedia")) return "bi-person-arms-up";
        if (valor.contains("psico")) return "bi-chat-heart";
        if (valor.contains("traumato") || valor.contains("ortopedia")) return "bi-universal-access";
        return "bi-plus-circle";
    }

    private String tonoEspecialidad(String nombre) {
        String valor = nombre == null ? "" : nombre.toLowerCase(Locale.ROOT);
        if (valor.contains("cardio")) return "tono-rosa";
        if (valor.contains("clínica") || valor.contains("clinica")) return "tono-azul";
        if (valor.contains("dermato")) return "tono-ambar";
        if (valor.contains("endocrino")) return "tono-verde";
        if (valor.contains("gineco") || valor.contains("obstetricia")) return "tono-violeta";
        if (valor.contains("neuro")) return "tono-indigo";
        if (valor.contains("pedia")) return "tono-celeste";
        if (valor.contains("psico")) return "tono-fucsia";
        if (valor.contains("traumato") || valor.contains("ortopedia")) return "tono-naranja";
        return "tono-azul";
    }

    private String codificar(String texto) {
        return URLEncoder.encode(texto == null ? "" : texto, StandardCharsets.UTF_8);
    }
%>
<%
    List<Especialidad> especialidades = request.getAttribute("especialidades") instanceof List<?>
            ? (List<Especialidad>) request.getAttribute("especialidades") : List.of();
    String error = (String) request.getAttribute("error");
    SesionNavegacionPublica navegacion = SesionNavegacionPublica.desde(session);
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="Catálogo público de especialidades médicas de SoraCare-FPUNE.">
    <title>Especialidades médicas | SoraCare-FPUNE</title>
    <link href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css"
          rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/libs/bootstrap-icons/bootstrap-icons.min.css"
          rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260815">
    <style>
        body { min-height: 100vh; background: #f4f7fb; color: #14213d; }
        .barra-publica { background: #fff; border-bottom: 1px solid #e5ebf3; }
        .marca-soracare { color: #235ee7; font-weight: 800; }
        .marca-soracare:hover { color: #184ac2; }
        .enlace-publico { color: #334155; font-weight: 600; }
        .enlace-publico:hover, .enlace-publico.activo { color: #235ee7; }
        .boton-principal { background: #2864e8; border-color: #2864e8; }
        .boton-principal:hover { background: #1d4ed8; border-color: #1d4ed8; }

        .encabezado-catalogo {
            position: relative; overflow: hidden; min-height: 22rem;
            border: 0; border-radius: 1.5rem; color: #fff;
            background: linear-gradient(125deg, #173da8 0%, #2563eb 55%, #1598b8 100%);
        }
        .encabezado-catalogo::before {
            content: ""; position: absolute; inset: 0 0 0 45%;
            background:
                    linear-gradient(90deg, #2158dd 0%, rgba(33, 88, 221, .12) 68%),
                    url('${pageContext.request.contextPath}/assets/home/img/why-soracare/medical-team.jpg')
                    center 36% / cover no-repeat;
            opacity: .72;
        }
        .encabezado-catalogo::after {
            content: ""; position: absolute; width: 18rem; height: 18rem;
            right: -4rem; bottom: -8rem; border-radius: 50%;
            background: rgba(255, 255, 255, .08);
        }
        .contenido-hero { position: relative; z-index: 1; }
        .sello-hero {
            display: inline-flex; align-items: center; gap: .45rem;
            padding: .45rem .8rem; border: 1px solid rgba(255,255,255,.45);
            border-radius: 999px; background: rgba(255,255,255,.12);
            font-size: .88rem; font-weight: 700;
        }

        .buscador-panel {
            position: relative; z-index: 2; margin-top: -2.4rem;
            border: 1px solid #e0e7f0; border-radius: 1.25rem;
        }
        .campo-busqueda { position: relative; }
        .campo-busqueda i {
            position: absolute; left: 1rem; top: 50%; transform: translateY(-50%);
            color: #64748b; font-size: 1.15rem; pointer-events: none;
        }
        .campo-busqueda input {
            min-height: 3.25rem; padding-left: 3rem; border-radius: .85rem;
        }
        .ayuda-buscador { color: #64748b; }

        .tarjeta-especialidad {
            position: relative; overflow: hidden; border: 1px solid #dfe7f1;
            border-radius: 1.25rem; background: #fff;
            transition: transform .2s ease, box-shadow .2s ease, border-color .2s ease;
        }
        .tarjeta-especialidad::before {
            content: ""; position: absolute; inset: 0 auto 0 0; width: .3rem;
            background: var(--tono);
        }
        .tarjeta-especialidad:hover {
            transform: translateY(-4px); border-color: #c7d7ee;
            box-shadow: 0 1rem 2.4rem rgba(30, 64, 175, .11) !important;
        }
        .icono-especialidad {
            display: grid; width: 3.7rem; height: 3.7rem; place-items: center;
            flex: 0 0 3.7rem; border-radius: 1.05rem;
            color: var(--tono); background: var(--fondo-tono); font-size: 1.65rem;
        }
        .duracion-consulta {
            display: inline-flex; align-items: center; gap: .35rem;
            padding: .4rem .65rem; border-radius: 999px;
            color: #475569; background: #f1f5f9; font-size: .8rem; font-weight: 700;
        }
        .enlace-especialidad {
            display: inline-flex; align-items: center; justify-content: space-between;
            gap: .75rem; color: #235ee7; font-weight: 700; text-decoration: none;
        }
        .enlace-especialidad i { transition: transform .2s ease; }
        .enlace-especialidad:hover i { transform: translateX(4px); }
        .tono-rosa { --tono: #e11d48; --fondo-tono: #fff1f2; }
        .tono-azul { --tono: #2563eb; --fondo-tono: #eff6ff; }
        .tono-ambar { --tono: #d97706; --fondo-tono: #fffbeb; }
        .tono-verde { --tono: #059669; --fondo-tono: #ecfdf5; }
        .tono-violeta { --tono: #7c3aed; --fondo-tono: #f5f3ff; }
        .tono-indigo { --tono: #4f46e5; --fondo-tono: #eef2ff; }
        .tono-celeste { --tono: #0284c7; --fondo-tono: #f0f9ff; }
        .tono-fucsia { --tono: #c026d3; --fondo-tono: #fdf4ff; }
        .tono-naranja { --tono: #ea580c; --fondo-tono: #fff7ed; }
        .sin-resultados {
            border: 1px dashed #cbd5e1; border-radius: 1.1rem; background: #fff;
        }
        .cta-especialidades {
            position: relative; overflow: hidden; border: 0; border-radius: 1.35rem;
            color: #fff; background: linear-gradient(120deg, #173da8, #2563eb 60%, #0ea5a7);
        }
        .cta-especialidades::after {
            content: ""; position: absolute; width: 13rem; height: 13rem;
            right: -4rem; top: -6rem; border: 2rem solid rgba(255,255,255,.08);
            border-radius: 50%;
        }
        .cta-contenido { position: relative; z-index: 1; }
        .pie-publico { background: #10213e; color: #cbd5e1; }

        @media (max-width: 991.98px) {
            .encabezado-catalogo::before { inset: 0; opacity: .22; }
            .encabezado-catalogo { min-height: auto; }
            .buscador-panel { margin-top: -1.5rem; }
        }
    </style>
</head>
<body class="app-public">
<nav class="navbar navbar-expand-lg barra-publica sticky-top">
    <div class="container py-2">
        <a class="navbar-brand marca-soracare" href="${pageContext.request.contextPath}/">
            SoraCare-FPUNE
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse"
                data-bs-target="#navegacionPublica" aria-controls="navegacionPublica"
                aria-expanded="false" aria-label="Abrir navegación">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navegacionPublica">
            <div class="navbar-nav ms-auto align-items-lg-center gap-lg-2">
                <a class="nav-link enlace-publico" href="${pageContext.request.contextPath}/">Inicio</a>
                <a class="nav-link enlace-publico activo"
                   href="${pageContext.request.contextPath}/especialidades"
                   aria-current="page">Especialidades</a>
                <a class="nav-link enlace-publico" href="${pageContext.request.contextPath}/medicos">Médicos</a>
                <a class="btn btn-primary boton-principal ms-lg-2"
                   href="<%= h(request.getContextPath() + navegacion.rutaAcceso()) %>">
                    <%= h(navegacion.textoAcceso()) %>
                </a>
            </div>
        </div>
    </div>
</nav>

<main class="container py-4 py-md-5">
    <section class="card encabezado-catalogo shadow-sm">
        <div class="card-body contenido-hero d-flex align-items-center p-4 p-md-5">
            <div class="row w-100">
                <div class="col-lg-7">
                    <span class="sello-hero mb-3">
                        <i class="bi bi-stars" aria-hidden="true"></i>
                        Atención integral · Cuidado especializado
                    </span>
                    <h1 class="display-4 fw-bold mb-3">Encontrá el cuidado que necesitás</h1>
                    <p class="lead mb-4">
                        Explorá nuestras áreas médicas y elegí la atención adecuada para cada etapa de tu salud.
                    </p>
                    <span class="small opacity-75">Especialidades activas de SoraCare-FPUNE</span>
                </div>
            </div>
        </div>
    </section>

    <% if (!especialidades.isEmpty()) { %>
    <section class="card buscador-panel shadow-sm mb-5">
        <div class="card-body p-3 p-md-4">
            <div class="row align-items-center g-3">
                <div class="col-lg-8">
                    <label for="buscadorEspecialidades" class="form-label fw-semibold">
                        ¿Qué tipo de atención estás buscando?
                    </label>
                    <div class="campo-busqueda">
                        <i class="bi bi-search" aria-hidden="true"></i>
                        <input id="buscadorEspecialidades" class="form-control" type="search"
                               placeholder="Ej.: cardiología, pediatría o neurología">
                    </div>
                </div>
                <div class="col-lg-4">
                    <div class="ayuda-buscador d-flex align-items-start gap-2 pt-lg-4">
                        <i class="bi bi-info-circle mt-1" aria-hidden="true"></i>
                        <span>Escribí el nombre o una palabra relacionada con la especialidad.</span>
                    </div>
                </div>
            </div>
        </div>
    </section>
    <% } %>

    <div class="mb-4">
        <p class="text-primary fw-semibold mb-1">Servicios médicos</p>
        <h2 class="h3 mb-1">Elegí un área de atención</h2>
        <p class="text-secondary mb-0">
            Consultá la duración estimada y conocé a los profesionales de cada especialidad.
        </p>
    </div>

    <% if (error != null && !error.isBlank()) { %>
    <div class="alert alert-danger" role="alert">
        <strong>No pudimos cargar el catálogo.</strong>
        <div class="mt-1"><%= h(error) %></div>
    </div>
    <% } %>

    <% if (especialidades.isEmpty()) { %>
    <section class="card border-0 shadow-sm">
        <div class="card-body text-center p-5">
            <div class="icono-especialidad tono-azul mx-auto mb-3">
                <i class="bi bi-hospital" aria-hidden="true"></i>
            </div>
            <h2 class="h4">No existen especialidades disponibles</h2>
            <p class="text-secondary mb-0">
                El catálogo todavía no posee especialidades activas para mostrar.
            </p>
        </div>
    </section>
    <% } else { %>
    <section id="listaEspecialidades" class="row g-4">
        <% for (Especialidad especialidad : especialidades) {
            String nombre = especialidad.getNombre();
            String descripcion = especialidad.getDescripcion();
            String textoBusqueda = nombre + " " + (descripcion == null ? "" : descripcion);
        %>
        <div class="col-12 col-md-6 col-xl-4 especialidad-item"
             data-busqueda="<%= h(textoBusqueda) %>">
            <article class="card tarjeta-especialidad <%= tonoEspecialidad(nombre) %> shadow-sm h-100">
                <div class="card-body p-4 ps-4 d-flex flex-column">
                    <div class="d-flex justify-content-between align-items-start gap-3 mb-4">
                        <div class="icono-especialidad">
                            <i class="bi <%= claseIcono(nombre) %>" aria-hidden="true"></i>
                        </div>
                        <span class="duracion-consulta">
                            <i class="bi bi-clock" aria-hidden="true"></i>
                            <%= especialidad.getDuracionConsultaMinutos() %> min
                        </span>
                    </div>

                    <h3 class="h4 mb-3"><%= h(nombre) %></h3>
                    <p class="text-secondary flex-grow-1 mb-4">
                        <%= descripcion == null || descripcion.isBlank()
                                ? "Conocé los profesionales y los próximos turnos disponibles para esta especialidad."
                                : h(descripcion) %>
                    </p>

                    <a class="enlace-especialidad pt-3 border-top"
                       href="${pageContext.request.contextPath}/medicos?especialidad=<%= codificar(nombre) %>">
                        <span>Ver profesionales</span>
                        <i class="bi bi-arrow-right" aria-hidden="true"></i>
                    </a>
                </div>
            </article>
        </div>
        <% } %>
    </section>
    <div id="sinResultados" class="sin-resultados text-center p-5 d-none">
        <i class="bi bi-search fs-2 text-primary" aria-hidden="true"></i>
        <h3 class="h5 mt-3">No encontramos esa especialidad</h3>
        <p class="text-secondary mb-0">Probá con otro nombre o una palabra más general.</p>
    </div>
    <% } %>

    <section class="card cta-especialidades shadow-sm mt-5">
        <div class="card-body cta-contenido p-4 p-md-5">
            <div class="row align-items-center g-4">
                <div class="col-lg-8">
                    <p class="small text-uppercase fw-bold opacity-75 mb-2">Tu próxima consulta</p>
                    <h2 class="h3 mb-2">¿Ya sabés con qué especialista querés atenderte?</h2>
                    <p class="mb-0 opacity-75">
                        <%= h(navegacion.mensajeAgenda()) %>
                    </p>
                </div>
                <div class="col-lg-4 text-lg-end">
                    <a href="<%= h(request.getContextPath() + navegacion.rutaAgenda()) %>"
                       class="btn btn-light text-primary fw-bold px-4 py-2">
                        <%= h(navegacion.textoAgenda()) %>
                    </a>
                </div>
            </div>
        </div>
    </section>
</main>

<footer class="pie-publico mt-5">
    <div class="container py-4 d-flex flex-column flex-md-row justify-content-between gap-2">
        <span>SoraCare-FPUNE</span>
        <span>Sistema de gestión hospitalaria</span>
    </div>
</footer>
<script src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js"></script>
<script>
    const buscador = document.getElementById('buscadorEspecialidades');
    const items = [...document.querySelectorAll('.especialidad-item')];
    const sinResultados = document.getElementById('sinResultados');
    const normalizar = valor => (valor || '')
        .normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();

    const filtrarEspecialidades = () => {
        const texto = normalizar(buscador?.value);
        let visibles = 0;
        items.forEach(item => {
            const coincide = !texto || normalizar(item.dataset.busqueda).includes(texto);
            item.classList.toggle('d-none', !coincide);
            if (coincide) visibles++;
        });
        sinResultados?.classList.toggle('d-none', visibles !== 0);
    };

    buscador?.addEventListener('input', filtrarEspecialidades);
</script>
</body>
</html>
