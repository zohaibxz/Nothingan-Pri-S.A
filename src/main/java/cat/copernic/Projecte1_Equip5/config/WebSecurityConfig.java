package cat.copernic.Projecte1_Equip5.config;

import cat.copernic.Projecte1_Equip5.entity.AuthorityType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
/**
 * @Configuration - Indica que la classe conté configuració de Spring.
 * @EnableWebSecurity - Activa la configuració de seguretat web de Spring Security.
 * @Bean - Indica que el mètode crea un objecte que Spring ha de gestionar dins del seu contenidor.
 */


/**
 * Configuración de seguridad: define qué rutas son públicas y
 * qué rol necesita cada una. Todo lo demás exige estar autenticado.
 */
@Configuration
@EnableWebSecurity // @EnableWebSecurity activa Spring Security en tu aplicación web. Es el interruptor que enciende la seguridad. 
public class WebSecurityConfig {

    private final CustomAuthenticationProvider authenticationProvider;

    /**
     * Constructor con inyección del proveedor de autenticación.
     *
     * @param authenticationProvider proveedor que valida email y contraseña
     */
    public WebSecurityConfig(CustomAuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    /**
     * Define las reglas de acceso por ruta, el login y el logout.
     *
     * @param http configuración HTTP de Spring Security
     * @param deniedHandler manejador de accesos denegados
     * @return la cadena de filtros de seguridad
     * @throws Exception si la configuración falla
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AccesDenegatHandler deniedHandler) throws Exception {
        http
            .authenticationProvider(authenticationProvider)
            .authorizeHttpRequests(auth -> auth
                // Permite acceso libre a login, pantalla de acceso denegado y recursos estáticos (CSS, JS e imágenes)
                .requestMatchers("/", "/index", "/login", "/logout", "/client/alta", "/acces-denegat", "/css/**", "/js/**", "/img/**").permitAll()
                // Restricciones por rol de AuthorityType
                // añadido estas lineas para que tanto admin como agent pueda acceder al html de mirar los detalles de los clientes que se han registrado..    
                // Validar y rebutjar: solo administrador (también en el servidor, no solo oculto en la pantalla)
                .requestMatchers(HttpMethod.POST, "/admin/agent/validacions/*/validar",
                        "/admin/agent/validacions/*/rebutjar")
                .hasRole(AuthorityType.ADMINISTRADOR.name())
                // Cola de validación, pantalla Revisa, edición y documentos: administrador y agente
                .requestMatchers("/admin/agent/validacions/**")
                .hasAnyRole(AuthorityType.ADMINISTRADOR.name(), AuthorityType.AGENT.name())
                // Gestió de clients i informes: administrador i agent (CRUD Clients i Visualitzar informes del diagrama)
                .requestMatchers("/admin/agent/clients/**", "/admin/agent/informes/**")
                .hasAnyRole(AuthorityType.ADMINISTRADOR.name(), AuthorityType.AGENT.name())
                // CRUD documentació de client (en el diagrama es un <<extend>> de CRUD Clients): administrador y agente.
                // Esta regla va ANTES de /admin/agent/** para que no la tape. Además, DocumentacioController
                // impide en el servidor que un agente toque la documentación de otro agente o administrador.
                .requestMatchers("/admin/agent/*/documentacio/**")
                .hasAnyRole(AuthorityType.ADMINISTRADOR.name(), AuthorityType.AGENT.name())
                .requestMatchers("/admin/agent/**").hasAnyRole(AuthorityType.ADMINISTRADOR.name()) // quitar AuthorityType.AGENT.name()
                .requestMatchers("/vehicles", "/vehicles/**").hasAnyRole(AuthorityType.ADMINISTRADOR.name(), AuthorityType.AGENT.name())
                .requestMatchers("/admin/**", "/api/admin/**").hasRole(AuthorityType.ADMINISTRADOR.name())
                .requestMatchers("/agent/**").hasAnyRole(AuthorityType.ADMINISTRADOR.name(), AuthorityType.AGENT.name())
                // Parte pública del flujo de reserva: buscar y ver vehículos (incluidas sus fotos)
                .requestMatchers(HttpMethod.GET, "/client/reserva/nueva", "/client/vehicles/**", "/client/reserves").permitAll() // para usuario anonimo 
                .requestMatchers("/client/**").hasRole(AuthorityType.CLIENT.name())
                // Denegar cualquier otra ruta si no se ha iniciado sesión
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .defaultSuccessUrl("/dashboard") // Redirige directamente al DashBoardController
                .failureUrl("/login?error")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout") // ruta del enlace 
                .logoutSuccessUrl("/login?logout") // Redirección tras tancar sessió
                .invalidateHttpSession(true)               // Destruye la sesión del servidor
                .clearAuthentication(true)                 // Borra las credenciales del usuario
                .deleteCookies("JSESSIONID")               // Elimina la cookie de sesión
                .permitAll())
            .exceptionHandling(ex -> ex.accessDeniedHandler(deniedHandler));
            /*// Desactiva la caché del navegador para evitar ver datos antiguos tras cerrar sesión
            .headers(headers -> headers
                .cacheControl(cache -> cache.disable())
            );*/  

        return http.build();
    }
}
 