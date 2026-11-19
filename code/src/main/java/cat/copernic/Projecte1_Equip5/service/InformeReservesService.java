package cat.copernic.Projecte1_Equip5.service;

import cat.copernic.Projecte1_Equip5.document.HistoricReserva;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.repository.HistoricReservaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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

    /**
     * Constructor amb injecció del repositori de l'històric.
     *
     * @param historicRepository repositori de l'històric de reserves (MongoDB)
     */
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
     * Dies de la finestra amb què es calcula l'ocupació mitjana.
     */
    public static final int DIES_OCUPACIO = 30;

    /**
     * Indicadors del panell d'informes.
     *
     * @param ingressos suma de l'import de les reserves no anul·lades
     * @param totalReserves nombre de reserves no anul·lades
     * @param valorMitja import mitjà per reserva (0 si no n'hi ha)
     * @param ocupacioMitjana percentatge d'ocupació de la flota en els últims
     * {@value #DIES_OCUPACIO} dies (0-100)
     */
    public record ResumInformes(BigDecimal ingressos, int totalReserves,
            BigDecimal valorMitja, BigDecimal ocupacioMitjana) {

    }

    /**
     * Calcula els indicadors del panell d'informes a partir de l'històric. Les
     * reserves anul·lades no compten (ni a ingressos, ni al total, ni a
     * l'ocupació). L'ocupació és el percentatge de dies-vehicle ocupats en els
     * últims {@value #DIES_OCUPACIO} dies sobre el total de dies-vehicle de la
     * flota; el dia de devolució queda lliure (dataFi exclosa).
     *
     * @param flota nombre de vehicles de la flota que es poden llogar
     * @param avui data d'avui (final de la finestra, exclosa)
     * @return indicadors del panell
     * @throws IllegalStateException si MongoDB no està disponible
     */
    public ResumInformes calcularResum(long flota, LocalDate avui) {
        List<HistoricReserva> reserves = reservesPerClient(null, true);

        BigDecimal ingressos = sumarImports(reserves);
        int total = reserves.size();
        BigDecimal valorMitja = total == 0
                ? BigDecimal.ZERO
                : ingressos.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        LocalDate inici = avui.minusDays(DIES_OCUPACIO);
        long diesOcupats = 0;
        for (HistoricReserva r : reserves) {
            if (r.getDataInici() == null || r.getDataFi() == null) {
                continue;
            }
            LocalDate desde = r.getDataInici().isAfter(inici) ? r.getDataInici() : inici;
            LocalDate fins = r.getDataFi().isBefore(avui) ? r.getDataFi() : avui;
            if (fins.isAfter(desde)) {
                diesOcupats += ChronoUnit.DAYS.between(desde, fins);
            }
        }
        BigDecimal ocupacio = BigDecimal.ZERO;
        if (flota > 0) {
            ocupacio = BigDecimal.valueOf(diesOcupats * 100L)
                    .divide(BigDecimal.valueOf(flota * DIES_OCUPACIO), 1, RoundingMode.HALF_UP)
                    .min(BigDecimal.valueOf(100));
        }
        return new ResumInformes(ingressos.setScale(2, RoundingMode.HALF_UP),
                total, valorMitja, ocupacio);
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
