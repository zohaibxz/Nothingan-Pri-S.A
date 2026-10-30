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
 
/**
 * Servicio con la lógica de negocio de los clientes:
 * registro (alta), listado de pendientes y validación/rechazo por parte de un agente.
 */
// @Service: Spring la registra como componente de la capa de servicio,
// para poder inyectarla en los controladores (ClientController, DashBoardController, AgentController)
@Service
public class ClientService {
 
    // Repositorio de clientes (tabla clients en MySQL)
    private final ClientRepository clientRepository;
    // Repositorio de usuarios: para comprobar duplicados de DNI y email (existsById, existsByEmail)
    private final UsuariRepository usuariRepository;
    // Cifra las contraseñas (BCrypt) antes de guardarlas
    private final PasswordEncoder passwordEncoder;
    // Clase con las validaciones de negocio (formato de DNI, fechas, tarjeta, contraseña...)
    private final UsuariLogic usuariLogic; // Utilitzem UsuariLogic
 
    // Inyección por constructor: Spring pasa las cuatro dependencias automáticamente
    public ClientService(ClientRepository clientRepository,
                         UsuariRepository usuariRepository,
                         PasswordEncoder passwordEncoder,
                         UsuariLogic usuariLogic) {
        this.clientRepository = clientRepository;
        this.usuariRepository = usuariRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuariLogic = usuariLogic;
    }
 
    /**
     * Registra un cliente nuevo (lo llama ClientController.registrarClient).
     * Si algo no es válido lanza IllegalArgumentException con el mensaje, que el controlador
     * captura y muestra en el formulario.
     */
    // @Transactional: todo el método es una transacción; si lanza una excepción, se deshace
    // cualquier cambio hecho en la BD
    @Transactional
    public void registrar(String dni, String email, String password, String nom, String cognoms,
                          LocalDate dataCaducitatDni, String tipusLlicencia,
                          LocalDate dataCaducitatLlicencia, String targeta, String adreca,
                          String nacionalitat, MultipartFile imatgeDocument, MultipartFile imatgeLlicencia) {
 
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
 
        // Validaciones de la tarjeta de crédito, en tres pasos:
        // 1) solo números
        if (!usuariLogic.isTargetaFormatValid(targeta)) {
            throw new IllegalArgumentException("Format de targeta incorrecte: només es permeten números.");
        }
        // 2) longitud correcta (13 a 19 dígitos)
        if (!usuariLogic.isTargetaLongitudValida(targeta)) {
            throw new IllegalArgumentException("La longitud de la targeta no és correcta: ha de tenir entre 13 i 19 dígits.");
        }
        // 3) algoritmo de Luhn (el mismo que comprueba @LuhnCheck en la entidad Client)
        if (!usuariLogic.isTargetaLuhnValida(targeta)) {
            throw new IllegalArgumentException("El número de la targeta de crèdit no és vàlid.");
        }
        // Se normaliza el número (por ejemplo, quitando espacios o guiones) antes de guardarlo
        String targetaNeta = usuariLogic.normalitzarTargeta(targeta);
        // ... new Client(..., targetaNeta, ...)
 
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
                targetaNeta, adreca, nacionalitat, Enums.EstatValidacio.Pendent, null, false, null);
 
        // INSERT en MySQL
        clientRepository.save(client);
        
        // falta guardar la documentacio 
 
        // Aquí anirà el desament de les imatges a MongoDB.
        // (De momento imatgeDocument e imatgeLlicencia llegan al método pero NO se usan:
        // las fotos del registro todavía no se guardan en ningún sitio)
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
 
    /**
     * Valida a un cliente: pasa a estado Validat y queda registrado qué agente lo validó.
     */
    @Transactional
    public void validarClient(String dniClient, Agent agentValidador) {
        // Se busca al cliente por DNI (su clave primaria); si no existe, error
        Client client = clientRepository.findById(dniClient)
                .orElseThrow(() -> new IllegalArgumentException("El cliente no existe: " + dniClient));
 
        // Solo se puede validar/rechazar si está pendiente (evita validar dos veces o cambiar uno ya rechazado)
        if (!usuariLogic.potSerValidatORebutjat(client)) {
            throw new IllegalStateException("Aquest client no està en estat pendent de validació.");
        }
 
        // Se actualiza el estado y se guarda quién lo validó (el @ManyToOne agentValidador de Client)
        client.setEstatValidacio(Enums.EstatValidacio.Validat);
        client.setAgentValidador(agentValidador);
        // UPDATE en MySQL
        clientRepository.save(client);
    }
 
    /**
     * Valida a un cliente asignándole un rol: "Client" (por defecto), "Agent" o "Administrador".
     * Si el rol es Client, es la validación normal. Si es Agent o Administrador, solo lo puede hacer
     * un administrador, y el cliente pasa a ser un Agent con ese rol.
     */
    @Transactional
    public void validarClientAmbRol(String dniClient, String rol, Agent validador) {
        // Rol vacío o "Client": validación normal, no hay conversión
        if (rol == null || rol.isBlank() || rol.equalsIgnoreCase("Client")) {
            validarClient(dniClient, validador);
            return;
        }

        // Seguridad en el servidor: aunque el desplegable solo se vea al administrador,
        // alguien podría enviar el POST a mano, así que se comprueba aquí también
        if (validador.getRol() != Enums.Rol.Administrador) {
            throw new IllegalStateException("Només un administrador pot assignar el rol d'agent o administrador.");
        }

        // Se traduce el texto del formulario al enum (Agent o Administrador)
        Enums.Rol nouRol;
        try {
            nouRol = Enums.Rol.valueOf(rol);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol no vàlid: " + rol);
        }

        Client client = clientRepository.findById(dniClient)
                .orElseThrow(() -> new IllegalArgumentException("El cliente no existe: " + dniClient));

        if (!usuariLogic.potSerValidatORebutjat(client)) {
            throw new IllegalStateException("Aquest client no està en estat pendent de validació.");
        }

        // En JPA no se puede cambiar el tipo de una entidad ya guardada (un Client no pasa a ser Agent).
        // Se crea un Agent con los mismos datos, ya Validat y con el administrador como validador
        Agent nouAgent = new Agent(client.getDniUsuari(), client.getEmail(), client.getPasswordHash(),
                client.getDataCreacio(), client.getNom(), client.getCognoms(),
                client.getDataCaducitatDni(), client.getTipusLlicencia(), client.getDataCaducitatLlicencia(),
                client.getNumTargetaCredit(), client.getAdreca(), client.getNacionalitat(),
                Enums.EstatValidacio.Validat, validador, client.getPremium(), client.getDescomptePremium(),
                nouRol);

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
        Client client = clientRepository.findById(dniClient)
                .orElseThrow(() -> new IllegalArgumentException("El cliente no existe: " + dniClient));
 
        // Misma regla: solo se puede rechazar a un cliente pendiente
        if (!usuariLogic.potSerValidatORebutjat(client)) {
            throw new IllegalStateException("Aquest client no està en estat pendent de validació.");
        }
 
        client.setEstatValidacio(Enums.EstatValidacio.Rebutjat);
        client.setAgentValidador(agentValidador);
        clientRepository.save(client);
    }
}
 