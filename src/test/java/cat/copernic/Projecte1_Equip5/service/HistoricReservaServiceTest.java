package cat.copernic.Projecte1_Equip5.service;

import cat.copernic.Projecte1_Equip5.document.HistoricReserva;
import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Reserva;
import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import cat.copernic.Projecte1_Equip5.repository.HistoricReservaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RN22 - Proves unitàries del registre de l'històric de reserves a MongoDB (JUnit 5 + Mockito).
 */
@ExtendWith(MockitoExtension.class)
class HistoricReservaServiceTest {

    @Mock
    private HistoricReservaRepository historicRepository;

    @InjectMocks
    private HistoricReservaService historicReservaService;

    private Reserva reservaExemple() {
        Vehicle v = new Vehicle("1234BCD", "Seat", Enums.CategoriaVehicle.Turisme, "Leon",
                2022, 12.0, LocalDate.now().minusMonths(6), Enums.Combustible.Híbrid,
                150, "Blanc", 5, Enums.Transmissio.Automàtic, 300, 30,
                new BigDecimal("400.00"), null);
        Client c = new Client("12345678A", "joan@test.cat", "hash", LocalDateTime.now(),
                "Joan", "Serra", LocalDate.now().plusYears(3), "B",
                LocalDate.now().plusYears(3), "Carrer Nou 5, 08221 Terrassa",
                "Espanyola", Enums.EstatValidacio.Validat, null, false, null);
        return new Reserva(7L, "EX-2026-0007", v, c,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(4),
                new BigDecimal("864.00"), new BigDecimal("400.00"),
                Enums.EstatReserva.Activa, LocalDateTime.now());
    }

    @Test
    @DisplayName("registrarCanvi: crea o actualitza el document amb totes les dades de la reserva")
    void registrarCanvi_ok() {
        Reserva r = reservaExemple();
        HistoricReserva existent = new HistoricReserva();
        existent.setIdReserva(7L);
        when(historicRepository.findById(7L)).thenReturn(Optional.of(existent));

        historicReservaService.registrarCanvi(r);

        ArgumentCaptor<HistoricReserva> captor = ArgumentCaptor.forClass(HistoricReserva.class);
        verify(historicRepository).save(captor.capture());
        HistoricReserva desat = captor.getValue();
        assertSame(existent, desat);
        assertEquals("EX-2026-0007", desat.getCodiReserva());
        assertEquals("12345678A", desat.getDniClient());
        assertEquals("Joan Serra", desat.getNomClient());
        assertEquals("1234BCD", desat.getMatricula());
        assertEquals("Activa", desat.getEstat());
    }

    @Test
    @DisplayName("registrarCanvi: converteix DataAccessException de MongoDB en IllegalStateException")
    void registrarCanvi_errorMongo() {
        Reserva r = reservaExemple();
        when(historicRepository.findById(7L)).thenReturn(Optional.empty());
        when(historicRepository.save(any())).thenThrow(new DataAccessResourceFailureException("Error Mongo"));

        assertThrows(IllegalStateException.class, () -> historicReservaService.registrarCanvi(r));
    }
}