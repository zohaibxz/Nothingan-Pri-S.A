package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.logic.UsuariLogic;
import cat.copernic.Projecte1_Equip5.service.ClientService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
     * Mostra el formulari de registre de client.
     *
     * @return plantilla del formulari d'alta
     */
    @GetMapping("/client/alta")
    public String formulariAlta() {
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
     * @param dniUsuari DNI, que fa d'identificador
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

        // El DNI se guarda siempre en mayúsculas y sin espacios (12345678z -> 12345678Z)
        dniUsuari = dniUsuari.trim().toUpperCase();

        // Las dos contraseñas tienen que ser iguales: evita que el usuario se equivoque al escribirla
        if (!usuariLogic.contrasenyesCoincideixen(password, confirmPassword)) {
            model.addAttribute("error", "Les contrasenyes no coincideixen.");
            return "client/alta";
        }

        try {
            // Client no tiene ciudad ni código postal:z de momento van dentro de la dirección
            String adrecaCompleta = adreca + ", " + codiPostal + " " + ciutat;

            // para tratar el tema de que ponga espacio al introducir el telfono en el formulario 
            String telefonNet = (telefon == null) ? "" : telefon.replaceAll("[\\s-]", "");

            String telefonComplet = telefonNet.isEmpty()
                    ? null
                    : (prefixTelefon == null ? "" : prefixTelefon.trim()) + " " + telefonNet;

            // los datos del registrarClient de arriba lo pasa al metodo de clientService.registrar para que lo pille y lo pase a BD 
            clientService.registrar(dniUsuari, email, password, nom, cognom, dataCaducitatDocument,
                    tipusLlicencia, dataCaducitatLlicencia, adrecaCompleta,
                    nacionalitat, imatgeDocument, imatgeLlicencia, dataNaixement, telefonComplet, premiumFinal);

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "client/alta";
        }

        // calcular si es personal agent o admin 
        if (esPersonal) {
            ra.addFlashAttribute("missatge", "Client " + dniUsuari + " registrat correctament.");
            return "redirect:/admin/agent/clients";
        }

        /* Si el servicio detecta un problema (por ejemplo, “Email ya registrado”), lanza una IllegalArgumentException con un mensaje. El controlador:
        Lo captura con catch.
        Mete el mensaje en el modelo con la clave "error".
        Vuelve a mostrar el formulario (return "client/alta"), no redirige. */
        // falta tratar el error en caso de que selecionemos un archivo no valido 
        return "redirect:/login";
        //Por qué redirect: y no return "login" Si devolvieras la plantilla directamente después de un POST 
        //y el usuario recargara la página, el navegador reenviaría el formulario y se intentaría registrar otra vez.
        //Con redirect:, la última petición es un GET a /login, y recargar es inofensivo. Es un patrón muy conocido (Post/Redirect/Get).
    }

}