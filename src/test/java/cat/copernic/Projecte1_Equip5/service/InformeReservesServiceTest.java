package cat.copernic.Projecte1_Equip5.service;

import cat.copernic.Projecte1_Equip5.document.HistoricReserva;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.repository.HistoricReservaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RN22 - Proves unitàries de l'informe de reserves per client (JUnit 5 + Mockito).
 */
@ExtendWith(MockitoExtension.class)
class InformeReservesServiceTest {

    @Mock
    private HistoricReservaRepository historicRepository;

    @InjectMocks
    private InformeReservesService informeReservesService;

    private HistoricReserva historic(String dni, String nom, String estat, BigDecimal importTotal) {
        HistoricReserva h = new HistoricReserva();
        h.setDniClient(dni);
        h.setNomClient(nom);
        h.setEstat(estat);
        h.setImportTotal(importTotal);
        return h;
    }

    @Test
    @DisplayName("reservesPerClient: consulta tots o un client concret i filtra les anul·lades si cal")
    void reservesPerClient() {
        HistoricReserva activa = historic("12345678A", "Anna Puig", Enums.EstatReserva.Finalitzada.name(), new BigDecimal("200.00"));
        HistoricReserva anullada = historic("12345678A", "Anna Puig", Enums.EstatReserva.Anul_lada.name(), new BigDecimal("150.00"));

        when(historicRepository.findAll(any(Sort.class))).thenReturn(List.of(activa, anullada));
        when(historicRepository.findByDniClient(eq("12345678A"), any(Sort.class)))
                .thenReturn(List.of(activa, anullada));

        assertEquals(2, informeReservesService.reservesPerClient(null, false).size());
        assertEquals(1, informeReservesService.reservesPerClient(" 12345678A ", true).size());
        assertEquals(Enums.EstatReserva.Finalitzada.name(),
                informeReservesService.reservesPerClient("12345678A", true).get(0).getEstat());
    }

    @Test
    @DisplayName("sumarImports: suma els imports no nuls i retorna 0 si la llista és buida")
    void sumarImports() {
        assertEquals(BigDecimal.ZERO, informeReservesService.sumarImports(List.of()));

        List<HistoricReserva> llista = List.of(
                historic("11111111A", "A", "Finalitzada", new BigDecimal("120.50")),
                historic("11111111A", "A", "Activa", null),
                historic("22222222B", "B", "Finalitzada", new BigDecimal("79.50")));

        assertEquals(new BigDecimal("200.00"), informeReservesService.sumarImports(llista));
    }

    @Test
    @DisplayName("llistarClients: retorna mapa ordenat per DNI sense duplicats")
    void llistarClients() {
        when(historicRepository.findAll()).thenReturn(List.of(
                historic("87654321B", "Pere Soler", "Activa", BigDecimal.TEN),
                historic("12345678A", "Anna Puig", "Finalitzada", BigDecimal.TEN),
                historic("12345678A", "Anna Puig", "Activa", BigDecimal.TEN)));

        Map<String, String> clients = informeReservesService.llistarClients();

        assertEquals(2, clients.size());
        assertEquals(List.of("12345678A", "87654321B"), List.copyOf(clients.keySet()));
    }
}