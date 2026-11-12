package cat.copernic.Projecte1_Equip5.logic;

import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RN22 - Proves unitàries de les regles de validació d'usuaris i documentació (JUnit 5).
 */
class UsuariLogicTest {

    private final UsuariLogic usuariLogic = new UsuariLogic();

    private static final long CINC_MB = 5L * 1024 * 1024;

    private MockMultipartFile fitxer(String nom, String tipus, byte[] dades) {
        return new MockMultipartFile("fitxer", nom, tipus, dades);
    }

    // =====================================================================
    // DNI / NIE i contrasenya
    // =====================================================================
    @ParameterizedTest
    @ValueSource(strings = {"12345678A", "12345678a", "X1234567L", "Y12345678Z", "Z7654321M"})
    @DisplayName("isDniValid: accepta formats vàlids de DNI i NIE")
    void isDniValid_valids(String dni) {
        assertTrue(usuariLogic.isDniValid(dni));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "12345678", "123456A", "A1234567B", "1234567890A", "12345678-"})
    @DisplayName("isDniValid: rebutja formats incorrectes")
    void isDniValid_invalids(String dni) {
        assertFalse(usuariLogic.isDniValid(dni));
    }

    @Test
    @DisplayName("isDniValid: null retorna false")
    void isDniValid_null() {
        assertFalse(usuariLogic.isDniValid(null));
    }

    @Test
    @DisplayName("isPasswordSecure: exigeix com a mínim 6 caràcters")
    void isPasswordSecure() {
        assertFalse(usuariLogic.isPasswordSecure(null));
        assertFalse(usuariLogic.isPasswordSecure("12345"));
        assertTrue(usuariLogic.isPasswordSecure("123456"));
    }

    // =====================================================================
    // Dates (caducitat, naixement i majoria d'edat)
    // =====================================================================
    @Test
    @DisplayName("isDocumentValid: només és vàlid si caduca després d'avui")
    void isDocumentValid() {
        assertFalse(usuariLogic.isDocumentValid(null));
        assertFalse(usuariLogic.isDocumentValid(LocalDate.now().minusDays(1)));
        assertFalse(usuariLogic.isDocumentValid(LocalDate.now()));
        assertTrue(usuariLogic.isDocumentValid(LocalDate.now().plusDays(1)));
    }

    @Test
    @DisplayName("isDataNaixementValida: accepta null, avui o passat, però no futur")
    void isDataNaixementValida() {
        assertTrue(usuariLogic.isDataNaixementValida(null));
        assertTrue(usuariLogic.isDataNaixementValida(LocalDate.now()));
        assertTrue(usuariLogic.isDataNaixementValida(LocalDate.now().minusYears(25)));
        assertFalse(usuariLogic.isDataNaixementValida(LocalDate.now().plusDays(1)));
    }

    @Test
    @DisplayName("isMajorDEdat: comprova si té 18 anys o més")
    void isMajorDEdat() {
        assertTrue(usuariLogic.isMajorDEdat(null));
        assertTrue(usuariLogic.isMajorDEdat(LocalDate.now().minusYears(18)));
        assertFalse(usuariLogic.isMajorDEdat(LocalDate.now().minusYears(18).plusDays(1)));
    }

    // =====================================================================
    // Telèfon, població i estat de validació
    // =====================================================================
    @Test
    @DisplayName("isTelefonValid i normalitzarTelefon: valida i neteja el format internacional")
    void telefon_validacioINormalitzacio() {
        assertTrue(usuariLogic.isTelefonValid(null));
        assertTrue(usuariLogic.isTelefonValid("+34 612345678"));
        assertFalse(usuariLogic.isTelefonValid("612345678"));
        assertFalse(usuariLogic.isTelefonValid("+34  612345678"));

        assertNull(usuariLogic.normalitzarTelefon("   "));
        assertEquals("+34 612345678", usuariLogic.normalitzarTelefon("  +34   612-34 56 78 "));
    }

    @Test
    @DisplayName("extreurPoblacio: obté la ciutat després de l'última coma sense codi postal")
    void extreurPoblacio() {
        assertNull(usuariLogic.extreurPoblacio(null));
        assertNull(usuariLogic.extreurPoblacio("Carrer sense coma 12"));
        assertEquals("Terrassa", usuariLogic.extreurPoblacio("Carrer Major 10, 08221 Terrassa"));
        assertEquals("Barcelona", usuariLogic.extreurPoblacio("Carrer de Mallorca 123, 3r 2a, 08036 Barcelona"));
    }

    @Test
    @DisplayName("potSerValidatORebutjat: només true si el client existeix i és Pendent")
    void potSerValidatORebutjat() {
        assertFalse(usuariLogic.potSerValidatORebutjat(null));

        Client c = new Client();
        c.setEstatValidacio(Enums.EstatValidacio.Pendent);
        assertTrue(usuariLogic.potSerValidatORebutjat(c));

        c.setEstatValidacio(Enums.EstatValidacio.Validat);
        assertFalse(usuariLogic.potSerValidatORebutjat(c));

        c.setEstatValidacio(Enums.EstatValidacio.Rebutjat);
        assertFalse(usuariLogic.potSerValidatORebutjat(c));
    }

    // =====================================================================
    // Validació de fitxers (DNI / carnet)
    // =====================================================================
    @Test
    @DisplayName("validarFitxer: rebutja fitxer buit, massa gran o de format no permès")
    void validarFitxer_errors() {
        String buit = usuariLogic.validarFitxer(fitxer("dni.jpg", "image/jpeg", new byte[0]), "document d'identitat");
        assertEquals("Cal adjuntar el document d'identitat.", buit);

        String gran = usuariLogic.validarFitxer(
                fitxer("dni.jpg", "image/jpeg", new byte[(int) CINC_MB + 1]), "document d'identitat");
        assertEquals("El document d'identitat no pot superar els 5 MB.", gran);

        String tipus = usuariLogic.validarFitxer(
                fitxer("dni.gif", "image/gif", new byte[]{1, 2, 3}), "document d'identitat");
        assertEquals("El document d'identitat ha de ser JPG, PNG o PDF.", tipus);
    }

    @Test
    @DisplayName("validarFitxer: accepta JPG, PNG i PDF dins de mida")
    void validarFitxer_ok() {
        assertNull(usuariLogic.validarFitxer(fitxer("dni.jpg", "image/jpeg", new byte[]{1, 2, 3})));
        assertNull(usuariLogic.validarFitxer(fitxer("dni.png", "image/png", new byte[]{1, 2, 3})));
        assertNull(usuariLogic.validarFitxer(fitxer("dni.pdf", "application/pdf", new byte[]{1, 2, 3})));
    }
}