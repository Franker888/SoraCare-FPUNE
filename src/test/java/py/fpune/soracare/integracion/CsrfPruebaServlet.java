package py.fpune.soracare.integracion;

import jakarta.servlet.http.*;
import jakarta.servlet.ServletException;
import java.io.IOException;

/** Solo se empaqueta en el WAR efímero de pruebas, nunca en SoraCare. */
public class CsrfPruebaServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse s) throws IOException {
        s.setContentType("text/plain;charset=UTF-8");
        s.getWriter().write((String)r.getAttribute("csrfToken"));
    }
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse s)
            throws IOException,ServletException {
        s.setContentType("text/plain;charset=UTF-8");
        if(r.getContentType()!=null && r.getContentType().startsWith("multipart/")) {
            s.getWriter().write(r.getParameter("texto")+":"+r.getPart("archivo").getSize());
        } else s.getWriter().write("OK");
    }
}
