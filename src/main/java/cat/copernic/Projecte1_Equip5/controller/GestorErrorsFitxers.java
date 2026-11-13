package cat.copernic.Projecte1_Equip5.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Gestió global de fitxers massa grans. Spring els rebutja (límits de
 * spring.servlet.multipart a application.properties) abans d'arribar al
 * controlador, així que cap validació del servei ho pot capturar. Aquí es torna
 * a la pàgina d'origen amb un missatge en comptes de mostrar una pàgina
 * d'error.
 */
// Prioridad máxima: así este gestor (fichero demasiado grande) se consulta antes que
// el gestor global GestorErrorsGlobal, que captura el resto de errores.
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GestorErrorsFitxers {

    /**
     * Mida màxima per fitxer, llegida d'application.properties perquè el
     * missatge sempre coincideixi amb el límit real.
     */
    @Value("${spring.servlet.multipart.max-file-size:5MB}")
    private String maxFitxer;

    /**
     * Captura l'error de fitxer massa gran i torna a la pàgina d'origen amb un
     * missatge.
     *
     * @param request petició que ha fallat (se'n treu la pàgina d'origen)
     * @param redirect atributs per passar el missatge d'error
     * @return redirecció a la pàgina d'origen
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String fitxerMassaGran(HttpServletRequest request, RedirectAttributes redirect) {
        long mb = DataSize.parse(maxFitxer).toMegabytes();
        redirect.addFlashAttribute("error",
                "El fitxer és massa gran. Només s'admeten fitxers amb un màxim de " + mb + " MB.");
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
