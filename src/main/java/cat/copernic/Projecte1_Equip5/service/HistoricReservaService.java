package cat.copernic.Projecte1_Equip5.service;

import cat.copernic.Projecte1_Equip5.document.HistoricReserva;
import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Reserva;
import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import cat.copernic.Projecte1_Equip5.repository.HistoricReservaRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * Manté l'històric de reserves a MongoDB (alta, anul·lació i, més endavant,
 * finalització). Només copia dades: no decideix res de negoci.
 */
@Service
public class HistoricReservaService {

    private final HistoricReservaRepository historicRepository;

    public HistoricReservaService(HistoricReservaRepository historicRepository) {
        this.historicRepository = historicRepository;
    }

    /**
     * Desa l'estat actual de la reserva a l'històric. Crea el document si no
     * existeix i l'actualitza si ja hi és. S'ha de cridar dins d'una transacció
     * (@Transactional) perquè es llegeixen el client i el vehicle, que són
     * LAZY.
     *
     * @param reserva reserva ja guardada (amb idReserva)
     * @throws IllegalStateException si MongoDB no està disponible
     */
    public void registrarCanvi(Reserva reserva) {
        Client client = reserva.getClient();
        Vehicle vehicle = reserva.getVehicle();

        try {
            // Si ja existeix, el reutilitzem per no perdre la data d'alta
            HistoricReserva historic = historicRepository
                    .findById(reserva.getIdReserva())
                    .orElseGet(HistoricReserva::new);

            historic.setIdReserva(reserva.getIdReserva());
            historic.setCodiReserva(reserva.getCodiReserva());
            historic.setDniClient(client.getDniUsuari());
            historic.setNomClient(client.getNom() + " " + client.getCognoms());
            historic.setMatricula(vehicle.getMatricula());
            historic.setMarca(vehicle.getMarca());
            historic.setModel(vehicle.getModel());
            historic.setDataInici(reserva.getDataInici());
            historic.setDataFi(reserva.getDataFi());
            historic.setImportTotal(reserva.getImportTotal());
            historic.setFianca(reserva.getFianca());
            historic.setEstat(reserva.getEstat().name());
            historic.setDataAlta(reserva.getDataCreacio());
            historic.setDataAnullacio(reserva.getDataAnulacio());
            historic.setImportRetornat(reserva.getImportRetornat());

            historicRepository.save(historic);
        } catch (DataAccessException e) {
            throw new IllegalStateException(
                    "No s'ha pogut registrar l'operació a l'històric. Torna-ho a provar.", e);
        }
    }
}
