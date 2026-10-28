package cat.copernic.Projecte1_Equip5.service;

import cat.copernic.Projecte1_Equip5.entity.Usuari;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UsuariService {

    private final UsuariRepository usuariRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuariService(UsuariRepository usuariRepository, PasswordEncoder passwordEncoder) {
        this.usuariRepository = usuariRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Buscar cualquier tipo de usuario por email (para login o perfil)
    @Transactional(readOnly = true)
    public Optional<Usuari> cercarPerEmail(String email) {
        return usuariRepository.findByEmail(email);
    }

    // Caso de uso: Reset password / Modificar contrasenya (Compartido)
    @Transactional
    public void canviarContrasenya(String email, String novaContrasenya) {
        Usuari usuari = usuariRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("L'usuari no existeix."));
        
        usuari.setPasswordHash(passwordEncoder.encode(novaContrasenya));
        usuariRepository.save(usuari);
    }

    // Caso de uso: Modificar dades personals generals (Compartido)
    @Transactional
    public Usuari actualitzarDadesPersonals(String email, String nom, String cognoms) {
        Usuari usuari = usuariRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("L'usuari no existeix."));
        
        usuari.setNom(nom);
        usuari.setCognoms(cognoms);
        return usuariRepository.save(usuari);
    }
}

