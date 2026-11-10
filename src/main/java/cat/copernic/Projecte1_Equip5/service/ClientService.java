package cat.copernic.Projecte1_Equip5.service;
 
import cat.copernic.Projecte1_Equip5.entity.Agent;
import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.logic.UsuariLogic;
import cat.copernic.Projecte1_Equip5.repository.ClientRepository;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
// estos 4 imports añadido para guardar imagenes 
import cat.copernic.Projecte1_Equip5.document.Documentacio;
import cat.copernic.Projecte1_Equip5.entity.AuthorityType;
import cat.copernic.Projecte1_Equip5.entity.Reserva;
import cat.copernic.Projecte1_Equip5.entity.Usuari;
// para metode baixa baja client creo que es 
// para baja client 
import java.util.stream.Collectors;
// para metode baixa baja client 
import cat.copernic.Projecte1_Equip5.repository.ReservaRepository;
// añadido para tema mensajes 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


 
/**
 * Servicio con la lógica de negocio de los clientes:
 * registro (alta), listado de pendientes y validación/rechazo por parte de un agente.
 */
// @Service: Spring la registra como componente de la capa de servicio,
// para poder inyectarla en los controladores (ClientController, DashBoardController, AgentController)
@Service
public class ClientService {
    
    
    private static final Logger log = LoggerFactory.getLogger(ClientService.class);
    
    // Repositorio de clientes (tabla clients en MySQL)
    private final ClientRepository clientRepository;
    // Repositorio de usuarios: para comprobar duplicados de DNI y email (existsById, existsByEmail)
    private final UsuariRepository usuariRepository;
    // Cifra las contraseñas (BCrypt) antes de guardarlas
    private final PasswordEncoder passwordEncoder;
    // Clase con las validaciones de negocio (formato de DNI, fechas, contraseña, archivos...)
    private final UsuariLogic usuariLogic;
    // Servicio de documentación: único sitio que valida y guarda en MongoDB el DNI y el carnet
    private final DocumentacioService documentacioService;
 
    // Repositorio de reservas: para comprobar si el cliente tiene reservas pendientes antes de darlo de baja
    private final ReservaRepository reservaRepository;

    // Inyección por constructor: Spring pasa las cinco dependencias automáticamente
    public ClientService(ClientRepository clientRepository,
                         UsuariRepository usuariRepository,
                         PasswordEncoder passwordEncoder,
                         UsuariLogic usuariLogic,
                         DocumentacioService documentacioService,
                         ReservaRepository reservaRepository) {
        this.clientRepository = clientRepository;
        this.usuariRepository = usuariRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuariLogic = usuariLogic;
        this.documentacioService = documentacioService;
        this.reservaRepository = reservaRepository;
    }
   
