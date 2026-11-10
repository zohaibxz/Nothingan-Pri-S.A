package cat.copernic.Projecte1_Equip5.service;

import cat.copernic.Projecte1_Equip5.document.HistoricReserva;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.repository.HistoricReservaRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Lògica de l'informe de reserves per client (RF91). Llegeix de l'històric de
 * MongoDB, no de MySQL, tal com demana l'enunciat: així l'informe conserva les
 * reserves encara que s'esborri el client o el vehicle.
 */
@Service
public class InformeReservesService {

    private static final Logger log = LoggerFactory.getLogger(InformeReservesService.class);

    /**
     * Ordre de l'informe: per client (nom i DNI) i, dins de cada client, la
     * reserva més recent primer.
     */
    private static final Sort ORDRE = Sort.by(
            Sort.Order.asc("nomClient"),
            Sort.Order.asc("dniClient"),
            Sort.Order.desc("dataAlta"));

    private final HistoricReservaRepository historicRepository;

    public InformeReservesService(HistoricReservaRepository historicRepository) {
        this.historicRepository = historicRepository;
    }

    /**
     * Reserves de l'històric d'un client o de tots, ordenades per client i per
     * data de reserva (la més recent primer).
     *
     * @param dniClient DNI del client; si és null o buit, tots els clients
     * @param senseAnullades si és true, s'exclouen les reserves anul·lades
     * @return llista de reserves de l'informe (pot ser buida)
     * @throws IllegalStateException si MongoDB no està disponible
     */
    public List<HistoricReserva> reservesPerClient(String dniClient, boolean senseAnullades) {
        List<HistoricReserva> reserves;
        try {
            if (dniClient == null || dniClient.isBlank()) {
                reserves = historicRepository.findAll(ORDRE);
            } else {
                reserves = historicRepository.findByDniClient(dniClient.trim(), ORDRE);
            }
        } catch (DataAccessException e) {
            log.error("Error de MongoDB en generar l'informe de reserves", e);
            throw new IllegalStateException(
                    "No s'ha pogut consultar l'històric. Torna-ho a provar.", e);
        }

        if (!senseAnullades) {
            return reserves;
        }
        List<HistoricReserva> filtrades = new ArrayList<>();
        for (HistoricReserva reserva : reserves) {
            if (!Enums.EstatReserva.Anul_lada.name().equals(reserva.getEstat())) {
                filtrades.add(reserva);
            }
        }
        return filtrades;
    }

    /**
     * Suma l'import total de les reserves d'una llista.
     *
     * @param reserves reserves de l'informe
     * @return suma dels imports (0 si la llista és buida)
     */
    public BigDecimal sumarImports(List<HistoricReserva> reserves) {
        BigDecimal total = BigDecimal.ZERO;
        for (HistoricReserva reserva : reserves) {
            if (reserva.getImportTotal() != null) {
                total = total.add(reserva.getImportTotal());
            }
        }
        return total;
    }

    /**
     * Clients que apareixen a l'històric (DNI i nom), per omplir el filtre. Es
     * treuen de l'històric i no de MySQL perquè l'informe només ha de mostrar
     * clients amb reserves registrades.
     *
     * @return mapa DNI a nom, ordenat per DNI
     * @throws IllegalStateException si MongoDB no està disponible
     */
    public Map<String, String> llistarClients() {
        Map<String, String> clients = new TreeMap<>();
        try {
            for (HistoricReserva reserva : historicRepository.findAll()) {
                clients.put(reserva.getDniClient(), reserva.getNomClient());
            }
        } catch (DataAccessException e) {
            log.error("Error de MongoDB en llegir els clients de l'històric", e);
            throw new IllegalStateException(
                    "No s'ha pogut consultar l'històric. Torna-ho a provar.", e);
        }
        return clients;
    }
}
