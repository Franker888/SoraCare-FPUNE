(function () {
    "use strict";

    var assistant = document.querySelector(".sora-assistant");
    if (!assistant) return;

    var contextPath = assistant.getAttribute("data-context-path") || "";
    var authenticated = assistant.getAttribute("data-authenticated") === "true";
    var patientSession = assistant.getAttribute("data-patient") === "true";
    var panelRoute = assistant.getAttribute("data-panel-route") || "/dashboard";
    var launcher = assistant.querySelector(".sora-launcher");
    var panel = assistant.querySelector(".sora-panel");
    var closeButton = assistant.querySelector(".sora-close");
    var form = assistant.querySelector(".sora-form");
    var input = assistant.querySelector("#sora-public-input");
    var submitButton = form.querySelector("button[type='submit']");
    var conversation = assistant.querySelector(".sora-conversation");
    var quickActions = assistant.querySelector(".sora-quick-actions");
    var status = assistant.querySelector(".sora-status");
    var statusText = assistant.querySelector(".sora-status-text");
    var pendingResponse = false;
    var conversationHistory = [];

    function normalizar(texto) {
        return (texto || "")
            .toLowerCase()
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .replace(/[^a-z0-9\s]/g, " ")
            .replace(/\s+/g, " ")
            .trim();
    }

    function contiene(texto, palabras) {
        return palabras.some(function (palabra) {
            return texto.indexOf(palabra) !== -1;
        });
    }

    function accion(etiqueta, ruta, icono) {
        return {etiqueta: etiqueta, ruta: ruta, icono: icono || "bi-arrow-right"};
    }

    function accesoPersonal(mensaje) {
        if (authenticated) {
            return {
                mensaje: mensaje,
                acciones: [accion(patientSession ? "Abrir mi portal" : "Abrir mi panel", panelRoute, "bi-grid")]
            };
        }
        return {
            mensaje: mensaje + " Si todavía no tenés una cuenta, podés registrarte como paciente en pocos pasos.",
            acciones: [
                accion("Crear cuenta", "/registro", "bi-person-plus"),
                accion("Iniciar sesión", "/login", "bi-box-arrow-in-right")
            ]
        };
    }

    function resolver(consulta) {
        var texto = normalizar(consulta);

        if (contiene(texto, ["hola", "buenas", "quien sos", "quien eres", "que haces", "ayuda", "orientame"])) {
            return {
                mensaje: "¡Hola! Soy Sora, la guía virtual de SoraCare. Puedo explicarte cómo registrarte, buscar profesionales, conocer especialidades o empezar una reserva.",
                acciones: [
                    accion("Soy nuevo", "/registro", "bi-person-plus"),
                    accion("Conocer especialidades", "/especialidades", "bi-heart-pulse")
                ]
            };
        }

        if (contiene(texto, ["soy nuevo", "primera vez", "registr", "crear cuenta", "cuenta nueva", "hacerme una cuenta"])) {
            return {
                mensaje: "Para usar la agenda como paciente, completá tu registro con datos personales, de contacto y una contraseña. Luego podrás reservar y administrar tus propias citas.",
                acciones: [
                    accion("Crear cuenta de paciente", "/registro", "bi-person-plus"),
                    accion("Ya tengo una cuenta", "/login", "bi-box-arrow-in-right")
                ]
            };
        }

        if (contiene(texto, ["iniciar sesion", "ingresar", "entrar", "acceder", "olvide", "contrasena", "usuario"])) {
            return authenticated ? {
                mensaje: "Ya tenés una sesión activa. Desde tu panel podés acceder a las funciones habilitadas para tu perfil.",
                acciones: [accion("Ir a mi panel", panelRoute, "bi-grid")]
            } : {
                mensaje: "Podés ingresar con tu usuario o correo electrónico y tu contraseña. Si sos paciente nuevo, primero necesitás crear una cuenta.",
                acciones: [
                    accion("Iniciar sesión", "/login", "bi-box-arrow-in-right"),
                    accion("Crear cuenta", "/registro", "bi-person-plus")
                ]
            };
        }

        if (contiene(texto, ["agendar", "sacar cita", "nuevo turno", "nueva cita", "reservar", "consulta medica"])) {
            if (authenticated && patientSession) {
                return {
                    mensaje: "Desde tu portal podés elegir especialidad, médico, una fecha disponible y el horario que más te convenga.",
                    acciones: [accion("Agendar una cita", "/mis-citas?nueva=1", "bi-calendar-plus")]
                };
            }
            return accesoPersonal("Para reservar una cita necesitás ingresar como paciente. Después podrás elegir especialidad, médico, fecha, turno y método de pago.");
        }

        if (contiene(texto, ["disponibilidad", "fecha", "horario", "hora", "cuando atiende", "turnos libres"])) {
            return {
                mensaje: "La disponibilidad depende del profesional y de sus horarios activos. Podés empezar seleccionando una especialidad o revisando el catálogo de médicos.",
                acciones: [
                    accion("Ver especialidades", "/especialidades", "bi-heart-pulse"),
                    accion("Ver médicos", "/medicos", "bi-person-badge")
                ]
            };
        }

        if (contiene(texto, ["medico", "doctor", "doctora", "profesional", "especialista"])) {
            return {
                mensaje: "En el catálogo de médicos podés conocer cada profesional, sus especialidades y sus horarios habituales de atención.",
                acciones: [
                    accion("Buscar médicos", "/medicos", "bi-person-badge"),
                    accion("Ver especialidades", "/especialidades", "bi-heart-pulse")
                ]
            };
        }

        if (contiene(texto, ["especialidad", "cardiologia", "pediatria", "dermatologia", "clinica", "servicio medico"])) {
            return {
                mensaje: "Podés explorar las áreas médicas disponibles y desde cada especialidad conocer a los profesionales asociados.",
                acciones: [accion("Explorar especialidades", "/especialidades", "bi-heart-pulse")]
            };
        }

        if (contiene(texto, ["pago", "pagar", "tarjeta", "transferencia", "qr", "efectivo", "comprobante", "factura"])) {
            return {
                mensaje: "SoraCare permite simular pagos con tarjeta, transferencia, QR o efectivo. Los pagos digitales se completan después de reservar; el efectivo queda pendiente para confirmación en recepción.",
                acciones: [
                    accion(authenticated && patientSession ? "Revisar mis citas" : "Ingresar para reservar", authenticated && patientSession ? "/mis-citas" : "/login", "bi-credit-card"),
                    accion("Cómo funciona SoraCare", "/#como-funciona", "bi-info-circle")
                ]
            };
        }

        if (contiene(texto, ["mis citas", "mi cita", "reprogram", "cancelar", "indicacion", "receta", "medicamento", "documento", "laboratorio", "notificacion", "mi perfil"])) {
            if (authenticated && patientSession) {
                return {
                    mensaje: "Todo eso está reunido en tu portal del paciente: citas, indicaciones médicas, documentos clínicos, notificaciones y perfil.",
                    acciones: [accion("Abrir mi portal", "/paciente/inicio", "bi-grid")]
                };
            }
            return accesoPersonal("Esas funciones pertenecen al portal privado del paciente para proteger la información clínica.");
        }

        if (contiene(texto, ["direccion", "ubicacion", "telefono", "correo", "contacto", "horario de atencion", "donde queda"])) {
            return {
                mensaje: "Encontrás la ubicación, el teléfono, el correo y el horario general del centro en la sección de contacto.",
                acciones: [accion("Ver contacto", "/#contacto", "bi-geo-alt")]
            };
        }

        if (contiene(texto, ["dolor", "urgencia", "emergencia", "diagnostico", "sintoma", "me siento mal", "medicina para"])) {
            return {
                mensaje: "No puedo evaluar síntomas, recomendar medicamentos ni atender emergencias. Si existe una urgencia, contactá al servicio de emergencias o acudí al centro asistencial más cercano.",
                acciones: [accion("Ver información de contacto", "/#contacto", "bi-telephone")]
            };
        }

        return {
            mensaje: "Puedo orientarte sobre registro, inicio de sesión, médicos, especialidades, citas, disponibilidad, pagos y contacto. Contame con otras palabras qué necesitás hacer.",
            acciones: [
                accion("Quiero agendar", authenticated && patientSession ? "/mis-citas?nueva=1" : "/login", "bi-calendar-plus"),
                accion("Soy paciente nuevo", "/registro", "bi-person-plus")
            ]
        };
    }

    function crearEnlace(item) {
        var enlace = document.createElement("a");
        enlace.className = "sora-result-link";
        enlace.href = item.ruta.indexOf("/#") === 0
            ? contextPath + item.ruta.substring(1)
            : contextPath + item.ruta;
        if (item.icono) {
            var icono = document.createElement("i");
            icono.className = "bi " + item.icono;
            icono.setAttribute("aria-hidden", "true");
            enlace.appendChild(icono);
        }
        enlace.appendChild(document.createTextNode(item.etiqueta));
        return enlace;
    }

    function actualizarEstado(texto, modo) {
        if (!status || !statusText) return;
        status.dataset.mode = modo;
        statusText.textContent = texto;
    }

    function agregarAccionesRespuesta(mensaje, respuesta) {
        if (respuesta && respuesta.acciones && respuesta.acciones.length) {
            var acciones = document.createElement("div");
            acciones.className = "sora-result-actions";
            respuesta.acciones.forEach(function (item) {
                acciones.appendChild(crearEnlace(item));
            });
            mensaje.appendChild(acciones);
        }
    }

    function agregarMensaje(texto, tipo, respuesta) {
        var mensaje = document.createElement("div");
        mensaje.className = "sora-message sora-message-" + tipo;
        var contenido = document.createElement("span");
        contenido.className = "sora-message-copy";
        contenido.textContent = texto;
        mensaje.appendChild(contenido);
        agregarAccionesRespuesta(mensaje, respuesta);

        conversation.appendChild(mensaje);
        conversation.scrollTop = conversation.scrollHeight;
        return {elemento: mensaje, contenido: contenido};
    }

    function mostrarEscribiendo() {
        actualizarEstado("Pensando con IA...", "thinking");
        var indicador = document.createElement("div");
        indicador.className = "sora-message sora-message-bot sora-typing";
        indicador.setAttribute("aria-label", "Sora está escribiendo");
        indicador.innerHTML = "<span></span><span></span><span></span>";
        conversation.appendChild(indicador);
        conversation.scrollTop = conversation.scrollHeight;
        return indicador;
    }

    function requiereRespuestaLocal(consulta) {
        var texto = normalizar(consulta);
        return contiene(texto, [
            "dolor", "urgencia", "emergencia", "diagnostico", "sintoma",
            "me siento mal", "medicina para", "medicamento para",
            "dificultad para respirar", "desmayo", "sangrado"
        ]);
    }

    function registrarTurno(consulta, respuesta) {
        conversationHistory.push({rol: "usuario", contenido: consulta});
        conversationHistory.push({rol: "asistente", contenido: respuesta});
        if (conversationHistory.length > 6) {
            conversationHistory.splice(0, conversationHistory.length - 6);
        }
    }

    function completarRespuesta(indicador, consulta, respuesta) {
        indicador.remove();
        var registro = agregarMensaje("", "bot");
        var mensaje = respuesta.mensaje;
        var reducirMovimiento = window.matchMedia
            && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
        var posicion = 0;
        var salto = Math.max(1, Math.ceil(mensaje.length / 180));

        registro.elemento.classList.add("is-writing");

        function finalizar() {
            registro.contenido.textContent = mensaje;
            registro.elemento.classList.remove("is-writing");
            agregarAccionesRespuesta(registro.elemento, respuesta);
            registrarTurno(consulta, mensaje);
            actualizarEstado(
                respuesta.modo === "ia"
                    ? "Conectada con OpenAI"
                    : "Orientación local disponible",
                respuesta.modo === "ia" ? "ready" : "local"
            );
            pendingResponse = false;
            submitButton.disabled = false;
            conversation.scrollTop = conversation.scrollHeight;
            input.focus();
        }

        if (reducirMovimiento) {
            finalizar();
            return;
        }

        function escribir() {
            posicion = Math.min(mensaje.length, posicion + salto);
            registro.contenido.textContent = mensaje.substring(0, posicion);
            conversation.scrollTop = conversation.scrollHeight;
            if (posicion < mensaje.length) {
                window.setTimeout(escribir, 12);
            } else {
                finalizar();
            }
        }

        escribir();
    }

    function responderLocal(indicador, consulta) {
        window.setTimeout(function () {
            var respuesta = resolver(consulta);
            respuesta.modo = "local";
            completarRespuesta(indicador, consulta, respuesta);
        }, 320);
    }

    function consultarSora(indicador, consulta) {
        var respuestaLocal = resolver(consulta);
        var controlador = new AbortController();
        var tiempoLimite = window.setTimeout(function () {
            controlador.abort();
        }, 40000);

        fetch(contextPath + "/api/sora/chat", {
            method: "POST",
            credentials: "same-origin",
            headers: {
                "Accept": "application/json",
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                mensaje: consulta,
                historial: conversationHistory.slice(-6)
            }),
            signal: controlador.signal
        }).then(function (respuesta) {
            if (!respuesta.ok) throw new Error("Sora no disponible");
            return respuesta.json();
        }).then(function (datos) {
            if (!datos || typeof datos.respuesta !== "string"
                    || !datos.respuesta.trim()) {
                throw new Error("Respuesta vacía");
            }
            completarRespuesta(indicador, consulta, {
                mensaje: datos.respuesta.trim(),
                acciones: respuestaLocal.acciones || [],
                modo: datos.modo === "ia" ? "ia" : "local"
            });
        }).catch(function () {
            responderLocal(indicador, consulta);
        }).finally(function () {
            window.clearTimeout(tiempoLimite);
        });
    }

    function preguntar(consulta) {
        var texto = (consulta || "").trim();
        if (!texto || pendingResponse) return;
        pendingResponse = true;
        submitButton.disabled = true;
        agregarMensaje(texto, "user");
        if (quickActions) quickActions.hidden = true;

        var indicador = mostrarEscribiendo();
        if (requiereRespuestaLocal(texto)) {
            responderLocal(indicador, texto);
            return;
        }
        consultarSora(indicador, texto);
    }

    function abrir() {
        panel.hidden = false;
        launcher.setAttribute("aria-expanded", "true");
        window.setTimeout(function () { input.focus(); }, 80);
    }

    function cerrar() {
        panel.hidden = true;
        launcher.setAttribute("aria-expanded", "false");
        launcher.focus();
    }

    launcher.addEventListener("click", function () {
        if (panel.hidden) abrir(); else cerrar();
    });
    closeButton.addEventListener("click", cerrar);
    form.addEventListener("submit", function (event) {
        event.preventDefault();
        var consulta = input.value;
        input.value = "";
        preguntar(consulta);
    });
    assistant.querySelectorAll("[data-sora-query]").forEach(function (button) {
        button.addEventListener("click", function () {
            preguntar(button.getAttribute("data-sora-query"));
        });
    });
    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && !panel.hidden) cerrar();
    });
}());