    /**
     * Registra un cliente nuevo (lo llama ClientController.registrarClient).
     * Si algo no es válido lanza IllegalArgumentException con el mensaje, que el controlador
     * captura y muestra en el formulario.
     */
    // @Transactional: todo el método es una transacción; si lanza una excepción, se deshace
    // cualquier cambio hecho en MySQL
    @Transactional
    public void registrar(String dni, String email, String password, String nom, String cognoms,
                          LocalDate dataCaducitatDni, String tipusLlicencia,
                          LocalDate dataCaducitatLlicencia, String adreca,
                          String nacionalitat, MultipartFile imatgeDocument, MultipartFile imatgeLlicencia,
                          LocalDate dataNaixement, String telefonComplet, boolean premium) {
 
        // 1. Validacions de negoci mitjançant UsuariLogic
        // Cada comprobación delega en UsuariLogic; si falla, se corta con un error descriptivo
 
        // Formato del DNI/NIE
        if (!usuariLogic.isDniValid(dni)) {
            throw new IllegalArgumentException("El format del DNI/NIE no és vàlid.");
        }
        // El documento de identidad no puede estar caducado
        if (!usuariLogic.isDocumentValid(dataCaducitatDni)) {
            throw new IllegalArgumentException("El document d'identificació està caducat.");
        }
        // El carnet de conducir tampoco (se reutiliza la misma comprobación de fecha)
        if (!usuariLogic.isDocumentValid(dataCaducitatLlicencia)) {
            throw new IllegalArgumentException("El permís de conduir està caducat.");
        }
        // Contraseña con la seguridad mínima exigida (6 caracteres)
        if (!usuariLogic.isPasswordSecure(password)) {
            throw new IllegalArgumentException("La contrasenya ha de tenir com a mínim 6 caràcters.");
        }
 
        // 2. Comprovacions de duplicats a la BD
 
        // existsById: el DNI es la clave primaria (@Id) de Usuari, así que busca por ahí
        if (usuariRepository.existsById(dni)) {
            throw new IllegalArgumentException("Ja existeix un usuari amb aquest DNI.");
        }
        // existsByEmail: el email tampoco puede repetirse (es lo que se usa para el login)
        if (usuariRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ja existeix un usuari amb aquest correu.");
        }
 
        if (!usuariLogic.isDataNaixementValida(dataNaixement)) {
            throw new IllegalArgumentException("La data de naixement no pot ser futura.");
        }
        // if (!usuariLogic.isMajorDEdat(dataNaixement)) {
        //     throw new IllegalArgumentException("Cal ser major d'edat.");
        // }
        
        
        if (!usuariLogic.isTelefonValid(telefonComplet)) {
            throw new IllegalArgumentException("El format del telèfon no és vàlid.");
        }
        
        // añadido para que en caso de que no rellen el campo telefono sale esta excepcion y se guarde de verdad 
        if (telefonComplet == null) {
            throw new IllegalArgumentException("El telèfon és obligatori.");
        }
 
        // Validación de las imágenes (presentes, máximo 5 MB, JPG/PNG/PDF).
        // Se hace aquí, antes de tocar la BD; las reglas viven en UsuariLogic.
        documentacioService.validarDocumentacio(imatgeDocument, imatgeLlicencia);
 
        // 3. Creació i desament de l'entitat Client
 
        // Se construye el cliente con el constructor completo. Puntos clave:
        // - passwordEncoder.encode(password): la contraseña se guarda cifrada, nunca en texto plano
        // - LocalDateTime.now(): fecha de alta
        // - Enums.EstatValidacio.Pendent: todo cliente nuevo queda pendiente de que un agente lo valide
        // - null: aún no hay agente validador (agentValidador)
        // - false: no es premium
        // - null: sin descuento premium (descomptePremium)
        Client client = new Client(dni, email, passwordEncoder.encode(password), LocalDateTime.now(),
                nom, cognoms, dataCaducitatDni, tipusLlicencia, dataCaducitatLlicencia,
                adreca, nacionalitat, Enums.EstatValidacio.Pendent, null, premium, null); // cambiado false por premium ahora el valor viene del controlador 
                            
        // Campos nuevos (no están en el constructor): se asignan mediante setter
        client.setDataNaixement(dataNaixement);
        client.setTelefon(telefonComplet);   // el parámetro se llama telefonComplet, la entidad usa setTelefon
        // INSERT en MySQL
        clientRepository.save(client);
 
        // 4. Desament de les imatges a MongoDB (si falla, @Transactional deshace el cliente en MySQL)
        documentacioService.guardarDocumentacio(dni, imatgeDocument, imatgeLlicencia);
        log.info("Documentació guardada per al client {}", dni);
    }
    
    // para que en caso que agent no valide client 
     // ...campos y constructor...
    private static final String MISSATGE_NOMES_ADMIN = "Només un administrador pot validar o rebutjar clients.";

    private void comprovarAdministrador(Agent agent, String missatge) {
        if (agent == null || agent.getRol() != Enums.Rol.Administrador) {
            throw new IllegalStateException(missatge);
        }
    }
    
    // --- MÉTODOS DE VALIDACIÓN USANDO LA CAPA LOGIC ---
 
    /**
     * Devuelve los clientes pendientes de validar (la "bandeja de entrada" del administrador).
     * Lo usan DashBoardController y AgentController.
     */
    // readOnly = true: transacción de solo lectura, más eficiente porque no se modifica nada
    @Transactional(readOnly = true)
    public List<Client> llistarClientsPendents() {
        // SELECT * FROM clients WHERE estat_validacio = 'Pendent'
        return clientRepository.findByEstatValidacio(Enums.EstatValidacio.Pendent);
    }
    
    
    // añadido para tratar en los html el tema de llistar clients que solo me liste clients y no hago un arreglo rapido por @client.com 
    @Transactional(readOnly = true)
    public List<Client> llistarNomesClients() {
        return clientRepository.findNomesClients();
    }
    
