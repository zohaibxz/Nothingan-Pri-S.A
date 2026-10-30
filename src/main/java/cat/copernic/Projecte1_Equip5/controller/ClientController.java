package cat.copernic.Projecte1_Equip5.controller;

// En tu ClientController

import cat.copernic.Projecte1_Equip5.service.ClientService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller // : Spring la registra como controlador web, y sus métodos devuelven nombres de plantillas.
public class ClientController {
// Responsabilidades separadas. ClientController gestiona el alta; DashBoardController gestiona la pantalla de inicio
    
    private final ClientService clientService;
    //Solo necesita ClientService: el controlador no habla con la BD, le pide todo al servicio.
    public ClientController(ClientService clientService) {
        this.clientService = clientService;
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
                                  @RequestParam String adreca,
                                  @RequestParam String ciutat,
                                  @RequestParam String codiPostal,
                                  @RequestParam String nacionalitat,
                                  @RequestParam String tipusLlicencia,
                                  @RequestParam String targetaCredit,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataCaducitatDocument, 
                                  // convierte el texto de valor de BD dataCaducitad a tipo valor LocalDate para procesar correctamente 
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataCaducitatLlicencia,
                                  @RequestParam("imatgeIdentificacio") MultipartFile imatgeDocument, // El nombre de la variable en Java, el que tú quieras
                                  // MultipartFile representa un fichero subido
                                  @RequestParam("imatgeLlicencia") MultipartFile imatgeLlicencia, // 
                                  //"imatgeLlicencia" nombre del campo en el formulario 
                                  Model model) {    
        try {
            // Client no tiene ciudad ni código postal:z de momento van dentro de la dirección
            String adrecaCompleta = adreca + ", " + codiPostal + " " + ciutat;
            
            // los datos del registrarClient de arriba lo pasa al metodo de clientService.registrar para que lo pille y lo pase a BD 
            clientService.registrar(dniUsuari, email, password, nom, cognom, dataCaducitatDocument,
                    tipusLlicencia, dataCaducitatLlicencia, targetaCredit, adrecaCompleta,
                    nacionalitat, imatgeDocument, imatgeLlicencia);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "client/alta";
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