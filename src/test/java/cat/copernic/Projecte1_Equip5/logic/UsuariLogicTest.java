package cat.copernic.Projecte1_Equip5.logic;

import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.logic.UsuariLogic;
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
    @ValueSource(strings = {"AB123456", "ab123456", "123456789", "X12345", "PAB123C45"})
    @DisplayName("isPassaportValid: accepta de 6 a 9 caràcters alfanumèrics")
    void isPassaportValid_valids(String passaport) {
        assertTrue(usuariLogic.isPassaportValid(passaport));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "AB123", "AB12345678", "AB-12345", "AB 12345", "ÀB123456"})
    @DisplayName("isPassaportValid: rebutja massa curt, massa llarg o amb símbols/espais")
    void isPassaportValid_invalids(String passaport) {
        assertFalse(usuariLogic.isPassaportValid(passaport));
    }

    @Test
    @DisplayName("isPassaportValid: null retorna false")
    void isPassaportValid_null() {
        assertFalse(usuariLogic.isPassaportValid(null));
    }

    @Test
    @DisplayName("validarDocumentIdentitat: valida el número segons el tipus de document triat")
    void validarDocumentIdentitat() {
        // Correctes
        assertNull(usuariLogic.validarDocumentIdentitat("DNI", "12345678Z"));
        assertNull(usuariLogic.validarDocumentIdentitat("dni", " 12345678z "));
        assertNull(usuariLogic.validarDocumentIdentitat("NIE", "X1234567L"));
        assertNull(usuariLogic.validarDocumentIdentitat("PASSAPORT", "AB123456"));

        // Lletra incorrecta
        assertEquals("La lletra del DNI no és correcta.", usuariLogic.validarDocumentIdentitat("DNI", "12345678A"));
        assertEquals("La lletra del NIE no és correcta.", usuariLogic.validarDocumentIdentitat("NIE", "X1234567A"));

        // Cada tipus exigeix el seu format: un NIE no val com a DNI, ni un DNI com a NIE
        assertNotNull(usuariLogic.validarDocumentIdentitat("DNI", "X1234567L"));
        assertNotNull(usuariLogic.validarDocumentIdentitat("NIE", "12345678Z"));
        assertNotNull(usuariLogic.validarDocumentIdentitat("NIE", "A1234567L"));

        // Passaport massa curt / amb símbols
        assertNotNull(usuariLogic.validarDocumentIdentitat("PASSAPORT", "AB12"));
        assertNotNull(usuariLogic.validarDocumentIdentitat("PASSAPORT", "AB-123456"));

        // Tipus desconegut, tipus null o número buit
        assertNotNull(usuariLogic.validarDocumentIdentitat("CARNET", "12345678Z"));
        assertNotNull(usuariLogic.validarDocumentIdentitat(null, "12345678Z"));
        assertNotNull(usuariLogic.validarDocumentIdentitat("DNI", "  "));
        assertNotNull(usuariLogic.validarDocumentIdentitat("DNI", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678Z", "12345678z", "00000000T", "X1234567L", "Y1234567X", "Z1234567R"})
    @DisplayName("isDniLletraCorrecta: accepta DNI/NIE amb la lletra que toca (número mod 23)")
    void isDniLletraCorrecta_valids(String dni) {
        assertTrue(usuariLogic.isDniLletraCorrecta(dni));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678A", "X1234567A", "Z7654321M", "Y12345678Z", "INVALID", ""})
    @DisplayName("isDniLletraCorrecta: rebutja lletra incorrecta o format no vàlid")
    void isDniLletraCorrecta_invalids(String dni) {
        assertFalse(usuariLogic.isDniLletraCorrecta(dni));
    }

    @Test
    @DisplayName("isDniLletraCorrecta: null retorna false")
    void isDniLletraCorrecta_null() {
        assertFalse(usuariLogic.isDniLletraCorrecta(null));
    }

    @Test
    @DisplayName("isPasswordSecure: exigeix 8 caràcters, una lletra i un número, i màxim 72 bytes")
    void isPasswordSecure() {
        assertFalse(usuariLogic.isPasswordSecure(null));
        assertFalse(usuariLogic.isPasswordSecure("12345"));
        assertFalse(usuariLogic.isPasswordSecure("123456"));        // massa curta
        assertFalse(usuariLogic.isPasswordSecure("12345678"));      // sense lletres
        assertFalse(usuariLogic.isPasswordSecure("abcdefgh"));      // sense números
        assertFalse(usuariLogic.isPasswordSecure("a1".repeat(37))); // 74 bytes: BCrypt no ho admet
        assertTrue(usuariLogic.isPasswordSecure("secret123"));
    }

    @Test
    @DisplayName("contrasenyesCoincideixen: només és true si les dues contrasenyes són iguals")
    void contrasenyesCoincideixen() {
        assertTrue(usuariLogic.contrasenyesCoincideixen("secret123", "secret123"));
        assertFalse(usuariLogic.contrasenyesCoincideixen("secret123", "secret124"));
        assertFalse(usuariLogic.contrasenyesCoincideixen("secret123", ""));
        assertFalse(usuariLogic.contrasenyesCoincideixen("secret123", "Secret123"));
        assertFalse(usuariLogic.contrasenyesCoincideixen(null, "secret123"));
        assertFalse(usuariLogic.contrasenyesCoincideixen("secret123", null));
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

    @Test
    @DisplayName("validarAdrecaRegistre: valida carrer, codi postal (5 xifres) i ciutat per separat")
    void validarAdrecaRegistre() {
        assertNull(usuariLogic.validarAdrecaRegistre("Carrer Major 1, 2n 1a", "08221", "Terrassa"));
        assertNull(usuariLogic.validarAdrecaRegistre("Carrer Major 1", " 28001 ", "L'Hospitalet de Llobregat"));

        // Codi postal: lletres, massa curt, massa llarg o buit
        assertTrue(usuariLogic.validarAdrecaRegistre("Carrer Major 1", "abcde", "Terrassa").startsWith("Codi postal"));
        assertTrue(usuariLogic.validarAdrecaRegistre("Carrer Major 1", "1234", "Terrassa").startsWith("Codi postal"));
        assertTrue(usuariLogic.validarAdrecaRegistre("Carrer Major 1", "123456", "Terrassa").startsWith("Codi postal"));
        assertTrue(usuariLogic.validarAdrecaRegistre("Carrer Major 1", "", "Terrassa").startsWith("Codi postal"));

        // Ciutat amb números o buida
        assertTrue(usuariLogic.validarAdrecaRegistre("Carrer Major 1", "08221", "123").startsWith("Ciutat"));
        assertTrue(usuariLogic.validarAdrecaRegistre("Carrer Major 1", "08221", " ").startsWith("Ciutat"));

        // Carrer massa llarg (el total ha de cabre a la BD)
        assertTrue(usuariLogic.validarAdrecaRegistre("a".repeat(151), "08221", "Terrassa").startsWith("Adreça"));
    }

    @Test
    @DisplayName("isPrefixTelefonValid: accepta els prefixos de tots els països i rebutja la resta")
    void isPrefixTelefonValid() {
        // Els d'abans i països de tots els continents
        for (String p : new String[]{"+34", "+33", "+351", "+44", "+49", "+1", "+7", "+81", "+86",
            "+91", "+212", "+54", "+61", "+998", "+380"}) {
            assertTrue(usuariLogic.isPrefixTelefonValid(p), p);
        }
        assertTrue(usuariLogic.isPrefixTelefonValid(" +34 "));
        assertFalse(usuariLogic.isPrefixTelefonValid("+999"));
        assertFalse(usuariLogic.isPrefixTelefonValid("+0"));
        assertFalse(usuariLogic.isPrefixTelefonValid("34"));
        assertFalse(usuariLogic.isPrefixTelefonValid(""));
        assertFalse(usuariLogic.isPrefixTelefonValid(null));
    }

    @Test
    @DisplayName("PrefixosTelefon: Espanya va la primera, no hi ha països buits i només un 'primer' per codi")
    void prefixosTelefon_llista() {
        var llista = PrefixosTelefon.llista();
        assertTrue(llista.size() > 200);
        assertEquals("+34", llista.get(0).codi());
        assertTrue(llista.stream().noneMatch(p -> p.nom() == null || p.nom().isBlank()));
        long codisDistints = llista.stream().map(PrefixosTelefon.Prefix::codi).distinct().count();
        assertEquals(codisDistints, llista.stream().filter(PrefixosTelefon.Prefix::primer).count());
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
    @DisplayName("validarFitxer: rebutja un fitxer el contingut del qual no coincideix amb el tipus declarat")
    void validarFitxer_contingutFals() {
        // Un executable (MZ...) enviat com si fos un PDF
        byte[] exe = {'M', 'Z', (byte) 0x90, 0x00, 0x03, 0x00};
        assertNotNull(usuariLogic.validarFitxer(fitxer("dni.pdf", "application/pdf", exe), "document"));
        // Un PDF enviat com si fos una imatge JPG
        byte[] pdf = {'%', 'P', 'D', 'F', '-', '1'};
        assertNotNull(usuariLogic.validarFitxer(fitxer("dni.jpg", "image/jpeg", pdf), "document"));
    }

    @Test
    @DisplayName("validarFitxer: accepta JPG, PNG i PDF dins de mida")
    void validarFitxer_ok() {
        byte[] jpg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        byte[] pdf = {'%', 'P', 'D', 'F', '-', '1', '.', '7'};
        assertNull(usuariLogic.validarFitxer(fitxer("dni.jpg", "image/jpeg", jpg)));
        assertNull(usuariLogic.validarFitxer(fitxer("dni.png", "image/png", png)));
        assertNull(usuariLogic.validarFitxer(fitxer("dni.pdf", "application/pdf", pdf)));
    }
}