    // esto es para poder filtrar clientes por diversos campos en html ... 
    @Transactional(readOnly = true)
    public List<Client> filtrarClients(String email, String dni, String cognoms,
            String telefon, String nacionalitat, String poblacio) {
        return llistarNomesClients().stream()
                .filter(Client::isActiu) // liena añadida para listar client en html llistar client.html 
                .filter(c -> conte(c.getEmail(), email))
                .filter(c -> conte(c.getDniUsuari(), dni))
                .filter(c -> conte(c.getCognoms(), cognoms))
                .filter(c -> conte(c.getTelefon(), telefon))
                .filter(c -> conte(c.getNacionalitat(), nacionalitat))
                // la població va dins de l'adreça: es filtra només per la població, no per tota l'adreça
                // (si no, "mallorca" o un número coincidirien amb el carrer o el codi postal)
                .filter(c -> conte(usuariLogic.extreurPoblacio(c.getAdreca()), poblacio))
                .toList();
    }

    // true si no se ha escrito nada, o si el valor contiene el texto (sin mayúsculas/minúsculas)
    private boolean conte(String valor, String cerca) {
        return cerca == null || cerca.isBlank()
                || (valor != null && valor.toLowerCase().contains(cerca.trim().toLowerCase()));
    }

    
 
    /**
     * Valida a un cliente: pasa a estado Validat y queda registrado qué agente
     * lo validó.
     */
    public void validarClient(String dniClient, Agent agentValidador) {
        validarClient(dniClient, agentValidador, null);
    }

    /**
     * Igual que el anterior, pero además puede fijar la condición premium.
     * premium == null significa "no tocar": se conserva el valor que ya tenía
     * el cliente.
     */
    @Transactional
    public void validarClient(String dniClient, Agent agentValidador, Boolean premium) {
        // solo puede validar admin 
        comprovarAdministrador(agentValidador, MISSATGE_NOMES_ADMIN);

        // Se busca al cliente por DNI (su clave primaria); si no existe, error
        Client client = clientRepository.findById(dniClient)
                .orElseThrow(() -> new IllegalArgumentException("El client no existeix: " + dniClient));

        // Solo se puede validar/rechazar si está pendiente (evita validar dos veces o cambiar uno ya rechazado)
        if (!usuariLogic.potSerValidatORebutjat(client)) {
            throw new IllegalStateException("Aquest client no està en estat pendent de validació.");
        }

        // Se actualiza el estado y se guarda quién lo validó (el @ManyToOne agentValidador de Client)
        client.setEstatValidacio(Enums.EstatValidacio.Validat);
        client.setAgentValidador(agentValidador);
        if (premium != null) {
            client.setPremium(premium);
        }
        // UPDATE en MySQL
        clientRepository.save(client);
    }

    /**
     * Valida a un cliente asignándole un rol: "Client" (por defecto), "Agent" o
     * "Administrador". Si el rol es Client, es la validación normal. Si es
     * Agent o Administrador, solo lo puede hacer un administrador, y el cliente
     * pasa a ser un Agent con ese rol.
     */
// CAMBIO 1: este método pasa a ser un atajo de una línea (sin @Transactional)
    public void validarClientAmbRol(String dniClient, String rol, Agent validador) {
        validarClientAmbRol(dniClient, rol, validador, null);
    }

