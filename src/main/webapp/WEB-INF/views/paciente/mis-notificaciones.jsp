<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.NotificacionDTO" %>
<%@ page import="py.fpune.soracare.service.DisponibilidadService" %>
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
%>
<%
    List<NotificacionDTO> notificaciones = request.getAttribute("notificaciones") instanceof List<?>
            ? (List<NotificacionDTO>) request.getAttribute("notificaciones") : List.of();
    long noLeidas = notificaciones.stream().filter(n -> !n.estaLeida()).count();
    String mensajeExito = (String) request.getAttribute("mensajeExito");
    String error = (String) request.getAttribute("error");
    String tokenCsrf = (String) request.getAttribute("tokenCsrf");
    String firebaseAdvertencia = (String) request.getAttribute("firebaseAdvertencia");
    boolean firebaseConfigurado = Boolean.TRUE.equals(request.getAttribute("firebaseConfigurado"));
    Object cantidadSuscripciones = request.getAttribute("suscripcionesPushActivas");
    int suscripcionesPushActivas = cantidadSuscripciones instanceof Number
            ? ((Number) cantidadSuscripciones).intValue() : 0;
    String firebaseApiKey = (String) request.getAttribute("firebaseApiKey");
    String firebaseAuthDomain = (String) request.getAttribute("firebaseAuthDomain");
    String firebaseProjectId = (String) request.getAttribute("firebaseProjectId");
    String firebaseStorageBucket = (String) request.getAttribute("firebaseStorageBucket");
    String firebaseMessagingSenderId = (String) request.getAttribute("firebaseMessagingSenderId");
    String firebaseAppId = (String) request.getAttribute("firebaseAppId");
    String firebaseVapidKey = (String) request.getAttribute("firebaseVapidKey");
    DateTimeFormatter fechaHora = DateTimeFormatter.ofPattern(
            "dd/MM/yyyy HH:mm", new Locale("es", "PY"));
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex, nofollow">
    <title>Mis notificaciones | SoraCare-FPUNE</title>
    <link href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/libs/bootstrap-icons/bootstrap-icons.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260815">
    <style>
        body { background: #f4f7fb; }
        .hero { border: 0; border-radius: 1.25rem; color: #fff;
            background: linear-gradient(135deg, #173da8, #2563eb 58%, #13a6a2); }
        .notificacion { border: 1px solid #dce5f1; border-radius: 1rem; }
        .notificacion.nueva { border-left: 5px solid #2563eb; background: #f8fbff; }
        .icono { width: 46px; height: 46px; display: grid; place-items: center;
            flex: 0 0 46px; border-radius: 14px; background: #eaf1ff; color: #2563eb; }
        .push-card { border: 1px solid #cfe0f5; border-radius: 1.1rem;
            background: linear-gradient(135deg, #f8fbff, #eef8ff); overflow: hidden; }
        .push-icon { width: 54px; height: 54px; display: grid; place-items: center;
            flex: 0 0 54px; border-radius: 16px; color: #fff;
            background: linear-gradient(135deg, #2563eb, #0ea5a4); font-size: 1.35rem; }
        .push-token { min-height: 78px; resize: none; font-family: ui-monospace, SFMono-Regular,
            Menlo, Monaco, Consolas, "Liberation Mono", monospace; font-size: .76rem; }
    </style>
</head>
<body class="app-shell app-patient">
<nav class="navbar bg-white border-bottom">
    <div class="container py-2">
        <a class="navbar-brand fw-bold text-primary" href="${pageContext.request.contextPath}/paciente/inicio">SoraCare-FPUNE</a>
        <a class="btn btn-outline-primary btn-sm" href="${pageContext.request.contextPath}/paciente/inicio">Volver al panel</a>
    </div>
</nav>
<main class="container py-4 py-lg-5">
    <section class="card hero shadow-sm mb-4">
        <div class="card-body p-4 p-lg-5 d-flex flex-column flex-lg-row justify-content-between gap-3">
            <div>
                <p class="mb-2 opacity-75">Portal del paciente</p>
                <h1 class="display-6 fw-bold mb-2">Mis notificaciones</h1>
                <p class="mb-0">Novedades importantes sobre tus citas, pagos, indicaciones médicas y documentos.</p>
            </div>
            <div class="align-self-lg-center">
                <span class="badge bg-white text-primary fs-6 px-3 py-2"><%= noLeidas %> sin leer</span>
            </div>
        </div>
    </section>

    <% if (mensajeExito != null) { %><div class="alert alert-success"><%= h(mensajeExito) %></div><% } %>
    <% if (error != null) { %><div class="alert alert-danger"><%= h(error) %></div><% } %>
    <% if (firebaseAdvertencia != null) { %><div class="alert alert-warning"><%= h(firebaseAdvertencia) %></div><% } %>

    <section id="firebasePushCard" class="card push-card shadow-sm mb-4"
             data-configurado="<%= firebaseConfigurado %>"
             data-api-key="<%= h(firebaseApiKey) %>"
             data-auth-domain="<%= h(firebaseAuthDomain) %>"
             data-project-id="<%= h(firebaseProjectId) %>"
             data-storage-bucket="<%= h(firebaseStorageBucket) %>"
             data-messaging-sender-id="<%= h(firebaseMessagingSenderId) %>"
             data-app-id="<%= h(firebaseAppId) %>"
             data-vapid-key="<%= h(firebaseVapidKey) %>"
             data-endpoint="${pageContext.request.contextPath}/mis-notificaciones/push"
             data-csrf-token="<%= h(tokenCsrf) %>"
             data-worker="${pageContext.request.contextPath}/firebase-messaging-sw.js?v=3">
        <div class="card-body p-4">
            <div class="d-flex flex-column flex-lg-row align-items-lg-center gap-3">
                <div class="push-icon"><i class="bi bi-phone-vibrate"></i></div>
                <div class="flex-grow-1">
                    <div class="d-flex flex-wrap align-items-center gap-2 mb-1">
                        <h2 class="h5 mb-0">Avisos en este dispositivo</h2>
                        <% if (suscripcionesPushActivas > 0) { %>
                        <span class="badge text-bg-success"><%= suscripcionesPushActivas %> activo<%= suscripcionesPushActivas == 1 ? "" : "s" %></span>
                        <% } %>
                    </div>
                    <p class="text-secondary mb-2">Recibí recordatorios generales aun cuando SoraCare no esté abierto.</p>
                    <p id="pushStatus" class="small mb-0" role="status" aria-live="polite">
                        <%= firebaseConfigurado
                                ? "Este navegador todavía no confirmó su permiso."
                                : "Firebase está pendiente de configuración en este servidor." %>
                    </p>
                </div>
                <% if (firebaseConfigurado) { %>
                <div class="d-flex flex-wrap gap-2">
                    <button id="activarPush" type="button" class="btn btn-primary">
                        <i class="bi bi-bell-fill me-1"></i> Activar avisos
                    </button>
                    <button id="desactivarPush" type="button" class="btn btn-outline-secondary d-none">
                        Desactivar
                    </button>
                </div>
                <% } %>
            </div>
            <% if (firebaseConfigurado) { %>
            <div id="pushTokenPanel" class="border-top mt-4 pt-3 d-none">
                <label for="pushToken" class="form-label fw-semibold">Token de prueba del dispositivo</label>
                <textarea id="pushToken" class="form-control push-token" readonly></textarea>
                <div class="d-flex flex-wrap align-items-center gap-2 mt-2">
                    <button id="copiarPushToken" type="button" class="btn btn-outline-primary btn-sm">
                        <i class="bi bi-clipboard me-1"></i> Copiar token
                    </button>
                    <span class="small text-secondary">Se usa únicamente para enviar una prueba desde Firebase Console.</span>
                </div>
            </div>
            <% } %>
        </div>
    </section>

    <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div>
            <h2 class="h4 mb-1">Centro de avisos</h2>
            <p class="text-secondary mb-0">Se muestran hasta las 100 notificaciones más recientes.</p>
        </div>
        <% if (noLeidas > 0) { %>
        <form method="post" action="${pageContext.request.contextPath}/mis-notificaciones">
            <input type="hidden" name="accion" value="todas">
            <input type="hidden" name="tokenCsrf" value="<%= h(tokenCsrf) %>">
            <button class="btn btn-outline-primary">Marcar todas como leídas</button>
        </form>
        <% } %>
    </div>

    <% if (notificaciones.isEmpty()) { %>
    <section class="card notificacion shadow-sm">
        <div class="card-body text-center py-5">
            <div class="icono mx-auto mb-3"><i class="bi bi-bell"></i></div>
            <h2 class="h5">No tenés notificaciones</h2>
            <p class="text-secondary mb-0">Los nuevos avisos aparecerán en esta sección.</p>
        </div>
    </section>
    <% } else { %>
    <div class="d-grid gap-3">
        <% for (NotificacionDTO notificacion : notificaciones) { %>
        <article class="card notificacion shadow-sm <%= notificacion.estaLeida() ? "" : "nueva" %>">
            <div class="card-body p-4 d-flex flex-column flex-md-row gap-3">
                <div class="icono"><i class="bi bi-bell-fill"></i></div>
                <div class="flex-grow-1">
                    <div class="d-flex flex-wrap justify-content-between gap-2 mb-2">
                        <div>
                            <h2 class="h5 mb-1"><%= h(notificacion.asunto()) %></h2>
                            <span class="text-secondary small">
                                <%= notificacion.fechaCreacion().atZoneSameInstant(
                                        DisponibilidadService.ZONA_HORARIA).format(fechaHora) %>
                            </span>
                        </div>
                        <span class="badge <%= notificacion.estaLeida() ? "text-bg-light" : "text-bg-primary" %> align-self-start">
                            <%= notificacion.estaLeida() ? "Leída" : "Nueva" %>
                        </span>
                    </div>
                    <p class="mb-3"><%= h(notificacion.contenido()) %></p>
                    <div class="d-flex flex-wrap gap-2">
                        <% if (notificacion.idReceta() != null) { %>
                        <a class="btn btn-primary btn-sm" href="${pageContext.request.contextPath}/mis-recetas">Ver indicaciones médicas</a>
                        <% } %>
                        <% if (notificacion.idCita() != null) { %>
                        <a class="btn btn-primary btn-sm" href="${pageContext.request.contextPath}/mis-citas">Ver mis citas</a>
                        <% } %>
                        <% if (!notificacion.estaLeida()) { %>
                        <form method="post" action="${pageContext.request.contextPath}/mis-notificaciones">
                            <input type="hidden" name="idNotificacion" value="<%= notificacion.idNotificacion() %>">
                            <input type="hidden" name="tokenCsrf" value="<%= h(tokenCsrf) %>">
                            <button class="btn btn-outline-secondary btn-sm">Marcar como leída</button>
                        </form>
                        <% } %>
                    </div>
                </div>
            </div>
        </article>
        <% } %>
    </div>
    <% } %>
</main>
<% if (firebaseConfigurado) { %>
<script src="https://www.gstatic.com/firebasejs/10.12.5/firebase-app-compat.js"></script>
<script src="https://www.gstatic.com/firebasejs/10.12.5/firebase-messaging-compat.js"></script>
<script>
    (function () {
        "use strict";

        const card = document.getElementById("firebasePushCard");
        const status = document.getElementById("pushStatus");
        const activar = document.getElementById("activarPush");
        const desactivar = document.getElementById("desactivarPush");
        const tokenPanel = document.getElementById("pushTokenPanel");
        const tokenCampo = document.getElementById("pushToken");
        const copiar = document.getElementById("copiarPushToken");
        let messaging = null;
        let tokenActual = "";

        function informar(mensaje, tipo) {
            status.textContent = mensaje;
            status.className = "small mb-0 " + (tipo === "error" ? "text-danger" :
                    tipo === "exito" ? "text-success" : "text-secondary");
        }

        function prepararFirebase() {
            if (!("serviceWorker" in navigator) || !("Notification" in window)) {
                throw new Error("Este navegador no admite notificaciones web.");
            }
            if (typeof firebase === "undefined" || !firebase.messaging) {
                throw new Error("No fue posible cargar Firebase en este momento.");
            }
            if (!firebase.apps.length) {
                firebase.initializeApp({
                    apiKey: card.dataset.apiKey,
                    authDomain: card.dataset.authDomain,
                    projectId: card.dataset.projectId,
                    storageBucket: card.dataset.storageBucket,
                    messagingSenderId: card.dataset.messagingSenderId,
                    appId: card.dataset.appId
                });
            }
            messaging = firebase.messaging();
        }

        async function esperarServiceWorkerActivo(registro) {
            if (registro.active) {
                return registro;
            }

            const worker = registro.installing || registro.waiting;
            if (worker) {
                await new Promise(function (resolve, reject) {
                    const timeout = window.setTimeout(function () {
                        worker.removeEventListener("statechange", verificarEstado);
                        reject(new Error("El servicio de avisos tardó demasiado en activarse. Volvé a intentarlo."));
                    }, 15000);

                    function verificarEstado() {
                        if (worker.state === "activated") {
                            window.clearTimeout(timeout);
                            worker.removeEventListener("statechange", verificarEstado);
                            resolve();
                        } else if (worker.state === "redundant") {
                            window.clearTimeout(timeout);
                            worker.removeEventListener("statechange", verificarEstado);
                            reject(new Error("No se pudo activar el servicio de avisos. Recargá la página e intentá nuevamente."));
                        }
                    }

                    worker.addEventListener("statechange", verificarEstado);
                    verificarEstado();
                });
            }

            if (registro.active) {
                return registro;
            }
            return navigator.serviceWorker.ready;
        }

        async function guardarToken(accion, token) {
            const datos = new URLSearchParams();
            datos.set("accion", accion);
            datos.set("token", token);
            datos.set("tokenCsrf", card.dataset.csrfToken);
            const respuesta = await fetch(card.dataset.endpoint, {
                method: "POST",
                credentials: "same-origin",
                headers: {"Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"},
                body: datos.toString()
            });
            const resultado = await respuesta.json();
            if (!respuesta.ok || !resultado.exito) {
                throw new Error(resultado.mensaje || "No fue posible actualizar el dispositivo.");
            }
            return resultado;
        }

        async function activarAvisos() {
            activar.disabled = true;
            informar("Solicitando permiso al navegador...", "normal");
            try {
                prepararFirebase();
                const permiso = await Notification.requestPermission();
                if (permiso !== "granted") {
                    throw new Error("El permiso fue rechazado. Podés habilitarlo desde la configuración del navegador.");
                }
                const registro = await navigator.serviceWorker.register(card.dataset.worker, {
                    updateViaCache: "none"
                });
                const registroActivo = await esperarServiceWorkerActivo(registro);
                tokenActual = await messaging.getToken({
                    vapidKey: card.dataset.vapidKey,
                    serviceWorkerRegistration: registroActivo
                });
                if (!tokenActual) {
                    throw new Error("Firebase no devolvió un token para este dispositivo.");
                }
                const resultado = await guardarToken("registrar", tokenActual);
                tokenCampo.value = tokenActual;
                tokenPanel.classList.remove("d-none");
                activar.classList.add("d-none");
                desactivar.classList.remove("d-none");
                informar(resultado.mensaje, "exito");
            } catch (error) {
                informar(error.message || "No fue posible activar los avisos.", "error");
            } finally {
                activar.disabled = false;
            }
        }

        async function desactivarAvisos() {
            desactivar.disabled = true;
            try {
                prepararFirebase();
                if (!tokenActual) {
                    const registro = await navigator.serviceWorker.getRegistration(card.dataset.worker);
                    if (registro) {
                        tokenActual = await messaging.getToken({
                            vapidKey: card.dataset.vapidKey,
                            serviceWorkerRegistration: registro
                        });
                    }
                }
                if (tokenActual) {
                    await guardarToken("desactivar", tokenActual);
                    await messaging.deleteToken();
                }
                tokenActual = "";
                tokenCampo.value = "";
                tokenPanel.classList.add("d-none");
                desactivar.classList.add("d-none");
                activar.classList.remove("d-none");
                informar("Los avisos de este dispositivo están desactivados.", "normal");
            } catch (error) {
                informar(error.message || "No fue posible desactivar los avisos.", "error");
            } finally {
                desactivar.disabled = false;
            }
        }

        activar.addEventListener("click", activarAvisos);
        desactivar.addEventListener("click", desactivarAvisos);
        copiar.addEventListener("click", async function () {
            if (!tokenActual) return;
            try {
                await navigator.clipboard.writeText(tokenActual);
                copiar.innerHTML = '<i class="bi bi-check-lg me-1"></i> Copiado';
            } catch (error) {
                tokenCampo.focus();
                tokenCampo.select();
                document.execCommand("copy");
            }
        });

        try {
            prepararFirebase();
            if (Notification.permission === "granted") {
                informar("El navegador ya tiene permiso. Presioná Activar avisos para vincularlo con tu cuenta.", "normal");
            } else if (Notification.permission === "denied") {
                informar("Los avisos están bloqueados en la configuración de este navegador.", "error");
            }
            messaging.onMessage(function (payload) {
                const aviso = payload.notification || {};
                informar(aviso.title || "Recibiste un nuevo aviso de SoraCare.", "exito");
            });
        } catch (error) {
            activar.disabled = true;
            informar(error.message, "error");
        }
    }());
</script>
<% } %>
</body>
</html>
