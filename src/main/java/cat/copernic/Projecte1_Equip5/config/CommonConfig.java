package cat.copernic.Projecte1_Equip5.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuració comuna de l'aplicació: beans compartits per la resta de classes.
 * Ara mateix només crea el codificador de contrasenyes, que Spring injecta allà
 * on cal (p. ex. al login i en registrar usuaris). {@code @Configuration}
 * indica a Spring que aquesta classe conté configuració i l'ha de llegir en
 * arrencar.
 */
@Configuration
public class CommonConfig {

    /**
     * Codificador de contrasenyes BCrypt, usat per desar i comprovar
     * contrasenyes. Gràcies a {@code @Bean}, Spring crea l'objecte una sola
     * vegada i el deixa disponible per a qualsevol altra classe; s'exposa amb
     * el tipus general {@link PasswordEncoder}.
     *
     * @return el codificador de contrasenyes
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
