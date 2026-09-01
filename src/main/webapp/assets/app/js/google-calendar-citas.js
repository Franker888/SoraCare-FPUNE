(function () {
    "use strict";

    const ALCANCE_CALENDAR = "https://www.googleapis.com/auth/calendar.events";
    const API_EVENTOS = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
    const ZONA_HORARIA = "America/Asuncion";
    const CLAVE_CONSENTIMIENTO = "soracare.googleCalendarConsent";
    const MARGEN_EXPIRACION_MS = 60 * 1000;
    let tokenActivo = null;
    let tokenExpiraEn = 0;
    let solicitudTokenEnCurso = null;

    function bibliotecaDisponible() {
        return Boolean(window.google
            && window.google.accounts
            && window.google.accounts.oauth2);
    }

    function claveGuardada(idCita) {
        return "soracare.googleCalendar.cita." + idCita;
    }

    function fechaRfc3339(valor, etiqueta) {
        const fecha = new Date(valor);
        if (Number.isNaN(fecha.getTime())) {
            throw new Error("La " + etiqueta + " de la cita no tiene un formato válido para Google Calendar.");
        }
        return fecha.toISOString();
    }

    function mostrarEstado(contenedor, mensaje, tipo) {
        const estado = contenedor.querySelector("[data-calendar-status]");
        estado.textContent = mensaje;
        estado.classList.remove("is-success", "is-error", "is-info");
        estado.classList.add("is-" + tipo);
    }

    function bloquearBotones(contenedor, bloqueados) {
        contenedor.querySelectorAll("button").forEach(function (boton) {
            boton.disabled = bloqueados;
        });
    }

    function solicitarAcceso(clientId, loginHint) {
        if (tokenActivo && Date.now() < tokenExpiraEn) {
            return Promise.resolve(tokenActivo);
        }
        if (solicitudTokenEnCurso) {
            return solicitudTokenEnCurso;
        }

        solicitudTokenEnCurso = new Promise(function (resolve, reject) {
            if (!bibliotecaDisponible()) {
                reject(new Error("Google todavía no terminó de cargar. Esperá unos segundos e intentá nuevamente."));
                return;
            }

            const configuracion = {
                client_id: clientId,
                scope: ALCANCE_CALENDAR,
                callback: function (respuesta) {
                    if (respuesta && respuesta.access_token) {
                        const duracionSegundos = Number(respuesta.expires_in) || 3600;
                        tokenActivo = respuesta.access_token;
                        tokenExpiraEn = Date.now()
                            + duracionSegundos * 1000
                            - MARGEN_EXPIRACION_MS;
                        window.localStorage.setItem(CLAVE_CONSENTIMIENTO, "1");
                        resolve(tokenActivo);
                        return;
                    }
                    reject(new Error("Google no autorizó el acceso al calendario."));
                },
                error_callback: function () {
                    reject(new Error("La autorización de Google se cerró o fue cancelada."));
                }
            };
            if (loginHint) {
                configuracion.login_hint = loginHint;
            }
            const cliente = window.google.accounts.oauth2.initTokenClient(configuracion);

            const yaAutorizado = window.localStorage.getItem(CLAVE_CONSENTIMIENTO) === "1";
            cliente.requestAccessToken({prompt: yaAutorizado ? "" : "consent"});
        });
        return solicitudTokenEnCurso.finally(function () {
            solicitudTokenEnCurso = null;
        });
    }

    function datosEvento(contenedor) {
        return {
            summary: contenedor.dataset.summary,
            description: "Profesional: " + contenedor.dataset.professional
                + "\nConsulta programada desde SoraCare-FPUNE.",
            location: contenedor.dataset.location,
            start: {
                dateTime: fechaRfc3339(contenedor.dataset.start, "fecha de inicio"),
                timeZone: ZONA_HORARIA
            },
            end: {
                dateTime: fechaRfc3339(contenedor.dataset.end, "fecha de finalización"),
                timeZone: ZONA_HORARIA
            },
            reminders: {useDefault: true},
            extendedProperties: {
                private: {soracareCitaId: contenedor.dataset.citaId}
            }
        };
    }

    async function respuestaJson(respuesta) {
        const texto = await respuesta.text();
        if (!texto) return null;
        try {
            return JSON.parse(texto);
        } catch (error) {
            return null;
        }
    }

    function mensajeApi(respuesta, cuerpo) {
        if (respuesta.status === 403) {
            return "Google Calendar todavía no está habilitado para el proyecto o no concediste el permiso.";
        }
        if (respuesta.status === 401) {
            tokenActivo = null;
            tokenExpiraEn = 0;
            window.localStorage.removeItem(CLAVE_CONSENTIMIENTO);
            return "La autorización de Google venció. Volvé a intentarlo para renovarla.";
        }
        if (cuerpo && cuerpo.error) {
            const detalle = Array.isArray(cuerpo.error.errors)
                ? cuerpo.error.errors[0]
                : null;
            if (cuerpo.error.message && cuerpo.error.message !== "Bad Request") {
                return cuerpo.error.message;
            }
            if (detalle && detalle.message && detalle.message !== "Bad Request") {
                return detalle.message;
            }
            if (detalle && detalle.reason) {
                return "Google rechazó los datos del evento (" + detalle.reason + ").";
            }
        }
        return "Google Calendar rechazó los datos del evento. Actualizá la página e intentá nuevamente.";
    }

    async function consultarEvento(contenedor, token) {
        const cabeceras = {Authorization: "Bearer " + token};
        const claveLocal = claveGuardada(contenedor.dataset.citaId);
        const idLocal = window.localStorage.getItem(claveLocal);

        if (idLocal && idLocal !== "1") {
            const respuestaGuardada = await fetch(
                API_EVENTOS + "/" + encodeURIComponent(idLocal),
                {headers: cabeceras}
            );
            if (respuestaGuardada.ok) {
                return await respuestaJson(respuestaGuardada);
            }
            if (respuestaGuardada.status !== 404) {
                const cuerpoGuardado = await respuestaJson(respuestaGuardada);
                throw new Error(mensajeApi(respuestaGuardada, cuerpoGuardado));
            }
            window.localStorage.removeItem(claveLocal);
        }

        const parametros = new URLSearchParams({
            privateExtendedProperty: "soracareCitaId=" + contenedor.dataset.citaId,
            maxResults: "1",
            singleEvents: "true",
            showDeleted: "false"
        });
        const respuestaBusqueda = await fetch(
            API_EVENTOS + "?" + parametros.toString(),
            {headers: cabeceras}
        );
        const cuerpoBusqueda = await respuestaJson(respuestaBusqueda);
        if (!respuestaBusqueda.ok) {
            throw new Error(mensajeApi(respuestaBusqueda, cuerpoBusqueda));
        }

        const evento = cuerpoBusqueda && Array.isArray(cuerpoBusqueda.items)
            ? cuerpoBusqueda.items[0]
            : null;
        if (evento && evento.id) {
            window.localStorage.setItem(claveLocal, evento.id);
        }
        return evento || null;
    }

    async function guardarEvento(contenedor, token) {
        const evento = datosEvento(contenedor);
        const existente = await consultarEvento(contenedor, token);
        let respuesta;
        if (existente && existente.id) {
            const urlEvento = API_EVENTOS + "/" + encodeURIComponent(existente.id);
            respuesta = await fetch(urlEvento, {
                method: "PUT",
                headers: {
                    Authorization: "Bearer " + token,
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(evento)
            });
        } else {
            respuesta = await fetch(API_EVENTOS, {
                method: "POST",
                headers: {
                    Authorization: "Bearer " + token,
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(evento)
            });
        }

        const cuerpo = await respuestaJson(respuesta);
        if (!respuesta.ok) throw new Error(mensajeApi(respuesta, cuerpo));
        window.localStorage.setItem(
            claveGuardada(contenedor.dataset.citaId),
            cuerpo.id
        );
    }

    async function retirarEvento(contenedor, token) {
        const evento = await consultarEvento(contenedor, token);
        if (!evento || !evento.id) {
            window.localStorage.removeItem(claveGuardada(contenedor.dataset.citaId));
            return;
        }
        const respuesta = await fetch(API_EVENTOS + "/" + encodeURIComponent(evento.id), {
            method: "DELETE",
            headers: {Authorization: "Bearer " + token}
        });

        if (!respuesta.ok && respuesta.status !== 404) {
            const cuerpo = await respuestaJson(respuesta);
            throw new Error(mensajeApi(respuesta, cuerpo));
        }
        window.localStorage.removeItem(claveGuardada(contenedor.dataset.citaId));
    }

    async function ejecutar(contenedor, accion) {
        bloquearBotones(contenedor, true);
        const reutilizando = tokenActivo && Date.now() < tokenExpiraEn;
        mostrarEstado(
            contenedor,
            reutilizando
                ? "Actualizando tu Google Calendar…"
                : "Verificando el permiso de Google Calendar…",
            "info"
        );
        try {
            const token = await solicitarAcceso(
                contenedor.dataset.clientId,
                contenedor.dataset.loginHint
            );
            if (accion === "guardar") {
                await guardarEvento(contenedor, token);
                const etiqueta = contenedor.querySelector("[data-calendar-save] span");
                etiqueta.textContent = "Actualizar Calendar";
                mostrarEstado(contenedor, "La cita quedó guardada en tu Google Calendar.", "success");
            } else {
                await retirarEvento(contenedor, token);
                const etiqueta = contenedor.querySelector("[data-calendar-save] span");
                etiqueta.textContent = "Guardar en Calendar";
                mostrarEstado(contenedor, "La cita fue retirada de tu Google Calendar.", "success");
            }
        } catch (error) {
            mostrarEstado(
                contenedor,
                error instanceof Error ? error.message : "No se pudo conectar con Google Calendar.",
                "error"
            );
        } finally {
            bloquearBotones(contenedor, false);
        }
    }

    document.querySelectorAll("[data-google-calendar]").forEach(function (contenedor) {
        const idGuardado = window.localStorage.getItem(claveGuardada(contenedor.dataset.citaId));
        const consentimientoPrevio = window.localStorage.getItem(CLAVE_CONSENTIMIENTO) === "1";
        if (idGuardado && idGuardado !== "1") {
            contenedor.querySelector("[data-calendar-save] span").textContent = "Actualizar Calendar";
            mostrarEstado(contenedor, "Esta cita ya fue guardada desde este navegador.", "info");
        } else if (idGuardado === "1") {
            window.localStorage.removeItem(claveGuardada(contenedor.dataset.citaId));
        } else if (!consentimientoPrevio) {
            contenedor.querySelector("[data-calendar-save] span").textContent = "Conectar y guardar";
        }

        contenedor.querySelector("[data-calendar-save]").addEventListener("click", function () {
            ejecutar(contenedor, "guardar");
        });
        contenedor.querySelector("[data-calendar-remove]").addEventListener("click", function () {
            ejecutar(contenedor, "retirar");
        });
    });
}());
