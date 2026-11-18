package cat.copernic.Projecte1_Equip5.config;

import cat.copernic.Projecte1_Equip5.entity.AuthorityType;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

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

    private static final Logger LOG = LoggerFactory.getLogger(WebSecurityConfig.class);

    private final CustomAuthenticationProvider authenticationProvider;
    private final UsuariRepository usuariRepository;

    /**
     * RN54: si és true, l'aplicació només accepta connexions HTTPS (per
     * defecte false perquè cada membre de l'equip pugui treballar en local
     * sense certificat). Es posa a true a application.properties quan hi ha
     * HTTPS configurat.
     */
    @Value("${app.https.obligatori:false}")
    private boolean httpsObligatori;

    /**
     * Constructor con inyección del proveedor de autenticación.
     *
     * @param authenticationProvider proveedor que valida email y contraseña
     */
    public WebSecurityConfig(CustomAuthenticationProvider authenticationProvider,
            UsuariRepository usuariRepository) {
        this.authenticationProvider = authenticationProvider;
        this.usuariRepository = usuariRepository;
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
                // Si un usuari es dóna de baixa mentre té la sessió oberta, se li tanca a la següent petició
                .addFilterBefore(new UsuariActiuFilter(usuariRepository), AuthorizationFilter.class)
                .authorizeHttpRequests(auth -> auth
                // Permite acceso libre a login, pantalla de acceso denegado y recursos estáticos (CSS, JS e imágenes)
                .requestMatchers("/", "/index", "/login", "/logout", "/client/alta", "/acces-denegat", "/error", "/css/**", "/js/**", "/img/**").permitAll()
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
                // RN21: el missatge diu el motiu real (compte pendent, de baixa, bloquejat...) sense
                // revelar detalls interns; es guarda a la sessió i login.html el mostra
                .failureHandler((request, response, exception) -> {
                    String missatge;
                    if (exception instanceof DisabledException || exception instanceof LockedException) {
                        missatge = exception.getMessage(); // missatges escrits per CustomAuthenticationProvider
                    } else if (exception instanceof BadCredentialsException) {
                        missatge = "El correu electrònic o la contrasenya són incorrectes.";
                    } else {
                        // RN04: error inesperat durant el login: es deixa al log per al personal de manteniment
                        LOG.error("Error inesperat durant l'inici de sessió", exception);
                        missatge = "Ara mateix no s'ha pogut iniciar la sessió. Torna-ho a provar d'aquí a una estona.";
                    }
                    request.getSession().setAttribute("ERROR_LOGIN", missatge);
                    response.sendRedirect(request.getContextPath() + "/login?error");
                })
                .permitAll())
                .logout(logout -> logout
                .logoutUrl("/logout") // ruta del enlace 
                .logoutSuccessUrl("/login?logout") // Redirección tras tancar sessió
                .invalidateHttpSession(true) // Destruye la sesión del servidor
                .clearAuthentication(true) // Borra las credenciales del usuario
                .deleteCookies("JSESSIONID") // Elimina la cookie de sesión
                .permitAll())
                .exceptionHandling(ex -> ex.accessDeniedHandler(deniedHandler))
                // RN53 / RN54: capçaleres de seguretat. Spring Security ja envia X-Content-Type-Options,
                // X-Frame-Options i Cache-Control (les dades personals no queden a la memòria cau del navegador).
                .headers(headers -> headers
                .httpStrictTransportSecurity(hsts -> hsts
                .includeSubDomains(true)
                .maxAgeInSeconds(31536000)) // el navegador recordarà 1 any que només s'ha d'entrar per HTTPS
                .referrerPolicy(referrer -> referrer
                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)) // no envia la URL a webs externes
                .contentSecurityPolicy(csp -> csp
                .policyDirectives("frame-ancestors 'none'; base-uri 'self'; form-action 'self'; object-src 'none'")));

        // RN54: dades entre el navegador i el servidor sempre xifrades (HTTPS) quan s'activa.
        // NOTA: http.requiresChannel(...) s'ha tret perquè en aquesta versió de Spring Security
        // llança NoClassDefFoundError (ChannelDecisionManager) i l'aplicació no arrenca amb el perfil "https".
        // No fa falta: amb server.ssl.enabled=true (application-https.properties) Tomcat només
        // atén connexions TLS pel port 8443, així que no hi ha cap canal HTTP sense xifrar.
        // La propietat app.https.obligatori es manté al fitxer de propietats per documentar-ho.
        /*
        if (httpsObligatori) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }
        */
        /*// Desactiva la caché del navegador para evitar ver datos antiguos tras cerrar sesión
            .headers(headers -> headers
                .cacheControl(cache -> cache.disable())
            );*/

        return http.build();
    }
}