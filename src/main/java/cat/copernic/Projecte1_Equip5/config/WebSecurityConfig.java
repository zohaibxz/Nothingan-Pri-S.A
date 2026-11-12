package cat.copernic.Projecte1_Equip5.config;

import cat.copernic.Projecte1_Equip5.entity.AuthorityType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad de la aplicación (Spring Security): define qué
 * rutas son públicas y qué rol necesita cada una. Todo lo demás exige estar
 * autenticado. Las reglas se evalúan en orden y gana la primera que coincide,
 * por eso las rutas más específicas van antes que las generales. Resumen:
 * <ul>
 * <li>Públicas: inicio, login, logout, alta de cliente, acceso denegado y
 * recursos estáticos (css, js e imágenes).</li>
 * <li>Validar y rechazar clientes: solo administrador.</li>
 * <li>Validaciones, clientes, informes, documentación de clientes y bajas
 * (rutas {@code /admin/agent/...}): administrador y agente. El resto de
 * {@code /admin/agent} y {@code /admin}: solo administrador.</li>
 * <li>Vehículos y zona {@code /agent}: administrador y agente.</li>
 * <li>Reserva: buscar y ver vehículos es público (GET); el resto de
 * {@code /client} es solo para clientes.</li>
 * </ul> {@code @Configuration} indica que la clase contiene configuración de
 * Spring, {@code @EnableWebSecurity} activa la seguridad web y {@code @Bean}
 * hace que Spring gestione el objeto que devuelve el método.
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
     * Define las reglas de acceso por ruta, el login y el logout. El login usa
     * la página {@code /login} con el campo {@code email} y, si va bien, lleva
     * a {@code /dashboard}; el logout invalida la sesión, borra las
     * credenciales y elimina la cookie de sesión. Los accesos denegados los
     * gestiona {@link AccesDenegatHandler}.
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
                // Reactivar usuarios dados de baja: administrador y agente. Esta regla va ANTES de /admin/agent/**.
                // El agente solo ve y reactiva CLIENTES: lo impone AgentController + UsuariService en el servidor.
                .requestMatchers("/admin/agent/baixes/**")
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
                .invalidateHttpSession(true) // Destruye la sesión del servidor
                .clearAuthentication(true) // Borra las credenciales del usuario
                .deleteCookies("JSESSIONID") // Elimina la cookie de sesión
                .permitAll())
                .exceptionHandling(ex -> ex.accessDeniedHandler(deniedHandler));
        /*// Desactiva la caché del navegador para evitar ver datos antiguos tras cerrar sesión
            .headers(headers -> headers
                .cacheControl(cache -> cache.disable())
            );*/

        return http.build();
    }
}
