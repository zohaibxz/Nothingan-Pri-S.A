package cat.copernic.Projecte1_Equip5.service;

import cat.copernic.Projecte1_Equip5.entity.Agent;
import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Reserva;
import cat.copernic.Projecte1_Equip5.logic.UsuariLogic;
import cat.copernic.Projecte1_Equip5.repository.ClientRepository;
import cat.copernic.Projecte1_Equip5.repository.ReservaRepository;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RN22 - Proves unitàries de la lògica de negoci dels clients (JUnit 5 + Mockito).
 */
@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private UsuariRepository usuariRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Spy
    private UsuariLogic usuariLogic = new UsuariLogic();
    @Mock
    private DocumentacioService documentacioService;
    @Mock
    private ReservaRepository reservaRepository;

    @InjectMocks
    private ClientService clientService;

    private Client clientExemple(String dni, Enums.EstatValidacio estat) {
        Client c = new Client(dni, dni.toLowerCase() + "@client.cat", "hash", LocalDateTime.now(),
                "Laura", "Vila", LocalDate.now().plusYears(2), "B",
                LocalDate.now().plusYears(2), "Carrer Major 1, 08221 Terrassa",
                "Espanyola", estat, null, false, null);
        c.setTelefon("+34 612345678");
        c.setDataNaixement(LocalDate.of(1995, 5, 10));
        return c;
    }

    private Agent agentAmbRol(String dni, Enums.Rol rol) {
        return new Agent(dni, rol.name().toLowerCase() + "@empresa.cat", "hash", LocalDateTime.now(),
                "Marc", "Roca", LocalDate.now().plusYears(5), "B",
                LocalDate.now().plusYears(5), "Carrer Centre 2, 08221 Terrassa",
                "Espanyola", Enums.EstatValidacio.Validat, null, false, null, rol);
    }

    private MockMultipartFile fitxerValid(String nom) {
        return new MockMultipartFile("f", nom, "application/pdf", new byte[]{1, 2, 3, 4});
    }

    // =====================================================================
    // Registre de clients
    // =====================================================================
    @Test
    @DisplayName("registrar: xifra la contrasenya, desa el client com a Pendent i guarda la documentació")
    void registrar_ok() {
        when(usuariRepository.existsById("12345678Z")).thenReturn(false);
        when(usuariRepository.existsByEmail("laura@client.cat")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$encoded");
        MockMultipartFile docDni = fitxerValid("dni.pdf");
        MockMultipartFile docCarnet = fitxerValid("carnet.pdf");

        clientService.registrar("12345678Z", "laura@client.cat", "secret123", "Laura", "Vila",
                LocalDate.now().plusYears(2), "B", LocalDate.now().plusYears(2),
                "Carrer Major 1, 08221 Terrassa", "Espanyola", docDni, docCarnet,
                LocalDate.of(1995, 5, 10), "+34 612345678", true);

        ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(captor.capture());
        assertEquals("$2a$encoded", captor.getValue().getPasswordHash());
        assertEquals(Enums.EstatValidacio.Pendent, captor.getValue().getEstatValidacio());
        assertTrue(captor.getValue().getPremium());
        verify(documentacioService).guardarDocumentacio("12345678Z", docDni, docCarnet);
    }

    @Test
    @DisplayName("registrar: rebutja DNI/email duplicats o documents caducats")
    void registrar_errorsValidacioIDuplicats() {
        MockMultipartFile f = fitxerValid("doc.pdf");
        LocalDate futur = LocalDate.now().plusYears(1);

        // DNI invàlid
        assertThrows(IllegalArgumentException.class, () -> clientService.registrar(
                "INVALID", "a@b.cat", "123456", "Nom", "Cog", futur, "B", futur,
                "Adreça, 08221 Terrassa", "ES", f, f, LocalDate.of(1990, 1, 1), "+34 612345678", false));

        // DNI amb format correcte però lletra incorrecta (12345678 correspon a la Z, no a la A)
        assertThrows(IllegalArgumentException.class, () -> clientService.registrar(
                "12345678A", "a@b.cat", "123456", "Nom", "Cog", futur, "B", futur,
                "Adreça, 08221 Terrassa", "ES", f, f, LocalDate.of(1990, 1, 1), "+34 612345678", false));

        // DNI duplicat
        when(usuariRepository.existsById("12345678Z")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> clientService.registrar(
                "12345678Z", "a@b.cat", "123456", "Nom", "Cog", futur, "B", futur,
                "Adreça, 08221 Terrassa", "ES", f, f, LocalDate.of(1990, 1, 1), "+34 612345678", false));

        verify(clientRepository, never()).save(any());
    }

    // =====================================================================
    // Validació, rebuig i assignació de rol (Admin)
    // =====================================================================
    @Test
    @DisplayName("validarClient i rebutjarClient: només un Administrador pot processar clients Pendents")
    void validarIRebutjarClient() {
        Agent agentNormal = agentAmbRol("11111111A", Enums.Rol.Agent);
        Agent admin = agentAmbRol("22222222B", Enums.Rol.Administrador);
        Client pendent = clientExemple("12345678A", Enums.EstatValidacio.Pendent);

        assertThrows(IllegalStateException.class,
                () -> clientService.validarClient("12345678A", agentNormal));

        when(clientRepository.findById("12345678A")).thenReturn(Optional.of(pendent));
        clientService.validarClient("12345678A", admin, true);

        assertEquals(Enums.EstatValidacio.Validat, pendent.getEstatValidacio());
        assertSame(admin, pendent.getAgentValidador());
        assertTrue(pendent.getPremium());

        // Si ja està Validat no es pot tornar a rebutjar
        assertThrows(IllegalStateException.class,
                () -> clientService.rebutjarClient("12345678A", admin));
    }

    @Test
    @DisplayName("validarClientAmbRol: converteix un Client Pendent en Agent executant delete + flush abans de save")
    void validarClientAmbRol_converteixAAgent() {
        Agent admin = agentAmbRol("22222222B", Enums.Rol.Administrador);
        Client pendent = clientExemple("12345678A", Enums.EstatValidacio.Pendent);
        pendent.setPremium(true);
        when(clientRepository.findById("12345678A")).thenReturn(Optional.of(pendent));

        clientService.validarClientAmbRol("12345678A", "Agent", admin, true);

        InOrder ordre = inOrder(clientRepository);
        ordre.verify(clientRepository).delete(pendent);
        ordre.verify(clientRepository).flush();
        ArgumentCaptor<Agent> captor = ArgumentCaptor.forClass(Agent.class);
        ordre.verify(clientRepository).save(captor.capture());

        assertEquals(Enums.Rol.Agent, captor.getValue().getRol());
        assertEquals(Enums.EstatValidacio.Validat, captor.getValue().getEstatValidacio());
        assertFalse(captor.getValue().getPremium());
    }

    // =====================================================================
    // Modificació de dades, permisos, premium i baixa
    // =====================================================================
    @Test
    @DisplayName("actualitzarDades: un client no pot modificar les dades d'un altre client")
    void actualitzarDades_sensePermis() {
        Client actor = clientExemple("11111111A", Enums.EstatValidacio.Validat);
        Client objectiu = clientExemple("22222222B", Enums.EstatValidacio.Validat);

        when(usuariRepository.findByEmail(actor.getEmail())).thenReturn(Optional.of(actor));
        when(clientRepository.findById("22222222B")).thenReturn(Optional.of(objectiu));

        assertThrows(IllegalStateException.class, () -> clientService.actualitzarDades(
                actor.getEmail(), "22222222B", "Nou", "Cognom", objectiu.getEmail(),
                "+34 612345678", LocalDate.of(1990, 1, 1), "ES", "Carrer 1, 08221 Terrassa",
                "B", LocalDate.now().plusYears(1), LocalDate.now().plusYears(1)));
        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("canviarPremium: permet canviar-ho a clients però no a agents")
    void canviarPremium() {
        Agent agent = agentAmbRol("11111111A", Enums.Rol.Agent);
        Client client = clientExemple("12345678A", Enums.EstatValidacio.Validat);

        when(usuariRepository.findByEmail(agent.getEmail())).thenReturn(Optional.of(agent));
        when(clientRepository.findById("12345678A")).thenReturn(Optional.of(client));
        when(clientRepository.findById("11111111A")).thenReturn(Optional.of(agent));

        clientService.canviarPremium(agent.getEmail(), "12345678A", true);
        assertTrue(client.getPremium());

        assertThrows(IllegalArgumentException.class,
                () -> clientService.canviarPremium(agent.getEmail(), "11111111A", true));
    }

    @Test
    @DisplayName("baixa: impedeix donar de baixa un client amb reserves Actives o Lliurades")
    void baixa_ambReservesPendentsIok() {
        Client client = clientExemple("12345678A", Enums.EstatValidacio.Validat);
        when(clientRepository.findById("12345678A")).thenReturn(Optional.of(client));

        Reserva pendent = new Reserva();
        pendent.setCodiReserva("EX-2026-0010");
        pendent.setEstat(Enums.EstatReserva.Activa);
        pendent.setDataInici(LocalDate.now().plusDays(1));
        pendent.setDataFi(LocalDate.now().plusDays(3));

        when(reservaRepository.findByClient_DniUsuariAndEstatIn(eq("12345678A"), any()))
                .thenReturn(List.of(pendent));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> clientService.baixa("12345678A"));
        assertTrue(e.getMessage().contains("EX-2026-0010"));
        assertTrue(client.isActiu());

        // Sense reserves pendents -> marca actiu = false
        when(reservaRepository.findByClient_DniUsuariAndEstatIn(eq("12345678A"), any()))
                .thenReturn(List.of());

        clientService.baixa("12345678A");

        assertFalse(client.isActiu());
        verify(clientRepository).save(client);
    }
}