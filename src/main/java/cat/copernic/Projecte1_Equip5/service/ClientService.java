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

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final UsuariRepository usuariRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuariLogic usuariLogic; // Utilitzem UsuariLogic

    public ClientService(ClientRepository clientRepository,
                         UsuariRepository usuariRepository,
                         PasswordEncoder passwordEncoder,
                         UsuariLogic usuariLogic) {
        this.clientRepository = clientRepository;
        this.usuariRepository = usuariRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuariLogic = usuariLogic;
    }

    @Transactional
    public void registrar(String dni, String email, String password, String nom, String cognoms,
                          LocalDate dataCaducitatDni, String tipusLlicencia,
                          LocalDate dataCaducitatLlicencia, String targeta, String adreca,
                          String nacionalitat, MultipartFile imatgeDocument, MultipartFile imatgeLlicencia) {

        // 1. Validacions de negoci mitjançant UsuariLogic
        if (!usuariLogic.isDniValid(dni)) {
            throw new IllegalArgumentException("El format del DNI/NIE no és vàlid.");
        }
        if (!usuariLogic.isDocumentValid(dataCaducitatDni)) {
            throw new IllegalArgumentException("El document d'identificació està caducat.");
        }
        if (!usuariLogic.isDocumentValid(dataCaducitatLlicencia)) {
            throw new IllegalArgumentException("El permís de conduir està caducat.");
        }
        if (!usuariLogic.isPasswordSecure(password)) {
            throw new IllegalArgumentException("La contrasenya ha de tenir com a mínim 6 caràcters.");
        }

        // 2. Comprovacions de duplicats a la BD
        if (usuariRepository.existsById(dni)) {
            throw new IllegalArgumentException("Ja existeix un usuari amb aquest DNI.");
        }
        if (usuariRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ja existeix un usuari amb aquest correu.");
        }
        
        if (!usuariLogic.isTargetaFormatValid(targeta)) {
            throw new IllegalArgumentException("Format de targeta incorrecte: només es permeten números.");
        }
        if (!usuariLogic.isTargetaLongitudValida(targeta)) {
            throw new IllegalArgumentException("La longitud de la targeta no és correcta: ha de tenir entre 13 i 19 dígits.");
        }
        if (!usuariLogic.isTargetaLuhnValida(targeta)) {
            throw new IllegalArgumentException("El número de la targeta de crèdit no és vàlid.");
        }
        String targetaNeta = usuariLogic.normalitzarTargeta(targeta);
        // ... new Client(..., targetaNeta, ...)

        // 3. Creació i desament de l'entitat Client
        Client client = new Client(dni, email, passwordEncoder.encode(password), LocalDateTime.now(),
                nom, cognoms, dataCaducitatDni, tipusLlicencia, dataCaducitatLlicencia,
                targetaNeta, adreca, nacionalitat, Enums.EstatValidacio.Pendent, null, false, null);

        clientRepository.save(client);

        // Aquí anirà el desament de les imatges a MongoDB.
    }
    
    // --- MÉTODOS DE VALIDACIÓN USANDO LA CAPA LOGIC ---

    @Transactional(readOnly = true)
    public List<Client> llistarClientsPendents() {
        return clientRepository.findByEstatValidacio(Enums.EstatValidacio.Pendent);
    }

    @Transactional
    public void validarClient(String dniClient, Agent agentValidador) {
        Client client = clientRepository.findById(dniClient)
                .orElseThrow(() -> new IllegalArgumentException("El cliente no existe: " + dniClient));

        if (!usuariLogic.potSerValidatORebutjat(client)) {
            throw new IllegalStateException("Aquest client no està en estat pendent de validació.");
        }

        client.setEstatValidacio(Enums.EstatValidacio.Validat);
        client.setAgentValidador(agentValidador);
        clientRepository.save(client);
    }

    @Transactional
    public void rebutjarClient(String dniClient, Agent agentValidador) {
        Client client = clientRepository.findById(dniClient)
                .orElseThrow(() -> new IllegalArgumentException("El cliente no existe: " + dniClient));

        if (!usuariLogic.potSerValidatORebutjat(client)) {
            throw new IllegalStateException("Aquest client no està en estat pendent de validació.");
        }

        client.setEstatValidacio(Enums.EstatValidacio.Rebutjat);
        client.setAgentValidador(agentValidador);
        clientRepository.save(client);
    }
}