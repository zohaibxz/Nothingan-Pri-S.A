package cat.copernic.Projecte1_Equip5.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utilitats per als logs: identifica qui fa cada acció sense exposar el correu sencer.
 */
public final class LogUtils {

    private LogUtils() {
    }

    /**
     * Identifica qui fa l'acció que es registra al log.
     *
     * @return correu emmascarat i rol de l'usuari autenticat, o "anònim"
     */
    public static String actor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "anònim";
        }
        String rol = auth.getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "")).orElse("?");
        return CustomAuthenticationProvider.emmascarar(auth.getName()) + " (" + rol + ")";
    }
}