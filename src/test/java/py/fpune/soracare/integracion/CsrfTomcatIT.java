package py.fpune.soracare.integracion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;
import py.fpune.soracare.security.FiltroCsrfFormularios;
import py.fpune.soracare.security.ProteccionCsrf;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.jar.*;
import static org.junit.jupiter.api.Assertions.*;

class CsrfTomcatIT {
    @TempDir Path temporal;

    @Test void validaFormulariosMultipartYErrorRealSinBaseNiIntegraciones() throws Exception {
        Path war=temporal.resolve("csrf-prueba.war");
        try(var zip=new JarOutputStream(Files.newOutputStream(war))) {
            for(Class<?> clase:new Class<?>[]{FiltroCsrfFormularios.class,ProteccionCsrf.class,CsrfPruebaServlet.class}) {
                String ruta=clase.getName().replace('.','/')+".class";
                try(var entrada=clase.getResourceAsStream("/"+ruta)) {
                    assertNotNull(entrada);
                    entrada(zip,"WEB-INF/classes/"+ruta,entrada.readAllBytes());
                }
            }
            entrada(zip,"WEB-INF/web.xml",("""
                    <web-app xmlns="https://jakarta.ee/xml/ns/jakartaee" version="6.0" metadata-complete="true">
                      <filter><filter-name>csrf</filter-name><filter-class>%s</filter-class></filter>
                      <filter-mapping><filter-name>csrf</filter-name><url-pattern>/*</url-pattern></filter-mapping>
                      <servlet><servlet-name>prueba</servlet-name><servlet-class>%s</servlet-class>
                        <multipart-config><max-file-size>128</max-file-size><max-request-size>2048</max-request-size></multipart-config>
                      </servlet>
                      <servlet-mapping><servlet-name>prueba</servlet-name><url-pattern>/form</url-pattern></servlet-mapping>
                      <error-page><error-code>403</error-code><location>/error.jsp</location></error-page>
                    </web-app>
                    """).formatted(FiltroCsrfFormularios.class.getName(),CsrfPruebaServlet.class.getName())
                    .getBytes(StandardCharsets.UTF_8));
            entrada(zip,"error.jsp",Files.readAllBytes(Path.of("src/main/webapp/WEB-INF/views/errores/403.jsp")));
        }
        try(var tomcat=new GenericContainer<>("tomcat:10.1.57-jre21-temurin-noble")
                .withExposedPorts(8080)
                .withCopyFileToContainer(MountableFile.forHostPath(war),"/usr/local/tomcat/webapps/ROOT.war")
                .waitingFor(Wait.forHttp("/form").forStatusCode(200))
                .withStartupTimeout(Duration.ofSeconds(90))) {
            tomcat.start();
            URI uri=URI.create("http://"+tomcat.getHost()+":"+tomcat.getMappedPort(8080)+"/form");
            var cliente=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
            String token=cliente.send(HttpRequest.newBuilder(uri).GET().build(),HttpResponse.BodyHandlers.ofString()).body();
            assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
            var sinToken=post(cliente,uri,"application/x-www-form-urlencoded","accion=prueba");
            assertEquals(403,sinToken.statusCode());
            assertTrue(sinToken.body().contains("Necesitamos confirmar tu solicitud"));
            assertFalse(sinToken.body().contains("Este sector no corresponde a tu acceso"));
            assertEquals(200,post(cliente,uri,"application/x-www-form-urlencoded","csrfToken="+token).statusCode());
            var archivo=post(cliente,uri,"multipart/form-data; boundary=CsrfTest",multipart(token,"hola"));
            assertEquals(200,archivo.statusCode());
            assertEquals("ñandú:4",archivo.body());
            assertEquals(403,post(cliente,uri,"multipart/form-data; boundary=CsrfTest",multipart("incorrecto","hola")).statusCode());
            assertEquals(413,post(cliente,uri,"multipart/form-data; boundary=CsrfTest",multipart(token,"x".repeat(256))).statusCode());
            // Tras el rechazo por tamaño, el formulario válido sigue funcionando.
            assertEquals(200,post(cliente,uri,"application/x-www-form-urlencoded","csrfToken="+token).statusCode());
        }
    }

    private static String multipart(String token,String archivo) {
        return "--CsrfTest\r\nContent-Disposition: form-data; name=\"csrfToken\"\r\n\r\n"+token
                +"\r\n--CsrfTest\r\nContent-Disposition: form-data; name=\"texto\"\r\n\r\nñandú"
                +"\r\n--CsrfTest\r\nContent-Disposition: form-data; name=\"archivo\"; filename=\"prueba.txt\"\r\nContent-Type: text/plain\r\n\r\n"
                +archivo+"\r\n--CsrfTest--\r\n";
    }
    private static HttpResponse<String> post(HttpClient cliente,URI uri,String tipo,String cuerpo) throws Exception {
        return cliente.send(HttpRequest.newBuilder(uri).header("Content-Type",tipo)
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo,StandardCharsets.UTF_8)).build(),HttpResponse.BodyHandlers.ofString());
    }
    private static void entrada(JarOutputStream zip,String nombre,byte[] bytes) throws Exception {
        zip.putNextEntry(new JarEntry(nombre));zip.write(bytes);zip.closeEntry();
    }
}
