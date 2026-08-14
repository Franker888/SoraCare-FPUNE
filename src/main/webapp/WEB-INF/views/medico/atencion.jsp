<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.AtencionMedicaDTO" %>
<%@ page import="py.fpune.soracare.dto.CitaAgendaMedicoDTO" %>
<%@ page import="py.fpune.soracare.dto.DocumentoClinicoDTO" %>
<%@ page import="py.fpune.soracare.dto.PanelClinicoPacienteDTO" %>
<%@ page import="py.fpune.soracare.dto.TipoDocumentoClinicoDTO" %>
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
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
          rel="stylesheet">
    <style>
        body { background: #f3f7fb; color: #172033; }
        .barra-superior { background: #ffffff; }
        .marca { color: #235ee7; font-weight: 800; }
        .cabecera-clinica {
            border: 0;
            border-radius: 1.25rem;
            color: #ffffff;
            background: linear-gradient(135deg, #173da8, #218bbf);
        }
        .tarjeta { border: 1px solid #dde6f0; border-radius: 1.1rem; }
        .tarjeta-dato { border: 0; border-radius: 1rem; }
        .etiqueta { color: #64748b; font-size: .8rem; }
        .valor-dato { font-weight: 700; }
        .estado-cita { border-radius: 999px; padding: .45rem .75rem; }
        .estado-confirmada { background: #ccfbf1; color: #0f766e; }
        .estado-atencion { background: #ede9fe; color: #6d28d9; }
        .estado-atendida { background: #dbeafe; color: #1d4ed8; }
        .estado-pendiente { background: #fef3c7; color: #92400e; }
        .seccion-formulario {
            border-bottom: 1px solid #e2e8f0;
            padding-bottom: .75rem;
            margin-bottom: 1rem;
        }
        .form-control { border-radius: .8rem; }
        textarea.form-control { min-height: 110px; }
        .historial-item {
            border: 1px solid #dde6f0;
            border-radius: 1rem;
            background: #ffffff;
        }
        .historial-item summary { cursor: pointer; list-style: none; }
        .historial-item summary::-webkit-details-marker { display: none; }
        .dato-clinico {
            border-left: 3px solid #bfdbfe;
            padding-left: .9rem;
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
    </style>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css">
</head>
<body class="app-shell app-doctor">
<nav class="navbar barra-superior border-bottom">
    <div class="container py-2">
        <a class="navbar-brand marca" href="${pageContext.request.contextPath}/dashboard">
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
        <div class="card-body p-4 p-md-5">
            <div class="row align-items-center g-3">
                <div class="col-lg-8">
                    <p class="mb-2 opacity-75">Panel clínico del paciente · Etapa 6</p>
                    <h1 class="display-6 fw-bold mb-2">
                        <%= h(paciente.getNombres() + " " + paciente.getApellidos()) %>
                    </h1>
                    <p class="mb-0">
                        Historia clínica <%= h(paciente.getNumeroHistoriaClinica()) %>
                        · CI <%= h(paciente.getCedula()) %>
                        <% if (edad != null) { %>· <%= edad %> años<% } %>
                    </p>
                </div>
                <div class="col-lg-4 text-lg-end">
                    <span class="badge bg-white text-primary fs-6 px-3 py-2">
                        Vista clínica integral
                    </span>
                </div>
            </div>
        </div>
    </section>

    <section class="row g-3 mb-4">
        <div class="col-6 col-lg-3">
            <div class="card tarjeta-dato shadow-sm h-100"><div class="card-body">
                <div class="etiqueta">Edad del paciente</div>
                <div class="valor-dato"><%= edad == null
                        ? "No informada" : edad + " años" %></div>
                <% if (paciente.getFechaNacimiento() != null) { %>
                <small class="text-secondary">Nacimiento: <%= paciente.getFechaNacimiento() %></small>
                <% } %>
            </div></div>
        </div>
        <div class="col-6 col-lg-3">
            <div class="card tarjeta-dato shadow-sm h-100"><div class="card-body">
                <div class="etiqueta">Grupo sanguíneo</div>
                <div class="valor-dato"><%= textoOGuion(paciente.getGrupoSanguineo()) %></div>
            </div></div>
        </div>
        <div class="col-6 col-lg-3">
            <div class="card tarjeta-dato shadow-sm h-100"><div class="card-body">
                <div class="etiqueta">Atenciones anteriores</div>
                <div class="valor-dato"><%= historial.size() %></div>
                <small class="text-secondary">Registros clínicos finalizados</small>
            </div></div>
        </div>
        <div class="col-6 col-lg-3">
            <div class="card tarjeta-dato shadow-sm h-100"><div class="card-body">
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

    <section class="card tarjeta shadow-sm mb-4">
        <div class="card-body p-4">
            <div class="row g-3 align-items-center">
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
                <div>
                    <h2 class="h4 mb-1">Información clínica permanente</h2>
                    <p class="text-secondary mb-0">
                        Datos vigentes del paciente que acompañan todas sus consultas.
                    </p>
                </div>
                <span class="badge text-bg-light align-self-start">Perfil del paciente</span>
            </div>
            <div class="row g-4">
                <div class="col-lg-4 dato-clinico">
                    <div class="etiqueta">Antecedentes patológicos personales</div>
                    <div><%= textoOGuion(paciente.getAntecedentesPatologicosPersonales()) %></div>
                </div>
                <div class="col-lg-4 dato-clinico">
                    <div class="etiqueta">Fármacos utilizados</div>
                    <div><%= textoOGuion(paciente.getFarmacosUtilizados()) %></div>
                </div>
                <div class="col-lg-4 dato-clinico">
                    <div class="etiqueta">Alergias conocidas</div>
                    <div><%= textoOGuion(paciente.getAlergias()) %></div>
                </div>
            </div>
        </div>
    </section>
    <% } %>

    <% if (atencion == null) { %>
    <section class="card tarjeta shadow-sm mb-4">
        <div class="card-body p-4 p-md-5 text-center">
            <% if (puedeIniciar) { %>
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
    <section class="card tarjeta shadow-sm mb-4">
        <div class="card-body p-4">
            <div class="d-flex flex-wrap justify-content-between gap-2 mb-4">
                <div>
                    <h2 class="h4 mb-1">Atención en curso</h2>
                    <p class="text-secondary mb-0">
                        Guardá avances cuando lo necesites. Finalizar cerrará la cita.
                    </p>
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
        <div class="card-body p-4">
            <div class="d-flex flex-wrap justify-content-between gap-2 mb-4">
                <div>
                    <h2 class="h4 mb-1">Atención finalizada</h2>
                    <p class="text-secondary mb-0">
                        Registro cerrado el
                        <%= atencion.fechaHoraFin() == null ? "—"
                                : atencion.fechaHoraFin().atZoneSameInstant(
                                        DisponibilidadService.ZONA_HORARIA).format(fechaHora) %>.
                    </p>
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

    <section class="card tarjeta shadow-sm mb-4">
        <div class="card-body p-4">
            <div class="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-4">
                <div>
                    <h2 class="h4 mb-1">Documentos clínicos</h2>
                    <p class="text-secondary mb-0">
                        Laboratorios, imágenes e informes incorporados a la historia del paciente.
                    </p>
                </div>
                <span class="badge text-bg-primary"><%= documentosClinicos.size() %> archivo<%= documentosClinicos.size() == 1 ? "" : "s" %></span>
            </div>

            <form method="post" action="${pageContext.request.contextPath}/medico/documentos/cargar"
                  enctype="multipart/form-data" class="row g-3 border rounded-4 p-3 mb-4">
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
        <div class="card-body p-4">
            <div class="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-4">
                <div>
                    <h2 class="h4 mb-1">Historial clínico anterior</h2>
                    <p class="text-secondary mb-0">
                        Consultas finalizadas previamente por profesionales de SoraCare.
                    </p>
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
                                    · Dr./Dra. <%= h(anterior.medicoNombreCompleto()) %>
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
</script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
