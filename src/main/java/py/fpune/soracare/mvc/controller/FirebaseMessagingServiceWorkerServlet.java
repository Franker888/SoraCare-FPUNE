package py.fpune.soracare.mvc.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/firebase-messaging-sw.js")
public class FirebaseMessagingServiceWorkerServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/javascript;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store, max-age=0");
        response.setHeader("Service-Worker-Allowed", request.getContextPath() + "/");

        response.getWriter().write("""
                const CONTEXTO_SORACARE = '%s';
                const RUTA_AVISOS = CONTEXTO_SORACARE + '/mis-notificaciones';

                function destinoSeguro(valor) {
                  const respaldo = new URL(RUTA_AVISOS, self.location.origin);
                  try {
                    const candidato = new URL(valor || respaldo.href, self.location.origin);
                    const prefijo = CONTEXTO_SORACARE + '/';
                    if (candidato.origin !== self.location.origin
                        || !candidato.pathname.startsWith(prefijo)) {
                      return respaldo.href;
                    }
                    return candidato.href;
                  } catch (error) {
                    return respaldo.href;
                  }
                }

                function esVentanaSoracare(ventana) {
                  try {
                    const ubicacion = new URL(ventana.url);
                    return ubicacion.origin === self.location.origin
                      && ubicacion.pathname.startsWith(CONTEXTO_SORACARE + '/');
                  } catch (error) {
                    return false;
                  }
                }

                self.addEventListener('install', () => {
                  self.skipWaiting();
                });

                self.addEventListener('activate', (event) => {
                  event.waitUntil(self.clients.claim());
                });

                self.addEventListener('push', (event) => {
                  let recibido = {};
                  if (event.data) {
                    try {
                      recibido = event.data.json();
                    } catch (error) {
                      recibido = {};
                    }
                  }

                  const mensaje = recibido.data && recibido.data.FCM_MSG
                    ? recibido.data.FCM_MSG
                    : recibido;
                  const notification = mensaje.notification || {};
                  const datos = mensaje.data || {};
                  const title = notification.title || datos.title || 'Nuevo aviso de SoraCare';
                  const options = {
                    body: notification.body || datos.body || 'Ingresá a SoraCare para consultar el aviso.',
                    icon: CONTEXTO_SORACARE + '/assets/home/img/logo/SoraCare.png',
                    badge: CONTEXTO_SORACARE + '/assets/home/img/logo/SoraCare.png',
                    data: {
                      url: destinoSeguro(datos.url)
                    },
                    tag: datos.tag || 'soracare-aviso'
                  };
                  event.waitUntil(self.registration.showNotification(title, options));
                });

                self.addEventListener('notificationclick', (event) => {
                  event.notification.close();
                  const datos = event.notification.data || {};
                  const destino = destinoSeguro(datos.url);
                  event.waitUntil(
                    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((ventanas) => {
                      for (const ventana of ventanas) {
                        if (esVentanaSoracare(ventana) && 'focus' in ventana) {
                          return ventana.navigate(destino).then(() => ventana.focus());
                        }
                      }
                      return clients.openWindow(destino);
                    })
                  );
                });
                """.formatted(
                js(request.getContextPath())
        ));
    }

    private static String js(String valor) {
        return valor == null ? "" : valor
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\u2028", "\\u2028")
                .replace("\u2029", "\\u2029");
    }
}