    /**
     * Igual que el anterior, pero con la condición premium. Solo se aplica si
     * el rol final es Client; si el cliente pasa a Agent o Administrador queda
     * como no premium.
     */
// CAMBIO 2: el método con todo el cuerpo ahora recibe también "Boolean premium"
    @Transactional
    public void validarClientAmbRol(String dniClient, String rol, Agent validador, Boolean premium) {
        // Rol vacío o "Client": validación normal, no hay conversión
        if (rol == null || rol.isBlank() || rol.equalsIgnoreCase("Client")) {
            validarClient(dniClient, validador, premium);   // CAMBIO 2: se pasa premium
            return;
        }

        // Seguridad en el servidor: aunque el desplegable solo se vea al administrador,
        // alguien podría enviar el POST a mano, así que se comprueba aquí también
        comprovarAdministrador(validador, "Només un administrador pot assignar el rol d'agent o administrador.");

        // Se traduce el texto del formulario al enum (Agent o Administrador)
        Enums.Rol nouRol;
        try {
            nouRol = Enums.Rol.valueOf(rol);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol no vàlid: " + rol);
        }

        Client client = clientRepository.findById(dniClient)
                .orElseThrow(() -> new IllegalArgumentException("El client no existeix: " + dniClient));

        if (!usuariLogic.potSerValidatORebutjat(client)) {
            throw new IllegalStateException("Aquest client no està en estat pendent de validació.");
        }

        // En JPA no se puede cambiar el tipo de una entidad ya guardada (un Client no pasa a ser Agent).
        // Se crea un Agent con los mismos datos, ya Validat y con el administrador como validador
        Agent nouAgent = new Agent(client.getDniUsuari(), client.getEmail(), client.getPasswordHash(),
                client.getDataCreacio(), client.getNom(), client.getCognoms(),
                client.getDataCaducitatDni(), client.getTipusLlicencia(), client.getDataCaducitatLlicencia(),
                client.getAdreca(), client.getNacionalitat(),
                Enums.EstatValidacio.Validat, validador, false, client.getDescomptePremium(), // CAMBIO 3: false en vez de client.getPremium()
                nouRol);
        // dataNaixement y telefon no están en el constructor: se copian del client al nuevo agent
        nouAgent.setDataNaixement(client.getDataNaixement());
        nouAgent.setTelefon(client.getTelefon());
        // Se borra el Client y se fuerza el DELETE (flush) ANTES de insertar el Agent con el mismo DNI.
        // Sin el flush, Hibernate ejecutaría el INSERT primero y chocaría con la clave primaria
        clientRepository.delete(client);
        clientRepository.flush();
        clientRepository.save(nouAgent);
    }
    /**
     * Rechaza a un cliente: igual que validarClient, pero el estado final es Rebutjat.
     */
    @Transactional
    public void rebutjarClient(String dniClient, Agent agentValidador) {
        
        comprovarAdministrador(agentValidador, MISSATGE_NOMES_ADMIN);
        
        Client client = clientRepository.findById(dniClient)
                .orElseThrow(() -> new IllegalArgumentException("El client no existeix: " + dniClient));
 
        // Misma regla: solo se puede rechazar a un cliente pendiente
        if (!usuariLogic.potSerValidatORebutjat(client)) {
            throw new IllegalStateException("Aquest client no està en estat pendent de validació.");
        }
 
        client.setEstatValidacio(Enums.EstatValidacio.Rebutjat);
        client.setAgentValidador(agentValidador);
        clientRepository.save(client);
    }
    
