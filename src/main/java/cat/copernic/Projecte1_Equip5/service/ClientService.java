package cat.copernic.Projecte1_Equip5.service;

import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.repository.ClientRepository;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final UsuariRepository usuariRepository;
    private final PasswordEncoder passwordEncoder;

    public ClientService(ClientRepository clientRepository,
                         UsuariRepository usuariRepository,
                         PasswordEncoder passwordEncoder) {
        this.clientRepository = clientRepository;
        this.usuariRepository = usuariRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void registrar(String dni, String email, String password, String nom, String cognoms,
                          LocalDate dataCaducitatDni, String tipusLlicencia,
                          LocalDate dataCaducitatLlicencia, String targeta, String adreca,
                          String nacionalitat, MultipartFile imatgeDocument, MultipartFile imatgeLlicencia) {

        if (usuariRepository.existsById(dni)) {
            throw new IllegalArgumentException("Ja existeix un usuari amb aquest DNI.");
        }
        if (usuariRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ja existeix un usuari amb aquest correu.");
        }

        Client client = new Client(dni, email, passwordEncoder.encode(password), LocalDateTime.now(),
                nom, cognoms, dataCaducitatDni, tipusLlicencia, dataCaducitatLlicencia,
                targeta, adreca, nacionalitat, Enums.EstatValidacio.Pendent, null, false, null);

        clientRepository.save(client);

        // Aquí irá el guardado de las imágenes en MongoDB.
    }
}