<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.AtencionMedicaDTO" %>
<%@ page import="py.fpune.soracare.dto.CitaAgendaMedicoDTO" %>
<%@ page import="py.fpune.soracare.dto.DocumentoClinicoDTO" %>
<%@ page import="py.fpune.soracare.dto.DetalleRecetaDTO" %>
<%@ page import="py.fpune.soracare.dto.PanelClinicoPacienteDTO" %>
<%@ page import="py.fpune.soracare.dto.RecetaMedicaDTO" %>
<%@ page import="py.fpune.soracare.dto.TipoDocumentoClinicoDTO" %>
<%@ page import="py.fpune.soracare.mvc.model.Medicamento" %>
<%@ page import="py.fpune.soracare.mvc.model.Paciente" %>
<%@ page import="py.fpune.soracare.service.DisponibilidadService" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.time.Period" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%!
    private String h(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String valorFormulario(
            jakarta.servlet.http.HttpServletRequest request,
            String nombre,
            String respaldo
    ) {
        return "POST".equalsIgnoreCase(request.getMethod())
                ? request.getParameter(nombre)
                : respaldo;
    }

    private String textoOGuion(String valor) {
        return valor == null || valor.isBlank() ? "—" : h(valor);
    }

    private String signoVital(Object valor, String unidad) {
        return valor == null ? "—" : h(valor.toString()) + " " + h(unidad);
    }
%>
<%
    PanelClinicoPacienteDTO panel =
            (PanelClinicoPacienteDTO) request.getAttribute("panel");
    String error = (String) request.getAttribute("error");
    String mensajeExito = (String) request.getAttribute("mensajeExito");
    long idCita = request.getAttribute("idCita") instanceof Long
            ? (Long) request.getAttribute("idCita") : 0L;

    Paciente paciente = panel == null ? null : panel.paciente();
    CitaAgendaMedicoDTO cita = panel == null ? null : panel.cita();
    AtencionMedicaDTO atencion = panel == null ? null : panel.atencionActual();
    List<AtencionMedicaDTO> historial = panel == null
            ? List.of() : panel.historial();
    List<DocumentoClinicoDTO> documentosClinicos =
            request.getAttribute("documentosClinicos") instanceof List<?>
                    ? (List<DocumentoClinicoDTO>) request.getAttribute("documentosClinicos")
                    : List.of();
    List<TipoDocumentoClinicoDTO> tiposDocumentoClinico =
            request.getAttribute("tiposDocumentoClinico") instanceof List<?>
                    ? (List<TipoDocumentoClinicoDTO>) request.getAttribute("tiposDocumentoClinico")
                    : List.of();
    List<Medicamento> medicamentosActivos =
            request.getAttribute("medicamentosActivos") instanceof List<?>
                    ? (List<Medicamento>) request.getAttribute("medicamentosActivos")
                    : List.of();
    List<RecetaMedicaDTO> recetasAtencion =
            request.getAttribute("recetasAtencion") instanceof List<?>
                    ? (List<RecetaMedicaDTO>) request.getAttribute("recetasAtencion")
                    : List.of();
    LocalDate hoyPanel = LocalDate.now(DisponibilidadService.ZONA_HORARIA);
    LocalDate fechaAgenda = cita == null
            ? hoyPanel
            : cita.fechaHoraInicio().atZoneSameInstant(
                    DisponibilidadService.ZONA_HORARIA).toLocalDate();
    boolean citaFutura = fechaAgenda.isAfter(hoyPanel);
    boolean enCurso = atencion != null && "EN_CURSO".equals(atencion.estado());
    boolean finalizada = atencion != null && "FINALIZADA".equals(atencion.estado());
    boolean puedeIniciar = atencion == null
            && cita != null
            && "CONFIRMADA".equals(cita.codigoEstado())
            && !citaFutura;
    boolean esPost = "POST".equalsIgnoreCase(request.getMethod());

    String sintomas = valorFormulario(
            request, "sintomas", atencion == null ? null : atencion.sintomas());
    String antecedentes = valorFormulario(
            request, "antecedentesRelevantes",
            atencion == null ? null : atencion.antecedentesRelevantes());
    String antecedentesPatologicos = valorFormulario(
            request,
            "antecedentesPatologicosPersonales",
            paciente == null ? null : paciente.getAntecedentesPatologicosPersonales());
    String farmacosUtilizados = valorFormulario(
            request,
            "farmacosUtilizados",
            paciente == null ? null : paciente.getFarmacosUtilizados());
    String alergias = valorFormulario(
            request,
            "alergias",
            paciente == null ? null : paciente.getAlergias());
    String temperatura = valorFormulario(
            request, "temperaturaC",
            atencion == null || atencion.temperaturaC() == null
                    ? null : atencion.temperaturaC().toPlainString());
    String frecuenciaCardiaca = valorFormulario(
            request, "frecuenciaCardiaca",
            atencion == null || atencion.frecuenciaCardiaca() == null
                    ? null : atencion.frecuenciaCardiaca().toString());
    String frecuenciaRespiratoria = valorFormulario(
            request, "frecuenciaRespiratoria",
            atencion == null || atencion.frecuenciaRespiratoria() == null
                    ? null : atencion.frecuenciaRespiratoria().toString());
    String presionSistolica = valorFormulario(
            request, "presionSistolica",
            atencion == null || atencion.presionSistolica() == null
                    ? null : atencion.presionSistolica().toString());
    String presionDiastolica = valorFormulario(
            request, "presionDiastolica",
            atencion == null || atencion.presionDiastolica() == null
                    ? null : atencion.presionDiastolica().toString());
    String saturacionOxigeno = valorFormulario(
            request, "saturacionOxigeno",
            atencion == null || atencion.saturacionOxigeno() == null
                    ? null : atencion.saturacionOxigeno().toString());
    String evaluacionAreaAfectada = valorFormulario(
            request, "evaluacionAreaAfectada",
            atencion == null ? null : atencion.evaluacionAreaAfectada());
    String examen = valorFormulario(
            request, "examenFisico", atencion == null ? null : atencion.examenFisico());
    String evaluacion = valorFormulario(
            request, "evaluacionClinica",
            atencion == null ? null : atencion.evaluacionClinica());
    String planTratamiento = valorFormulario(
            request, "planTratamiento",
            atencion == null ? null : atencion.planTratamiento());
    String estudiosClinicosSolicitados = valorFormulario(
            request, "estudiosClinicosSolicitados",
            atencion == null ? null : atencion.estudiosClinicosSolicitados());
    String indicaciones = valorFormulario(
            request, "indicaciones", atencion == null ? null : atencion.indicaciones());
    String observaciones = valorFormulario(
            request, "observaciones", atencion == null ? null : atencion.observaciones());
    boolean seguimiento = esPost
            ? request.getParameter("requiereSeguimiento") != null
            : atencion != null && atencion.requiereSeguimiento();
    String fechaControl = valorFormulario(
            request,
            "fechaProximoControl",
            atencion == null || atencion.fechaProximoControl() == null
                    ? null : atencion.fechaProximoControl().toString());

    DateTimeFormatter fechaHora = DateTimeFormatter.ofPattern(
            "dd/MM/yyyy HH:mm", new Locale("es", "PY"));
    DateTimeFormatter fechaLarga = DateTimeFormatter.ofPattern(
            "EEEE d 'de' MMMM 'de' yyyy", new Locale("es", "PY"));
    Integer edad = paciente == null || paciente.getFechaNacimiento() == null
            ? null
            : Period.between(
                    paciente.getFechaNacimiento(),
                    LocalDate.now(DisponibilidadService.ZONA_HORARIA)
            ).getYears();
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex, nofollow">
    <title>Panel clínico | SoraCare-FPUNE</title>
    <link href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css"
          rel="stylesheet">
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260815">
    <style>
        :root {
            --clinico-azul: #2457d6;
            --clinico-celeste: #2f9fc9;
            --clinico-verde: #16a394;
            --clinico-coral: #f28b75;
            --clinico-tinta: #17233c;
            --clinico-suave: #f5f8fc;
        }
        body {
            color: var(--clinico-tinta);
            background:
                    radial-gradient(circle at 4% 14%, rgba(47, 159, 201, .10), transparent 25rem),
                    radial-gradient(circle at 96% 42%, rgba(242, 139, 117, .09), transparent 24rem),
                    linear-gradient(180deg, #f9fbfe 0%, #f3f7fb 42%, #f8fafc 100%) !important;
        }
        .barra-superior { background: rgba(255, 255, 255, .94) !important; backdrop-filter: blur(14px); }
        .marca { color: #235ee7; font-weight: 800; }
        .cabecera-clinica {
            position: relative;
            overflow: hidden;
            min-height: 265px;
            border: 0 !important;
            border-radius: 1.55rem !important;
            color: #ffffff;
            background: linear-gradient(120deg, #183b95 0%, #2668d9 52%, #22a6af 112%) !important;
            box-shadow: 0 24px 55px rgba(30, 64, 175, .20) !important;
        }
        .cabecera-clinica::before,
        .cabecera-clinica::after {
            position: absolute;
            content: "";
            border-radius: 50%;
            background: rgba(255, 255, 255, .09);
        }
        .cabecera-clinica::before { width: 260px; height: 260px; top: -155px; right: 9%; }
        .cabecera-clinica::after { width: 190px; height: 190px; right: -55px; bottom: -105px; }
        .hero-contenido { position: relative; z-index: 1; }
        .hero-kicker {
            display: inline-flex; align-items: center; gap: .5rem;
            margin-bottom: 1rem; padding: .45rem .8rem; border-radius: 999px;
            color: #e7f8ff; background: rgba(255, 255, 255, .13);
            font-size: .82rem; font-weight: 600; letter-spacing: .02em;
        }
        .hero-clinical-mark {
            display: inline-grid; width: 112px; height: 112px; place-items: center;
            border: 1px solid rgba(255, 255, 255, .3); border-radius: 2rem;
            color: #fff; background: rgba(255, 255, 255, .15);
            box-shadow: inset 0 1px rgba(255, 255, 255, .3), 0 18px 35px rgba(15, 23, 42, .16);
            font-size: 3.2rem; transform: rotate(3deg);
        }
        .hero-identificacion { color: rgba(255, 255, 255, .86); font-size: .98rem; }
        .hero-sello {
            display: inline-flex; align-items: center; gap: .45rem;
            padding: .55rem .85rem; border: 1px solid rgba(255, 255, 255, .4);
            border-radius: 999px; color: #143a91; background: rgba(255, 255, 255, .93);
            font-weight: 700; font-size: .86rem;
        }
        .tarjeta {
            overflow: hidden; border: 1px solid rgba(214, 224, 237, .9) !important;
            border-radius: 1.3rem !important; background: rgba(255, 255, 255, .96);
            box-shadow: 0 15px 38px rgba(30, 55, 90, .075) !important;
        }
        .tarjeta::before {
            display: block; width: 100%; height: 4px; content: "";
            background: linear-gradient(90deg, var(--clinico-azul), var(--clinico-celeste), var(--clinico-verde));
        }
        .tarjeta-dato {
            overflow: hidden; position: relative; border: 1px solid #e2eaf4 !important;
            border-radius: 1.15rem !important; background: rgba(255, 255, 255, .97);
            transition: transform .2s ease, box-shadow .2s ease;
        }
        .tarjeta-dato:hover { transform: translateY(-3px); box-shadow: 0 18px 35px rgba(37, 99, 235, .12) !important; }
        .stat-icono {
            display: grid; width: 2.65rem; height: 2.65rem; place-items: center;
            margin-bottom: .85rem; border-radius: .85rem; font-size: 1.25rem;
            color: var(--clinico-azul); background: #edf4ff;
        }
        .stat-sangre .stat-icono { color: #d85d62; background: #fff0f0; }
        .stat-historial .stat-icono { color: #7c4cc5; background: #f3edff; }
        .stat-ultima .stat-icono { color: #087e73; background: #e8faf7; }
        .etiqueta { color: #6a778d; font-size: .77rem; font-weight: 500; letter-spacing: .015em; }
        .valor-dato { margin-top: .15rem; color: #192944; font-weight: 700; }
        .tarjeta-cita { background: linear-gradient(115deg, #fff 0%, #f8fbff 65%, #eefbfa 100%); }
        .cita-icono {
            display: grid; width: 3.35rem; height: 3.35rem; place-items: center;
            border-radius: 1rem; color: #fff;
            background: linear-gradient(135deg, var(--clinico-azul), var(--clinico-celeste));
            box-shadow: 0 10px 25px rgba(37, 99, 235, .22); font-size: 1.45rem;
        }
        .section-heading { display: flex; align-items: flex-start; gap: .85rem; }
        .section-icono {
            display: grid; width: 2.9rem; height: 2.9rem; place-items: center;
            flex: 0 0 2.9rem; border-radius: .9rem; color: var(--clinico-azul);
            background: #edf4ff; font-size: 1.25rem;
        }
        .section-icono.verde { color: #087e73; background: #e7faf6; }
        .section-icono.coral { color: #bd5b49; background: #fff1ed; }
        .section-icono.violeta { color: #7244b9; background: #f2ecff; }
        .perfil-dato {
            height: 100%; padding: 1rem 1.05rem; border: 1px solid #e3eaf3;
            border-radius: 1rem; background: #fbfdff;
        }
        .perfil-dato.alergias { border-color: #ffd8cf; background: #fff8f6; }
        .estado-cita { border-radius: 999px; padding: .45rem .75rem; }
        .estado-confirmada { background: #ccfbf1; color: #0f766e; }
        .estado-atencion { background: #ede9fe; color: #6d28d9; }
        .estado-atendida { background: #dbeafe; color: #1d4ed8; }
        .estado-pendiente { background: #fef3c7; color: #92400e; }
        .seccion-formulario {
            display: flex; align-items: center; gap: .65rem;
            margin: 1.5rem 0 1rem; padding: .8rem 1rem;
            border: 0; border-radius: .9rem; color: #23426d;
            background: linear-gradient(90deg, #edf5ff, #f7fbff);
        }
        .seccion-formulario::before {
            width: .65rem; height: .65rem; content: ""; border-radius: 50%;
            background: var(--clinico-verde); box-shadow: 0 0 0 .28rem rgba(22, 163, 148, .12);
        }
        .form-control, .form-select { background-color: #fcfdff; }
        textarea.form-control { min-height: 110px; }
        .form-control:hover, .form-select:hover { border-color: #aebed3; }
        .atencion-card { background: linear-gradient(180deg, #fff 0%, #fbfdff 100%); }
        .callout-atencion {
            border: 1px solid #d9e8f6; border-radius: 1.3rem;
            background: linear-gradient(135deg, #f7fbff 0%, #effbf9 100%);
        }
        .callout-icono {
            display: grid; width: 4.5rem; height: 4.5rem; place-items: center;
            margin: 0 auto 1rem; border-radius: 1.35rem; color: #fff;
            background: linear-gradient(135deg, var(--clinico-verde), var(--clinico-celeste));
            box-shadow: 0 14px 30px rgba(15, 159, 150, .22); font-size: 2rem;
        }
        .historial-item {
            overflow: hidden; border: 1px solid #dfe7f1;
            border-radius: 1rem; background: #ffffff;
            transition: border-color .2s ease, box-shadow .2s ease;
        }
        .historial-item:hover { border-color: #bcd2f4; box-shadow: 0 10px 25px rgba(37, 99, 235, .08); }
        .historial-item[open] { border-color: #b6cef4; background: #fbfdff; }
        .historial-item summary { cursor: pointer; list-style: none; }
        .historial-item summary::-webkit-details-marker { display: none; }
        .dato-clinico {
            padding: .85rem 1rem; border: 1px solid #e2eaf4;
            border-left: 4px solid #9fc5fb; border-radius: .75rem;
            background: #fbfdff; white-space: pre-line;
        }
        .documento-icono {
            display: grid; width: 3rem; height: 3rem; place-items: center;
            flex: 0 0 3rem; border-radius: .85rem; color: #2563eb;
            background: #eff6ff; font-size: 1.35rem;
        }
        .documento-miniatura {
            width: 4rem; height: 4rem; flex: 0 0 4rem; object-fit: cover;
            border: 1px solid #dde6f0; border-radius: .85rem; background: #f8fafc;
        }
        .detalle-receta { border: 1px solid #dbe5f1; border-radius: 1rem; background: #f8fbff; }
        .receta-emitida { border-left: 4px solid #2563eb; }
        .receta-anulada { border-left: 4px solid #94a3b8; opacity: .75; }
        .texto-receta { white-space: pre-line; }
        .formulario-documento { border-color: #dbe7f3 !important; background: #f8fbff; }
        .alert { box-shadow: 0 8px 22px rgba(30, 64, 175, .06); }
        @media (max-width: 767.98px) {
            .cabecera-clinica { min-height: auto; }
            .hero-clinical-mark { width: 82px; height: 82px; border-radius: 1.4rem; font-size: 2.35rem; }
            .hero-contenido h1 { font-size: 2rem; }
        }
    </style>
</head>
<body class="app-shell app-doctor">
<nav class="navbar barra-superior border-bottom">
    <div class="container py-2">
        <a class="navbar-brand marca" href="${pageContext.request.contextPath}/medico/inicio">
            SoraCare-FPUNE
        </a>
        <div class="d-flex align-items-center gap-2">
            <a class="btn btn-outline-primary btn-sm"
               href="${pageContext.request.contextPath}/medico/agenda?fecha=<%= fechaAgenda %>">
                Volver a mi agenda
            </a>
            <form method="post" action="${pageContext.request.contextPath}/logout" class="m-0">
                <button type="submit" class="btn btn-outline-secondary btn-sm">Cerrar sesión</button>
            </form>
        </div>
    </div>
</nav>

<main class="container py-4 py-md-5">
    <% if (mensajeExito != null && !mensajeExito.isBlank()) { %>
    <div class="alert alert-success"><%= h(mensajeExito) %></div>
    <% } %>
    <% if (error != null && !error.isBlank()) { %>
    <div class="alert alert-danger">
        <strong>No se pudo completar la operación.</strong>
        <div class="mt-1"><%= h(error) %></div>
    </div>
    <% } %>

    <% if (panel != null) { %>
    <section class="card cabecera-clinica shadow-sm mb-4">
        <div class="card-body p-4 p-md-5 hero-contenido">
            <div class="row align-items-center g-4">
                <div class="col-lg-9">
                    <div class="hero-kicker"><i class="bi bi-shield-check"></i> Historia clínica digital protegida</div>
                    <h1 class="display-6 fw-bold mb-3">
                        <%= h(paciente.getNombres() + " " + paciente.getApellidos()) %>
                    </h1>
                    <p class="hero-identificacion mb-3">
                        Historia clínica <%= h(paciente.getNumeroHistoriaClinica()) %>
                        · CI <%= h(paciente.getCedula()) %>
                        <% if (edad != null) { %>· <%= edad %> años<% } %>
                    </p>
                    <span class="hero-sello"><i class="bi bi-heart-pulse"></i> Vista clínica integral</span>
                </div>
                <div class="col-lg-3 text-lg-end">
                    <div class="hero-clinical-mark ms-lg-auto"><i class="bi bi-person-heart"></i></div>
                </div>
            </div>
        </div>
    </section>

    <section class="row g-3 mb-4">
        <div class="col-6 col-lg-3">
            <div class="card tarjeta-dato shadow-sm h-100 stat-edad"><div class="card-body p-4">
                <div class="stat-icono"><i class="bi bi-person-vcard"></i></div>
                <div class="etiqueta">Edad del paciente</div>
                <div class="valor-dato"><%= edad == null
                        ? "No informada" : edad + " años" %></div>
                <% if (paciente.getFechaNacimiento() != null) { %>
                <small class="text-secondary">Nacimiento: <%= paciente.getFechaNacimiento() %></small>
                <% } %>
            </div></div>
        </div>
        <div class="col-6 col-lg-3">
            <div class="card tarjeta-dato shadow-sm h-100 stat-sangre"><div class="card-body p-4">
                <div class="stat-icono"><i class="bi bi-droplet-half"></i></div>
                <div class="etiqueta">Grupo sanguíneo</div>
                <div class="valor-dato"><%= textoOGuion(paciente.getGrupoSanguineo()) %></div>
            </div></div>
        </div>
        <div class="col-6 col-lg-3">
            <div class="card tarjeta-dato shadow-sm h-100 stat-historial"><div class="card-body p-4">
                <div class="stat-icono"><i class="bi bi-journal-medical"></i></div>
                <div class="etiqueta">Atenciones anteriores</div>
                <div class="valor-dato"><%= historial.size() %></div>
                <small class="text-secondary">Registros clínicos finalizados</small>
            </div></div>
        </div>
        <div class="col-6 col-lg-3">
            <div class="card tarjeta-dato shadow-sm h-100 stat-ultima"><div class="card-body p-4">
                <div class="stat-icono"><i class="bi bi-clock-history"></i></div>
                <div class="etiqueta">Última atención</div>
                <% if (historial.isEmpty()) { %>
                <div class="valor-dato">Sin registros</div>
                <small class="text-secondary">Primera atención clínica</small>
                <% } else { %>
                <div class="valor-dato"><%= historial.getFirst().fechaHoraInicio()
                        .atZoneSameInstant(DisponibilidadService.ZONA_HORARIA)
                        .toLocalDate() %></div>
                <small class="text-secondary"><%= h(historial.getFirst().especialidad()) %></small>
                <% } %>
            </div></div>
        </div>
    </section>

    <section class="card tarjeta tarjeta-cita shadow-sm mb-4">
        <div class="card-body p-4">
            <div class="row g-3 align-items-center">
                <div class="col-12 d-flex align-items-center gap-3 mb-1">
                    <div class="cita-icono"><i class="bi bi-calendar2-heart"></i></div>
                    <div>
                        <div class="fw-bold">Resumen de la cita seleccionada</div>
                        <div class="small text-secondary">Información proporcionada antes de iniciar la atención.</div>
                    </div>
                </div>
                <div class="col-lg-2">
                    <div class="etiqueta">Cita seleccionada</div>
                    <div class="valor-dato text-capitalize">
                        <%= h(fechaAgenda.format(fechaLarga)) %>
                    </div>
                    <div class="text-secondary">
                        <%= cita.fechaHoraInicio().atZoneSameInstant(
                                DisponibilidadService.ZONA_HORARIA)
                                .format(DateTimeFormatter.ofPattern("HH:mm")) %>
                    </div>
                </div>
                <div class="col-lg-2">
                    <div class="etiqueta">Especialidad y consultorio</div>
                    <div class="valor-dato"><%= h(cita.especialidad()) %></div>
                    <div class="text-secondary"><%= h(cita.consultorio()) %></div>
                </div>
                <div class="col-lg-3">
                    <div class="etiqueta">Motivo informado</div>
                    <div><%= h(cita.motivoConsulta()) %></div>
                </div>
                <div class="col-lg-3">
                    <div class="etiqueta">Observaciones previas</div>
                    <div><%= textoOGuion(cita.observaciones()) %></div>
                </div>
                <div class="col-lg-2 text-lg-end">
                    <span class="badge estado-cita <%= "CONFIRMADA".equals(cita.codigoEstado())
                            ? "estado-confirmada"
                            : "EN_ATENCION".equals(cita.codigoEstado())
                                    ? "estado-atencion"
                                    : "ATENDIDA".equals(cita.codigoEstado())
                                            ? "estado-atendida" : "estado-pendiente" %>">
                        <%= h(cita.nombreEstado()) %>
                    </span>
                </div>
            </div>
        </div>
    </section>

    <% if (!enCurso) { %>
    <section class="card tarjeta shadow-sm mb-4">
        <div class="card-body p-4">
            <div class="d-flex flex-wrap justify-content-between gap-2 mb-4">
                <div class="section-heading">
                    <div class="section-icono violeta"><i class="bi bi-clipboard2-pulse"></i></div>
                    <div>
                        <h2 class="h4 mb-1">Información clínica permanente</h2>
                        <p class="text-secondary mb-0">
                            Datos vigentes del paciente que acompañan todas sus consultas.
                        </p>
                    </div>
                </div>
                <span class="badge text-bg-light align-self-start">Perfil del paciente</span>
            </div>
            <div class="row g-4">
                <div class="col-lg-4">
                    <div class="perfil-dato">
                        <div class="etiqueta"><i class="bi bi-activity me-1"></i> Antecedentes patológicos personales</div>
                        <div class="mt-2"><%= textoOGuion(paciente.getAntecedentesPatologicosPersonales()) %></div>
                    </div>
                </div>
                <div class="col-lg-4">
                    <div class="perfil-dato">
                        <div class="etiqueta"><i class="bi bi-capsule me-1"></i> Fármacos utilizados</div>
                        <div class="mt-2"><%= textoOGuion(paciente.getFarmacosUtilizados()) %></div>
                    </div>
                </div>
                <div class="col-lg-4">
                    <div class="perfil-dato alergias">
                        <div class="etiqueta"><i class="bi bi-exclamation-triangle me-1"></i> Alergias conocidas</div>
                        <div class="mt-2"><%= textoOGuion(paciente.getAlergias()) %></div>
                    </div>
                </div>
            </div>
        </div>
    </section>
    <% } %>

    <% if (atencion == null) { %>
    <section class="card tarjeta callout-atencion shadow-sm mb-4">
        <div class="card-body p-4 p-md-5 text-center">
            <% if (puedeIniciar) { %>
            <div class="callout-icono"><i class="bi bi-play-circle"></i></div>
            <h2 class="h4">El paciente está listo para la atención</h2>
            <p class="text-secondary">
                Al iniciar, la cita cambiará a “En atención” y se habilitará
                la ficha para registrar la evolución clínica.
            </p>
            <form method="post" action="${pageContext.request.contextPath}/medico/atencion">
                <input type="hidden" name="idCita" value="<%= idCita %>">
                <input type="hidden" name="accion" value="iniciar">
                <button class="btn btn-primary btn-lg">Iniciar atención</button>
            </form>
            <% } else { %>
            <div class="callout-icono"><i class="bi bi-hourglass-split"></i></div>
            <h2 class="h4">La atención todavía no puede iniciarse</h2>
            <p class="text-secondary mb-0">
                <% if (citaFutura && "CONFIRMADA".equals(cita.codigoEstado())) { %>
                El registro clínico se habilitará el día de la cita:
                <strong><%= fechaAgenda %></strong>.
                <% } else { %>
                La cita debe estar confirmada antes de habilitar el registro clínico.
                Estado actual: <strong><%= h(cita.nombreEstado()) %></strong>.
                <% } %>
            </p>
            <% } %>
        </div>
    </section>
    <% } else if (enCurso) { %>
    <section class="card tarjeta atencion-card shadow-sm mb-4">
        <div class="card-body p-4 p-lg-5">
            <div class="d-flex flex-wrap justify-content-between gap-2 mb-4">
                <div class="section-heading">
                    <div class="section-icono verde"><i class="bi bi-heart-pulse"></i></div>
                    <div>
                        <h2 class="h4 mb-1">Atención en curso</h2>
                        <p class="text-secondary mb-0">
                            Guardá avances cuando lo necesites. Finalizar cerrará la cita.
                        </p>
                    </div>
                </div>
                <span class="badge estado-cita estado-atencion align-self-start">En atención</span>
            </div>

            <form method="post" action="${pageContext.request.contextPath}/medico/atencion">
                <input type="hidden" name="idCita" value="<%= idCita %>">

                <h3 class="h6 seccion-formulario">Información clínica permanente</h3>
                <p class="text-secondary small">
                    Actualizá estos datos con la información confirmada durante la consulta.
                    Quedarán visibles en las próximas atenciones del paciente.
                </p>
                <div class="row g-3 mb-4">
                    <div class="col-lg-4">
                        <label for="antecedentesPatologicosPersonales" class="form-label fw-semibold">
                            Antecedentes patológicos personales
                        </label>
                        <textarea id="antecedentesPatologicosPersonales"
                                  name="antecedentesPatologicosPersonales" class="form-control"
                                  maxlength="6000" placeholder="Ej.: hipertensión, diabetes, cirugías previas."><%= h(antecedentesPatologicos) %></textarea>
                    </div>
                    <div class="col-lg-4">
                        <label for="farmacosUtilizados" class="form-label fw-semibold">
                            Fármacos utilizados
                        </label>
                        <textarea id="farmacosUtilizados" name="farmacosUtilizados"
                                  class="form-control" maxlength="6000"
                                  placeholder="Ej.: losartán 50 mg una vez al día."><%= h(farmacosUtilizados) %></textarea>
                    </div>
                    <div class="col-lg-4">
                        <label for="alergias" class="form-label fw-semibold">
                            Alergias conocidas
                        </label>
                        <textarea id="alergias" name="alergias" class="form-control"
                                  maxlength="6000" placeholder="Ej.: penicilina. Si no posee, indicar Ninguna conocida."><%= h(alergias) %></textarea>
                    </div>
                </div>

                <h3 class="h6 seccion-formulario">Evaluación del paciente</h3>
                <div class="row g-3 mb-4">
                    <div class="col-6 col-lg-2">
                        <label for="temperaturaC" class="form-label fw-semibold">
                            Temperatura
                        </label>
                        <div class="input-group">
                            <input type="number" id="temperaturaC" name="temperaturaC"
                                   class="form-control" min="30" max="45" step="0.1"
                                   inputmode="decimal" value="<%= h(temperatura) %>">
                            <span class="input-group-text">°C</span>
                        </div>
                    </div>
                    <div class="col-6 col-lg-2">
                        <label for="frecuenciaCardiaca" class="form-label fw-semibold">
                            Frecuencia cardíaca
                        </label>
                        <div class="input-group">
                            <input type="number" id="frecuenciaCardiaca" name="frecuenciaCardiaca"
                                   class="form-control" min="20" max="250" step="1"
                                   inputmode="numeric" value="<%= h(frecuenciaCardiaca) %>">
                            <span class="input-group-text">lpm</span>
                        </div>
                    </div>
                    <div class="col-6 col-lg-2">
                        <label for="frecuenciaRespiratoria" class="form-label fw-semibold">
                            Frecuencia respiratoria
                        </label>
                        <div class="input-group">
                            <input type="number" id="frecuenciaRespiratoria"
                                   name="frecuenciaRespiratoria" class="form-control"
                                   min="5" max="80" step="1" inputmode="numeric"
                                   value="<%= h(frecuenciaRespiratoria) %>">
                            <span class="input-group-text">rpm</span>
                        </div>
                    </div>
                    <div class="col-6 col-lg-2">
                        <label for="presionSistolica" class="form-label fw-semibold">
                            Presión sistólica
                        </label>
                        <div class="input-group">
                            <input type="number" id="presionSistolica" name="presionSistolica"
                                   class="form-control" min="50" max="300" step="1"
                                   inputmode="numeric" value="<%= h(presionSistolica) %>">
                            <span class="input-group-text">mmHg</span>
                        </div>
                    </div>
                    <div class="col-6 col-lg-2">
                        <label for="presionDiastolica" class="form-label fw-semibold">
                            Presión diastólica
                        </label>
                        <div class="input-group">
                            <input type="number" id="presionDiastolica" name="presionDiastolica"
                                   class="form-control" min="30" max="200" step="1"
                                   inputmode="numeric" value="<%= h(presionDiastolica) %>">
                            <span class="input-group-text">mmHg</span>
                        </div>
                    </div>
                    <div class="col-6 col-lg-2">
                        <label for="saturacionOxigeno" class="form-label fw-semibold">
                            Saturación O₂
                        </label>
                        <div class="input-group">
                            <input type="number" id="saturacionOxigeno" name="saturacionOxigeno"
                                   class="form-control" min="0" max="100" step="1"
                                   inputmode="numeric" value="<%= h(saturacionOxigeno) %>">
                            <span class="input-group-text">%</span>
                        </div>
                    </div>
                    <div class="col-12">
                        <div class="form-text">Todos los signos vitales son obligatorios para finalizar la atención.</div>
                    </div>
                </div>
                <div class="row g-3 mb-4">
                    <div class="col-md-6">
                        <label for="sintomas" class="form-label fw-semibold">Síntomas y evolución</label>
                        <textarea id="sintomas" name="sintomas" class="form-control"
                                  maxlength="6000"><%= h(sintomas) %></textarea>
                    </div>
                    <div class="col-md-6">
                        <label for="antecedentesRelevantes" class="form-label fw-semibold">
                            Antecedentes relevantes para esta consulta
                        </label>
                        <textarea id="antecedentesRelevantes" name="antecedentesRelevantes"
                                  class="form-control" maxlength="6000"><%= h(antecedentes) %></textarea>
                    </div>
                    <div class="col-md-6">
                        <label for="evaluacionAreaAfectada" class="form-label fw-semibold">
                            Evaluación del área afectada
                        </label>
                        <textarea id="evaluacionAreaAfectada" name="evaluacionAreaAfectada"
                                  class="form-control" maxlength="6000"
                                  placeholder="Describí ubicación, aspecto, dolor, movilidad y demás hallazgos locales."><%= h(evaluacionAreaAfectada) %></textarea>
                        <div class="form-text">Obligatorio para finalizar.</div>
                    </div>
                    <div class="col-md-6">
                        <label for="examenFisico" class="form-label fw-semibold">Examen físico general</label>
                        <textarea id="examenFisico" name="examenFisico" class="form-control"
                                  maxlength="6000"><%= h(examen) %></textarea>
                        <div class="form-text">Obligatorio para finalizar.</div>
                    </div>
                </div>

                <h3 class="h6 seccion-formulario">Conclusión y conducta médica</h3>
                <div class="row g-3 mb-4">
                    <div class="col-md-6">
                        <label for="evaluacionClinica" class="form-label fw-semibold">
                            Diagnóstico de la consulta
                        </label>
                        <textarea id="evaluacionClinica" name="evaluacionClinica"
                                  class="form-control" maxlength="6000"><%= h(evaluacion) %></textarea>
                        <div class="form-text">Obligatorio para finalizar.</div>
                    </div>
                    <div class="col-md-6">
                        <label for="planTratamiento" class="form-label fw-semibold">
                            Plan de tratamiento
                        </label>
                        <textarea id="planTratamiento" name="planTratamiento"
                                  class="form-control" maxlength="6000"><%= h(planTratamiento) %></textarea>
                        <div class="form-text">Obligatorio para finalizar.</div>
                    </div>
                    <div class="col-md-6">
                        <label for="estudiosClinicosSolicitados" class="form-label fw-semibold">
                            Estudios clínicos solicitados
                        </label>
                        <textarea id="estudiosClinicosSolicitados"
                                  name="estudiosClinicosSolicitados"
                                  class="form-control" maxlength="6000"
                                  placeholder="Ej.: hemograma completo, radiografía de tórax o ecografía abdominal."><%= h(estudiosClinicosSolicitados) %></textarea>
                        <div class="form-text">Opcional. Dejalo vacío si no se requieren estudios.</div>
                    </div>
                    <div class="col-md-6">
                        <label for="indicaciones" class="form-label fw-semibold">Indicaciones</label>
                        <textarea id="indicaciones" name="indicaciones" class="form-control"
                                  maxlength="6000"><%= h(indicaciones) %></textarea>
                        <div class="form-text">Obligatorio para finalizar.</div>
                    </div>
                    <div class="col-md-6">
                        <label for="observaciones" class="form-label fw-semibold">Observaciones</label>
                        <textarea id="observaciones" name="observaciones" class="form-control"
                                  maxlength="6000"><%= h(observaciones) %></textarea>
                    </div>
                </div>

                <div class="row g-3 align-items-end mb-4">
                    <div class="col-md-5">
                        <div class="form-check mt-4">
                            <input class="form-check-input" type="checkbox"
                                   id="requiereSeguimiento" name="requiereSeguimiento"
                                   value="true" <%= seguimiento ? "checked" : "" %>>
                            <label class="form-check-label fw-semibold" for="requiereSeguimiento">
                                Requiere próximo control
                            </label>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <label for="fechaProximoControl" class="form-label fw-semibold">
                            Fecha del próximo control
                        </label>
                        <input type="date" id="fechaProximoControl" name="fechaProximoControl"
                               class="form-control" value="<%= h(fechaControl) %>"
                               min="<%= LocalDate.now(DisponibilidadService.ZONA_HORARIA) %>"
                               <%= seguimiento ? "required" : "disabled" %>>
                    </div>
                </div>

                <div class="d-flex flex-wrap justify-content-end gap-2 border-top pt-4">
                    <button type="submit" name="accion" value="guardar"
                            class="btn btn-outline-primary">
                        Guardar evolución
                    </button>
                    <button type="submit" name="accion" value="finalizar"
                            class="btn btn-primary">
                        Finalizar atención
                    </button>
                </div>
            </form>
        </div>
    </section>
    <% } else if (finalizada) { %>
    <section class="card tarjeta shadow-sm mb-4">
        <div class="card-body p-4 p-lg-5">
            <div class="d-flex flex-wrap justify-content-between gap-2 mb-4">
                <div class="section-heading">
                    <div class="section-icono verde"><i class="bi bi-check2-circle"></i></div>
                    <div>
                        <h2 class="h4 mb-1">Atención finalizada</h2>
                        <p class="text-secondary mb-0">
                            Registro cerrado el
                            <%= atencion.fechaHoraFin() == null ? "—"
                                    : atencion.fechaHoraFin().atZoneSameInstant(
                                            DisponibilidadService.ZONA_HORARIA).format(fechaHora) %>.
                        </p>
                    </div>
                </div>
                <span class="badge estado-cita estado-atendida align-self-start">Atendida</span>
            </div>
            <div class="row g-4">
                <div class="col-md-6 dato-clinico"><div class="etiqueta">Síntomas</div><div><%= textoOGuion(atencion.sintomas()) %></div></div>
                <div class="col-md-6 dato-clinico"><div class="etiqueta">Antecedentes relevantes</div><div><%= textoOGuion(atencion.antecedentesRelevantes()) %></div></div>
                <div class="col-md-6 dato-clinico"><div class="etiqueta">Evaluación del área afectada</div><div><%= textoOGuion(atencion.evaluacionAreaAfectada()) %></div></div>
                <div class="col-md-6 dato-clinico"><div class="etiqueta">Examen físico general</div><div><%= textoOGuion(atencion.examenFisico()) %></div></div>
                <div class="col-md-6 dato-clinico"><div class="etiqueta">Diagnóstico de la consulta</div><div><%= textoOGuion(atencion.evaluacionClinica()) %></div></div>
                <div class="col-md-6 dato-clinico"><div class="etiqueta">Plan de tratamiento</div><div><%= textoOGuion(atencion.planTratamiento()) %></div></div>
                <div class="col-md-6 dato-clinico"><div class="etiqueta">Estudios clínicos solicitados</div><div><%= textoOGuion(atencion.estudiosClinicosSolicitados()) %></div></div>
                <div class="col-md-6 dato-clinico"><div class="etiqueta">Indicaciones</div><div><%= textoOGuion(atencion.indicaciones()) %></div></div>
                <% if (atencion.requiereSeguimiento()) { %>
                <div class="col-12 dato-clinico"><div class="etiqueta">Próximo control</div><div><%= atencion.fechaProximoControl() %></div></div>
                <% } %>
            </div>
            <h3 class="h6 seccion-formulario mt-4">Signos vitales registrados</h3>
            <div class="row g-4 mb-4">
                <div class="col-6 col-md-4 col-lg-2 dato-clinico"><div class="etiqueta">Temperatura</div><div><%= signoVital(atencion.temperaturaC(), "°C") %></div></div>
                <div class="col-6 col-md-4 col-lg-2 dato-clinico"><div class="etiqueta">Frecuencia cardíaca</div><div><%= signoVital(atencion.frecuenciaCardiaca(), "lpm") %></div></div>
                <div class="col-6 col-md-4 col-lg-2 dato-clinico"><div class="etiqueta">Frecuencia respiratoria</div><div><%= signoVital(atencion.frecuenciaRespiratoria(), "rpm") %></div></div>
                <div class="col-6 col-md-4 col-lg-2 dato-clinico"><div class="etiqueta">Presión sistólica</div><div><%= signoVital(atencion.presionSistolica(), "mmHg") %></div></div>
                <div class="col-6 col-md-4 col-lg-2 dato-clinico"><div class="etiqueta">Presión diastólica</div><div><%= signoVital(atencion.presionDiastolica(), "mmHg") %></div></div>
                <div class="col-6 col-md-4 col-lg-2 dato-clinico"><div class="etiqueta">Saturación O₂</div><div><%= signoVital(atencion.saturacionOxigeno(), "%") %></div></div>
            </div>
        </div>
    </section>
    <% } %>

    <% if (atencion != null) { %>
    <section class="card tarjeta shadow-sm mb-4">
        <div class="card-body p-4 p-lg-5">
            <div class="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-4">
                <div class="section-heading">
                    <div class="section-icono coral"><i class="bi bi-prescription2"></i></div>
                    <div>
                        <h2 class="h4 mb-1">Indicaciones médicas de esta atención</h2>
                        <p class="text-secondary mb-0">
                            Medicación formal vinculada con la consulta y disponible para el paciente.
                        </p>
                    </div>
                </div>
                <span class="badge text-bg-primary"><%= recetasAtencion.size() == 1
                        ? "1 indicación"
                        : recetasAtencion.size() + " indicaciones" %></span>
            </div>

            <% if (enCurso) { %>
                <% if (paciente.getAlergias() != null && !paciente.getAlergias().isBlank()) { %>
                <div class="alert alert-warning">
                    <strong>Alergias informadas:</strong> <%= h(paciente.getAlergias()) %>
                </div>
                <% } %>

                <% if (medicamentosActivos.isEmpty()) { %>
                <div class="alert alert-info mb-4">
                    El catálogo no posee medicamentos activos. Un administrador debe registrarlos antes de guardar una indicación médica.
                </div>
                <% } else { %>
                <form method="post" action="${pageContext.request.contextPath}/medico/recetas" class="mb-4">
                    <input type="hidden" name="accion" value="emitir">
                    <input type="hidden" name="idCita" value="<%= idCita %>">

                    <div class="row g-3 mb-3">
                        <div class="col-md-4">
                            <label for="fechaVencimiento" class="form-label fw-semibold">Válida hasta (opcional)</label>
                            <input type="date" id="fechaVencimiento" name="fechaVencimiento"
                                   class="form-control" min="<%= hoyPanel %>">
                        </div>
                        <div class="col-md-8">
                            <label for="indicacionesGenerales" class="form-label fw-semibold">Indicaciones generales</label>
                            <textarea id="indicacionesGenerales" name="indicacionesGenerales"
                                      class="form-control" maxlength="4000"
                                      placeholder="Cuidados generales relacionados con toda la indicación."></textarea>
                        </div>
                    </div>

                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <h3 class="h6 mb-0">Medicamentos e indicaciones</h3>
                        <button type="button" id="agregarMedicamento" class="btn btn-outline-primary btn-sm">
                            Agregar otro medicamento
                        </button>
                    </div>
                    <div id="detallesReceta" class="d-grid gap-3"></div>

                    <div class="d-flex justify-content-end border-top pt-4 mt-4">
                        <button type="submit" class="btn btn-primary">Guardar indicación médica</button>
                    </div>
                </form>

                <template id="detalleRecetaTemplate">
                    <article class="detalle-receta p-3 p-md-4">
                        <div class="d-flex justify-content-between gap-3 mb-3">
                            <strong class="numero-medicamento">Medicamento</strong>
                            <button type="button" class="btn btn-outline-danger btn-sm quitar-medicamento">Quitar</button>
                        </div>
                        <div class="row g-3">
                            <div class="col-12">
                                <label class="form-label fw-semibold">Medicamento</label>
                                <select name="idMedicamento" class="form-select" required>
                                    <option value="">Seleccionar medicamento</option>
                                    <% for (Medicamento medicamento : medicamentosActivos) { %>
                                    <option value="<%= medicamento.getIdMedicamento() %>">
                                        <%= h(medicamento.getDescripcionCompleta()) %>
                                        · <%= h(medicamento.getViaAdministracion().getNombreVisible()) %>
                                    </option>
                                    <% } %>
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label fw-semibold">Dosis</label>
                                <input name="dosis" class="form-control" maxlength="80" required
                                       placeholder="Ej.: 1 comprimido o 10 ml">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label fw-semibold">Frecuencia</label>
                                <input name="frecuencia" class="form-control" maxlength="80" required
                                       placeholder="Ej.: cada 8 horas">
                            </div>
                            <div class="col-6 col-md-3">
                                <label class="form-label fw-semibold">Duración</label>
                                <div class="input-group">
                                    <input type="number" name="duracionDias" class="form-control"
                                           min="1" max="3650" placeholder="7">
                                    <span class="input-group-text">días</span>
                                </div>
                            </div>
                            <div class="col-6 col-md-3">
                                <label class="form-label fw-semibold">Cantidad total</label>
                                <input type="number" name="cantidad" class="form-control"
                                       min="0.01" max="99999999.99" step="0.01" required placeholder="21">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label fw-semibold">Unidad</label>
                                <input name="unidad" class="form-control" maxlength="30" required
                                       placeholder="comprimidos">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label fw-semibold">Indicaciones especiales</label>
                                <input name="instruccionesDetalle" class="form-control" maxlength="2000"
                                       placeholder="Ej.: después de comer">
                            </div>
                        </div>
                    </article>
                </template>
                <% } %>
            <% } %>

            <% if (recetasAtencion.isEmpty()) { %>
            <div class="text-center border rounded-4 py-4 px-3">
                <h3 class="h6">Sin indicaciones médicas registradas</h3>
                <p class="text-secondary mb-0">La consulta todavía no posee medicación ni cuidados formales registrados.</p>
            </div>
            <% } else { %>
            <div class="d-grid gap-3">
                <% for (RecetaMedicaDTO receta : recetasAtencion) { %>
                <article class="p-3 p-md-4 border rounded-4 <%= receta.estaEmitida() ? "receta-emitida" : "receta-anulada" %>">
                    <div class="d-flex flex-wrap justify-content-between gap-3 mb-3">
                        <div>
                            <strong><%= h(receta.codigo()) %></strong>
                            <div class="text-secondary small">
                                Emitida el <%= receta.fechaEmision().atZoneSameInstant(
                                        DisponibilidadService.ZONA_HORARIA).format(fechaHora) %>
                                <% if (receta.fechaVencimiento() != null) { %>
                                · Válida hasta <%= receta.fechaVencimiento() %>
                                <% } %>
                            </div>
                        </div>
                        <span class="badge <%= receta.estaEmitida() ? "text-bg-success" : "text-bg-secondary" %> align-self-start">
                            <%= h(receta.estado()) %>
                        </span>
                    </div>
                    <% if (!"NO_SOLICITADO".equals(receta.estadoEnvioIndicacionesEmail())) { %>
                    <div class="small mb-3 <%= "ENVIADO".equals(receta.estadoEnvioIndicacionesEmail())
                            ? "text-success" : ("FALLIDO".equals(receta.estadoEnvioIndicacionesEmail())
                            ? "text-danger" : "text-warning") %>">
                        <i class="bi bi-envelope-check me-1"></i>
                        Correo al paciente: <%= h(receta.textoEstadoEnvioIndicaciones()) %>
                    </div>
                    <% } %>
                    <div class="d-grid gap-2">
                        <% for (DetalleRecetaDTO detalle : receta.detalles()) { %>
                        <div class="p-3 bg-light rounded-3">
                            <strong><%= h(detalle.nombreComercial()) %> <%= h(detalle.concentracion()) %></strong>
                            <div><%= h(detalle.dosis()) %> · <%= h(detalle.frecuencia()) %>
                                <% if (detalle.duracionDias() != null) { %>· <%= detalle.duracionDias() %> días<% } %>
                            </div>
                            <div class="text-secondary small">
                                Cantidad: <%= detalle.cantidad().stripTrailingZeros().toPlainString() %> <%= h(detalle.unidad()) %>
                                <% if (detalle.instrucciones() != null) { %>· <%= h(detalle.instrucciones()) %><% } %>
                            </div>
                        </div>
                        <% } %>
                    </div>
                    <% if (receta.indicacionesGenerales() != null) { %>
                    <p class="texto-receta mt-3 mb-0"><strong>Indicaciones generales:</strong> <%= h(receta.indicacionesGenerales()) %></p>
                    <% } %>
                    <div class="d-flex justify-content-end gap-2 mt-3">
                        <a class="btn btn-outline-primary btn-sm"
                           href="${pageContext.request.contextPath}/medico/recetas/pdf?id=<%= receta.idReceta() %>">
                            Ver PDF
                        </a>
                        <a class="btn btn-primary btn-sm"
                           href="${pageContext.request.contextPath}/medico/recetas/pdf?id=<%= receta.idReceta() %>&descargar=1">
                            Descargar
                        </a>
                    </div>
                    <% if (receta.estaEmitida()) { %>
                    <form method="post" action="${pageContext.request.contextPath}/medico/recetas"
                          class="row g-2 align-items-end mt-3">
                        <input type="hidden" name="accion" value="anular">
                        <input type="hidden" name="idCita" value="<%= idCita %>">
                        <input type="hidden" name="idReceta" value="<%= receta.idReceta() %>">
                        <div class="col-md">
                            <label class="form-label small fw-semibold">Motivo para anular</label>
                            <input name="motivoAnulacion" class="form-control form-control-sm"
                                   maxlength="500" required placeholder="Explicá por qué debe anularse">
                        </div>
                        <div class="col-md-auto">
                            <button class="btn btn-outline-danger btn-sm" type="submit">Anular indicación</button>
                        </div>
                    </form>
                    <% } else if (receta.motivoAnulacion() != null) { %>
                    <div class="alert alert-secondary mt-3 mb-0 py-2">
                        <strong>Motivo de anulación:</strong> <%= h(receta.motivoAnulacion()) %>
                    </div>
                    <% } %>
                </article>
                <% } %>
            </div>
            <% } %>
        </div>
    </section>
    <% } %>

    <section class="card tarjeta shadow-sm mb-4">
        <div class="card-body p-4 p-lg-5">
            <div class="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-4">
                <div class="section-heading">
                    <div class="section-icono verde"><i class="bi bi-folder2-open"></i></div>
                    <div>
                        <h2 class="h4 mb-1">Documentos clínicos</h2>
                        <p class="text-secondary mb-0">
                            Laboratorios, imágenes e informes incorporados a la historia del paciente.
                        </p>
                    </div>
                </div>
                <span class="badge text-bg-primary"><%= documentosClinicos.size() %> archivo<%= documentosClinicos.size() == 1 ? "" : "s" %></span>
            </div>

            <form method="post" action="${pageContext.request.contextPath}/medico/documentos/cargar"
                  enctype="multipart/form-data" class="row g-3 border rounded-4 p-3 p-md-4 mb-4 formulario-documento">
                <input type="hidden" name="idCita" value="<%= idCita %>">
                <div class="col-12"><h3 class="h6 mb-0">Adjuntar un documento</h3></div>
                <div class="col-md-4">
                    <label class="form-label" for="idTipoDocumentoClinico">Tipo</label>
                    <select class="form-select" id="idTipoDocumentoClinico"
                            name="idTipoDocumento" required>
                        <option value="">Seleccionar</option>
                        <% for (TipoDocumentoClinicoDTO tipo : tiposDocumentoClinico) { %>
                        <option value="<%= tipo.idTipoDocumento() %>"><%= h(tipo.nombre()) %></option>
                        <% } %>
                    </select>
                </div>
                <div class="col-md-3">
                    <label class="form-label" for="fechaDocumentoClinico">Fecha del estudio</label>
                    <input class="form-control" type="date" id="fechaDocumentoClinico"
                           name="fechaDocumento" value="<%= hoyPanel %>" max="<%= hoyPanel %>" required>
                </div>
                <div class="col-md-5">
                    <label class="form-label" for="archivoClinico">Archivo</label>
                    <input class="form-control" type="file" id="archivoClinico" name="archivo"
                           accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png" required>
                </div>
                <div class="col-lg-9">
                    <label class="form-label" for="descripcionDocumentoClinico">Descripción</label>
                    <input class="form-control" id="descripcionDocumentoClinico" name="descripcion"
                           maxlength="500" placeholder="Ej.: Resultado de hemograma de control.">
                </div>
                <div class="col-lg-3 d-grid align-self-end">
                    <button class="btn btn-primary"><i class="bi bi-paperclip me-2"></i>Adjuntar archivo</button>
                </div>
                <div class="col-12"><small class="text-secondary">PDF, JPG, JPEG o PNG. Máximo 10 MB.</small></div>
            </form>

            <% if (documentosClinicos.isEmpty()) { %>
            <div class="text-center border rounded-4 py-5 px-3">
                <div class="documento-icono mx-auto mb-3"><i class="bi bi-file-earmark-medical"></i></div>
                <h3 class="h5">Sin documentos clínicos</h3>
                <p class="text-secondary mb-0">El paciente todavía no posee laboratorios, imágenes o informes cargados.</p>
            </div>
            <% } else { %>
            <div class="d-grid gap-3">
                <% for (DocumentoClinicoDTO documento : documentosClinicos) { %>
                <article class="border rounded-4 p-3">
                    <div class="d-flex flex-wrap align-items-center gap-3">
                        <% if (documento.esImagen()) { %>
                        <img class="documento-miniatura" loading="lazy" alt="Vista previa"
                             src="${pageContext.request.contextPath}/documentos/clinicos/archivo?id=<%= documento.idDocumento() %>&idCita=<%= idCita %>">
                        <% } else { %>
                        <div class="documento-icono"><i class="bi bi-file-earmark-pdf"></i></div>
                        <% } %>
                        <div class="flex-grow-1">
                            <div class="d-flex flex-wrap gap-2 align-items-center mb-1">
                                <strong class="text-break"><%= h(documento.nombreOriginal()) %></strong>
                                <span class="badge text-bg-light"><%= h(documento.nombreTipo()) %></span>
                            </div>
                            <div class="small text-secondary">
                                Estudio: <%= documento.fechaDocumento() == null ? "Sin fecha" : documento.fechaDocumento() %>
                                · Cargado por <%= h(documento.cargadoPor()) %>
                            </div>
                            <% if (documento.descripcion() != null) { %>
                            <div class="small mt-1"><%= h(documento.descripcion()) %></div>
                            <% } %>
                        </div>
                        <div class="d-flex gap-2">
                            <a class="btn btn-primary btn-sm" target="_blank"
                               href="${pageContext.request.contextPath}/documentos/clinicos/archivo?id=<%= documento.idDocumento() %>&idCita=<%= idCita %>">Ver</a>
                            <a class="btn btn-outline-primary btn-sm"
                               href="${pageContext.request.contextPath}/documentos/clinicos/archivo?id=<%= documento.idDocumento() %>&idCita=<%= idCita %>&descargar=1"><i class="bi bi-download"></i></a>
                        </div>
                    </div>
                </article>
                <% } %>
            </div>
            <% } %>
        </div>
    </section>

    <section class="card tarjeta shadow-sm">
        <div class="card-body p-4 p-lg-5">
            <div class="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-4">
                <div class="section-heading">
                    <div class="section-icono violeta"><i class="bi bi-clock-history"></i></div>
                    <div>
                        <h2 class="h4 mb-1">Historial clínico anterior</h2>
                        <p class="text-secondary mb-0">
                            Consultas finalizadas previamente por profesionales de SoraCare.
                        </p>
                    </div>
                </div>
                <span class="badge text-bg-primary"><%= historial.size() %> registro<%= historial.size() == 1 ? "" : "s" %></span>
            </div>

            <% if (historial.isEmpty()) { %>
            <div class="text-center border rounded-4 py-5 px-3">
                <h3 class="h5">Sin atenciones anteriores</h3>
                <p class="text-secondary mb-0">
                    Esta será la primera atención clínica registrada para el paciente.
                </p>
            </div>
            <% } else { %>
            <div class="d-grid gap-3">
                <% for (AtencionMedicaDTO anterior : historial) { %>
                <details class="historial-item p-3 p-md-4">
                    <summary>
                        <div class="d-flex flex-wrap justify-content-between gap-3">
                            <div>
                                <strong><%= anterior.fechaHoraInicio().atZoneSameInstant(
                                        DisponibilidadService.ZONA_HORARIA).format(fechaHora) %></strong>
                                <div class="text-secondary">
                                    <%= h(anterior.especialidad()) %>
                                    · <%= h(anterior.medicoNombreCompleto()) %>
                                </div>
                            </div>
                            <span class="badge estado-cita estado-atendida align-self-start">Ver detalle</span>
                        </div>
                    </summary>
                    <div class="row g-4 border-top mt-3 pt-3">
                        <div class="col-12 dato-clinico"><div class="etiqueta">Motivo</div><div><%= h(anterior.motivoConsulta()) %></div></div>
                        <div class="col-12 dato-clinico"><div class="etiqueta">Signos vitales</div><div>
                            T° <%= signoVital(anterior.temperaturaC(), "°C") %> ·
                            FC <%= signoVital(anterior.frecuenciaCardiaca(), "lpm") %> ·
                            FR <%= signoVital(anterior.frecuenciaRespiratoria(), "rpm") %> ·
                            PA <%= anterior.presionSistolica() == null || anterior.presionDiastolica() == null
                                    ? "—" : anterior.presionSistolica() + "/" + anterior.presionDiastolica() + " mmHg" %> ·
                            SpO₂ <%= signoVital(anterior.saturacionOxigeno(), "%") %>
                        </div></div>
                        <div class="col-md-6 dato-clinico"><div class="etiqueta">Síntomas</div><div><%= textoOGuion(anterior.sintomas()) %></div></div>
                        <div class="col-md-6 dato-clinico"><div class="etiqueta">Antecedentes relevantes</div><div><%= textoOGuion(anterior.antecedentesRelevantes()) %></div></div>
                        <div class="col-md-6 dato-clinico"><div class="etiqueta">Evaluación del área afectada</div><div><%= textoOGuion(anterior.evaluacionAreaAfectada()) %></div></div>
                        <div class="col-md-6 dato-clinico"><div class="etiqueta">Examen físico general</div><div><%= textoOGuion(anterior.examenFisico()) %></div></div>
                        <div class="col-md-6 dato-clinico"><div class="etiqueta">Diagnóstico de la consulta</div><div><%= textoOGuion(anterior.evaluacionClinica()) %></div></div>
                        <div class="col-md-6 dato-clinico"><div class="etiqueta">Plan de tratamiento</div><div><%= textoOGuion(anterior.planTratamiento()) %></div></div>
                        <div class="col-md-6 dato-clinico"><div class="etiqueta">Estudios clínicos solicitados</div><div><%= textoOGuion(anterior.estudiosClinicosSolicitados()) %></div></div>
                        <div class="col-12 dato-clinico"><div class="etiqueta">Indicaciones</div><div><%= textoOGuion(anterior.indicaciones()) %></div></div>
                    </div>
                </details>
                <% } %>
            </div>
            <% } %>
        </div>
    </section>
    <% } %>
</main>
<script>
    const seguimiento = document.getElementById('requiereSeguimiento');
    const fechaControl = document.getElementById('fechaProximoControl');
    if (seguimiento && fechaControl) {
        seguimiento.addEventListener('change', () => {
            fechaControl.disabled = !seguimiento.checked;
            fechaControl.required = seguimiento.checked;
            if (!seguimiento.checked) fechaControl.value = '';
        });
    }

    const contenedorReceta = document.getElementById('detallesReceta');
    const plantillaReceta = document.getElementById('detalleRecetaTemplate');
    const botonAgregarMedicamento = document.getElementById('agregarMedicamento');
    const actualizarNumerosReceta = () => {
        if (!contenedorReceta) return;
        const filas = [...contenedorReceta.querySelectorAll('.detalle-receta')];
        filas.forEach((fila, indice) => {
            fila.querySelector('.numero-medicamento').textContent = `Medicamento ${indice + 1}`;
            fila.querySelector('.quitar-medicamento').disabled = filas.length === 1;
        });
    };
    const agregarFilaReceta = () => {
        if (!contenedorReceta || !plantillaReceta) return;
        if (contenedorReceta.children.length >= 10) return;
        const fragmento = plantillaReceta.content.cloneNode(true);
        fragmento.querySelector('.quitar-medicamento').addEventListener('click', evento => {
            evento.currentTarget.closest('.detalle-receta').remove();
            actualizarNumerosReceta();
        });
        contenedorReceta.appendChild(fragmento);
        actualizarNumerosReceta();
    };
    if (botonAgregarMedicamento) {
        botonAgregarMedicamento.addEventListener('click', agregarFilaReceta);
        agregarFilaReceta();
    }
</script>
<script src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js"></script>
</body>
</html>
