package cat.copernic.Projecte1_Equip5.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Registra en el log los accesos denegados y redirige a una
 * página informativa para el usuario.
 */
@Component
public class AccesDenegatHandler implements AccessDeniedHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AccesDenegatHandler.class);

    /**
     * Se ejecuta cuando un usuario autenticado intenta entrar en una ruta
     * para la que no tiene permiso.
     *
     * @param request petición recibida
     * @param response respuesta al usuario
     * @param ex excepción de acceso denegado
     * @throws IOException si falla la redirección
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        String usuari = request.getUserPrincipal() != null
                ? request.getUserPrincipal().getName() : "anónimo";
        LOG.warn("Acceso denegado: usuario={}, ruta={}", usuari, request.getRequestURI());
        response.sendRedirect(request.getContextPath() + "/acces-denegat");
    }
}
