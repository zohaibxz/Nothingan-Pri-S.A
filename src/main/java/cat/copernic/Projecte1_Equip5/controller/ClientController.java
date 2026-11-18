package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.logic.PrefixosTelefon;
import cat.copernic.Projecte1_Equip5.logic.UsuariLogic;
import cat.copernic.Projecte1_Equip5.service.ClientService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador del registre (alta) de clients. És una pantalla pública; si qui
 * registra és un agent o un administrador també pot marcar el client com a
 * premium. La pantalla d'inici la gestiona DashBoardController.
 */
@Controller
public class ClientController {

    private final ClientService clientService;
    private final UsuariLogic usuariLogic;

    /**
     * Constructor amb injecció del servei de clients i de la lògica
     * d'usuari.
     *
     * @param clientService servei amb la lògica de negoci del client
     * @param usuariLogic validacions de negoci (p. ex. que les contrasenyes
     * coincideixin)
     */
    public ClientController(ClientService clientService, UsuariLogic usuariLogic) {
        this.clientService = clientService;
        this.usuariLogic = usuariLogic;
    }

    /**
     * Prefixos telefònics de tots els països per al desplegable del formulari.
     * Com que és un {@code @ModelAttribute}, Spring l'afegeix al model de totes
     * les peticions d'aquest controlador: també quan es torna a mostrar el
     * formulari després d'un error de validació.
     *
     * @return llista de prefixos (Espanya la primera)
     */
    @ModelAttribute("prefixos")
    public List<PrefixosTelefon.Prefix> prefixos() {
        return PrefixosTelefon.llista();
    }

    /**
     * Mostra el formulari de registre de client.
     *
     * Crea la sessió ABANS de renderitzar la plantilla. El formulari porta el
     * token CSRF, i Spring Security el desa a la sessió la primera vegada que
     * la plantilla el demana. Si en aquell moment la resposta ja s'ha començat
     * a enviar (la pàgina és llarga), Tomcat no pot crear la sessió i llança
     * "Cannot create a session after the response has been committed". Així el
     * token ja es pot llegir sense haver de crear res durant el renderitzat.
     *
     * @param request petició HTTP (per crear la sessió)
     * @return plantilla del formulari d'alta
     */
    @GetMapping("/client/alta")
    public String formulariAlta(HttpServletRequest request) {
        request.getSession(true);
        return "client/alta";
    }

    /**
     * Processa el formulari de registre. El DNI es normalitza (sense espais i
     * en majúscules) i es comprova que la contrasenya i la seva confirmació
     * coincideixin. El prefix i el telèfon, i l'adreça, la ciutat i el codi
     * postal, s'ajunten abans de cridar el servei. Només el personal (agent o
     * administrador) pot registrar un client com a premium; per a la resta el
     * valor enviat s'ignora. Si hi ha un error de validació es torna a mostrar
     * el formulari amb el missatge. Si va bé, el personal torna al llistat de
     * clients i un visitant va al login (patró Post/Redirect/Get).
     *
     * @param nom nom del client
     * @param cognom cognoms del client
     * @param tipusDocument tipus de document d'identitat: DNI, NIE o PASSAPORT
     * (per defecte DNI)
     * @param dniUsuari número del document, que fa d'identificador
     * @param email correu electrònic (únic)
     * @param password contrasenya en clar, que el servei guarda xifrada
     * @param confirmPassword repetició de la contrasenya
     * @param adreca carrer i número
     * @param ciutat població
     * @param codiPostal codi postal
     * @param nacionalitat nacionalitat
     * @param tipusLlicencia tipus de carnet de conduir
     * @param dataCaducitatDocument caducitat del document d'identitat
     * @param dataCaducitatLlicencia caducitat del carnet de conduir
     * @param imatgeDocument imatge del document d'identitat
     * @param imatgeLlicencia imatge del carnet de conduir
     * @param dataNaixement data de naixement (opcional)
     * @param prefixTelefon prefix telefònic (p. ex. +34)
     * @param telefon número de telèfon
     * @param premium true si es vol registrar com a premium (només personal)
     * @param authentication usuari autenticat, si n'hi ha
     * @param ra atributs per a la redirecció
     * @param model model de la vista (per mostrar errors)
     * @return el formulari amb l'error, o una redirecció si s'ha registrat
     */
    @PostMapping("/client/alta")
    public String registrarClient(@RequestParam String nom,
            @RequestParam String cognom,
            @RequestParam(value = "tipusDocument", defaultValue = "DNI") String tipusDocument,
            @RequestParam String dniUsuari,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(value = "confirmPassword", defaultValue = "") String confirmPassword,
            @RequestParam String adreca,
            @RequestParam String ciutat,
            @RequestParam String codiPostal,
            @RequestParam String nacionalitat,
            @RequestParam String tipusLlicencia,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataCaducitatDocument,
            // convierte el texto de valor de BD dataCaducitad a tipo valor LocalDate para procesar correctamente 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataCaducitatLlicencia,
            @RequestParam("imatgeIdentificacio") MultipartFile imatgeDocument,
            // MultipartFile representa un fichero subido
            @RequestParam("imatgeLlicencia") MultipartFile imatgeLlicencia,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataNaixement,
            @RequestParam(required = true) String prefixTelefon,
            @RequestParam(required = true) String telefon,
            @RequestParam(value = "premium", defaultValue = "false") boolean premium,
            Authentication authentication,
            RedirectAttributes ra,
            Model model) {

        // Calcular si es personal
        ///client/alta es pública (permitAll). Sin esta comprobación, cualquiera podría enviar premium=true a mano y registrarse como premium.
        boolean esPersonal = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGENT")
                || a.getAuthority().equals("ROLE_ADMINISTRADOR"));
        boolean premiumFinal = esPersonal && premium;

