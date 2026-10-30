package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Usuari;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import cat.copernic.Projecte1_Equip5.service.ClientService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
 
import java.util.Optional;
 
/**
 * Controlador del panel principal (dashboard).
 * Según el rol del usuario autenticado, muestra una pantalla distinta:
 * administrador, agente o cliente.
 */

    /*Qué hace, paso a paso
    Comprueba que hay sesión. Si no hay usuario autenticado, redirige a /login.
    Busca al usuario en la BD con findByEmail. El email viene de authentication.getName(), que es el “nombre de usuario” con el que se hizo login.
    Prepara datos para la vista: el nombre a mostrar, las iniciales (para un avatar tipo “JG”) y, si es un Client, el objeto cliente.
    Mira los roles del usuario (ROLE_ADMINISTRADOR, ROLE_AGENT...), que son los que genera AuthorityType.
    Devuelve la plantilla correspondiente: administrador, agente o cliente. */ 

@Controller // Indica a Spring que esta clase atiende peticiones web y devuelve vistas (plantillas HTML)
public class DashBoardController {
 
    
    // La clase necesita dos colaboradores: uno para leer usuarios de la BD y otro para obtener clientes pendientes. No los creas tú con new:
    // Repositorio para buscar al usuario en la base de datos (MySQL)
    private final UsuariRepository usuariRepository;
 
    // Servicio de clientes: se usa solo para el dashboard del administrador,
    // para obtener la lista de clientes pendientes de validar
    private final ClientService clientService;
 
    // Inyección por constructor: Spring pasa automáticamente las dos dependencias al crear el controlador
    public DashBoardController(UsuariRepository usuariRepository, ClientService clientService) {
        this.usuariRepository = usuariRepository;
        this.clientService = clientService;
    }
 
    /**
     * Atiende las peticiones GET a la URL /dashboard.
     *
     * @param authentication contiene los datos del usuario logueado (email y roles); lo inyecta Spring Security
     * @param model          sirve para enviar datos a la plantilla HTML
     * @return el nombre de la plantilla a mostrar, o una redirección
     */
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
 
        // Si no hay sesión iniciada, se manda al usuario a la pantalla de login
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
 
        // El "nombre" de la autenticación es el email con el que se hizo login
        String email = authentication.getName();
 
        // Se busca el usuario en la BD. Optional porque podría no existir
        Optional<Usuari> usuariOpt = usuariRepository.findByEmail(email);
        //Hibernate devuelve el objeto con su clase real (Client o Agent), aunque el tipo declarado sea Usuari.
        // Nombre por defecto por si el usuario no tiene nombre guardado
        String nomMostrar = "Usuari";
 
        if (usuariOpt.isPresent()) {
            Usuari usuari = usuariOpt.get();
 
            // Si el usuario tiene nombre y no está vacío, se usa ese nombre
            if (usuari.getNom() != null && !usuari.getNom().trim().isEmpty()) {
                nomMostrar = usuari.getNom();
            }
 
            // Se envían las iniciales a la vista (por ejemplo, para un avatar circular)
            model.addAttribute("inicials", calcularInicials(usuari));
            // Mete en la mochila un dato con clave "inicials" y como valor el resultado de calcularInicials. En la plantilla se leerá con ${inicials}.
            // para leer en html ${inicials}.
           
 
            // Si el usuario es un Client (o un Agent, que también extiende Client),
            // se envía el objeto completo a la plantilla: Esto significa: “guarda este objeto Client en la mochila con el nombre client,
            // para que la plantilla pueda leer sus datos”. Luego, en el HTML (con Thymeleaf), accedes a él por ese nombre:
            if (usuari instanceof Client client) {
                model.addAttribute("client", client);
            }
        } else {
            // Si no se encuentra el usuario, iniciales genéricas
            model.addAttribute("inicials", "US");
        }
 
        // Nombre que mostrará la plantilla (por ejemplo, "Hola, Marta")
        model.addAttribute("nomClient", nomMostrar);
 
        // ¿El usuario tiene el rol de administrador?
        // Se acepta con y sin el prefijo "ROLE_" y sin distinguir mayúsculas
        boolean isAdmin = authentication.getAuthorities().stream() // getAuthorities deuvle una colecccion de authorities roles 
                // Un stream es una forma de recorrer una colección aplicando operaciones, sin escribir un for a mano. No es un flujo de red ni de ficheros, a pesar del nombre.
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMINISTRADOR")
                            || a.getAuthority().equalsIgnoreCase("ADMINISTRADOR"));
 
        // ¿El usuario tiene el rol de agente?
        boolean isAgent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_AGENT")
                        // anyMatchtrue si al menos un elemento cumple la condición
                        // a.getAuthority() mira la autoridad 
                            || a.getAuthority().equalsIgnoreCase("AGENT"));
 
        // Se elige la pantalla según el rol, del más alto al más bajo
        if (isAdmin) {
            // El administrador ve la lista de clientes pendientes de validar:
            // ClientService -> ClientRepository.findByEstatValidacio(Pendent)
            model.addAttribute("clientsPendents", clientService.llistarClientsPendents());
            // model.addAttribute(...) solo puede estar en esta capa controlador 
            return "administrador/dashboard";
        } else if (isAgent) {
            return "agent/dashboard";
        } else {
            // Cualquier otro usuario es un cliente normal
            return "client/inici-client";
        }
    }
 
    /**
     * Calcula las iniciales del usuario a partir de su nombre y apellidos.
     * Ejemplo: "Marta" + "Soler" -> "MS". Si no hay datos, devuelve "US".
     */
    private String calcularInicials(Usuari usuari) {
        // Se evita el null y se quitan espacios sobrantes
        String nom = usuari.getNom() == null ? "" : usuari.getNom().trim();
        String cognoms = usuari.getCognoms() == null ? "" : usuari.getCognoms().trim();
 
        String inicials = "";
        // Primera letra del nombre
        if (!nom.isEmpty()) inicials += nom.charAt(0);
        // Primera letra de los apellidos
        if (!cognoms.isEmpty()) inicials += cognoms.charAt(0);
 
        // Si no hay ninguna inicial, "US"; si las hay, en mayúsculas
        return inicials.isEmpty() ? "US" : inicials.toUpperCase();
    }
}
 

