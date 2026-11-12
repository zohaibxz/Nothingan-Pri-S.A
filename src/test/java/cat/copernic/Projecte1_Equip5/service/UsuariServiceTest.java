package cat.copernic.Projecte1_Equip5.service;

import cat.copernic.Projecte1_Equip5.entity.Agent;
import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.logic.UsuariLogic;
import cat.copernic.Projecte1_Equip5.logic.UsuariLogic;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RN22 - Proves unitàries de gestió comuna d'usuaris, contrasenyes i reactivacions (JUnit 5 + Mockito).
 */
@ExtendWith(MockitoExtension.class)
class UsuariServiceTest {

    @Mock
    private UsuariRepository usuariRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Spy
    private UsuariLogic usuariLogic = new UsuariLogic();

    @InjectMocks
    private UsuariService usuariService;

    private Client clientDeBaixa(String dni, String nom, String cognoms) {
        Client c = new Client(dni, dni.toLowerCase() + "@test.cat", "hashActual", LocalDateTime.now(),
                nom, cognoms, LocalDate.now().plusYears(1), "B", LocalDate.now().plusYears(1),
                "Adreça, 08221 Terrassa", "ES", Enums.EstatValidacio.Validat, null, false, null);
        c.setActiu(false);
        return c;
    }

    private Agent agentDeBaixa(String dni) {
        Agent a = new Agent(dni, "agent@test.cat", "hashActual", LocalDateTime.now(),
                "Oriol", "Soler", LocalDate.now().plusYears(1), "B", LocalDate.now().plusYears(1),
                "Adreça, 08221 Terrassa", "ES", Enums.EstatValidacio.Validat, null, false, null, Enums.Rol.Agent);
        a.setActiu(false);
        return a;
    }

    @Test
    @DisplayName("canviarContrasenya: valida contrasenya actual, confirmació i diferència amb l'anterior")
    void canviarContrasenya() {
        Client c = clientDeBaixa("12345678A", "Clara", "Pons");
        when(usuariRepository.findByEmail(c.getEmail())).thenReturn(Optional.of(c));
        when(passwordEncoder.matches("incorrecta", "hashActual")).thenReturn(false);
        when(passwordEncoder.matches("actual123", "hashActual")).thenReturn(true);
        when(passwordEncoder.matches("actual123", "hashActual")).thenReturn(true);
        when(passwordEncoder.matches("nova1234", "hashActual")).thenReturn(false);
        when(passwordEncoder.encode("nova1234")).thenReturn("hashNova");

        assertThrows(IllegalArgumentException.class,
                () -> usuariService.canviarContrasenya(c.getEmail(), "incorrecta", "nova1234", "nova1234"));
        assertThrows(IllegalArgumentException.class,
                () -> usuariService.canviarContrasenya(c.getEmail(), "actual123", "nova1234", "diferent"));
        assertThrows(IllegalArgumentException.class,
                () -> usuariService.canviarContrasenya(c.getEmail(), "actual123", "actual123", "actual123"));

        usuariService.canviarContrasenya(c.getEmail(), "actual123", "nova1234", "nova1234");

        assertEquals("hashNova", c.getPasswordHash());
        verify(usuariRepository).save(c);
    }

    @Test
    @DisplayName("llistarUsuarisDeBaixa: filtra només clients quan nomesClients és true i ordena per cognoms")
    void llistarUsuarisDeBaixa() {
        Client c1 = clientDeBaixa("11111111A", "Berta", "Zafra");
        Client c2 = clientDeBaixa("22222222B", "Albert", "Alba");
        Agent a1 = agentDeBaixa("33333333C");

        when(usuariRepository.findByActiuFalse()).thenReturn(List.of(c1, c2, a1));

        List<UsuariService.UsuariBaixa> nomesClients = usuariService.llistarUsuarisDeBaixa(null, true);
        assertEquals(2, nomesClients.size());
        assertEquals("Alba", nomesClients.get(0).cognoms());
        assertEquals("Zafra", nomesClients.get(1).cognoms());

        List<UsuariService.UsuariBaixa> tots = usuariService.llistarUsuarisDeBaixa("oriol", false);
        assertEquals(1, tots.size());
        assertEquals("Agent", tots.get(0).tipus());
    }

    @Test
    @DisplayName("reactivar: impedeix a un agent reactivar un altre agent i reactiva clients de baixa")
    void reactivar() {
        Agent agentBaixa = agentDeBaixa("33333333C");
        Client clientBaixa = clientDeBaixa("11111111A", "Berta", "Zafra");

        when(usuariRepository.findById("33333333C")).thenReturn(Optional.of(agentBaixa));
        when(usuariRepository.findById("11111111A")).thenReturn(Optional.of(clientBaixa));

        assertThrows(IllegalArgumentException.class, () -> usuariService.reactivar("33333333C", true));

        usuariService.reactivar("11111111A", true);

        assertTrue(clientBaixa.isActiu());
        verify(usuariRepository).save(clientBaixa);
    }
}