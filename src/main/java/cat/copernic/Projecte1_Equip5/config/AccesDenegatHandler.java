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
    // Es una herramienta para escribir mensajes en la consola o en un fichero de registro (los logs), 
    //mejor que System.out.println porque permite niveles (info, warn, error) y dice de qué clase viene cada mensaje.
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
        // Paso 1: averiguar quién es el usuario
        String usuari = request.getUserPrincipal() != null
                ? request.getUserPrincipal().getName() : "anónimo";
        
        // Paso 2: dejar constancia en el log
        LOG.warn("Acceso denegado: usuario={}, ruta={}", usuari, request.getRequestURI());
        response.sendRedirect(request.getContextPath() + "/acces-denegat");
    }
}

