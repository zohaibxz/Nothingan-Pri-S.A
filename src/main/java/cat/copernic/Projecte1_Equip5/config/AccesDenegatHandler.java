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
 * Manejador de accesos denegados. Cuando un usuario autenticado intenta entrar
 * en una ruta para la que su rol no tiene permiso, deja constancia en el log y
 * lo redirige a una página informativa ({@code /acces-denegat}). Lo registra
 * {@link WebSecurityConfig} como manejador de excepciones de seguridad.
 */
@Component
public class AccesDenegatHandler implements AccessDeniedHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AccesDenegatHandler.class);

    // Es una herramienta para escribir mensajes en la consola o en un fichero de registro (los logs), 
    //mejor que System.out.println porque permite niveles (info, warn, error) y dice de qué clase viene cada mensaje.
    /**
     * Se ejecuta cuando un usuario autenticado intenta entrar en una ruta para
     * la que no tiene permiso. Escribe en el log un aviso con el usuario (o
     * "anónimo") y la ruta pedida, y redirige a la página de acceso denegado.
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
        // RN53: el correu no s'escriu sencer al log
        LOG.warn("Acceso denegado: usuario={}, ruta={}",
                request.getUserPrincipal() != null ? CustomAuthenticationProvider.emmascarar(usuari) : usuari,
                request.getRequestURI());
        response.sendRedirect(request.getContextPath() + "/acces-denegat");
    }
}