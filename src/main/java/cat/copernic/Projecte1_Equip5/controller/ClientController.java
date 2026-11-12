package cat.copernic.Projecte1_Equip5.controller;

// En tu ClientController

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
// para el tema de client premium ..
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller // : Spring la registra como controlador web, y sus métodos devuelven nombres de plantillas.
public class ClientController {
// Responsabilidades separadas. ClientController gestiona el alta; DashBoardController gestiona la pantalla de inicio
    
    private final ClientService clientService;
    // Validaciones de negocio (aquí: que la contraseña y su confirmación coincidan)
    private final UsuariLogic usuariLogic;
    //El controlador no habla con la BD: le pide el alta al servicio y las validaciones a UsuariLogic.
    public ClientController(ClientService clientService, UsuariLogic usuariLogic) {
        this.clientService = clientService;
        this.usuariLogic = usuariLogic;
    }
    
    /* esto al principio para que carga el html de registro con la ruta /client/alta*/
    @GetMapping("/client/alta")
    public String formulariAlta() { // el nombre del metodo que se encarga de devolver el alta 
        return "client/alta";
    }
    /*Cuando el usuario pulsa “Registrar”, el navegador envía los datos con POST a la misma URL, y este método los recibe.
    Que GET y POST compartan URL es habitual: una muestra el formulario y la otra lo procesa. */
    @PostMapping("/client/alta")
    // @RequestParam le dice a Spring: “saca este valor del formulario 
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
                                  @RequestParam("imatgeIdentificacio") MultipartFile imatgeDocument, // El nombre de la variable en Java, el que tú quieras
                                  // MultipartFile representa un fichero subido
                                  @RequestParam("imatgeLlicencia") MultipartFile imatgeLlicencia, // 
                                  //"imatgeLlicencia" nombre del campo en el formulario 
                                  // añadiddo para los campos adicionales del formulario 
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataNaixement, // no hace falta puede decidir no escribirlo por eso pongo required = flae 
                                  @RequestParam(required = true) String prefixTelefon, // tiene que si o si escribir telefono en el formulario 
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