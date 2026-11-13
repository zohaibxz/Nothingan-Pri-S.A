package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.entity.Agent;
import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Usuari;
import cat.copernic.Projecte1_Equip5.repository.ClientRepository;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import cat.copernic.Projecte1_Equip5.service.AgentService;
import cat.copernic.Projecte1_Equip5.service.ClientService;
import cat.copernic.Projecte1_Equip5.service.UsuariService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Optional;

/**
 * Controlador de la zona de administración: gestión de agentes (alta, baja,
 * listado) y gestión de clientes (validar, rechazar, consultar, informes).
 *
 * @Controller: Spring la registra como controlador web; sus métodos devuelven
 * nombres de plantillas HTML.
 * @RequestMapping("/admin/agent"): prefijo común de todas las URLs de la clase.
 * Así, @GetMapping("/alta") responde en /admin/agent/alta,
 * @GetMapping("/llista") en /admin/agent/llista, etc.
 */
@Controller
@RequestMapping("/admin/agent")
public class AgentController {

    // Servicio con la lógica de agentes (alta, baja, listados)
    private final AgentService agentService;
    // Servicio con la lógica de clientes (pendientes, validar, rechazar)
    private final ClientService clientService;
    // Repositorio para buscar al usuario logueado por email
    private final UsuariRepository usuariRepository;
    // Repositorio de clientes, usado directamente en la gestión general y el detalle
    private final ClientRepository clientRepository;
    // Servicio común a todos los usuarios: aquí se usa para listar y reactivar usuarios dados de baja
    private final UsuariService usuariService;

    /**
     * Constructor amb injecció de dependències.
     *
     * @param agentService servei d'agents i administradors
     * @param clientService servei de clients
     * @param usuariRepository repositori d'usuaris
     * @param clientRepository repositori de clients
     * @param usuariService servei comú d'usuaris (baixes i reactivacions)
     */
    public AgentController(AgentService agentService,
            ClientService clientService,
            UsuariRepository usuariRepository,
            ClientRepository clientRepository,
            UsuariService usuariService) {
        this.agentService = agentService;
        this.clientService = clientService;
        this.usuariRepository = usuariRepository;
        this.clientRepository = clientRepository;
        this.usuariService = usuariService;
    }

