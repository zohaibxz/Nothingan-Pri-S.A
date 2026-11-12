
package cat.copernic.Projecte1_Equip5.service;

import cat.copernic.Projecte1_Equip5.document.ReservaArxiu;
import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Reserva;
import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import cat.copernic.Projecte1_Equip5.repository.ClientRepository;
import cat.copernic.Projecte1_Equip5.repository.ReservaRepository;
import cat.copernic.Projecte1_Equip5.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RN22 - Proves unitàries de la lògica de negoci de reserves (JUnit 5 + Mockito).
 */
@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private HistoricReservaService historicReservaService;
    @Mock
    private ReservaArxiuService reservaArxiuService;

    @InjectMocks
    private ReservaService reservaService;

    // ---------- Utilitats ----------
    private Vehicle vehicleValid() {
        Vehicle v = new Vehicle("1234BCD", "Seat", Enums.CategoriaVehicle.Turisme, "Ibiza",
                2021, 10.0, LocalDate.now().minusDays(10), Enums.Combustible.Gasolina,
                110, "Blau", 5, Enums.Transmissio.Manual, 200, 15,
                new BigDecimal("300.00"), null);
        v.setMin_dies(2);
        v.setEstat(Enums.EstatVehicle.Disponible);
        return v;
    }

    private Client clientValid(String dni, boolean premium) {
        return new Client(dni, dni.toLowerCase() + "@test.cat", "hash", LocalDateTime.now(),
                "Anna", "Puig", LocalDate.now().plusYears(2), "B",
                LocalDate.now().plusYears(2), "Carrer Major 1, 08221 Terrassa",
                "Espanyola", Enums.EstatValidacio.Validat, null, premium, null);
    }

    private Reserva reservaAmb(Long id, Enums.EstatReserva estat, LocalDate inici, LocalDate fi) {
        Vehicle v = vehicleValid();
        Client c = clientValid("12345678A", false);
        return new Reserva(id, "EX-2026-0001", v, c, inici, fi,
                new BigDecimal("480.00"), new BigDecimal("300.00"), estat, LocalDateTime.now());
    }

    // =====================================================================
    // Validació de dates, durada i càlcul d'imports (RF50)
    // =====================================================================
    @Test
    @DisplayName("validarDates: rebutja dates nul·les, inici passat o fi no posterior a inici")
    void validarDates_errors() {
        LocalDate avui = LocalDate.now();
        assertThrows(IllegalArgumentException.class, () -> reservaService.validarDates(null, avui.plusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> reservaService.validarDates(avui, null));
        assertThrows(IllegalArgumentException.class, () -> reservaService.validarDates(avui.minusDays(1), avui.plusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> reservaService.validarDates(avui.plusDays(2), avui.plusDays(2)));
        assertDoesNotThrow(() -> reservaService.validarDates(avui, avui.plusDays(1)));
    }

    @Test
    @DisplayName("validarDiesPermesos: comprova mínim i màxim de dies del vehicle")
    void validarDiesPermesos() {
        Vehicle v = vehicleValid(); // min_dies = 2, max_dies = 15
        LocalDate inici = LocalDate.now().plusDays(1);

        IllegalArgumentException massaCurt = assertThrows(IllegalArgumentException.class,
                () -> reservaService.validarDiesPermesos(v, inici, inici.plusDays(1)));
        assertTrue(massaCurt.getMessage().contains("com a mínim 2 dies"));

        IllegalArgumentException massaLlarg = assertThrows(IllegalArgumentException.class,
                () -> reservaService.validarDiesPermesos(v, inici, inici.plusDays(16)));
        assertTrue(massaLlarg.getMessage().contains("com a màxim 15 dies"));

        assertDoesNotThrow(() -> reservaService.validarDiesPermesos(v, inici, inici.plusDays(5)));
    }

    @Test
    @DisplayName("calcularImportTotal i calcularFianca: aplica preu/hora i descompte premium del 25%")
    void calcularImportIFianca() {
        Vehicle v = vehicleValid(); // 10 €/h, fiança 300.00 €
        LocalDate inici = LocalDate.now().plusDays(1);
        LocalDate fi = inici.plusDays(3); // 3 dies = 72 hores -> 720.00 €

        assertEquals(new BigDecimal("720.00"), reservaService.calcularImportTotal(v, inici, fi));
        assertEquals(new BigDecimal("300.00"), reservaService.calcularFianca(v, clientValid("12345678A", false)));
        assertEquals(new BigDecimal("225.00"), reservaService.calcularFianca(v, clientValid("87654321B", true)));
    }

    @Test
    @DisplayName("calcularFianca: llança excepció si el vehicle no té fiança configurada")
    void calcularFianca_senseFianca() {
        Vehicle v = vehicleValid();
        v.setFianca(null);
        Client c = clientValid("12345678A", false);

        assertThrows(IllegalArgumentException.class, () -> reservaService.calcularFianca(v, c));
    }

    // =====================================================================
    // Disponibilitat i creació de reserva (RF50)
    // =====================================================================
    @Test
    @DisplayName("estaDisponible: false si el vehicle és Inactiu o té reserves solapades")
    void estaDisponible() {
        Vehicle v = vehicleValid();
        LocalDate inici = LocalDate.now().plusDays(2);
        LocalDate fi = inici.plusDays(3);

        v.setEstat(Enums.EstatVehicle.Inactiu);
        assertFalse(reservaService.estaDisponible(v, inici, fi));
        verifyNoInteractions(reservaRepository);

        v.setEstat(Enums.EstatVehicle.Disponible);
        when(reservaRepository.comptarSolapaments(eq("1234BCD"), eq(inici), eq(fi), any())).thenReturn(1L);
        assertFalse(reservaService.estaDisponible(v, inici, fi));

        when(reservaRepository.comptarSolapaments(eq("1234BCD"), eq(inici), eq(fi), any())).thenReturn(0L);
        assertTrue(reservaService.estaDisponible(v, inici, fi));
    }

    @Test
    @DisplayName("crearReserva: crea la reserva Activa, desa a MySQL i registra l'històric")
    void crearReserva_ok() {
        Client client = clientValid("12345678A", true);
        Vehicle vehicle = vehicleValid();
        LocalDate inici = LocalDate.now().plusDays(3);
        LocalDate fi = inici.plusDays(2); // 48 h * 10 = 480.00 €, fiança premium = 225.00 €

        when(clientRepository.findById("12345678A")).thenReturn(Optional.of(client));
        when(vehicleRepository.findById("1234BCD")).thenReturn(Optional.of(vehicle));
        when(reservaRepository.comptarSolapaments(eq("1234BCD"), eq(inici), eq(fi), any())).thenReturn(0L);
        when(reservaRepository.count()).thenReturn(4L);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            r.setIdReserva(10L);
            return r;
        });

        Reserva creada = reservaService.crearReserva("12345678A", "1234BCD", inici, fi);

        assertEquals("EX-" + LocalDate.now().getYear() + "-0005", creada.getCodiReserva());
        assertEquals(Enums.EstatReserva.Activa, creada.getEstat());
        assertEquals(new BigDecimal("480.00"), creada.getImportTotal());
        assertEquals(new BigDecimal("225.00"), creada.getFianca());
        verify(historicReservaService).registrarCanvi(creada);
    }

    @Test
    @DisplayName("crearReserva: llança IllegalStateException si el vehicle ja no està disponible")
    void crearReserva_noDisponible() {
        Client client = clientValid("12345678A", false);
        Vehicle vehicle = vehicleValid();
        LocalDate inici = LocalDate.now().plusDays(3);
        LocalDate fi = inici.plusDays(2);

        when(clientRepository.findById("12345678A")).thenReturn(Optional.of(client));
        when(vehicleRepository.findById("1234BCD")).thenReturn(Optional.of(vehicle));
        when(reservaRepository.comptarSolapaments(eq("1234BCD"), eq(inici), eq(fi), any())).thenReturn(1L);

        assertThrows(IllegalStateException.class,
                () -> reservaService.crearReserva("12345678A", "1234BCD", inici, fi));
        verify(reservaRepository, never()).save(any());
        verifyNoInteractions(historicReservaService);
    }

    // =====================================================================
    // Consulta i anul·lació del client (RF52, RF53, RF55)
    // =====================================================================
    @Test
    @DisplayName("llistarProperes: normalitza pàgina negativa a 0")
    void llistarProperes_paginaNegativa() {
        when(reservaRepository.findByClient_DniUsuariAndEstatInOrderByDataIniciAsc(
                eq("12345678A"), any(), eq(PageRequest.of(0, 5))))
                .thenReturn(new PageImpl<>(List.of()));

        reservaService.llistarProperes("12345678A", -3);

        verify(reservaRepository).findByClient_DniUsuariAndEstatInOrderByDataIniciAsc(
                eq("12345678A"), any(), eq(PageRequest.of(0, 5)));
    }

    @Test
    @DisplayName("obtenirDetallsReserva: denega l'accés si la reserva pertany a un altre client")
    void obtenirDetallsReserva_altreClient() {
        Reserva r = reservaAmb(1L, Enums.EstatReserva.Activa, LocalDate.now().plusDays(5), LocalDate.now().plusDays(7));
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(r));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> reservaService.obtenirDetallsReserva(1L, "99999999Z"));
        assertEquals("No s'ha trobat la reserva.", e.getMessage());
    }

    @Test
    @DisplayName("anullarReservaClient: retorna el 100% si falten més de 2 dies i 0 € si en falten 2 o menys")
    void anullarReservaClient_politicaAntelacio() {
        // Més de 2 dies d'antelació -> devolució total
        Reserva ambAntelacio = reservaAmb(1L, Enums.EstatReserva.Activa,
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(5));
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(ambAntelacio));

        BigDecimal retornat = reservaService.anullarReservaClient(1L, "12345678A");

        assertEquals(new BigDecimal("480.00"), retornat);
        assertEquals(Enums.EstatReserva.Anul_lada, ambAntelacio.getEstat());
        assertNotNull(ambAntelacio.getDataAnulacio());
        verify(historicReservaService).registrarCanvi(ambAntelacio);

        // Exactament 2 dies d'antelació -> devolució 0 €
        Reserva senseAntelacio = reservaAmb(2L, Enums.EstatReserva.Activa,
                LocalDate.now().plusDays(2), LocalDate.now().plusDays(4));
        when(reservaRepository.findById(2L)).thenReturn(Optional.of(senseAntelacio));

        BigDecimal retornatZero = reservaService.anullarReservaClient(2L, "12345678A");

        assertEquals(BigDecimal.ZERO, retornatZero);
        assertEquals(Enums.EstatReserva.Anul_lada, senseAntelacio.getEstat());
    }

    @Test
    @DisplayName("anullarReservaClient: no permet anul·lar una reserva que no sigui Activa")
    void anullarReservaClient_noActiva() {
        Reserva lliurada = reservaAmb(1L, Enums.EstatReserva.Lliurada,
                LocalDate.now(), LocalDate.now().plusDays(2));
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(lliurada));

        assertThrows(IllegalStateException.class,
                () -> reservaService.anullarReservaClient(1L, "12345678A"));
        verify(reservaRepository, never()).save(any());
    }

    // =====================================================================
    // Anul·lació per part de l'agent (RF54 / RF55)
    // =====================================================================
    @Test
    @DisplayName("anullarReservaAgent: aplica el percentatge sobre lloguer + fiança")
    void anullarReservaAgent_ok() {
        // importTotal = 480.00, fianca = 300.00 -> total pagat = 780.00; 50% = 390.00
        Reserva r = reservaAmb(1L, Enums.EstatReserva.Activa,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
        when(reservaRepository.findWithClientAndVehicleByIdReserva(1L)).thenReturn(Optional.of(r));

        BigDecimal devolucio = reservaService.anullarReservaAgent(1L, "  Avaria mecànica  ", new BigDecimal("50"));

        assertEquals(new BigDecimal("390.00"), devolucio);
        assertEquals(Enums.EstatReserva.Anul_lada, r.getEstat());
        assertEquals("Avaria mecànica", r.getMotiuAnulacio());
        assertEquals(new BigDecimal("50.00"), r.getPercentatgeDevolucio());
        verify(reservaRepository).save(r);
        verify(historicReservaService).registrarCanvi(r);
    }

    @Test
    @DisplayName("anullarReservaAgent: valida motiu obligatori i percentatge entre 0 i 100")
    void anullarReservaAgent_validacions() {
        Reserva r = reservaAmb(1L, Enums.EstatReserva.Activa,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
        when(reservaRepository.findWithClientAndVehicleByIdReserva(1L)).thenReturn(Optional.of(r));

        assertThrows(IllegalArgumentException.class,
                () -> reservaService.anullarReservaAgent(1L, "   ", new BigDecimal("50")));
        assertThrows(IllegalArgumentException.class,
                () -> reservaService.anullarReservaAgent(1L, "Motiu", new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class,
                () -> reservaService.anullarReservaAgent(1L, "Motiu", new BigDecimal("101")));
        verify(reservaRepository, never()).save(any());
    }

    // =====================================================================
    // Lliurament de vehicle (RF49)
    // =====================================================================
    @Test
    @DisplayName("motiuNoLliurable: detecta reserves fora de dates o vehicles no disponibles")
    void motiuNoLliurable() {
        LocalDate avui = LocalDate.now();

        Reserva futura = reservaAmb(1L, Enums.EstatReserva.Activa, avui.plusDays(1), avui.plusDays(3));
        assertTrue(reservaService.motiuNoLliurable(futura).contains("a partir del"));

        Reserva caducada = reservaAmb(2L, Enums.EstatReserva.Activa, avui.minusDays(4), avui.minusDays(1));
        assertTrue(reservaService.motiuNoLliurable(caducada).contains("ja ha passat"));

        Reserva inactiu = reservaAmb(3L, Enums.EstatReserva.Activa, avui, avui.plusDays(2));
        inactiu.getVehicle().setEstat(Enums.EstatVehicle.Inactiu);
        assertTrue(reservaService.motiuNoLliurable(inactiu).contains("inactiu"));

        Reserva ok = reservaAmb(4L, Enums.EstatReserva.Activa, avui, avui.plusDays(2));
        assertNull(reservaService.motiuNoLliurable(ok));
    }

    @Test
    @DisplayName("lliurarVehicle: passa la reserva a Lliurada, el vehicle a Llogat i desa fotos")
    void lliurarVehicle_ok() {
        LocalDate avui = LocalDate.now();
        Reserva r = reservaAmb(1L, Enums.EstatReserva.Activa, avui, avui.plusDays(2));
        LocalDateTime ara = LocalDateTime.now().minusMinutes(5);
        List<ReservaArxiu> arxius = List.of(new ReservaArxiu());

        when(reservaRepository.findWithClientAndVehicleByIdReserva(1L)).thenReturn(Optional.of(r));
        when(reservaArxiuService.prepararFotos(eq(1L), eq(ReservaArxiu.Moment.LLIURAMENT), any()))
                .thenReturn(arxius);

        reservaService.lliurarVehicle(1L, ara, "  Sense rascades  ", List.of());

        assertEquals(Enums.EstatReserva.Lliurada, r.getEstat());
        assertEquals(Enums.EstatVehicle.Llogat, r.getVehicle().getEstat());
        assertEquals("Sense rascades", r.getDescripcioLliurament());
        InOrder ordre = inOrder(vehicleRepository, reservaRepository, historicReservaService, reservaArxiuService);
        ordre.verify(vehicleRepository).save(r.getVehicle());
        ordre.verify(reservaRepository).save(r);
        ordre.verify(historicReservaService).registrarCanvi(r);
        ordre.verify(reservaArxiuService).guardar(arxius);
    }

    // =====================================================================
    // Retorn de vehicle (RF48)
    // =====================================================================
    @Test
    @DisplayName("calcularHoresRetard: arrodoneix qualsevol fracció d'hora cap amunt")
    void calcularHoresRetard() {
        LocalDate avui = LocalDate.now();
        Reserva r = reservaAmb(1L, Enums.EstatReserva.Lliurada, avui.minusDays(2), avui);
        r.setDataLliurament(avui.minusDays(2).atTime(10, 0));

        // Límit: avui a les 10:00
        assertEquals(0, reservaService.calcularHoresRetard(r, avui.atTime(10, 0)));
        assertEquals(1, reservaService.calcularHoresRetard(r, avui.atTime(10, 1)));
        assertEquals(2, reservaService.calcularHoresRetard(r, avui.atTime(11, 10)));
    }

    @Test
    @DisplayName("retornarVehicle: descompta retard de la fiança i allibera el vehicle")
    void retornarVehicle_ambRetard() {
        LocalDate avui = LocalDate.now();
        // Lliurat fa 2 dies a les 08:00 -> límit avui a les 08:00. Preu/h = 10 €, fiança = 300 €
        Reserva r = reservaAmb(1L, Enums.EstatReserva.Lliurada, avui.minusDays(2), avui);
        r.setDataLliurament(avui.minusDays(2).atTime(8, 0));
        // Retornat 1 h 15 min tard -> 2 hores de retard -> 20.00 € de descompte -> 280.00 € retornats
        LocalDateTime retorn = avui.atTime(9, 15);

        when(reservaRepository.findWithClientAndVehicleByIdReserva(1L)).thenReturn(Optional.of(r));
        when(reservaArxiuService.prepararFotos(eq(1L), eq(ReservaArxiu.Moment.RETORN), any()))
                .thenReturn(List.of());

        Reserva finalitzada = reservaService.retornarVehicle(
                1L, retorn, "Tot correcte", null, false, List.of());

        assertEquals(Enums.EstatReserva.Finalitzada, finalitzada.getEstat());
        assertEquals(Enums.EstatVehicle.Disponible, finalitzada.getVehicle().getEstat());
        assertEquals(2, finalitzada.getHoresRetard());
        assertEquals(new BigDecimal("20.00"), finalitzada.getDescompteRetard());
        assertEquals(new BigDecimal("280.00"), finalitzada.getImportRetornat());
        verify(historicReservaService).registrarCanvi(finalitzada);
    }

    @Test
    @DisplayName("retornarVehicle: reté tota la fiança si hi ha desperfecte imputable i exigeix incidència")
    void retornarVehicle_desperfecteImputable() {
        LocalDate avui = LocalDate.now();
        Reserva r = reservaAmb(1L, Enums.EstatReserva.Lliurada, avui.minusDays(2), avui);
        r.setDataLliurament(avui.minusDays(2).atTime(8, 0));
        LocalDateTime retorn = avui.atTime(8, 0);

        when(reservaRepository.findWithClientAndVehicleByIdReserva(1L)).thenReturn(Optional.of(r));

        // Sense descripció d'incidència llança error
        assertThrows(IllegalArgumentException.class, () -> reservaService.retornarVehicle(
                1L, retorn, null, "   ", true, List.of()));

        when(reservaArxiuService.prepararFotos(eq(1L), eq(ReservaArxiu.Moment.RETORN), any()))
                .thenReturn(List.of());

        Reserva finalitzada = reservaService.retornarVehicle(
                1L, retorn, null, "Para-xocs trencat", true, List.of());

        assertEquals(new BigDecimal("0.00"), finalitzada.getImportRetornat());
        assertEquals(new BigDecimal("0.00"), finalitzada.getDescompteRetard());
        assertTrue(finalitzada.getDesperfecteImputable());
        assertEquals("Para-xocs trencat", finalitzada.getIncidenciaRetorn());
    }
}