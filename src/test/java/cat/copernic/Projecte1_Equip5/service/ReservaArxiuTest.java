package cat.copernic.Projecte1_Equip5.service;

import cat.copernic.Projecte1_Equip5.document.ReservaArxiu;
import cat.copernic.Projecte1_Equip5.repository.ReservaArxiuRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RN22 - Proves unitàries de la gestió de fotos de lliurament i retorn (JUnit 5 + Mockito).
 */
@ExtendWith(MockitoExtension.class)
class ReservaArxiuServiceTest {

    @Mock
    private ReservaArxiuRepository reservaArxiuRepository;

    @InjectMocks
    private ReservaArxiuService reservaArxiuService;

    private static final int CINC_MB = 5 * 1024 * 1024;

    private MockMultipartFile foto(String nom, String tipus, byte[] bytes) {
        return new MockMultipartFile("fotos", nom, tipus, bytes);
    }

    @Test
    @DisplayName("prepararFotos: ignora fitxers buits i prepara JPG/PNG vàlids")
    void prepararFotos_valides() {
        List<MultipartFile> entrada = List.of(
                foto("buida.jpg", "image/jpeg", new byte[0]),
                foto("frontal.jpg", "image/jpeg", new byte[]{1, 2, 3}),
                foto("lateral.png", "image/png", new byte[]{4, 5, 6}));

        List<ReservaArxiu> resultat = reservaArxiuService.prepararFotos(
                15L, ReservaArxiu.Moment.LLIURAMENT, entrada);

        assertEquals(2, resultat.size());
        assertEquals(15L, resultat.get(0).getIdReserva());
        assertEquals(ReservaArxiu.Moment.LLIURAMENT, resultat.get(0).getMoment());
        assertEquals("frontal.jpg", resultat.get(0).getNom());
    }

    @Test
    @DisplayName("prepararFotos: rebutja més de 10 fotos, fotos > 5 MB o formats no JPG/PNG")
    void prepararFotos_errors() {
        List<MultipartFile> onzeFotos = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            onzeFotos.add(foto("f" + i + ".jpg", "image/jpeg", new byte[]{1}));
        }
        assertThrows(IllegalArgumentException.class, () -> reservaArxiuService.prepararFotos(
                1L, ReservaArxiu.Moment.RETORN, onzeFotos));

        assertThrows(IllegalArgumentException.class, () -> reservaArxiuService.prepararFotos(
                1L, ReservaArxiu.Moment.RETORN,
                List.of(foto("gran.jpg", "image/jpeg", new byte[CINC_MB + 1]))));

        assertThrows(IllegalArgumentException.class, () -> reservaArxiuService.prepararFotos(
                1L, ReservaArxiu.Moment.RETORN,
                List.of(foto("doc.pdf", "application/pdf", new byte[]{1, 2}))));
    }

    @Test
    @DisplayName("guardar: no crida saveAll si la llista és buida i encapsula errors de MongoDB")
    void guardar_buitIErrorMongo() {
        reservaArxiuService.guardar(List.of());
        verifyNoInteractions(reservaArxiuRepository);

        ReservaArxiu arxiu = new ReservaArxiu(1L, ReservaArxiu.Moment.RETORN, "a.jpg", "image/jpeg", 3L, new byte[]{1});
        when(reservaArxiuRepository.saveAll(any())).thenThrow(new DataAccessResourceFailureException("Mongo caigut"));

        assertThrows(IllegalStateException.class, () -> reservaArxiuService.guardar(List.of(arxiu)));
    }

    @Test
    @DisplayName("llistarFotos: retorna llista buida si MongoDB falla perquè el detall es pugui veure")
    void llistarFotos_toleranciaFalladaMongo() {
        when(reservaArxiuRepository.findMetadades(eq(1L), eq(ReservaArxiu.Moment.LLIURAMENT), any()))
                .thenThrow(new DataAccessResourceFailureException("Mongo caigut"));

        List<ReservaArxiu> fotos = reservaArxiuService.llistarFotos(1L, ReservaArxiu.Moment.LLIURAMENT);

        assertTrue(fotos.isEmpty());
    }

    @Test
    @DisplayName("obtenirFoto: comprova que la foto existeixi i pertanyi a la reserva")
    void obtenirFoto() {
        ReservaArxiu arxiu = new ReservaArxiu(10L, ReservaArxiu.Moment.RETORN, "a.jpg", "image/jpeg", 3L, new byte[]{1});
        when(reservaArxiuRepository.findById("foto-1")).thenReturn(Optional.of(arxiu));

        assertSame(arxiu, reservaArxiuService.obtenirFoto(10L, "foto-1"));
        assertThrows(IllegalArgumentException.class, () -> reservaArxiuService.obtenirFoto(99L, "foto-1"));
        assertThrows(IllegalArgumentException.class, () -> reservaArxiuService.obtenirFoto(10L, "inexistent"));
    }
}