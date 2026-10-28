package cat.copernic.Projecte1_Equip5.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


@Configuration
public class CommonConfig {
    /* CommonConfig crea el “cifrador de contraseñas” de la aplicación y lo deja disponible para el resto de clases.
    /**
     * Codificador de contraseñas BCrypt, usado para guardar y comprobar contraseñas.
     *
     * @return el codificador de contraseñas
     */
    
    /* Le dice a Spring: “esta clase contiene configuración, léela al arrancar la aplicación”. */
    @Bean
    /* @Bean el objeto que devuelve este método, créalo una sola vez y guárdalo para que cualquier otra clase pueda pedirlo”.
    Aquí crea un BCryptPasswordEncoder, y lo expone con el tipo general PasswordEncoder. */
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


}