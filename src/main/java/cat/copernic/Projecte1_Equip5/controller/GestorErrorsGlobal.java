package cat.copernic.Projecte1_Equip5.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * RN21 i RN20: gestió global d'excepcions. Qualsevol error que els
 * controladors no hagin capturat acaba aquí i l'usuari rep un missatge en
 * català, adaptat a què ha passat i amb què pot fer a continuació, en lloc de
 * la pàgina d'error per defecte de Spring (que mostra "Whitelabel Error Page"
 * o dades tècniques).
 * <ul>
 * <li>Errors de les dades enviades (camp que falta, valor amb tipus
 * incorrecte, data mal escrita...): si venen d'un formulari (POST) es torna a
 * la mateixa pàgina amb el missatge; si no, es mostra la pàgina d'error.</li>
 * <li>Errors de la base de dades, concurrència i serveis caiguts: missatge
 * adaptat i, si l'error és inesperat, un codi d'incidència que apareix també
 * al log per poder-lo localitzar. Mai es mostra la traça tècnica.</li>
 * </ul>
 * Els errors de seguretat (accés denegat / no autenticat) es tornen a llançar
 * perquè els gestioni Spring Security ({@link AccesDenegatHandler} i el
 * login). Té prioritat baixa: {@link GestorErrorsFitxers} (fitxer massa gran)
 * es consulta abans.
 */
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GestorErrorsGlobal {

    private static final Logger log = LoggerFactory.getLogger(GestorErrorsGlobal.class);
    private static final int MAX_MISSATGE = 300;

    // =====================================================================
    // RN20: dades enviades per l'usuari que no són correctes
    // =====================================================================
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public String campObligatori(MissingServletRequestParameterException ex, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirect) {
        return respondre(request, response, model, redirect, HttpStatus.BAD_REQUEST, "Falten dades",
                "Falta el camp obligatori «" + nomCamp(ex.getParameterName()) + "».",
                "Omple tots els camps obligatoris i torna-ho a provar.", null, true);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public String fitxerObligatori(MissingServletRequestPartException ex, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirect) {
        return respondre(request, response, model, redirect, HttpStatus.BAD_REQUEST, "Falta un fitxer",
                "Cal adjuntar el fitxer «" + nomCamp(ex.getRequestPartName()) + "».",
                "Selecciona el fitxer (JPG, PNG o PDF de màxim 5 MB) i torna-ho a enviar.", null, true);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, TypeMismatchException.class})
    public String valorIncorrecte(TypeMismatchException ex, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirect) {
        String camp = ex instanceof MethodArgumentTypeMismatchException m ? m.getName() : ex.getPropertyName();
        return respondre(request, response, model, redirect, HttpStatus.BAD_REQUEST, "Dada incorrecta",
                "El valor del camp «" + nomCamp(camp) + "» no té un format vàlid.",
                "Revisa que les dates i els números estiguin ben escrits (per exemple, data en format dd/mm/aaaa).",
                null, true);
    }

    @ExceptionHandler(BindException.class)
    public String formulariNoValid(BindException ex, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirect) {
        String camp = ex.getFieldError() != null ? ex.getFieldError().getField() : null;
        String missatge = camp != null
                ? "El camp «" + nomCamp(camp) + "» no és vàlid."
                : "Alguna de les dades del formulari no és vàlida.";
        return respondre(request, response, model, redirect, HttpStatus.BAD_REQUEST, "Dades no vàlides",
                missatge, "Revisa el formulari i torna-ho a provar.", null, true);
    }

    @ExceptionHandler(MultipartException.class)
    public String fitxerNoValid(MultipartException ex, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirect) {
        return respondre(request, response, model, redirect, HttpStatus.BAD_REQUEST, "Fitxer no vàlid",
                "No s'ha pogut rebre el fitxer que has pujat.",
                "Comprova que sigui un JPG, PNG o PDF de màxim 5 MB i torna-ho a provar.", null, true);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public String metodeNoPermes(HttpServletRequest request, HttpServletResponse response,
            Model model, RedirectAttributes redirect) {
        return respondre(request, response, model, redirect, HttpStatus.METHOD_NOT_ALLOWED, "Acció no permesa",
                "Aquesta acció no es pot fer d'aquesta manera.",
                "Torna a la pàgina anterior i utilitza els botons de l'aplicació.", null, false);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public String noTrobat(HttpServletRequest request, HttpServletResponse response,
            Model model, RedirectAttributes redirect) {
        return respondre(request, response, model, redirect, HttpStatus.NOT_FOUND, "Pàgina no trobada",
                "La pàgina que busques no existeix o ha canviat de lloc.",
                "Comprova l'adreça o torna a l'inici.", null, false);
    }

    // =====================================================================
    // Errors de negoci que cap controlador ha capturat
    // =====================================================================
    @ExceptionHandler(IllegalArgumentException.class)
    public String dadesNoValides(IllegalArgumentException ex, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirect) {
        return respondre(request, response, model, redirect, HttpStatus.BAD_REQUEST, "Dades no vàlides",
                missatgeSegur(ex, "Alguna de les dades enviades no és vàlida."),
                "Revisa les dades i torna-ho a provar.", null, true);
    }

    @ExceptionHandler(IllegalStateException.class)
    public String operacioNoPossible(IllegalStateException ex, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirect) {
        return respondre(request, response, model, redirect, HttpStatus.CONFLICT, "No es pot fer aquesta operació",
                missatgeSegur(ex, "L'operació no es pot fer en l'estat actual."),
                "Actualitza la pàgina per veure les dades més recents i torna-ho a provar.", null, true);
    }

    // =====================================================================
    // Base de dades i concurrència (RN21, RN01)
    // =====================================================================
    @ExceptionHandler(ConcurrencyFailureException.class)
    public String concurrencia(HttpServletRequest request, HttpServletResponse response,
            Model model, RedirectAttributes redirect) {
        return respondre(request, response, model, redirect, HttpStatus.CONFLICT, "Un altre usuari ho ha canviat",
                "Mentre feies aquesta operació, un altre usuari ha modificat les mateixes dades "
                + "(per exemple, ha reservat el mateix vehicle).",
                "Torna-ho a provar: veuràs les dades actualitzades.", null, true);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String integritatDades(DataIntegrityViolationException ex, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirect) {
        String ref = novaReferencia();
        log.warn("[{}] Violació d'integritat de dades: {}", ref, ex.getClass().getSimpleName());
        return respondre(request, response, model, redirect, HttpStatus.CONFLICT, "No s'han pogut desar les dades",
                "Alguna de les dades ja existeix o supera la longitud permesa.",
                "Revisa que no estiguis repetint un DNI, un correu o una matrícula i que els textos no siguin massa llargs.",
                ref, true);
    }

    @ExceptionHandler({DataAccessResourceFailureException.class, CannotCreateTransactionException.class})
    public String baseDeDadesNoDisponible(Exception ex, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirect) {
        String ref = novaReferencia();
        log.error("[{}] Base de dades no disponible", ref, ex);
        return respondre(request, response, model, redirect, HttpStatus.SERVICE_UNAVAILABLE, "Servei no disponible",
                "Ara mateix no podem accedir a les dades de l'aplicació.",
                "Espera uns minuts i torna-ho a provar. Si el problema continua, avisa l'administrador.",
                ref, false);
    }

    @ExceptionHandler(DataAccessException.class)
    public String errorBaseDeDades(DataAccessException ex, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirect) {
        String ref = novaReferencia();
        log.error("[{}] Error d'accés a dades", ref, ex);
        return respondre(request, response, model, redirect, HttpStatus.INTERNAL_SERVER_ERROR,
                "Error en gestionar les dades",
                "No s'ha pogut completar l'operació amb les dades.",
                "Torna-ho a provar. Si el problema continua, avisa l'administrador indicant el codi d'incidència.",
                ref, false);
    }

    // =====================================================================
    // Qualsevol altra cosa: mai es mostra la traça tècnica a l'usuari
    // =====================================================================
    @ExceptionHandler(Exception.class)
    public String errorInesperat(Exception ex, HttpServletRequest request, HttpServletResponse response,
            Model model, RedirectAttributes redirect) throws Exception {
        // Seguretat: els gestiona Spring Security (pàgina d'accés denegat o login)
        if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
            throw ex;
        }
        // Errors amb estat HTTP explícit (p. ex. 404 llançat expressament)
        if (ex instanceof ResponseStatusException rse) {
            HttpStatus estat = HttpStatus.resolve(rse.getStatusCode().value());
            return respondre(request, response, model, redirect,
                    estat != null ? estat : HttpStatus.INTERNAL_SERVER_ERROR,
                    estat != null ? estat.getReasonPhrase() : "Error",
                    missatgeSegur(rse.getReason(), "No s'ha pogut completar la petició."),
                    null, null, false);
        }
        // Altres errors de petició que Spring ja classifica com a 4xx (tipus de contingut, paràmetres...)
        if (ex instanceof ErrorResponse er && er.getStatusCode().is4xxClientError()) {
            HttpStatus estat = HttpStatus.resolve(er.getStatusCode().value());
            return respondre(request, response, model, redirect,
                    estat != null ? estat : HttpStatus.BAD_REQUEST, "Petició incorrecta",
                    "La petició enviada no és correcta o no es pot processar.",
                    "Torna a la pàgina anterior i torna-ho a provar.", null, true);
        }
        String ref = novaReferencia();
        log.error("[{}] Error inesperat a {} {}", ref, request.getMethod(), request.getRequestURI(), ex);
        return respondre(request, response, model, redirect, HttpStatus.INTERNAL_SERVER_ERROR, "Error inesperat",
                "Ha passat alguna cosa inesperada mentre es processava la teva petició. "
                + "Les teves dades no s'han perdut.",
                "Torna-ho a provar. Si el problema continua, avisa l'administrador indicant el codi d'incidència.",
                ref, false);
    }

    // =====================================================================
    // Utilitats
    // =====================================================================
    /**
     * Mostra l'error: si ve d'un formulari (POST) i es pot, torna a la pàgina
     * d'origen amb el missatge (així l'usuari no perd el context); si no,
     * mostra la pàgina error.html amb l'estat HTTP correcte.
     */
    private String respondre(HttpServletRequest request, HttpServletResponse response, Model model,
            RedirectAttributes redirect, HttpStatus estat, String titol, String missatge,
            String consell, String referencia, boolean tornarAlFormulari) {
        if (tornarAlFormulari && "POST".equalsIgnoreCase(request.getMethod())) {
            String ruta = rutaOrigen(request);
            if (ruta != null) {
                redirect.addFlashAttribute("error", consell == null ? missatge : missatge + " " + consell);
                return "redirect:" + ruta;
            }
        }
        response.setStatus(estat.value());
        model.addAttribute("codi", estat.value());
        model.addAttribute("titol", titol);
        model.addAttribute("missatge", missatge);
        model.addAttribute("consell", consell);
        model.addAttribute("referencia", referencia);
        return "error";
    }

    /**
     * Ruta (sense domini) de la pàgina d'on ve la petició, o null si no n'hi
     * ha. Només s'usa el path i la query, mai el host, per evitar redireccions
     * a webs externes.
     */
    private String rutaOrigen(HttpServletRequest request) {
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (referer == null || referer.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(referer);
            String path = uri.getRawPath();
            if (path == null || path.isEmpty() || path.startsWith("//") || path.startsWith("/error")) {
                return null;
            }
            return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Converteix un nom de paràmetre tècnic (dataCaducitatLlicencia) en text
     * llegible (data caducitat llicencia).
     */
    private String nomCamp(String nom) {
        if (nom == null || nom.isBlank()) {
            return "desconegut";
        }
        return nom.replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT);
    }

    private String novaReferencia() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String missatgeSegur(Throwable ex, String perDefecte) {
        return missatgeSegur(ex.getMessage(), perDefecte);
    }

    /**
     * Els serveis de l'aplicació llancen IllegalArgumentException i
     * IllegalStateException amb missatges pensats per a l'usuari. Si el
     * missatge sembla tècnic (de Spring o de Java) o és massa llarg, se'n
     * mostra un de genèric.
     */
    private String missatgeSegur(String missatge, String perDefecte) {
        if (missatge == null || missatge.isBlank() || missatge.length() > MAX_MISSATGE
                || missatge.contains("java.") || missatge.contains("org.")
                || missatge.contains("Exception") || missatge.contains("No enum constant")) {
            return perDefecte;
        }
        return missatge;
    }
}