    /**
     * Modifica los datos personales de un cliente (lo usa la pantalla
     * "Revisa"). El DNI no se puede cambiar (es la clave primaria) y la
     * contraseña tampoco se toca desde aquí. Aplica las mismas reglas
     * que el registro.
     */
    @Transactional
    public void actualitzarDades(String emailActor /* para comprobar quien esta actualizando */, String dni, String nom, String cognoms, String email, String telefon,
            LocalDate dataNaixement, String nacionalitat, String adreca,
            String tipusLlicencia, LocalDate dataCaducitatDni,
            LocalDate dataCaducitatLlicencia) {
        
        // esto para comprobar que es la persona correcta la que actualiza los datos osea un admin de admin y todo agent solo client client el mismo 
        Usuari actor = usuariRepository.findByEmail(emailActor)
                .orElseThrow(() -> new IllegalStateException("Cal estar identificat per modificar dades."));
        
        Client client = clientRepository.findById(dni)
                .orElseThrow(() -> new IllegalArgumentException("El client no existeix: " + dni));  
        
        // verifica que puede modificar 
        comprovarPotModificar(actor, client);
        
        
        
        if (esBuit(nom) || esBuit(cognoms) || esBuit(email) || esBuit(nacionalitat)
                || esBuit(adreca) || esBuit(tipusLlicencia)) {
            throw new IllegalArgumentException("Falten camps obligatoris.");
        }
        if (dataCaducitatDni == null || dataCaducitatLlicencia == null) {
            throw new IllegalArgumentException("Les dates de caducitat són obligatòries.");
        }

        // El correu es el usuario del login: no puede repetirse (se ignora si no ha cambiado)
        String emailNet = email.trim();
        if (!emailNet.equalsIgnoreCase(client.getEmail()) && usuariRepository.existsByEmail(emailNet)) {
            throw new IllegalArgumentException("Ja existeix un usuari amb aquest correu.");
        }

        if (!usuariLogic.isDocumentValid(dataCaducitatDni)) {
            throw new IllegalArgumentException("El document d'identificació està caducat.");
        }
        if (!usuariLogic.isDocumentValid(dataCaducitatLlicencia)) {
            throw new IllegalArgumentException("El permís de conduir està caducat.");
        }
        if (!usuariLogic.isDataNaixementValida(dataNaixement)) {
            throw new IllegalArgumentException("La data de naixement no pot ser futura.");
        }

        String telefonNet = usuariLogic.normalitzarTelefon(telefon);
        if (telefonNet == null) {
            throw new IllegalArgumentException("El telèfon és obligatori.");
        }
        if (!usuariLogic.isTelefonValid(telefonNet)) {
            throw new IllegalArgumentException("El format del telèfon no és vàlid (ex. +34 612345678).");
        }

        client.setNom(nom.trim());
        client.setCognoms(cognoms.trim());
        client.setEmail(emailNet);
        client.setTelefon(telefonNet);
        client.setDataNaixement(dataNaixement);
        client.setNacionalitat(nacionalitat.trim());
        client.setAdreca(adreca.trim());
        client.setTipusLlicencia(tipusLlicencia.trim());
        client.setDataCaducitatDni(dataCaducitatDni);
        client.setDataCaducitatLlicencia(dataCaducitatLlicencia);
        clientRepository.save(client);
    }
    
    // Lo hago como método aparte, sin tocar actualitzarDades, porque ese también lo 
    //usa el cliente para editar su propio perfil (línea ~493). Si metiera premium ahí, un cliente podría darse premium a sí mismo.
    // metodo para poder asignar premium 
    @Transactional
    public void canviarPremium(String emailActor, String dni, boolean premium) {
        Usuari actor = usuariRepository.findByEmail(emailActor)
                .orElseThrow(() -> new IllegalStateException("Cal estar identificat per modificar dades."));
        if (!(actor instanceof Agent)) {
            throw new IllegalStateException("Només un agent o administrador pot canviar la condició premium.");
        }
        Client client = clientRepository.findById(dni)
                .orElseThrow(() -> new IllegalArgumentException("El client no existeix: " + dni));
        if (client instanceof Agent) {
            throw new IllegalArgumentException("Els agents i administradors no poden ser premium.");
        }
        client.setPremium(premium);
        clientRepository.save(client);
    }
    private void comprovarPotModificar(Usuari actor, Client objectiu) {
        if (actor.getDniUsuari().equals(objectiu.getDniUsuari())) {
            return;
        }
        switch (AuthorityType.of(actor)) {
            case ADMINISTRADOR:
                return;
            case AGENT:
                if (AuthorityType.of(objectiu) == AuthorityType.CLIENT) {
                    return;
                }
                break;
            default:
                break;
        }
        throw new IllegalStateException("No tens permís per modificar les dades d'aquest usuari.");
    }
    private boolean esBuit(String s) {
        return s == null || s.isBlank();
    }
    
    //PARA EL TEMA DE DESDE CLIENT PODER CANVIAR DOCUMENT, HACER RESET CRUD DOCUMENTOS 
    
    // ============================================================================
    // ENGANXA AQUESTS MÈTODES DINS DE ClientService (abans de l'últim "}" de la classe).
    //
    // No cal afegir camps ni modificar el constructor: ClientService ja té
    // clientRepository, usuariRepository, usuariLogic i documentacioRepository.
    // Imports que potser et falten: Documentacio, MultipartFile, IOException.
    //
    // Qui els crida: PerfilClientController (pantalla /client/perfil).
    // ============================================================================
    /*
     * Busca el cliente que ha iniciado sesión.
     * Recibe el email del login (auth.getName()), NUNCA un DNI de la URL: así un cliente
     * solo puede acceder a sus propios datos.
     * Lo usan todos los demás métodos de este bloque para saber de quién se trata.
     */
    @Transactional(readOnly = true)
    public Client obtenirClientPropi(String emailActor) {
        Usuari u = usuariRepository.findByEmail(emailActor)
                .orElseThrow(() -> new IllegalStateException("Cal estar identificat."));
        // Usuari es abstracta: aquí se comprueba que sea un Client (un Agent también lo es)
        if (!(u instanceof Client client)) {
            throw new IllegalStateException("L'usuari no és un client.");
        }
        return client;
    }