        // El número de documento (DNI, NIE o pasaporte) se guarda siempre en mayúsculas y sin espacios (12345678z -> 12345678Z)
        dniUsuari = dniUsuari.trim().toUpperCase();

        // Las dos contraseñas tienen que ser iguales: evita que el usuario se equivoque al escribirla
        if (!usuariLogic.contrasenyesCoincideixen(password, confirmPassword)) {
            model.addAttribute("error", "Les contrasenyes no coincideixen.");
            return "client/alta";
        }

        try {
            // Cada parte de la dirección se valida por separado, con su propio mensaje de error
            String errorAdreca = usuariLogic.validarAdrecaRegistre(adreca, codiPostal, ciutat);
            if (errorAdreca != null) {
                throw new IllegalArgumentException(errorAdreca);
            }
            // El prefijo tiene que ser uno de los del desplegable (no se puede inventar a mano)
            if (!usuariLogic.isPrefixTelefonValid(prefixTelefon)) {
                throw new IllegalArgumentException("Prefix de telèfon: no és un dels permesos.");
            }

            // Client no tiene ciudad ni código postal: de momento van dentro de la dirección
            String adrecaCompleta = adreca.trim() + ", " + codiPostal.trim() + " " + ciutat.trim();

            // para tratar el tema de que ponga espacio al introducir el telfono en el formulario 
            String telefonNet = (telefon == null) ? "" : telefon.replaceAll("[\\s-]", "");

            String telefonComplet = telefonNet.isEmpty()
                    ? null
                    : (prefixTelefon == null ? "" : prefixTelefon.trim()) + " " + telefonNet;

            // los datos del registrarClient de arriba lo pasa al metodo de clientService.registrar para que lo pille y lo pase a BD 
            clientService.registrar(tipusDocument, dniUsuari, email, password, nom, cognom, dataCaducitatDocument,
                    tipusLlicencia, dataCaducitatLlicencia, adrecaCompleta,
                    nacionalitat, imatgeDocument, imatgeLlicencia, dataNaixement, telefonComplet, premiumFinal);

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "client/alta";
        }

        // calcular si es personal agent o admin 
        if (esPersonal) {
            // El registre sempre deixa el compte en estat Pendent, també quan el fa el personal:
            // per convertir-lo en agent o administrador cal validar-lo i triar-hi el rol
            ra.addFlashAttribute("missatge", "Compte " + dniUsuari + " registrat i pendent de validació. "
                    + "Si ha de ser agent o administrador, valida'l a «Validacions» i tria-hi el rol.");
            return "redirect:/admin/agent/clients";
        }

        /* Si el servicio detecta un problema (por ejemplo, “Email ya registrado”), lanza una IllegalArgumentException con un mensaje. El controlador:
        Lo captura con catch.
        Mete el mensaje en el modelo con la clave "error".
        Vuelve a mostrar el formulario (return "client/alta"), no redirige. */
        // falta tratar el error en caso de que selecionemos un archivo no valido 
        // El compte queda pendent de validació: es porta l'usuari al login amb un missatge que ho explica
        // (genèric: no revela res de cap compte; es mostra una sola vegada gràcies al flash attribute)
        ra.addFlashAttribute("missatgeAlta",
                "Sol·licitud enviada. El teu compte està pendent de validació per part d'un administrador. "
                + "Quan l'hagi aprovat, inicia sessió amb el correu i la contrasenya que has indicat al registre.");
        return "redirect:/login";
        //Por qué redirect: y no return "login" Si devolvieras la plantilla directamente después de un POST 
        //y el usuario recargara la página, el navegador reenviaría el formulario y se intentaría registrar otra vez.
        //Con redirect:, la última petición es un GET a /login, y recargar es inofensivo. Es un patrón muy conocido (Post/Redirect/Get).
    }

}