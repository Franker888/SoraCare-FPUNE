<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.mvc.util.SesionNavegacionPublica" %>
<%@ page import="py.fpune.soracare.mvc.model.Especialidad" %>
<%@ page import="py.fpune.soracare.mvc.model.HorarioMedico" %>
<%@ page import="py.fpune.soracare.mvc.model.Medico" %>
<%@ page import="py.fpune.soracare.mvc.model.MedicoEspecialidad" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%!
    private String h(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String inicial(String texto) {
        return texto == null || texto.isBlank()
                ? "M" : texto.strip().substring(0, 1).toUpperCase();
    }

    private String archivoImagenSeguro(String nombre) {
        if (nombre == null || !nombre.matches("[A-Za-z0-9._-]+")) return "";
        return nombre;
    }

    private String nombreDia(int diaSemana) {
        switch (diaSemana) {
            case 1:
                return "Lunes";
            case 2:
                return "Martes";
            case 3:
                return "Miércoles";
            case 4:
                return "Jueves";
            case 5:
                return "Viernes";
            case 6:
                return "Sábado";
            case 7:
                return "Domingo";
            default:
                return "Día por confirmar";
        }
    }

    private String horaCorta(java.time.LocalTime hora) {
        return hora == null ? "--:--"
                : hora.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
%>
<%
    List<Medico> medicos = request.getAttribute("medicos") instanceof List<?>
            ? (List<Medico>) request.getAttribute("medicos") : List.of();
    Map<Long, List<MedicoEspecialidad>> especialidadesPorMedico =
            request.getAttribute("especialidadesPorMedico") instanceof Map<?, ?>
                    ? (Map<Long, List<MedicoEspecialidad>>) request.getAttribute("especialidadesPorMedico")
                    : Map.of();
    Map<Integer, Especialidad> especialidadesPorId =
            request.getAttribute("especialidadesPorId") instanceof Map<?, ?>
                    ? (Map<Integer, Especialidad>) request.getAttribute("especialidadesPorId")
                    : Map.of();
    Map<Long, List<HorarioMedico>> horariosPorMedico =
            request.getAttribute("horariosPorMedico") instanceof Map<?, ?>
                    ? (Map<Long, List<HorarioMedico>>) request.getAttribute("horariosPorMedico")
                    : Map.of();
    String error = (String) request.getAttribute("error");
    String especialidadSeleccionada = request.getParameter("especialidad");
    SesionNavegacionPublica navegacion = SesionNavegacionPublica.desde(session);
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="Catálogo público de médicos de SoraCare-FPUNE.">
    <title>Médicos | SoraCare-FPUNE</title>
    <link href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css"
          rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260815">
    <style>
        body { min-height: 100vh; background: #f4f7fb; color: #14213d; }
        .barra-publica { background: #fff; border-bottom: 1px solid #e5ebf3; }
        .marca-soracare { color: #235ee7; font-weight: 800; }
        .marca-soracare:hover { color: #184ac2; }
        .enlace-publico { color: #334155; font-weight: 600; }
        .enlace-publico:hover, .enlace-publico.activo { color: #235ee7; }
        .encabezado-catalogo {
            position: relative; overflow: hidden; min-height: 22rem;
            border: 0; border-radius: 1.5rem; color: #fff;
            background: linear-gradient(125deg, #173da8 0%, #2563eb 55%, #218bbf 100%);
        }
        .encabezado-catalogo::before {
            content: ""; position: absolute; inset: 0 0 0 46%;
            background:
                linear-gradient(90deg, #2158dd 0%, rgba(33, 88, 221, .15) 65%),
                url('${pageContext.request.contextPath}/assets/home/img/hero/hero2.jpg') center 42% / cover no-repeat;
            opacity: .72;
        }
        .encabezado-catalogo::after {
            content: ""; position: absolute; inset: auto -3rem -6rem auto;
            width: 18rem; height: 18rem; border-radius: 50%;
            background: rgba(255, 255, 255, .08);
        }
        .contenido-hero { position: relative; z-index: 1; }
        .sello-hero {
            display: inline-flex; align-items: center; padding: .45rem .8rem;
            border: 1px solid rgba(255,255,255,.45); border-radius: 999px;
            background: rgba(255,255,255,.12); font-weight: 700; font-size: .88rem;
        }
        .buscador-panel {
            margin-top: -2.4rem; position: relative; z-index: 2;
            border: 1px solid #e0e7f0; border-radius: 1.25rem;
        }
        .campo-busqueda { position: relative; }
        .campo-busqueda svg {
            position: absolute; left: 1rem; top: 50%; transform: translateY(-50%);
            width: 1.15rem; color: #64748b; pointer-events: none;
        }
        .campo-busqueda input { padding-left: 2.9rem; }
        .form-control, .form-select { min-height: 3rem; border-radius: .8rem; }
        .tarjeta-medico {
            overflow: hidden; border: 1px solid #dfe7f1; border-radius: 1.25rem;
            transition: transform .2s ease, box-shadow .2s ease, border-color .2s ease;
        }
        .tarjeta-medico:hover {
            transform: translateY(-3px); border-color: #bfdbfe;
            box-shadow: 0 1rem 2.4rem rgba(30, 64, 175, .11) !important;
        }
        .foto-contenedor { position: relative; min-height: 18rem; background: #dbeafe; }
        .foto-medico { width: 100%; height: 100%; min-height: 18rem; object-fit: cover; }
        .foto-sin-perfil {
            min-height: 18rem; height: 100%; display: grid; place-items: center;
            align-content: center; gap: .7rem; color: #1d4ed8;
            background: linear-gradient(145deg, #dbeafe, #eff6ff);
        }
        .foto-sin-perfil strong { font-size: 2rem; }
        .foto-sin-perfil span { color: #64748b; font-size: .86rem; font-weight: 600;
        }
        .avatar-medico {
            display: grid; width: 3.25rem; height: 3.25rem; place-items: center;
            flex: 0 0 3.25rem; border-radius: .9rem; color: #1d4ed8;
            background: #eff6ff; font-weight: 800; font-size: 1.1rem;
        }
        .matricula-medico { color: #64748b; font-size: .9rem; }
        .etiqueta-principal { background: #dbeafe; color: #1d4ed8; }
        .etiqueta-secundaria { background: #ecfeff; color: #0e7490; }
        .horarios-medico {
            border: 1px solid #dbe7f5; border-radius: 1rem; background: #f8fbff;
        }
        .horario-encabezado svg { width: 1.15rem; height: 1.15rem; color: #2563eb; }
        .horario-item {
            display: inline-flex; flex-wrap: wrap; align-items: center; gap: .35rem;
            padding: .55rem .75rem; border: 1px solid #dbe7f5; border-radius: .75rem;
            background: #fff; color: #334155; font-size: .9rem;
        }
        .horario-item strong { color: #1e3a8a; }
        .horario-especialidad { color: #64748b; font-size: .82rem; }
        .boton-principal { background: #2864e8; border-color: #2864e8; }
        .boton-principal:hover { background: #1d4ed8; border-color: #1d4ed8; }
        .estado-activo { color: #047857; font-size: .88rem; font-weight: 700; }
        .estado-activo::before {
            content: ""; display: inline-block; width: .55rem; height: .55rem;
            margin-right: .45rem; border-radius: 50%; background: #10b981;
        }
        .sin-resultados {
            border: 1px dashed #cbd5e1; border-radius: 1.1rem; background: #fff;
        }
        .pie-publico { background: #10213e; color: #cbd5e1; }
        @media (max-width: 991.98px) {
            .encabezado-catalogo::before { inset: 0; opacity: .22; }
            .encabezado-catalogo { min-height: auto; }
            .buscador-panel { margin-top: -1.5rem; }
        }
        @media (max-width: 767.98px) {
            .foto-contenedor, .foto-medico { min-height: 15rem; height: 15rem; }
        }
    </style>
</head>
<body class="app-public">
<nav class="navbar navbar-expand-lg barra-publica sticky-top">
    <div class="container py-2">
        <a class="navbar-brand marca-soracare" href="${pageContext.request.contextPath}/">SoraCare-FPUNE</a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse"
                data-bs-target="#navegacionPublica" aria-controls="navegacionPublica"
                aria-expanded="false" aria-label="Abrir navegación">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navegacionPublica">
            <div class="navbar-nav ms-auto align-items-lg-center gap-lg-2">
                <a class="nav-link enlace-publico" href="${pageContext.request.contextPath}/">Inicio</a>
                <a class="nav-link enlace-publico" href="${pageContext.request.contextPath}/especialidades">Especialidades</a>
                <a class="nav-link enlace-publico activo" href="${pageContext.request.contextPath}/medicos"
                   aria-current="page">Médicos</a>
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
                    <span class="sello-hero mb-3">Equipo médico · Atención profesional</span>
                    <h1 class="display-4 fw-bold mb-3">Encontrá al profesional indicado para vos</h1>
                    <p class="lead mb-4">
                        Conocé su experiencia, especialidades y disponibilidad antes de reservar.
                    </p>
                    <span class="small opacity-75">Profesionales activos de SoraCare-FPUNE</span>
                </div>
            </div>
        </div>
    </section>

    <section class="card buscador-panel shadow-sm mb-5">
        <div class="card-body p-3 p-md-4">
            <div class="row g-3 align-items-end">
                <div class="col-lg-7">
                    <label for="buscadorMedicos" class="form-label fw-semibold">Buscar un médico</label>
                    <div class="campo-busqueda">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"
                             stroke-width="2" aria-hidden="true">
                            <circle cx="11" cy="11" r="7"></circle>
                            <path d="m20 20-3.5-3.5"></path>
                        </svg>
                        <input id="buscadorMedicos" class="form-control" type="search"
                               placeholder="Nombre, matrícula o especialidad">
                    </div>
                </div>
                <div class="col-lg-5">
                    <label for="filtroEspecialidad" class="form-label fw-semibold">Especialidad</label>
                    <select id="filtroEspecialidad" class="form-select">
                        <option value="">Todas las especialidades</option>
                        <% for (Especialidad especialidad : especialidadesPorId.values()) { %>
                        <option value="<%= h(especialidad.getNombre()) %>"
                                <%= especialidadSeleccionada != null
                                        && especialidadSeleccionada.equalsIgnoreCase(especialidad.getNombre())
                                        ? "selected" : "" %>>
                            <%= h(especialidad.getNombre()) %>
                        </option>
                        <% } %>
                    </select>
                </div>
            </div>
        </div>
    </section>

    <div class="mb-4">
        <p class="text-primary fw-semibold mb-1">Directorio profesional</p>
        <h2 class="h3 mb-1">Nuestros médicos</h2>
        <p class="text-secondary mb-0">Revisá cada perfil y consultá sus próximos turnos.</p>
    </div>

    <% if (error != null && !error.isBlank()) { %>
    <div class="alert alert-danger">
        <strong>No pudimos cargar el catálogo.</strong>
        <div class="mt-1"><%= h(error) %></div>
    </div>
    <% } %>

    <% if (medicos.isEmpty()) { %>
    <section class="card border-0 shadow-sm">
        <div class="card-body text-center p-5">
            <h2 class="h4">No existen médicos disponibles</h2>
            <p class="text-secondary mb-0">El catálogo todavía no posee profesionales activos.</p>
        </div>
    </section>
    <% } else { %>
    <section id="listaMedicos" class="row g-4">
        <% for (int indice = 0; indice < medicos.size(); indice++) {
            Medico medico = medicos.get(indice);
            List<MedicoEspecialidad> relaciones = especialidadesPorMedico.getOrDefault(
                    medico.getIdMedico(), List.of());
            MedicoEspecialidad relacionPrincipal = null;
            StringBuilder textoEspecialidades = new StringBuilder();
            for (MedicoEspecialidad relacion : relaciones) {
                Especialidad especialidad = especialidadesPorId.get(relacion.getIdEspecialidad());
                if (especialidad != null) {
                    textoEspecialidades.append(' ').append(especialidad.getNombre());
                }
                if (relacion.isPrincipal()) relacionPrincipal = relacion;
            }
            Especialidad especialidadPrincipal = relacionPrincipal == null
                    ? null : especialidadesPorId.get(relacionPrincipal.getIdEspecialidad());
            String textoBusqueda = medico.getNombres() + " " + medico.getApellidos()
                    + " " + medico.getMatriculaProfesional() + " "
                    + (medico.getBiografia() == null ? "" : medico.getBiografia())
                    + textoEspecialidades;
            String imagen = archivoImagenSeguro(medico.getFotoPerfil());
            boolean tieneFoto = !imagen.isBlank();
            List<HorarioMedico> horarios = horariosPorMedico.getOrDefault(
                    medico.getIdMedico(), List.of());
            String rutaAgendaMedico = request.getContextPath()
                    + navegacion.rutaAgenda();
            if (navegacion.paciente()) {
                rutaAgendaMedico += "&idMedico=" + medico.getIdMedico();
                if (especialidadPrincipal != null) {
                    rutaAgendaMedico += "&idEspecialidad="
                            + especialidadPrincipal.getIdEspecialidad();
                }
            }
        %>
        <div class="col-12 medico-item" data-busqueda="<%= h(textoBusqueda) %>">
            <article class="card tarjeta-medico shadow-sm">
                <div class="row g-0">
                    <div class="col-md-4 col-xl-3 foto-contenedor">
                        <% if (tieneFoto) { %>
                        <img class="foto-medico" loading="lazy"
                             src="${pageContext.request.contextPath}/assets/home/img/doctors/<%= imagen %>"
                             alt="Retrato profesional de <%= h(medico.getNombres() + " " + medico.getApellidos()) %>">
                        <% } else { %>
                        <div class="foto-sin-perfil" aria-label="Profesional sin fotografía cargada">
                            <strong><%= h(inicial(medico.getNombres())) %><%= h(inicial(medico.getApellidos())) %></strong>
                            <span>Fotografía pendiente</span>
                        </div>
                        <% } %>
                    </div>
                    <div class="col-md-8 col-xl-9">
                        <div class="card-body p-4 p-xl-5 h-100 d-flex flex-column">
                            <div class="d-flex flex-wrap justify-content-between gap-3 mb-4">
                                <div class="d-flex align-items-start gap-3">
                                    <div class="avatar-medico">
                                        <%= h(inicial(medico.getNombres())) %><%= h(inicial(medico.getApellidos())) %>
                                    </div>
                                    <div>
                                        <div class="estado-activo mb-1">Profesional activo</div>
                                        <h3 class="h3 mb-1">
                                            <%= medico.getTratamiento() == null
                                                    ? "Dr." : h(medico.getTratamiento().getAbreviatura()) %>
                                            <%= h(medico.getNombres()) %>
                                            <%= h(medico.getApellidos()) %>
                                        </h3>
                                        <div class="matricula-medico">
                                            Matrícula profesional:
                                            <strong><%= h(medico.getMatriculaProfesional()) %></strong>
                                        </div>
                                    </div>
                                </div>
                                <% if (especialidadPrincipal != null) { %>
                                <span class="badge etiqueta-principal align-self-start px-3 py-2">
                                    <%= h(especialidadPrincipal.getNombre()) %>
                                </span>
                                <% } %>
                            </div>

                            <div class="row g-4 mb-4">
                                <div class="col-lg-7">
                                    <h4 class="h6 text-uppercase text-secondary">Perfil profesional</h4>
                                    <p class="mb-0">
                                        <%= medico.getBiografia() == null || medico.getBiografia().isBlank()
                                                ? "La presentación profesional está pendiente de actualización."
                                                : h(medico.getBiografia()) %>
                                    </p>
                                </div>
                                <div class="col-lg-5">
                                    <h4 class="h6 text-uppercase text-secondary">Especialidades</h4>
                                    <div class="d-flex flex-wrap gap-2">
                                        <% for (MedicoEspecialidad relacion : relaciones) {
                                            Especialidad especialidad =
                                                    especialidadesPorId.get(relacion.getIdEspecialidad());
                                            if (especialidad == null) continue;
                                        %>
                                        <span class="badge <%= relacion.isPrincipal()
                                                ? "etiqueta-principal" : "etiqueta-secundaria" %>">
                                            <%= h(especialidad.getNombre()) %>
                                            <%= relacion.isPrincipal() ? " · Principal" : "" %>
                                        </span>
                                        <% } %>
                                        <% if (relaciones.isEmpty()) { %>
                                        <span class="text-secondary">Pendiente de confirmación.</span>
                                        <% } %>
                                    </div>
                                </div>
                            </div>

                            <section class="horarios-medico p-3 mb-4"
                                     aria-label="Horarios de atención de <%= h(medico.getNombres()) %>">
                                <div class="horario-encabezado d-flex align-items-center gap-2 mb-3">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                         stroke-width="2" aria-hidden="true">
                                        <circle cx="12" cy="12" r="9"></circle>
                                        <path d="M12 7v5l3 2"></path>
                                    </svg>
                                    <h4 class="h6 mb-0">Días y horarios de atención</h4>
                                </div>
                                <% if (horarios.isEmpty()) { %>
                                <p class="small text-secondary mb-0">
                                    El horario habitual está pendiente de confirmación.
                                    Podés consultar los próximos turnos disponibles.
                                </p>
                                <% } else { %>
                                <div class="d-flex flex-wrap gap-2">
                                    <% for (HorarioMedico horario : horarios) {
                                        Especialidad especialidadHorario =
                                                especialidadesPorId.get(horario.getIdEspecialidad());
                                    %>
                                    <span class="horario-item">
                                        <strong><%= h(nombreDia(horario.getDiaSemana())) %></strong>
                                        <span>·</span>
                                        <span><%= h(horaCorta(horario.getHoraInicio())) %>
                                            a <%= h(horaCorta(horario.getHoraFin())) %></span>
                                        <% if (especialidadHorario != null && relaciones.size() > 1) { %>
                                        <span class="horario-especialidad">
                                            · <%= h(especialidadHorario.getNombre()) %>
                                        </span>
                                        <% } %>
                                    </span>
                                    <% } %>
                                </div>
                                <% } %>
                            </section>

                            <div class="mt-auto d-flex flex-wrap gap-2">
                                <a class="btn btn-primary boton-principal"
                                   href="${pageContext.request.contextPath}/disponibilidad?idMedico=<%= medico.getIdMedico() %><%= especialidadPrincipal == null ? "" : "&idEspecialidad=" + especialidadPrincipal.getIdEspecialidad() %>">
                                    Consultar disponibilidad
                                </a>
                                <a class="btn btn-outline-primary" href="<%= h(rutaAgendaMedico) %>">
                                    <%= h(navegacion.textoAgenda()) %>
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </article>
        </div>
        <% } %>
    </section>
    <div id="sinResultados" class="sin-resultados text-center p-5 d-none">
        <h3 class="h5">No encontramos médicos con esos filtros</h3>
        <p class="text-secondary mb-0">Probá con otro nombre o seleccioná otra especialidad.</p>
    </div>
    <% } %>
</main>

<footer class="pie-publico mt-5">
    <div class="container py-4 d-flex flex-column flex-md-row justify-content-between gap-2">
        <span>SoraCare-FPUNE</span>
        <span>Sistema de gestión hospitalaria</span>
    </div>
</footer>
<script src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js"></script>
<script>
    const buscador = document.getElementById('buscadorMedicos');
    const filtro = document.getElementById('filtroEspecialidad');
    const items = [...document.querySelectorAll('.medico-item')];
    const sinResultados = document.getElementById('sinResultados');
    const normalizar = valor => (valor || '')
        .normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();

    const filtrarMedicos = () => {
        const texto = normalizar(buscador?.value);
        const especialidad = normalizar(filtro?.value);
        let visibles = 0;
        items.forEach(item => {
            const contenido = normalizar(item.dataset.busqueda);
            const coincide = (!texto || contenido.includes(texto))
                    && (!especialidad || contenido.includes(especialidad));
            item.classList.toggle('d-none', !coincide);
            if (coincide) visibles++;
        });
        sinResultados?.classList.toggle('d-none', visibles !== 0);
    };

    buscador?.addEventListener('input', filtrarMedicos);
    filtro?.addEventListener('change', filtrarMedicos);
    filtrarMedicos();
</script>
</body>
</html>
