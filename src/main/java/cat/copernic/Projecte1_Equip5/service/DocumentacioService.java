package cat.copernic.Projecte1_Equip5.service;

import cat.copernic.Projecte1_Equip5.document.Documentacio;
import cat.copernic.Projecte1_Equip5.logic.UsuariLogic;
import cat.copernic.Projecte1_Equip5.repository.DocumentacioRepository;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Únic lloc on es valida i es desa a MongoDB la documentació d'un usuari
 * (document d'identitat i carnet de conduir). Hi ha un sol document per DNI: si
 * l'usuari ja en tenia, se substitueix i torna a quedar en estat PENDENT.
 *
 * Les regles de validació (present, màx. 5 MB, JPG/PNG/PDF) no són aquí sinó a
 * {@link UsuariLogic#validarFitxer(MultipartFile, String)}. Aquesta classe la
 * fan servir el registre, la reserva, el perfil i la pujada de l'administrador.
 */
@Service
public class DocumentacioService {

    private static final Logger log = LoggerFactory.getLogger(DocumentacioService.class);

    public static final String TIPUS_IDENTITAT = "identitat";
    public static final String TIPUS_CARNET = "carnet";

    private static final String NOM_IDENTITAT = "document d'identitat";
    private static final String NOM_CARNET = "carnet de conduir";

    private final DocumentacioRepository documentacioRepository;
    private final UsuariLogic usuariLogic;

    public DocumentacioService(
            DocumentacioRepository documentacioRepository,
            UsuariLogic usuariLogic) {
        this.documentacioRepository = documentacioRepository;
        this.usuariLogic = usuariLogic;
    }

    /**
     * Retorna la documentació de l'usuari, si existeix.
     */
    public Optional<Documentacio> obtenir(String dni) {
        return documentacioRepository.findByDniUsuari(dni);
    }

    /**
     * Indica si l'usuari ja té els dos documents guardats i amb contingut.
     */
    public boolean teDocumentacioCompleta(String dni) {
        return obtenir(dni)
                .map(doc -> teContingut(doc.getIdentitat())
                && teContingut(doc.getCarnet()))
                .orElse(false);
    }

    /**
     * Completa la documentació necessària per reservar sense tornar a demanar
     * els fitxers que ja existeixen a MongoDB.
     *
     * Els fitxers rebuts substitueixen només el document corresponent.
     */
    public void completarDocumentacioPerReserva(
            String dni,
            MultipartFile identitat,
            MultipartFile carnet) {

        Documentacio existent = obtenir(dni).orElse(null);

        boolean teIdentitat = existent != null
                && teContingut(existent.getIdentitat());

        boolean teCarnet = existent != null
                && teContingut(existent.getCarnet());

        boolean pujaIdentitat = teFitxer(identitat);
        boolean pujaCarnet = teFitxer(carnet);

        if (!teIdentitat && !pujaIdentitat) {
            throw new IllegalArgumentException(
                    "Cal adjuntar el document d'identitat.");
        }

        if (!teCarnet && !pujaCarnet) {
            throw new IllegalArgumentException(
                    "Cal adjuntar el carnet de conduir.");
        }

        if (pujaIdentitat) {
            comprovar(identitat, NOM_IDENTITAT);
        }

        if (pujaCarnet) {
            comprovar(carnet, NOM_CARNET);
        }

        // Si ja existeixen tots dos documents i no se'n puja cap de nou,
        // no cal tornar a desar-los ni modificar-ne l'estat.
        if (pujaIdentitat || pujaCarnet) {
            desar(
                    dni,
                    pujaIdentitat ? identitat : null,
                    pujaCarnet ? carnet : null);
        }
    }

    private boolean teContingut(byte[] bytes) {
        return bytes != null && bytes.length > 0;
    }

    private boolean teFitxer(MultipartFile fitxer) {
        return fitxer != null && !fitxer.isEmpty();
    }

    /**
     * Valida els dos arxius sense desar-los.
     */
    public void validarDocumentacio(
            MultipartFile identitat,
            MultipartFile carnet) {

        comprovar(identitat, NOM_IDENTITAT);
        comprovar(carnet, NOM_CARNET);
    }

    /**
     * Valida els dos arxius i els desa a MongoDB.
     */
    public void guardarDocumentacio(
            String dni,
            MultipartFile identitat,
            MultipartFile carnet) {

        validarDocumentacio(identitat, carnet);
        desar(dni, identitat, carnet);
    }

    /**
     * Desa només els documents que s'hagin adjuntat.
     *
     * Els documents no rebuts (null o buits) es conserven.
     *
     * Aquest mètode és necessari per mantenir la compatibilitat amb
     * DocumentacioController.
     */
    public void guardarDocumentsOpcionals(
            String dni,
            MultipartFile identitat,
            MultipartFile carnet) {

        boolean pujaIdentitat = teFitxer(identitat);
        boolean pujaCarnet = teFitxer(carnet);

        if (!pujaIdentitat && !pujaCarnet) {
            return;
        }

        if (pujaIdentitat) {
            comprovar(identitat, NOM_IDENTITAT);
        }

        if (pujaCarnet) {
            comprovar(carnet, NOM_CARNET);
        }

        desar(
                dni,
                pujaIdentitat ? identitat : null,
                pujaCarnet ? carnet : null);
    }

    /**
     * Valida i desa un sol document. L'altre document es conserva.
     */
    public void guardarDocument(
            String dni,
            String tipus,
            MultipartFile fitxer) {

        comprovarTipus(tipus);
        comprovar(fitxer, nomDocument(tipus));

        if (TIPUS_IDENTITAT.equals(tipus)) {
            desar(dni, fitxer, null);
        } else {
            desar(dni, null, fitxer);
        }
    }

    /**
     * Elimina un sol document. Si no en queda cap, elimina la fitxa sencera de
     * MongoDB.
     */
    public void eliminarDocument(String dni, String tipus) {

        comprovarTipus(tipus);

        Documentacio doc = documentacioRepository.findByDniUsuari(dni)
                .orElseThrow(() -> new IllegalArgumentException(
                "No tens cap document pujat."));

        if (TIPUS_IDENTITAT.equals(tipus)) {
            doc.setNomIdentitat(null);
            doc.setTipusIdentitat(null);
            doc.setIdentitat(null);
        } else {
            doc.setNomCarnet(null);
            doc.setTipusCarnet(null);
            doc.setCarnet(null);
        }

        boolean senseIdentitat = doc.getIdentitat() == null
                || doc.getIdentitat().length == 0;

        boolean senseCarnet = doc.getCarnet() == null
                || doc.getCarnet().length == 0;

        if (senseIdentitat && senseCarnet) {
            documentacioRepository.delete(doc);
        } else {
            documentacioRepository.save(doc);
        }
    }

    /**
     * Únic lloc que llegeix els bytes i desa a MongoDB.
     *
     * Un argument null significa que no s'ha de modificar aquell document.
     */
    private void desar(
            String dni,
            MultipartFile identitat,
            MultipartFile carnet) {

        try {
            Documentacio doc = documentacioRepository.findByDniUsuari(dni)
                    .orElseGet(Documentacio::new);

            doc.setDniUsuari(dni);

            if (identitat != null) {
                doc.setNomIdentitat(identitat.getOriginalFilename());
                doc.setTipusIdentitat(identitat.getContentType());
                doc.setIdentitat(identitat.getBytes());
            }

            if (carnet != null) {
                doc.setNomCarnet(carnet.getOriginalFilename());
                doc.setTipusCarnet(carnet.getContentType());
                doc.setCarnet(carnet.getBytes());
            }

            doc.setEstat(Documentacio.Estat.PENDENT);

            documentacioRepository.save(doc);

        } catch (IOException e) {
            log.error(
                    "No s'ha pogut llegir un arxiu de la documentació",
                    e);

            throw new IllegalStateException(
                    "No s'ha pogut llegir un dels arxius adjuntats. "
                    + "Torna-ho a provar.",
                    e);

        } catch (DataAccessException e) {
            log.error(
                    "Error de MongoDB en desar la documentació",
                    e);

            throw new IllegalStateException(
                    "No s'ha pogut desar la documentació. "
                    + "Torna-ho a provar.",
                    e);
        }
    }

    private void comprovar(MultipartFile arxiu, String nom) {

        String error = usuariLogic.validarFitxer(arxiu, nom);

        if (error != null) {
            throw new IllegalArgumentException(error);
        }
    }

    private void comprovarTipus(String tipus) {

        if (!TIPUS_IDENTITAT.equals(tipus)
                && !TIPUS_CARNET.equals(tipus)) {

            throw new IllegalArgumentException(
                    "Tipus de document no vàlid.");
        }
    }

    private String nomDocument(String tipus) {

        return TIPUS_IDENTITAT.equals(tipus)
                ? NOM_IDENTITAT
                : NOM_CARNET;
    }
}
