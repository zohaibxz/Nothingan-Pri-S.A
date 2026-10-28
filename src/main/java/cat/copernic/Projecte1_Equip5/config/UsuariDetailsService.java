package cat.copernic.Projecte1_Equip5.config;

import cat.copernic.Projecte1_Equip5.entity.AuthorityType;
import cat.copernic.Projecte1_Equip5.service.*;
import cat.copernic.Projecte1_Equip5.entity.Usuari;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Servicio que carga los usuarios de la BD para Spring Security.
 * Permite que solo accedan a cada URL los usuarios autenticados
 * y con el rol adecuado.
 */
@Service
public class UsuariDetailsService implements UserDetailsService {

   
    //ROL_CLIENT evita escribir el texto "CLIENT" suelto por el código. El repositorio llega 
    //por el constructor (inyección de dependencias) y permite consultar la tabla de usuarios.
    private final UsuariRepository usuariRepository;

    /**
     * Constructor con inyección del repositorio de usuarios.
     *
     * @param usuariRepository repositorio para consultar los usuarios
     */
    public UsuariDetailsService(UsuariRepository usuariRepository) {
        this.usuariRepository = usuariRepository;
    }

    /**
     * Busca un usuario por su email y lo convierte en un UserDetails
     * que Spring Security entiende.
     *
     * @param email email introducido en el login
     * @return datos del usuario (email, contraseña cifrada y rol)
     * @throws UsernameNotFoundException si no existe ningún usuario con ese email
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuari u = usuariRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        return User.withUsername(u.getEmail())
                .password(u.getPasswordHash())   // debe estar cifrada con BCrypt
                .roles(AuthorityType.of(u).name())   // CLIENT, AGENT o ADMINISTRADOR
                .build();
    }

}
 