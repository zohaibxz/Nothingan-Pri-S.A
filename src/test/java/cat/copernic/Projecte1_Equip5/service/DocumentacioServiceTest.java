package cat.copernic.Projecte1_Equip5.service;

import cat.copernic.Projecte1_Equip5.document.Documentacio;
import cat.copernic.Projecte1_Equip5.logic.UsuariLogic;
import cat.copernic.Projecte1_Equip5.logic.UsuariLogic;
import cat.copernic.Projecte1_Equip5.repository.DocumentacioRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RN22 - Proves unitàries de la documentació d'usuaris a MongoDB (JUnit 5 + Mockito).
 */
@ExtendWith(MockitoExtension.class)
class DocumentacioServiceTest {

    @Mock
    private DocumentacioRepository documentacioRepository;
    @Spy
    private UsuariLogic usuariLogic = new UsuariLogic();

    @InjectMocks
    private DocumentacioService documentacioService;

    private MockMultipartFile pdf(String nom) {
        return new MockMultipartFile("f", nom, "application/pdf", new byte[]{'%', 'P', 'D', 'F', '-'});
    }

    @Test
    @DisplayName("completarDocumentacioPerReserva: només demana els documents que falten a MongoDB")
    void completarDocumentacioPerReserva() {
        Documentacio existent = new Documentacio("12345678A",
                "dni.pdf", "application/pdf", new byte[]{'%', 'P', 'D', 'F', '-'},
                null, null, null);
        when(documentacioRepository.findByDniUsuari("12345678A")).thenReturn(Optional.of(existent));

        // Falta el carnet -> falla si no s'adjunta
        assertThrows(IllegalArgumentException.class,
                () -> documentacioService.completarDocumentacioPerReserva("12345678A", null, null));

        // S'adjunta només el carnet -> conserva el DNI i desa en estat PENDENT
        documentacioService.completarDocumentacioPerReserva("12345678A", null, pdf("carnet.pdf"));

        ArgumentCaptor<Documentacio> captor = ArgumentCaptor.forClass(Documentacio.class);
        verify(documentacioRepository).save(captor.capture());
        assertEquals("dni.pdf", captor.getValue().getNomIdentitat());
        assertEquals("carnet.pdf", captor.getValue().getNomCarnet());
        assertEquals(Documentacio.Estat.PENDENT, captor.getValue().getEstat());
    }

    @Test
    @DisplayName("eliminarDocument: conserva la fitxa si queda l'altre document i l'esborra si no en queda cap")
    void eliminarDocument() {
        Documentacio doc = new Documentacio("12345678A",
                "dni.pdf", "application/pdf", new byte[]{'%', 'P', 'D', 'F', '-'},
                "carnet.pdf", "application/pdf", new byte[]{'%', 'P', 'D', 'F', '-'});
        when(documentacioRepository.findByDniUsuari("12345678A")).thenReturn(Optional.of(doc));

        // Elimina només identitat -> encara queda el carnet -> save
        documentacioService.eliminarDocument("12345678A", DocumentacioService.TIPUS_IDENTITAT);
        assertNull(doc.getIdentitat());
        assertNotNull(doc.getCarnet());
        verify(documentacioRepository).save(doc);

        // Elimina ara el carnet -> ja no queda cap fitxer -> delete
        documentacioService.eliminarDocument("12345678A", DocumentacioService.TIPUS_CARNET);
        verify(documentacioRepository).delete(doc);
    }

    @Test
    @DisplayName("guardarDocument: rebutja tipus de document desconegut")
    void guardarDocument_tipusInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> documentacioService.guardarDocument("12345678A", "passaport", pdf("p.pdf")));
        verifyNoInteractions(documentacioRepository);
    }
}