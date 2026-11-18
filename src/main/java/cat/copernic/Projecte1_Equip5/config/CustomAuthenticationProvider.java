package cat.copernic.Projecte1_Equip5.config;

import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Usuari;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proveedor de autenticación propio: busca el usuario por email y comprueba que
 * la contraseña introducida coincide con el hash guardado. Además, antes de
 * dejar entrar, comprueba el estado de la cuenta: tiene que estar activa y, si
 * es un cliente, validada (ni pendiente ni rechazada). Lo registra
 * {@link WebSecurityConfig} y lo usa Spring Security en el login.
 */
@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    /* CustomAuthenticationProvider es la pieza que decide si un login es válido o no. Cuando alguien pulsa “Entrar”, 
       Spring le pasa el email y la contraseña escritos, y esta clase responde “sí, es quien dice ser” o “no”. */
    private final CustomUserDetailsService customUserDetailsService;
    /* Busca al usuario en la BD usando CustomUserDetailsService */
 /* Cargar los datos del usuario en el formato que entiende Spring Security (email, hash, roles) */
    private final PasswordEncoder passwordEncoder;
    /* Compara la contraseña escrita con el hash guardado, usando PasswordEncoder. */
    private final UsuariRepository usuariRepository;

    // RN04: registre en fitxer dels intents d'accés fallits, bloquejos i accessos correctes
    private static final Logger LOG = LoggerFactory.getLogger(CustomAuthenticationProvider.class);

    // RN52: hash BCrypt fictici. Quan el correu no existeix es compara igualment la contrasenya
    // contra aquest hash perquè el temps de resposta sigui el mateix que amb un correu real i
    // no es pugui descobrir quins comptes existeixen mirant el temps que tarda el login.
    private final String hashFictici;

    // RN53: protección contra adivinar contraseñas. Tras MAX_INTENTS fallos seguidos
    // con el mismo correo DESDE LA MISMA IP se bloquea el acceso durante BLOQUEIG_MS
    // (se aplica también a correos que no existen, para no revelar qué cuentas hay).
    // Se cuenta por correo + IP para que alguien desde otro equipo no pueda dejar
    // bloqueada la cuenta de otra persona (p. ej. la del administrador).
    // 10 intentos y 5 minutos: un despiste en una demo no bloquea a nadie, y adivinar
    // una contraseña a ese ritmo sigue siendo inviable.
    private static final int MAX_INTENTS = 10;
    private static final long BLOQUEIG_MS = 5 * 60 * 1000L;
    private static final String MISSATGE_CREDENCIALS = "El correu electrònic o la contrasenya són incorrectes.";
    // clave = correo en minúsculas; valor = {fallos, inicio de la ventana, bloqueado hasta}
    private final Map<String, long[]> intentsFallits = new ConcurrentHashMap<>();

    /* Añadido para la validación del estado del cliente */
    // Buscar el usuario en la BD por su email y mirar su estado


    /**
     * Constructor con inyección de dependencias.
     *
     * @param customUserDetailsService servicio que carga los usuarios de la BD
     * @param passwordEncoder codificador de contraseñas
     * @param usuariRepository repositorio para consultar el estado del usuario
     */
    public CustomAuthenticationProvider(CustomUserDetailsService customUserDetailsService,
            PasswordEncoder passwordEncoder,
            UsuariRepository usuariRepository) {
        this.customUserDetailsService = customUserDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.usuariRepository = usuariRepository;
        this.hashFictici = passwordEncoder.encode("contrasenya-fictícia-RN52");
    }

    /**
     * Autentica al usuario con el email y la contraseña recibidos. Pasos: busca
     * el usuario por email; comprueba la contraseña (con el mismo error
     * genérico si el email no existe o la contraseña falla, para no revelar qué
     * cuentas existen); rechaza las cuentas dadas de baja; y, si es un cliente,
     * rechaza los que están en estado Pendent o Rebutjat.
     *
     * @param authentication datos introducidos en el login
     * @return la autenticación completada, con los roles del usuario
     * @throws AuthenticationException si el usuario no existe, está
     * deshabilitado o la contraseña es incorrecta
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName() == null ? "" : authentication.getName().trim();
        // RN20: una petició sense contrasenya mai ha de provocar un error intern (NullPointerException)
        String password = authentication.getCredentials() == null ? "" : authentication.getCredentials().toString();
        String clau = email.toLowerCase() + "|" + adrecaRemota(authentication);

        // RN53: si ja hi ha demasiats fallos seguits, ni es mira la contrasenya
        comprovarBloqueig(clau);

        // 1. Obtener entidad de la BD para comprobar el estado de validación
        Usuari usuariBD = usuariRepository.findByEmail(email).orElse(null);
        if (usuariBD == null) {
            // RN52: es gasta el mateix temps que amb un usuari real (vegeu hashFictici)
            passwordEncoder.matches(password, hashFictici);
            registrarFall(clau);
            LOG.warn("Intent d'accés fallit: el correu {} no existeix", emmascarar(email));
            throw new BadCredentialsException(MISSATGE_CREDENCIALS);
        }

        // 3. Cargar el UserDetails y validar la contraseña
        UserDetails user = customUserDetailsService.loadUserByUsername(email);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            registrarFall(clau);
            LOG.warn("Intent d'accés fallit: contrasenya incorrecta per a {}", emmascarar(email));
            throw new BadCredentialsException(MISSATGE_CREDENCIALS);
        }
        // Contraseña correcta: se borra el contador de fallos de este correo
        intentsFallits.remove(clau);

        // aqui comprueba que el usuario que quiere hacer login este activo 
        /* Por qué aquí y no en CustomUserDetailsService: tu proveedor es propio y construye él mismo el token de autenticación, así que no pasa por las 
        comprobaciones automáticas de Spring (cuenta deshabilitada, etc.). Si no pones tu propia comprobación, el agente “de baja” entraría igual. 
        Va después de la contraseña para que alguien que no la conoce no pueda saber qué cuentas existen o están de baja.

        Si el usuario ya tiene la sesión abierta cuando se le da de baja, lo cierra UsuariActiuFilter en la siguiente petición. */
        if (!usuariBD.isActiu()) {
            LOG.warn("Accés rebutjat: el compte {} està donat de baixa", emmascarar(email));
            throw new DisabledException("El teu compte ha estat donat de baixa.");
        }

        // 2. Si es de tipo Client, comprobar que esté Validat
        if (usuariBD instanceof Client client) {
            if (client.getEstatValidacio() == Enums.EstatValidacio.Pendent) {
                LOG.warn("Accés rebutjat: el compte {} està pendent de validació", emmascarar(email));
                throw new DisabledException("El teu compte està pendent de validació per un administrador.");
            } else if (client.getEstatValidacio() == Enums.EstatValidacio.Rebutjat) {
                LOG.warn("Accés rebutjat: el compte {} ha estat rebutjat", emmascarar(email));
                throw new DisabledException("El teu compte ha estat rebutjat.");
            }
        }

        LOG.info("Sessió iniciada: {}", emmascarar(email));
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()); // RN53: la contrasenya no es guarda a la sessió;
    }

    /**
     * Adreça IP des de la qual es fa el login (buida si no es coneix, p. ex. als tests).
     */
    private String adrecaRemota(Authentication authentication) {
        if (authentication.getDetails() instanceof WebAuthenticationDetails detalls
                && detalls.getRemoteAddress() != null) {
            return detalls.getRemoteAddress();
        }
        return "";
    }

    /**
     * Lanza LockedException si el correo está bloqueado por demasiados fallos.
     */
    private void comprovarBloqueig(String clau) {
        long[] estat = intentsFallits.get(clau);
        if (estat != null && estat[2] > System.currentTimeMillis()) {
            LOG.warn("Accés bloquejat temporalment per massa intents fallits: {}", emmascarar(clau));
            throw new LockedException("Has superat el nombre d'intents d'accés permesos. "
                    + "Torna-ho a provar d'aquí a " + (BLOQUEIG_MS / 60000) + " minuts.");
        }
    }

    /**
     * Apunta un intento fallido y, al llegar al máximo, bloquea el correo.
     */
    private void registrarFall(String clau) {
        long ara = System.currentTimeMillis();
        intentsFallits.compute(clau, (k, estat) -> {
            // Sin datos, bloqueo caducado o ventana vieja: se empieza de cero
            if (estat == null || estat[2] != 0 && estat[2] <= ara || ara - estat[1] > BLOQUEIG_MS) {
                estat = new long[]{0, ara, 0};
            }
            estat[0]++;
            if (estat[0] >= MAX_INTENTS) {
                estat[2] = ara + BLOQUEIG_MS;
                LOG.warn("Correu {} bloquejat {} minuts després de {} intents fallits",
                        emmascarar(k), BLOQUEIG_MS / 60000, MAX_INTENTS);
            }
            return estat;
        });
        // Evita que el mapa crezca sin límite con correos inventados
        if (intentsFallits.size() > 10_000) {
            intentsFallits.values().removeIf(e -> ara - e[1] > BLOQUEIG_MS && e[2] <= ara);
        }
    }

    /**
     * RN53: als logs mai es desa el correu sencer (és una dada personal): només
     * la primera lletra i el domini (p. ex. j***@exemple.cat).
     */
    static String emmascarar(String email) {
        if (email == null || email.isBlank()) {
            return "(buit)";
        }
        int arroba = email.indexOf('@');
        if (arroba < 1) {
            return email.charAt(0) + "***";
        }
        return email.charAt(0) + "***" + email.substring(arroba);
    }

    /**
     * Indica que este proveedor gestiona logins de usuario y contraseña.
     *
     * @param authentication tipo de autenticación a comprobar
     * @return true si el tipo es soportado
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}