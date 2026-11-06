package cat.copernic.Projecte1_Equip5.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Gestió global de fitxers massa grans. Spring els rebutja (límits de
 * spring.servlet.multipart a application.properties) abans d'arribar al
 * controlador, així que cap validació del servei ho pot capturar. Aquí es torna
 * a la pàgina d'origen amb un missatge en comptes de mostrar una pàgina d'error.
 */
@ControllerAdvice
public class GestorErrorsFitxers {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String fitxerMassaGran(HttpServletRequest request, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", "Els fitxers no poden superar els 5 MB.");
        return "redirect:" + rutaOrigen(request);
    }

    /**
     * Ruta (sense domini) de la pàgina d'on ve la petició. Només s'usa el path
     * i la query, mai el host, per evitar redireccions a webs externes.
     */
    private String rutaOrigen(HttpServletRequest request) {
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (referer == null || referer.isBlank()) {
            return "/";
        }
        try {
            URI uri = URI.create(referer);
            String path = uri.getRawPath();
            if (path == null || path.isEmpty() || path.startsWith("//")) {
                return "/";
            }
            return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
        } catch (IllegalArgumentException e) {
            return "/";
        }
    }
}
 