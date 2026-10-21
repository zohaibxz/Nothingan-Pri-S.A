package cat.copernic.Projecte1_Equip5.service;

import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Reserva;
import cat.copernic.Projecte1_Equip5.repository.ReservaRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class ReservaService {

    /**
     * Estats que es consideren "properes reserves".
     */
    private static final Collection<Enums.EstatReserva> ESTATS_PROPERES
            = List.of(Enums.EstatReserva.Activa, Enums.EstatReserva.Lliurada);

    private final ReservaRepository reservaRepository;

    /**
     * @param reservaRepository repositori de reserves (injectat per Spring)
     */
    public ReservaService(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    /**
     * Reserves actives o lliurades d'un client, ordenades per data d'inici.
     *
     * @param dniClient DNI del client
     * @return llista de properes reserves
     */
    @Transactional(readOnly = true)
    public List<Reserva> llistarProperes(String dniClient) {
        return reservaRepository.findByClient_DniUsuariAndEstatInOrderByDataIniciAsc(dniClient, ESTATS_PROPERES);
    }

    /**
     * Reserves d'un client que ja no estan en curs (finalitzades o anul·lades),
     * de la més recent a la més antiga.
     *
     * @param dniClient DNI del client
     * @return llista de l'historial
     */
    @Transactional(readOnly = true)
    public List<Reserva> llistarHistorial(String dniClient) {
        return reservaRepository.findByClient_DniUsuariAndEstatNotInOrderByDataIniciDesc(dniClient, ESTATS_PROPERES);
    }
}
