package cat.copernic.Projecte1_Equip5.config;

import cat.copernic.Projecte1_Equip5.entity.AuthorityType;
import cat.copernic.Projecte1_Equip5.service.*;
import cat.copernic.Projecte1_Equip5.entity.Usuari;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Servicio que carga los usuarios de la BD para Spring Security. Permite que
 * solo accedan a cada URL los usuarios autenticados y con el rol adecuado.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuariRepository usuariRepository;

    /**
     * Constructor con inyección del repositorio de usuarios.
     *
     * @param usuariRepository repositorio para consultar los usuarios
     */
    public CustomUserDetailsService(UsuariRepository usuariRepository) {
        this.usuariRepository = usuariRepository;
    }

    /**
     * Busca un usuario por su email y lo convierte en un UserDetails que Spring
     * Security entiende. En concreto devuelve un {@link UsuariPrincipal}, que
     * lleva también el DNI, y le asigna el rol de seguridad que calcula
     * {@link AuthorityType#of(Usuari)} (ROLE_CLIENT, ROLE_AGENT o
     * ROLE_ADMINISTRADOR).
     *
     * @param email email introducido en el login
     * @return datos del usuario (email, contraseña cifrada, rol y DNI)
     * @throws UsernameNotFoundException si no existe ningún usuario con ese
     * email
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuari u = usuariRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        // Es retorna un UsuariPrincipal (User + DNI) perquè els controladors puguin
        // llegir el DNI de la sessió sense tornar a consultar la BD.
        return new UsuariPrincipal(
                u.getEmail(),
                u.getPasswordHash(), // ha d'estar xifrada amb BCrypt
                AuthorityUtils.createAuthorityList("ROLE_" + AuthorityType.of(u).name()), // CLIENT, AGENT o ADMINISTRADOR
                u.getDniUsuari());
    }
}