    // 1. Mostrar el formulari d'alta d'agent
    // GET /admin/agent/alta -> muestra el formulario vacío
    /*@GetMapping("/alta")
    public String mostrarFormulariAlta(Model model) {
        // Se crea un Agent vacío con el rol "Agent" preseleccionado
        Agent agent = new Agent();
        agent.setRol(Enums.Rol.Agent);
 
        // Se pasa a la plantilla con el nombre "agent". Thymeleaf lo enlaza con los campos
        // del formulario (th:object="${agent}", th:field="*{nom}"...)
        model.addAttribute("agent", agent);
 
        // Enums.Rol.values() devuelve un array con todos los valores del enum (Administrador, Agent).
        // Sirve para rellenar un desplegable (<select>) de roles
        model.addAttribute("roles", Enums.Rol.values());
        return "administrador/alta-agent";
    } */
    // 2. Processar el formulari d'alta d'agent
    // POST /admin/agent/alta -> recibe los datos del formulario y guarda el agente
    /* no hace falta porque ahora no llama a administrador/alta-agent ahora llama a alta-client ..@PostMapping("/alta")
    public String guardarAgent(@ModelAttribute("agent") Agent agent, Model model) {
        // @ModelAttribute("agent") Agent agent: Spring toma todos los campos del formulario y los rellena
        // automáticamente en un objeto Agent, emparejando cada name del input con un atributo de la clase.
        // Es una alternativa a escribir un @RequestParam por campo (como en ClientController).
        try {
            // El servicio valida y guarda el agente
            agentService.alta(agent);
        } catch (IllegalArgumentException e) {
            // Si el servicio detecta un problema (p. ej. DNI duplicado), se muestra el mensaje
            // y se vuelve a enseñar el formulario
            model.addAttribute("error", e.getMessage());
            // Hay que volver a añadir los roles: el modelo no se conserva entre peticiones
            model.addAttribute("roles", Enums.Rol.values());
            return "administrador/alta-agent";
        }
        // Todo bien: redirect al listado (patrón Post/Redirect/Get, evita duplicar el alta al recargar)
        return "redirect:/admin/agent/llista";
    } */
    /**
     * Llista els agents actius, amb filtre opcional per DNI. Només és
     * accessible per a l'administrador.
     *
     * @param dni DNI (o part) per filtrar; si és null es mostren tots
     * @param authentication usuari autenticat
     * @param model model de la vista
     * @return plantilla del llistat d'agents
     */
    @GetMapping("/llista")
    public String llistaAgents(
            // Lee el parámetro "dni" de la URL. required = false: es opcional; si no viene vale null
            // (listado completo). Sin required = false, Spring daría error 400 al no venir.
            @RequestParam(value = "dni", required = false) String dni,
            // Datos del usuario logueado (email y roles), inyectados por Spring Security
            Authentication authentication,
            // "Mochila" para enviar datos a la plantilla
            Model model) {

        // Cabecera de la página: nombre e iniciales del usuario logueado (ej. "Marta" y "MS")
        // Se comprueba primero que hay sesión. Si authentication fuera null, la segunda condición
        // daría NullPointerException, pero && no la evalúa si la primera ya es falsa (cortocircuito)
        if (authentication != null && authentication.isAuthenticated()) {
            // getName() es el email con el que se hizo login; findByEmail devuelve un Optional
            usuariRepository.findByEmail(authentication.getName()).ifPresent(usuari -> {
                // ifPresent: el bloque solo se ejecuta si el usuario existe en la BD
                model.addAttribute("nomClient", usuari.getNom());
                model.addAttribute("inicials", calcularInicials(usuari));
            });
        }

        // Lista de agentes (filtrada por DNI si viene, completa si es null)
        model.addAttribute("agents", agentService.llistarAgents(dni));
        // Total sin filtrar, para mostrar "X de N agentes"
        // (trae todos los agentes solo para contarlos; sería más eficiente un count() en el repositorio)
        model.addAttribute("totalAgents", agentService.llistarAgents(null).size());
        // Se devuelve lo que escribió el usuario para que el buscador conserve el texto
        model.addAttribute("dni", dni);
        // La plantilla llista.html sirve para agentes y para administradores: aquí es la vista de agentes
        model.addAttribute("vistaAdmins", false);
        model.addAttribute("urlLlista", "/admin/agent/llista");

        return "administrador/llista";
    }

    /**
     * Llista els administradors actius amb la mateixa pantalla que els agents.
     * Passa el DNI de l'usuari actual perquè no se li ofereixi donar-se de
     * baixa a si mateix.
     *
     * @param dni DNI (o part) per filtrar; si és null es mostren tots
     * @param authentication usuari autenticat
     * @param model model de la vista
     * @return plantilla del llistat
     */
    @GetMapping("/administradors")
    public String llistaAdministradors(
            @RequestParam(value = "dni", required = false) String dni,
            Authentication authentication,
            Model model) {

        // Cabecera + DNI del usuario logueado (para no ofrecerle darse de baja a sí mismo)
        if (authentication != null && authentication.isAuthenticated()) {
            usuariRepository.findByEmail(authentication.getName()).ifPresent(usuari -> {
                model.addAttribute("nomClient", usuari.getNom());
                model.addAttribute("inicials", calcularInicials(usuari));
                model.addAttribute("dniActual", usuari.getDniUsuari());
            });
        }

        // La plantilla recorre la variable "agents": aquí contiene los administradores
        model.addAttribute("agents", agentService.llistarAdministradors(dni));
        model.addAttribute("totalAgents", agentService.llistarAdministradors(null).size());
        model.addAttribute("dni", dni);
        model.addAttribute("vistaAdmins", true);
        model.addAttribute("urlLlista", "/admin/agent/administradors");

        return "administrador/llista";
    }

