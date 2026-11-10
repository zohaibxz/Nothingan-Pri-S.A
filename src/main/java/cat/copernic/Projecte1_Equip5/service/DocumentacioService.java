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
 * fan servir el registre, la reserva, el perfil i la gestió de documents de l'administrador i de l'agent.
 */
@Service
public class DocumentacioService {

    private static final Logger log = LoggerFactory.getLogger(DocumentacioService.class);

    /** Valors acceptats per a "tipus" quan es desa o s'elimina un sol document. */
    public static final String TIPUS_IDENTITAT = "identitat";
    public static final String TIPUS_CARNET = "carnet";

    private static final String NOM_IDENTITAT = "document d'identitat";
    private static final String NOM_CARNET = "carnet de conduir";

    private final DocumentacioRepository documentacioRepository;
    private final UsuariLogic usuariLogic;

    public DocumentacioService(DocumentacioRepository documentacioRepository,
            UsuariLogic usuariLogic) {
        this.documentacioRepository = documentacioRepository;
        this.usuariLogic = usuariLogic;
    }

    /**
     * @param dni DNI de l'usuari
     * @return la documentació de l'usuari, si n'ha pujat
     */
    public Optional<Documentacio> obtenir(String dni) {
        return documentacioRepository.findByDniUsuari(dni);
    }

    /**
     * Comprova els dos arxius sense desar res (p. ex. per fallar abans de
     * crear un client).
     *
     * @throws IllegalArgumentException si falta un arxiu o no és vàlid
     */
    public void validarDocumentacio(MultipartFile identitat, MultipartFile carnet) {
        comprovar(identitat, NOM_IDENTITAT);
        comprovar(carnet, NOM_CARNET);
    }

    /**
     * Valida els dos arxius i els desa (o substitueix) a la col·lecció
     * documentacio. La documentació queda en estat PENDENT.
     *
     * @throws IllegalArgumentException si falta un arxiu o no és vàlid
     * @throws IllegalStateException si no es pot llegir l'arxiu o MongoDB no
     * està disponible
     */
    public void guardarDocumentacio(String dni, MultipartFile identitat, MultipartFile carnet) {
        validarDocumentacio(identitat, carnet);
        desar(dni, identitat, carnet);
    }

    /**
     * Desa un o els dos documents: el que arriba buit es deixa com estava. Serveix per
     * a la pujada de l'administrador i de l'agent, on es pot canviar només un document.
     * Es validen tots els arxius enviats abans de desar res, perquè no quedi un desat
     * i l'altre no.
     *
     * @throws IllegalArgumentException si no s'envia cap arxiu o algun no és vàlid
     * @throws IllegalStateException si no es pot llegir l'arxiu o MongoDB no
     * està disponible
     */
    public void guardarDocumentsOpcionals(String dni, MultipartFile identitat, MultipartFile carnet) {
        boolean hiHaIdentitat = identitat != null && !identitat.isEmpty();
        boolean hiHaCarnet = carnet != null && !carnet.isEmpty();
        if (!hiHaIdentitat && !hiHaCarnet) {
            throw new IllegalArgumentException("Cal pujar almenys un document.");
        }
        if (hiHaIdentitat) {
            comprovar(identitat, NOM_IDENTITAT);
        }
        if (hiHaCarnet) {
            comprovar(carnet, NOM_CARNET);
        }
        // desar: un argument a null vol dir "no tocar aquest document"
        desar(dni, hiHaIdentitat ? identitat : null, hiHaCarnet ? carnet : null);
    }

    /**
     * Valida i desa un sol document (l'altre queda com estava).
     *
     * @param tipus {@link #TIPUS_IDENTITAT} o {@link #TIPUS_CARNET}
     * @throws IllegalArgumentException si el tipus o l'arxiu no són vàlids
     * @throws IllegalStateException si no es pot llegir l'arxiu o MongoDB no
     * està disponible
     */
    public void guardarDocument(String dni, String tipus, MultipartFile fitxer) {
        comprovarTipus(tipus);
        comprovar(fitxer, nomDocument(tipus));
        if (TIPUS_IDENTITAT.equals(tipus)) {
            desar(dni, fitxer, null);
        } else {
            desar(dni, null, fitxer);
        }
    }

    /**
     * Elimina un sol document. Si no en queda cap dels dos, esborra la fitxa
     * sencera de MongoDB per no deixar-la buida.
     *
     * @param tipus {@link #TIPUS_IDENTITAT} o {@link #TIPUS_CARNET}
     * @throws IllegalArgumentException si el tipus no és vàlid o no hi ha res
     * pujat
     */
    public void eliminarDocument(String dni, String tipus) {
        comprovarTipus(tipus);
        Documentacio doc = documentacioRepository.findByDniUsuari(dni)
                .orElseThrow(() -> new IllegalArgumentException("No hi ha cap document pujat."));
        if (TIPUS_IDENTITAT.equals(tipus)) {
            doc.setNomIdentitat(null);
            doc.setTipusIdentitat(null);
            doc.setIdentitat(null);
        } else {
            doc.setNomCarnet(null);
            doc.setTipusCarnet(null);
            doc.setCarnet(null);
        }
        boolean senseIdentitat = doc.getIdentitat() == null || doc.getIdentitat().length == 0;
        boolean senseCarnet = doc.getCarnet() == null || doc.getCarnet().length == 0;
        if (senseIdentitat && senseCarnet) {
            documentacioRepository.delete(doc);
        } else {
            documentacioRepository.save(doc);
        }
    }

    /**
     * Únic lloc que llegeix els bytes i fa el save. Un argument a null vol dir
     * "no tocar aquest document".
     */
    private void desar(String dni, MultipartFile identitat, MultipartFile carnet) {
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
            log.error("No s'ha pogut llegir un arxiu de la documentació", e);
            throw new IllegalStateException(
                    "No s'ha pogut llegir un dels arxius adjuntats. Torna-ho a provar.", e);
        } catch (DataAccessException e) {
            log.error("Error de MongoDB en desar la documentació", e);
            throw new IllegalStateException(
                    "No s'ha pogut desar la documentació. Torna-ho a provar.", e);
        }
    }

    private void comprovar(MultipartFile arxiu, String nom) {
        String error = usuariLogic.validarFitxer(arxiu, nom);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
    }

    // Només s'accepten aquests dos valors (evita que algú escrigui un altre text a la ruta)
    private void comprovarTipus(String tipus) {
        if (!TIPUS_IDENTITAT.equals(tipus) && !TIPUS_CARNET.equals(tipus)) {
            throw new IllegalArgumentException("Tipus de document no vàlid.");
        }
    }

    private String nomDocument(String tipus) {
        return TIPUS_IDENTITAT.equals(tipus) ? NOM_IDENTITAT : NOM_CARNET;
    }
}
 