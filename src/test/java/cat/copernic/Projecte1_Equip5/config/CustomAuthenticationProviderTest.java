package cat.copernic.Projecte1_Equip5.config;

import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * RN22 / RN52 / RN53 - Proves unitàries del login: verificació segura de la
 * contrasenya, bloqueig per intents fallits i estat del compte (JUnit 5).
 */
class CustomAuthenticationProviderTest {

    private static final String EMAIL = "laura@client.cat";

    private UsuariRepository usuariRepository;
    private CustomUserDetailsService userDetailsService;
    private PasswordEncoder passwordEncoder;
    private CustomAuthenticationProvider provider;
    private Client client;

    @BeforeEach
    void preparar() {
        usuariRepository = mock(UsuariRepository.class);
        userDetailsService = mock(CustomUserDetailsService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenReturn("hashFictici");
        provider = new CustomAuthenticationProvider(userDetailsService, passwordEncoder, usuariRepository);

        LocalDate futur = LocalDate.now().plusYears(2);
        client = new Client("12345678Z", EMAIL, "hashReal", LocalDateTime.now(), "Laura", "Vila",
                futur, "B", futur, "Carrer Major 1, 08221 Terrassa", "Espanyola",
                Enums.EstatValidacio.Validat, null, false, null);
        when(usuariRepository.findByEmail(EMAIL)).thenReturn(Optional.of(client));
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(new UsuariPrincipal(
                EMAIL, "hashReal", AuthorityUtils.createAuthorityList("ROLE_CLIENT"), "12345678Z"));
    }

    private Authentication login(String email, String password) {
        return new UsernamePasswordAuthenticationToken(email, password);
    }

    @Test
    @DisplayName("Credencials correctes: s'autentica i la contrasenya no es guarda al token")
    void credencialsCorrectes() {
        when(passwordEncoder.matches("secret123", "hashReal")).thenReturn(true);

        Authentication auth = provider.authenticate(login(EMAIL, "secret123"));

        assertTrue(auth.isAuthenticated());
        assertNull(auth.getCredentials());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT")));
    }

    @Test
    @DisplayName("Contrasenya incorrecta: error genèric de credencials")
    void contrasenyaIncorrecta() {
        when(passwordEncoder.matches("equivocada1", "hashReal")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> provider.authenticate(login(EMAIL, "equivocada1")));
    }

    @Test
    @DisplayName("Correu inexistent: mateix error i es comprova igualment un hash (mateix temps de resposta)")
    void correuInexistent() {
        when(usuariRepository.findByEmail("no@existeix.cat")).thenReturn(Optional.empty());

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> provider.authenticate(login("no@existeix.cat", "qualsevol1")));

        assertEquals("El correu electrònic o la contrasenya són incorrectes.", ex.getMessage());
        verify(passwordEncoder).matches("qualsevol1", "hashFictici");
    }

    @Test
    @DisplayName("Contrasenya null: error de credencials, mai un NullPointerException")
    void contrasenyaNull() {
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(login(EMAIL, null)));
    }

    @Test
    @DisplayName("Compte donat de baixa: no pot entrar encara que la contrasenya sigui correcta")
    void compteDeBaixa() {
        when(passwordEncoder.matches("secret123", "hashReal")).thenReturn(true);
        client.setActiu(false);

        assertThrows(DisabledException.class, () -> provider.authenticate(login(EMAIL, "secret123")));
    }

    @Test
    @DisplayName("Client pendent de validació: no pot entrar")
    void clientPendent() {
        when(passwordEncoder.matches("secret123", "hashReal")).thenReturn(true);
        client.setEstatValidacio(Enums.EstatValidacio.Pendent);

        assertThrows(DisabledException.class, () -> provider.authenticate(login(EMAIL, "secret123")));
    }

    @Test
    @DisplayName("Després de 5 intents fallits el correu queda bloquejat, fins i tot amb la contrasenya correcta")
    void bloqueigPerIntents() {
        when(passwordEncoder.matches("equivocada1", "hashReal")).thenReturn(false);
        when(passwordEncoder.matches("secret123", "hashReal")).thenReturn(true);

        for (int i = 0; i < 5; i++) {
            assertThrows(BadCredentialsException.class, () -> provider.authenticate(login(EMAIL, "equivocada1")));
        }

        assertThrows(LockedException.class, () -> provider.authenticate(login(EMAIL, "secret123")));
    }

    @Test
    @DisplayName("emmascarar: als logs el correu no apareix sencer")
    void emmascarar() {
        assertEquals("l***@client.cat", CustomAuthenticationProvider.emmascarar(EMAIL));
        assertEquals("(buit)", CustomAuthenticationProvider.emmascarar(" "));
        assertEquals("x***", CustomAuthenticationProvider.emmascarar("xyz"));
    }
}
