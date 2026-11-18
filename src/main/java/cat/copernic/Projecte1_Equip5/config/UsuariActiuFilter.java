package cat.copernic.Projecte1_Equip5.config;

import cat.copernic.Projecte1_Equip5.entity.Usuari;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Tanca la sessió d'un usuari que s'ha donat de baixa mentre estava connectat.
 *
 * El login ja rebutja els comptes de baixa, però només en el moment d'entrar.
 * Aquest filtre ho torna a comprovar a cada petició: si l'usuari de la sessió
 * ja no està actiu, s'invalida la sessió i se'l porta al login amb un avís.
 * No es registra com a @Component perquè Spring Boot l'executaria també fora de
 * la cadena de seguretat; el crea WebSecurityConfig.
 */
public class UsuariActiuFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(UsuariActiuFilter.class);

    private final UsuariRepository usuariRepository;

    public UsuariActiuFilter(UsuariRepository usuariRepository) {
        this.usuariRepository = usuariRepository;
    }

    /**
     * Els fitxers estàtics no necessiten la comprovació (no porten dades i
     * així es evita una consulta a la BD per cada imatge o full d'estils).
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        return ruta.startsWith("/css/") || ruta.startsWith("/js/") || ruta.startsWith("/img/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            // Si l'usuari no es troba a la BD no es toca res (només es tanca si consta com a baixa)
            Usuari usuari = usuariRepository.findByEmail(auth.getName()).orElse(null);
            if (usuari != null && !usuari.isActiu()) {
                LOG.warn("Sessió tancada: el compte {} s'ha donat de baixa", auth.getName().replaceAll("(?<=.).(?=[^@]*@)", "*"));
                SecurityContextHolder.clearContext();
                HttpSession sessio = request.getSession(false);
                if (sessio != null) {
                    sessio.invalidate();
                }
                response.sendRedirect(request.getContextPath() + "/login?baixa");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
