package cat.copernic.Projecte1_Equip5.config;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * Usuari autenticat de Spring Security amb el seu DNI inclòs. Spring Security
 * guarda aquest objecte a la sessió (és el "principal" de l'Authentication),
 * així els controladors poden saber el DNI de qui ha iniciat sessió sense
 * consultar la base de dades a cada petició. El crea
 * {@link CustomUserDetailsService} en fer el login.
 */
public class UsuariPrincipal extends User {

    private static final long serialVersionUID = 1L;

    private final String dni;

    /**
     * @param email email de l'usuari (és el nom d'usuari del login)
     * @param passwordHash contrasenya xifrada amb BCrypt
     * @param authorities rols de l'usuari (p. ex. ROLE_CLIENT)
     * @param dni DNI de l'usuari (clau primària de la taula usuaris)
     */
    public UsuariPrincipal(String email, String passwordHash,
            Collection<? extends GrantedAuthority> authorities, String dni) {
        super(email, passwordHash, authorities);
        this.dni = dni;
    }

    /**
     * @return DNI de l'usuari que ha iniciat sessió
     */
    public String getDni() {
        return dni;
    }
}
