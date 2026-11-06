package cat.copernic.Projecte1_Equip5.service;

import cat.copernic.Projecte1_Equip5.document.Documentacio;
import cat.copernic.Projecte1_Equip5.repository.DocumentacioRepository;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Valida i desa a MongoDB la documentació d'un client (document d'identitat i
 * carnet de conduir). Hi ha un sol document per DNI: si el client ja en tenia,
 * se substitueix.
 */
@Service
public class DocumentacioService {

    private static final Logger log = LoggerFactory.getLogger(DocumentacioService.class);

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final List<String> TIPUS_PERMESOS
            = List.of("application/pdf", "image/jpeg", "image/png");

    private final DocumentacioRepository documentacioRepository;

    public DocumentacioService(DocumentacioRepository documentacioRepository) {
        this.documentacioRepository = documentacioRepository;
    }

    /**
     * Valida els dos arxius i els desa (o substitueix) a la col·lecció
     * documentacio. La documentació queda en estat PENDENT.
     *
     * @param dni DNI del client
     * @param identitat document d'identitat (PDF, JPG o PNG, màx. 5 MB)
     * @param carnet carnet de conduir (PDF, JPG o PNG, màx. 5 MB)
     * @throws IllegalArgumentException si falta un arxiu o no és vàlid
     * @throws IllegalStateException si no es pot llegir l'arxiu o MongoDB no
     * està disponible
     */
    public void guardarDocumentacio(String dni, MultipartFile identitat, MultipartFile carnet) {
        validarArxiu(identitat, "document d'identitat");
        validarArxiu(carnet, "carnet de conduir");

        try {
            Documentacio doc = documentacioRepository.findByDniUsuari(dni)
                    .orElse(new Documentacio());

            doc.setDniUsuari(dni);
            doc.setNomIdentitat(identitat.getOriginalFilename());
            doc.setTipusIdentitat(identitat.getContentType());
            doc.setIdentitat(identitat.getBytes());
            doc.setNomCarnet(carnet.getOriginalFilename());
            doc.setTipusCarnet(carnet.getContentType());
            doc.setCarnet(carnet.getBytes());
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

    /**
     * Comprova que l'arxiu existeix, no supera 5 MB i és PDF, JPG o PNG.
     */
    private void validarArxiu(MultipartFile arxiu, String nom) {
        if (arxiu == null || arxiu.isEmpty()) {
            throw new IllegalArgumentException("Cal adjuntar el " + nom + ".");
        }
        if (arxiu.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("El " + nom + " supera els 5 MB.");
        }
        String tipus = arxiu.getContentType();
        if (tipus == null || !TIPUS_PERMESOS.contains(tipus)) {
            throw new IllegalArgumentException("El " + nom + " ha de ser PDF, JPG o PNG.");
        }
    }
}