    /*
     * Devuelve la documentación (DNI y carnet) del cliente que ha iniciado sesión.
     * Devuelve null si todavía no tiene nada guardado en MongoDB.
     * Lo usa la pantalla del perfil para mostrar qué documentos hay y para ver un fichero.
     */
    @Transactional(readOnly = true)
    public Documentacio obtenirDocumentacioPropia(String emailActor) {
        Client client = obtenirClientPropi(emailActor);
        return documentacioService.obtenir(client.getDniUsuari()).orElse(null);
    }

    /*
     * Modifica los datos personales del propio cliente.
     * NO repite validaciones: delega en actualitzarDades (que ya existe más arriba) con
     * el DNI y el email del cliente autenticado.
     * El correo no se cambia porque es el nombre de usuario del login: si cambiara, la
     * sesión activa dejaría de encontrar al usuario.
     */
    @Transactional
    public void actualitzarDadesPropies(String emailActor, String nom, String cognoms, String telefon,
            LocalDate dataNaixement, String nacionalitat, String adreca, String tipusLlicencia,
            LocalDate dataCaducitatDni, LocalDate dataCaducitatLlicencia) {
        Client client = obtenirClientPropi(emailActor);
        actualitzarDades(emailActor, client.getDniUsuari(), nom, cognoms, client.getEmail(), telefon,
                dataNaixement, nacionalitat, adreca, tipusLlicencia, dataCaducitatDni, dataCaducitatLlicencia);
    }

    /*
     * CREATE / UPDATE de un documento propio.
     * tipus = "identitat" o "carnet". Si el cliente ya tenía ese documento, lo sustituye;
     * si no tenía ficha en MongoDB, la crea. Se pueden subir de uno en uno.
     * La validación y el guardado están en DocumentacioService (único sitio).
     */
    @Transactional
    public void desarDocumentPropi(String emailActor, String tipus, MultipartFile fitxer) {
        Client client = obtenirClientPropi(emailActor);
        documentacioService.guardarDocument(client.getDniUsuari(), tipus, fitxer);
    }

    /*
     * DELETE de un documento propio (solo uno de los dos: "identitat" o "carnet").
     * No confundir con AgentService.baixa, que borra toda la documentación de un agente.
     */
    @Transactional
    public void eliminarDocumentPropi(String emailActor, String tipus) {
        Client client = obtenirClientPropi(emailActor);
        documentacioService.eliminarDocument(client.getDniUsuari(), tipus);
    }
    
    
    @Transactional(readOnly = true)
    public Client obtenirClientPerBaixa(String dni) {
        Client client = clientRepository.findById(dni).orElse(null);
        // findById también encuentra agentes, porque Agent extiende Client
        if (client == null || client instanceof Agent || !client.isActiu()) {
            throw new IllegalArgumentException("No s'ha trobat cap client actiu amb aquest DNI.");
        }
        return client;
    }

    @Transactional(readOnly = true)
    public List<Reserva> reservesPendents(String dni) {
        return reservaRepository.findByClient_DniUsuariAndEstatIn(dni,
                List.of(Enums.EstatReserva.Activa, Enums.EstatReserva.Lliurada));
    }

    @Transactional
    public void baixa(String dni) {
        Client client = obtenirClientPerBaixa(dni);
        List<Reserva> pendents = reservesPendents(dni);

        if (!pendents.isEmpty()) {
            // el mismo mensaje detallado que ya tenías
            String detall = pendents.stream()
                    .map(r -> r.getCodiReserva() + " (" + r.getEstat()
                    + ", del " + r.getDataInici() + " al " + r.getDataFi() + ")")
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("No es pot donar de baixa el client " + dni
                    + ": té " + pendents.size() + " reserva(es) pendent(s): " + detall
                    + ". Cal que finalitzin o s'anul·lin abans.");
        }

        client.setActiu(false);
        clientRepository.save(client);
    }
    
 
 
}