    // 4. Donar de baixa un agent
    // POST /admin/agent/baixa/{dni}. Se usa POST porque la acción modifica datos
    /**
     * Dona de baixa un agent.
     *
     * @param dni DNI de l'agent
     * @param ra atributs per mostrar el missatge a la pantalla següent
     * @return redirecció al llistat d'agents
     */
    @PostMapping("/baixa/{dni}")
    public String donarDeBaixa(@PathVariable("dni") String dni, RedirectAttributes ra) {
        try {
            agentService.baixa(dni);
            // Flash attribute: se guarda temporalmente, la siguiente petición (el listado) lo recibe
            // y luego desaparece. Un Model normal se perdería con el redirect
            ra.addFlashAttribute("missatge", "Agent " + dni + " donat de baixa correctament.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/agent/llista";
    }

    /**
     * Dona de baixa un administrador. Només ho pot fer un altre administrador i
     * el servei ho torna a comprovar al servidor.
     *
     * @param dni DNI de l'administrador
     * @param authentication usuari que fa l'acció
     * @param ra atributs per mostrar el missatge a la pantalla següent
     * @return redirecció al llistat d'administradors
     */
    @PostMapping("/administradors/{dni}/baixa")
    public String donarDeBaixaAdministrador(@PathVariable("dni") String dni,
            Authentication authentication, RedirectAttributes ra) {
        try {
            agentService.baixaAdministrador(dni, authentication == null ? null : authentication.getName());
            ra.addFlashAttribute("missatge", "Administrador " + dni + " donat de baixa correctament.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/agent/administradors";
    }

    /**
     * Dona de baixa un client (confirmada abans a la pantalla de confirmació).
     *
     * @param dni DNI del client
     * @param ra atributs per mostrar el missatge a la pantalla següent
     * @return redirecció a la gestió de clients
     */
    @PostMapping("/clients/{dni}/baixa")
    public String donarDeBaixaClient(@PathVariable("dni") String dni, RedirectAttributes ra) {
        try {
            clientService.baixa(dni);
            ra.addFlashAttribute("missatge", "Client " + dni + " donat de baixa correctament.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/agent/clients";
    }

    /**
     * Mostra la pantalla per confirmar la baixa d'un client, amb les reserves
     * pendents que tindria afectades.
     *
     * @param dni DNI del client
     * @param authentication usuari autenticat
     * @param model model de la vista
     * @param ra atributs per mostrar errors
     * @return plantilla de confirmació, o redirecció si el client no existeix
     */
    @GetMapping("/clients/{dni}/baixa/confirmar")
    public String confirmarBaixaClient(@PathVariable("dni") String dni,
            Authentication authentication,
            Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("client", clientService.obtenirClientPerBaixa(dni));
            model.addAttribute("pendents", clientService.reservesPendents(dni));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/agent/clients";
        }

        // Cabecera: nombre e iniciales del usuario logueado (igual que en los otros métodos)
        if (authentication != null && authentication.isAuthenticated()) {
            usuariRepository.findByEmail(authentication.getName()).ifPresent(usuari -> {
                model.addAttribute("nomClient", usuari.getNom());
                model.addAttribute("inicials", calcularInicials(usuari));
            });
        }
        return "administrador/confirmar-baixa-client";
    }

    /**
     * Llista els usuaris donats de baixa, amb cercador opcional.
     * L'administrador veu tots els tipus d'usuari i l'agent només els clients.
     *
     * @param q text a cercar (DNI, nom, cognoms o correu); pot ser null
     * @param authentication usuari autenticat
     * @param model model de la vista
     * @return plantilla del llistat de baixes
     */
    @GetMapping("/baixes")
    public String llistarUsuarisDeBaixa(@RequestParam(value = "q", required = false) String q,
            Authentication authentication, Model model) {
        boolean nomesClients = !esAdministrador(authentication);
        model.addAttribute("usuaris", usuariService.llistarUsuarisDeBaixa(q, nomesClients));
        model.addAttribute("q", q);
        model.addAttribute("nomesClients", nomesClients);
        return "administrador/Baixes";
    }

    /**
     * Reactiva un usuari donat de baixa. Un agent només pot reactivar clients;
     * un administrador, tothom.
     *
     * @param dni DNI de l'usuari
     * @param authentication usuari que fa l'acció
     * @param ra atributs per mostrar el missatge a la pantalla següent
     * @return redirecció al llistat de baixes
     */
    @PostMapping("/baixes/{dni}/reactivar")
    public String reactivarUsuari(@PathVariable("dni") String dni,
            Authentication authentication, RedirectAttributes ra) {
        try {
            // Un agente solo puede reactivar clientes; se comprueba aquí y en el servicio, no solo en la pantalla
            usuariService.reactivar(dni, !esAdministrador(authentication));
            ra.addFlashAttribute("missatge", "Usuari " + dni + " reactivat correctament.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/agent/baixes";
    }

    /**
     * Indica si l'usuari autenticat és administrador.
     *
     * @param authentication usuari autenticat
     * @return true si té el rol d'administrador
     */
    private boolean esAdministrador(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
    }

    /**
     * Mostra la cua de validació: els clients pendents de ser validats o
     * rebutjats.
     *
     * @param authentication usuari autenticat
     * @param model model de la vista
     * @return plantilla de la cua de validacions
     */
    @GetMapping("/validacions")
    public String llistarUsuarisPendents(Authentication authentication, Model model) {
        // Cabecera: nombre e iniciales del usuario logueado (mismo efecto que ifPresent en el método 3)
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            Optional<Usuari> usuariOpt = usuariRepository.findByEmail(email);
            if (usuariOpt.isPresent()) {
                Usuari usuari = usuariOpt.get();
                model.addAttribute("nomClient", usuari.getNom());
                model.addAttribute("inicials", calcularInicials(usuari));
            }
        }

        // Clientes con estatValidacio = Pendent (ClientService -> ClientRepository.findByEstatValidacio)
        model.addAttribute("clientsPendents", clientService.llistarClientsPendents());
        return "administrador/validar-clients";
    }

    /**
     * Valida un client pendent. Si el rol triat és Agent o Administrador, el
     * converteix. Si es vol, també el marca com a premium.
     *
     * @param dni DNI del client
     * @param rol rol a assignar ("Client" per defecte)
     * @param premium true per marcar-lo premium (opcional)
     * @param authentication agent o administrador que valida
     * @param ra atributs per mostrar el missatge a la pantalla següent
     * @return redirecció a la cua de validacions
     */
    @PostMapping("/validacions/{dni}/validar")
    public String validarClient(@PathVariable("dni") String dni,
            // Rol elegido en el desplegable. Si el formulario no lo envía
            // (un agente no lo ve), vale "Client"
            @RequestParam(value = "rol", defaultValue = "Client") String rol,
            @RequestParam(value = "premium", required = false) Boolean premium,
            Authentication authentication,
            RedirectAttributes ra) {
        try {
            // Quién valida: el agente/administrador que tiene la sesión iniciada
            Agent adminValidador = obtenirAgentAutenticat(authentication);
            // El servicio valida al cliente y, si el rol es Agent o Administrador, lo convierte
            clientService.validarClientAmbRol(dni, rol, adminValidador, premium);
            ra.addFlashAttribute("missatge", "L'usuari amb DNI " + dni + " ha sigut validat amb el rol " + rol + ".");
        } catch (Exception e) {
            // Aquí se captura cualquier excepción y se muestra su mensaje al usuario
            ra.addFlashAttribute("error", e.getMessage());
        }
        // Se vuelve a la cola de validaciones, que se recarga sin ese cliente
        return "redirect:/admin/agent/validacions";
    }

    /**
     * Rebutja un client pendent de validació (passa a l'estat Rebutjat).
     *
     * @param dni DNI del client
     * @param authentication agent o administrador que rebutja
     * @param ra atributs per mostrar el missatge a la pantalla següent
     * @return redirecció a la cua de validacions
     */
    @PostMapping("/validacions/{dni}/rebutjar")
    public String rebutjarClient(@PathVariable("dni") String dni,
            Authentication authentication,
            RedirectAttributes ra) {
        try {
            Agent adminValidador = obtenirAgentAutenticat(authentication);
            clientService.rebutjarClient(dni, adminValidador);
            ra.addFlashAttribute("missatge", "El client amb DNI " + dni + " ha sigut rebutjat.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/agent/validacions";
    }

    /**
     * Gestió i consulta de clients, amb filtres opcionals i els comptadors per
     * estat.
     *
     * @param email filtre per correu
     * @param dni filtre per DNI
     * @param cognoms filtre per cognoms
     * @param telefon filtre per telèfon
     * @param nacionalitat filtre per nacionalitat
     * @param poblacio filtre per població
     * @param authentication usuari autenticat
     * @param model model de la vista
     * @return plantilla del llistat de clients
     */
    @GetMapping("/clients")
    public String gestioClients(@RequestParam(required = false) String email,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String cognoms,
            @RequestParam(required = false) String telefon,
            @RequestParam(required = false) String nacionalitat,
            @RequestParam(required = false) String poblacio, Authentication authentication, Model model) {
        // Cabecera: nombre e iniciales del usuario logueado
        if (authentication != null && authentication.isAuthenticated()) {
            Optional<Usuari> usuariOpt = usuariRepository.findByEmail(authentication.getName());
            if (usuariOpt.isPresent()) {
                Usuari usuari = usuariOpt.get();
                model.addAttribute("nomClient", usuari.getNom());
                model.addAttribute("inicials", calcularInicials(usuari));
            }
        }
        /*
        // Trae TODOS los clientes de la BD (como Agent extiende Client, también vienen los agentes)
        List<Client> totsElsClients = clientRepository.findAll();
 
        // Con un stream se descartan los de email @empresa.com (los empleados), quedando solo los clientes reales.
        // filter deja pasar los que cumplen la condición; toList() convierte el resultado en lista
        List<Client> clientsReals = totsElsClients.stream()
                .filter(c -> c.getEmail() != null && !c.getEmail().endsWith("@empresa.com"))
                .toList();
         */
        // para tratar el tema de html llsitar clietns con los campos que toca  
        List<Client> clientsReals = clientService.filtrarClients(email, dni, cognoms, telefon, nacionalitat, poblacio);

        model.addAttribute("clients", clientsReals);

        // Cuántos clientes están validados (filter + count)
        long validatsCount = clientsReals.stream()
                .filter(c -> c.getEstatValidacio() != null
                && c.getEstatValidacio() == Enums.EstatValidacio.Validat)
                .count();

        // Cuántos están pendientes (también cuenta los que tengan estado null)
        long pendentsCount = clientsReals.stream()
                .filter(c -> c.getEstatValidacio() == null
                || c.getEstatValidacio() == Enums.EstatValidacio.Pendent)
                .count();

        model.addAttribute("validatsCount", validatsCount);
        model.addAttribute("pendentsCount", pendentsCount);

        // Ojo: la plantilla lleva un espacio en el nombre. Funciona solo si el fichero se llama así de verdad
        return "administrador/llistar clients";
    }

    /**
     * Mostra la pantalla amb els informes disponibles.
     *
     * @param authentication usuari autenticat
     * @param model model de la vista
     * @return plantilla dels informes
     */
    @GetMapping("/informes")
    public String mostrarInformes(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            Optional<Usuari> usuariOpt = usuariRepository.findByEmail(email);
            if (usuariOpt.isPresent()) {
                Usuari usuari = usuariOpt.get();
                model.addAttribute("nomClient", usuari.getNom());
                model.addAttribute("inicials", calcularInicials(usuari));
            }
        }
        return "administrador/informes";
    }

    /**
     * Devuelve el Agent que tiene la sesión iniciada. Se usa para saber quién
     * valida o rechaza a un cliente.
     */
    private Agent obtenirAgentAutenticat(Authentication authentication) {
        // Email del usuario logueado
        String email = authentication.getName();
        // Se busca en la BD; si no existe, se lanza un error
        Usuari usuari = usuariRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuari no trobat a la sessió."));
        // instanceof comprueba la clase real del objeto: si es un cliente normal (no Agent), no tiene permisos
        if (!(usuari instanceof Agent)) {
            throw new IllegalStateException("L'usuari autenticat no té permisos d'agent o administrador.");
        }
        // Cast: se convierte de Usuari a Agent para poder devolverlo como Agent
        return (Agent) usuari;
    }

    /**
     * Calcula las iniciales a partir de nombre y apellidos (ej. "Marta" +
     * "Soler" -> "MS"). Si no hay datos, devuelve "AG".
     */
    private String calcularInicials(Usuari usuari) {
        // Se convierten los null en texto vacío y se quitan los espacios sobrantes
        String nom = usuari.getNom() == null ? "" : usuari.getNom().trim();
        String cognoms = usuari.getCognoms() == null ? "" : usuari.getCognoms().trim();

        String inicials = "";
        // Primera letra del nombre y primera letra de los apellidos
        if (!nom.isEmpty()) {
            inicials += nom.charAt(0);
        }
        if (!cognoms.isEmpty()) {
            inicials += cognoms.charAt(0);
        }

        // Si no se obtuvo ninguna letra, "AG"; si no, las iniciales en mayúsculas
        return inicials.isEmpty() ? "AG" : inicials.toUpperCase();
    }
}
