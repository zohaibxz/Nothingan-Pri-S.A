package cat.copernic.Projecte1_Equip5.service;

import cat.copernic.Projecte1_Equip5.entity.Agent;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.repository.AgentRepository;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RN22 - Proves unitàries d'alta i baixa d'agents i administradors (JUnit 5 + Mockito).
 */
@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private AgentRepository agentRepository;
    @Mock
    private UsuariRepository usuariRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AgentService agentService;

    private Agent nouAgent(String dni, String email, Enums.Rol rol) {
        return new Agent(dni, email, "plainPassword", null,
                "Pol", "Martí", LocalDate.now().plusYears(3), "B",
                LocalDate.now().plusYears(3), "Carrer Gran 10, 08221 Terrassa",
                "Espanyola", Enums.EstatValidacio.Pendent, null, true, null, rol);
    }

    @Test
    @DisplayName("alta: xifra la contrasenya, marca com a Validat i no premium")
    void alta_ok() {
        Agent agent = nouAgent("12345678A", "pol@empresa.cat", Enums.Rol.Agent);
        when(usuariRepository.existsById("12345678A")).thenReturn(false);
        when(usuariRepository.existsByEmail("pol@empresa.cat")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("hashedPwd");
        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

        Agent desat = agentService.alta(agent);

        assertEquals("hashedPwd", desat.getPasswordHash());
        assertEquals(Enums.EstatValidacio.Validat, desat.getEstatValidacio());
        assertFalse(desat.getPremium());
        assertNotNull(desat.getDataCreacio());
    }

    @Test
    @DisplayName("alta: rebutja DNI o correu repetits")
    void alta_duplicats() {
        Agent agent = nouAgent("12345678A", "pol@empresa.cat", Enums.Rol.Agent);
        when(usuariRepository.existsById("12345678A")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> agentService.alta(agent));
        verify(agentRepository, never()).save(any());
    }

    @Test
    @DisplayName("baixa: desactiva un Agent però no permet donar de baixa un Administrador")
    void baixa_agentVsAdministrador() {
        Agent agent = nouAgent("12345678A", "pol@empresa.cat", Enums.Rol.Agent);
        Agent admin = nouAgent("87654321B", "admin@empresa.cat", Enums.Rol.Administrador);

        when(agentRepository.findById("12345678A")).thenReturn(Optional.of(agent));
        when(agentRepository.findById("87654321B")).thenReturn(Optional.of(admin));

        agentService.baixa("12345678A");
        assertFalse(agent.isActiu());
        verify(agentRepository).save(agent);

        assertThrows(IllegalArgumentException.class, () -> agentService.baixa("87654321B"));
    }

    @Test
    @DisplayName("baixaAdministrador: impedeix auto-baixa i impedeix eliminar l'últim administrador actiu")
    void baixaAdministrador_reglesSeguretat() {
        Agent adminActor = nouAgent("11111111A", "admin1@empresa.cat", Enums.Rol.Administrador);
        Agent adminObjectiu = nouAgent("22222222B", "admin2@empresa.cat", Enums.Rol.Administrador);

        when(usuariRepository.findByEmail("admin1@empresa.cat")).thenReturn(Optional.of(adminActor));

        // No es pot donar de baixa a si mateix
        assertThrows(IllegalArgumentException.class,
                () -> agentService.baixaAdministrador("11111111A", "admin1@empresa.cat"));

        // Si només queda 1 administrador actiu, no es pot donar de baixa
        when(agentRepository.findById("22222222B")).thenReturn(Optional.of(adminObjectiu));
        when(agentRepository.findByRolAndActiuTrue(Enums.Rol.Administrador)).thenReturn(List.of(adminActor));

        assertThrows(IllegalArgumentException.class,
                () -> agentService.baixaAdministrador("22222222B", "admin1@empresa.cat"));

        // Amb 2 administradors actius -> baixa correcta
        when(agentRepository.findByRolAndActiuTrue(Enums.Rol.Administrador))
                .thenReturn(List.of(adminActor, adminObjectiu));

        agentService.baixaAdministrador("22222222B", "admin1@empresa.cat");

        assertFalse(adminObjectiu.isActiu());
        verify(agentRepository).save(adminObjectiu);
    }
}