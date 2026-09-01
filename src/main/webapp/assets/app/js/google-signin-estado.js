(function () {
    "use strict";

    const TIEMPO_ESPERA_MS = 8000;

    function botonRenderizado(contenedor) {
        return Boolean(contenedor.querySelector(".g_id_signin iframe"));
    }

    function mostrarListo(contenedor) {
        const estado = contenedor.querySelector("[data-google-signin-status]");
        const reintentar = contenedor.querySelector("[data-google-signin-retry]");
        if (estado) estado.hidden = true;
        if (reintentar) reintentar.hidden = true;
    }

    function mostrarError(contenedor) {
        if (botonRenderizado(contenedor)) {
            mostrarListo(contenedor);
            return;
        }
        const estado = contenedor.querySelector("[data-google-signin-status]");
        const reintentar = contenedor.querySelector("[data-google-signin-retry]");
        if (estado) {
            estado.hidden = false;
            estado.textContent = "Google no terminó de cargar. Revisá tu conexión o bloqueador de contenido.";
            estado.classList.add("is-error");
        }
        if (reintentar) reintentar.hidden = false;
    }

    document.querySelectorAll("[data-google-signin-provider]").forEach(function (contenedor) {
        const observador = new MutationObserver(function () {
            if (botonRenderizado(contenedor)) {
                mostrarListo(contenedor);
                observador.disconnect();
            }
        });
        observador.observe(contenedor, {childList: true, subtree: true});

        const reintentar = contenedor.querySelector("[data-google-signin-retry]");
        if (reintentar) {
            reintentar.addEventListener("click", function () {
                window.location.reload();
            });
        }

        if (botonRenderizado(contenedor)) {
            mostrarListo(contenedor);
            observador.disconnect();
            return;
        }
        window.setTimeout(function () {
            mostrarError(contenedor);
            observador.disconnect();
        }, TIEMPO_ESPERA_MS);
    });
}());
