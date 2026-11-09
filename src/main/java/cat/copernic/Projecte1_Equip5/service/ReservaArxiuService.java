package cat.copernic.Projecte1_Equip5.service;

import cat.copernic.Projecte1_Equip5.document.ReservaArxiu;
import cat.copernic.Projecte1_Equip5.repository.ReservaArxiuRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Valida i desa a MongoDB les fotos que l'agent adjunta en lliurar o retornar
 * un vehicle (RF49 i RF48).
 */
@Service
public class ReservaArxiuService {

    private static final Logger log = LoggerFactory.getLogger(ReservaArxiuService.class);

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final int MAX_FOTOS = 10;
    private static final List<String> TIPUS_PERMESOS = List.of("image/jpeg", "image/png");

    private final ReservaArxiuRepository reservaArxiuRepository;

    public ReservaArxiuService(ReservaArxiuRepository reservaArxiuRepository) {
        this.reservaArxiuRepository = reservaArxiuRepository;
    }

    /**
     * Valida les fotos rebudes i en prepara els documents, però NO els desa. Es
     * crida abans de modificar res a MySQL perquè un arxiu incorrecte aturi
     * l'operació sense deixar canvis a mitges. Els camps de fitxer buits
     * (l'usuari no n'ha triat cap) s'ignoren.
     *
     * @param idReserva reserva a què pertanyen les fotos
     * @param moment lliurament o retorn
     * @param fotos fitxers rebuts del formulari (pot ser null)
     * @return documents llestos per desar (llista buida si no hi ha fotos)
     * @throws IllegalArgumentException si n'hi ha massa, pesen més de 5 MB o no
     * són JPG o PNG
     * @throws IllegalStateException si no es pot llegir un fitxer
     */
    public List<ReservaArxiu> prepararFotos(Long idReserva,
            ReservaArxiu.Moment moment, List<MultipartFile> fotos) {

        List<ReservaArxiu> arxius = new ArrayList<>();
        if (fotos == null) {
            return arxius;
        }

        for (MultipartFile foto : fotos) {
            if (foto == null || foto.isEmpty()) {
                continue;
            }
            if (arxius.size() >= MAX_FOTOS) {
                throw new IllegalArgumentException(
                        "Es poden adjuntar com a màxim " + MAX_FOTOS + " fotos.");
            }
            if (foto.getSize() > MAX_BYTES) {
                throw new IllegalArgumentException(
                        "La foto '" + foto.getOriginalFilename() + "' supera els 5 MB.");
            }
            String tipus = foto.getContentType();
            if (tipus == null || !TIPUS_PERMESOS.contains(tipus)) {
                throw new IllegalArgumentException("Les fotos han de ser JPG o PNG.");
            }
            try {
                arxius.add(new ReservaArxiu(idReserva, moment,
                        foto.getOriginalFilename(), tipus, foto.getSize(), foto.getBytes()));
            } catch (IOException e) {
                throw new IllegalStateException(
                        "No s'ha pogut llegir una de les fotos. Torna-ho a provar.", e);
            }
        }
        return arxius;
    }

    /**
     * Desa a MongoDB les fotos preparades amb prepararFotos.
     *
     * @param arxius documents a desar (si és buida no fa res)
     * @throws IllegalStateException si MongoDB no està disponible
     */
    public void guardar(List<ReservaArxiu> arxius) {
        if (arxius.isEmpty()) {
            return;
        }
        try {
            reservaArxiuRepository.saveAll(arxius);
        } catch (DataAccessException e) {
            log.error("Error de MongoDB en desar les fotos de la reserva {}",
                    arxius.get(0).getIdReserva(), e);
            throw new IllegalStateException(
                    "No s'han pogut desar les fotos. Torna-ho a provar.", e);
        }
    }

    /**
     * Fotos d'una reserva en un moment, sense els bytes (només id i nom). Si
     * MongoDB no està disponible torna una llista buida: el detall de la
     * reserva s'ha de poder veure igualment, només sense fotos.
     *
     * @param idReserva reserva
     * @param moment lliurament o retorn
     * @return llista ordenada per data de pujada
     */
    public List<ReservaArxiu> llistarFotos(Long idReserva, ReservaArxiu.Moment moment) {
        try {
            return reservaArxiuRepository.findMetadades(
                    idReserva, moment, Sort.by("dataPujada"));
        } catch (DataAccessException e) {
            log.error("Error de MongoDB en llegir les fotos de la reserva {}", idReserva, e);
            return List.of();
        }
    }

    /**
     * Una foto completa (amb bytes). Comprova que pertany a la reserva indicada
     * perquè no es puguin llegir fotos d'altres reserves canviant l'id de la
     * ruta.
     *
     * @param idReserva reserva de la ruta
     * @param idArxiu id de la foto
     * @return la foto
     * @throws IllegalArgumentException si no existeix o no és d'aquesta reserva
     */
    public ReservaArxiu obtenirFoto(Long idReserva, String idArxiu) {
        ReservaArxiu arxiu = reservaArxiuRepository.findById(idArxiu)
                .orElseThrow(() -> new IllegalArgumentException("No s'ha trobat la foto."));
        if (!idReserva.equals(arxiu.getIdReserva())) {
            throw new IllegalArgumentException("No s'ha trobat la foto.");
        }
        return arxiu;
    }
}
