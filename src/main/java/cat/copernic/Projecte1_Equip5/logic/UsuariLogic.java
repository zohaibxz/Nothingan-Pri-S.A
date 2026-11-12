package cat.copernic.Projecte1_Equip5.logic;

import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
// añadido los 2 imports de abajo para el tema de validar imageens que cumpla con el tamaño por ejemplo 
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.stereotype.Component;
// para validar el telefon
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Capa de lógica: reglas de validación de negocio, sin acceso a la BD ni a la
 * web. Son métodos sencillos que reciben un dato y devuelven true/false. Los
 * usa ClientService.registrar (y validarClient/rebutjarClient) y, al no
 * depender de nada más, son fáciles de probar con tests.
 */

// @Component: Spring la registra como componente genérico (ni controlador, ni servicio, ni repositorio),
// para poder inyectarla en otras clases, como hace ClientService en su constructor
@Component
public class UsuariLogic {

    private static final Pattern PATRO_TELEFON = Pattern.compile("^(\\+\\d{1,3})\\s+([\\d\\s-]+)$");
    // para validat el telefon 
    private static final long MIDA_MAXIMA = 5 * 1024 * 1024; // 5 MB // variable añadido para validar tamaño archivos 
    private static final Set<String> TIPUS_PERMESOS // // variable añadido para validar tamaño archivos 
            = Set.of("image/jpeg", "image/png", "application/pdf");

    // Lletres oficials del DNI/NIE: la posició és (número mod 23)
    private static final String LLETRES_DNI = "TRWAGMYFPDXBNJZSQVHLCKE";

    // RN20: límits i formats de les dades de text (coincideixen amb la longitud de les columnes de BD)
    private static final int MIN_PASSWORD = 8; // RN52: longitud mínima
    private static final int MAX_BYTES_PASSWORD = 72; // BCrypt ignora/rebutja el que passa de 72 bytes
    private static final int MAX_EMAIL = 100;
    private static final Pattern PATRO_EMAIL
            = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");
    // Noms i cognoms: lletres (amb accents), espais, punt, apòstrof, guió i punt volat (l·l)
    private static final Pattern PATRO_NOM
            = Pattern.compile("^\\p{L}[\\p{L} .'\\u2019\\u00B7-]*$");
    // Adreça: lletres, números i la puntuació habitual d'una adreça (sense < > " ni caràcters de control)
    private static final Pattern PATRO_ADRECA
            = Pattern.compile("^[\\p{L}\\p{N}][\\p{L}\\p{N} .,'\\u2019\\u00B7/#()-]*$");
    // Tipus de carnet: A, A2, B, B+E, C1...
    private static final Pattern PATRO_LLICENCIA = Pattern.compile("^[A-Za-z0-9+ ]+$");

    /**
     * Valida si la fecha del documento o carné no está caducada.
     */
    public boolean isDocumentValid(LocalDate dataCaducitat) {
        // Sin fecha, no es válido
        if (dataCaducitat == null) {
            return false;
        }
        // Válido solo si la fecha de caducidad es POSTERIOR a hoy
        // (si caduca hoy mismo, ya cuenta como caducado)
        return dataCaducitat.isAfter(LocalDate.now());
    }

    /**
     * El fitxer existeix i no està buit.
     */
    public boolean isFitxerPresent(MultipartFile fitxer) {
        return fitxer != null && !fitxer.isEmpty();
    }

    /**
     * El fitxer no supera els 5 MB.
     */
    public boolean isFitxerMidaValida(MultipartFile fitxer) {
        return fitxer != null && fitxer.getSize() <= MIDA_MAXIMA;
    }

    /**
     * El fitxer és JPG, PNG o PDF.
     */
    public boolean isFitxerTipusValid(MultipartFile fitxer) {
        return fitxer != null && TIPUS_PERMESOS.contains(fitxer.getContentType());
    }

    /**
     * RN20 / RN53: el contingut real del fitxer coincideix amb el tipus que
     * declara el navegador. El Content-Type el posa el client i es pot
     * falsejar; per això es miren els primers bytes (la "signatura" del
     * format): JPG = FF D8 FF, PNG = 89 50 4E 47 0D 0A 1A 0A, PDF = %PDF-.
     *
     * @param fitxer fitxer pujat
     * @return true si els primers bytes corresponen al tipus declarat
     */
    public boolean isFitxerContingutValid(MultipartFile fitxer) {
        if (fitxer == null || fitxer.getContentType() == null) {
            return false;
        }
        byte[] cap = new byte[8];
        int llegits;
        try (java.io.InputStream in = fitxer.getInputStream()) {
            llegits = in.readNBytes(cap, 0, cap.length);
        } catch (java.io.IOException e) {
            return false;
        }
        return switch (fitxer.getContentType()) {
            case "image/jpeg" -> llegits >= 3 && (cap[0] & 0xFF) == 0xFF
                    && (cap[1] & 0xFF) == 0xD8 && (cap[2] & 0xFF) == 0xFF;
            case "image/png" -> llegits >= 8 && (cap[0] & 0xFF) == 0x89 && cap[1] == 'P'
                    && cap[2] == 'N' && cap[3] == 'G' && cap[4] == 0x0D && cap[5] == 0x0A
                    && cap[6] == 0x1A && cap[7] == 0x0A;
            case "application/pdf" -> llegits >= 5 && cap[0] == '%' && cap[1] == 'P'
                    && cap[2] == 'D' && cap[3] == 'F' && cap[4] == '-';
            default -> false;
        };
    }

    /**
     * Comprova només el FORMAT d'un DNI o NIE espanyol: DNI: 8 xifres + 1
     * lletra (ex. 12345678Z); NIE: X, Y o Z + 7 xifres + 1 lletra (ex.
     * X1234567L).
     *
     * @param dni DNI o NIE (pot ser null)
     * @return true si el format és correcte
     */
    public boolean isDniFormatValid(String dni) {
        if (dni == null || dni.trim().isEmpty()) {
            return false;
        }
        // toUpperCase per acceptar també "12345678z"
        return dni.trim().toUpperCase().matches("^([0-9]{8}|[XYZ][0-9]{7})[A-Z]$");
    }

    /**
     * Comprova que la lletra final és la que toca segons l'algorisme oficial:
     * es divideix el número entre 23 i la resta indica la lletra dins de
     * LLETRES_DNI. En un NIE, la X/Y/Z inicial es canvia per 0/1/2 abans de
     * dividir. Si el format no és vàlid retorna false.
     *
     * @param dni DNI o NIE (pot ser null)
     * @return true si el format i la lletra són correctes
     */
    public boolean isDniLletraCorrecta(String dni) {
        if (!isDniFormatValid(dni)) {
            return false;
        }
        String net = dni.trim().toUpperCase();
        char primera = net.charAt(0);
        String numero = net.substring(0, net.length() - 1);
        // NIE: X -> 0, Y -> 1, Z -> 2
        if (primera == 'X') {
            numero = "0" + numero.substring(1);
        } else if (primera == 'Y') {
            numero = "1" + numero.substring(1);
        } else if (primera == 'Z') {
            numero = "2" + numero.substring(1);
        }
        int resta = (int) (Long.parseLong(numero) % 23);
        return LLETRES_DNI.charAt(resta) == net.charAt(net.length() - 1);
    }

    /**
     * Valida el formato básico de un DNI o NIE español.
     * Solo comprueba el FORMATO (se mantiene tal como estaba): la letra se verifica
     * a parte con isDniLletraCorrecta, que es lo que usa el registro de clientes.
     */
    public boolean isDniValid(String dni) {
        // null o solo espacios -> no válido
        if (dni == null || dni.trim().isEmpty()) {
            return false;
        }
        // 8 números y 1 letra (o NIE con X, Y, Z delante); se pasa a mayúsculas para aceptar "12345678a"
        return dni.toUpperCase().matches("^[XYZ]?\\d{7,8}[A-Z]$");
    }

    /**
     * RN52: valida que la contrasenya sigui prou segura: entre 8 caràcters i
     * 72 bytes (BCrypt no admet més) i amb com a mínim una lletra i un número.
     *
     * @param password contrasenya en clar (pot ser null)
     * @return true si compleix la política de contrasenyes
     */
    public boolean isPasswordSecure(String password) {
        return password != null
                && password.length() >= MIN_PASSWORD
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES_PASSWORD
                && password.chars().anyMatch(Character::isLetter)
                && password.chars().anyMatch(Character::isDigit);
    }

    /**
     * Valida el format d'un correu electrònic: obligatori, màxim 100 caràcters
     * i amb forma usuari@domini.ext (RN20).
     *
     * @param email correu (pot ser null)
     * @return true si el format és correcte
     */
    public boolean isEmailValid(String email) {
        if (email == null) {
            return false;
        }
        String net = email.trim();
        return !net.isEmpty() && net.length() <= MAX_EMAIL && PATRO_EMAIL.matcher(net).matches();
    }

    /**
     * Valida les dades de text d'una persona (RN20): que no estiguin buides,
     * que no superin la longitud de la columna de la BD i que no portin
     * caràcters estranys. Tot en un sol lloc perquè el registre i la
     * modificació de dades apliquin exactament les mateixes regles.
     *
     * @return el missatge de l'error (indicant quin camp falla), o null si
     * tot és correcte
     */
    public String validarDadesPersonals(String nom, String cognoms, String email,
            String adreca, String nacionalitat, String tipusLlicencia) {
        String error = validarText(nom, "Nom", 1, 50, PATRO_NOM);
        if (error == null) {
            error = validarText(cognoms, "Cognoms", 1, 100, PATRO_NOM);
        }
        if (error == null && !isEmailValid(email)) {
            error = "Correu electrònic: ha de tenir un format vàlid (ex. nom@domini.cat) i un màxim de "
                    + MAX_EMAIL + " caràcters.";
        }
        if (error == null) {
            error = validarText(adreca, "Adreça", 3, 255, PATRO_ADRECA);
        }
        if (error == null) {
            error = validarText(nacionalitat, "Nacionalitat", 2, 50, PATRO_NOM);
        }
        if (error == null) {
            error = validarText(tipusLlicencia, "Tipus de carnet", 1, 30, PATRO_LLICENCIA);
        }
        return error;
    }

    /**
     * Valida un camp de text: obligatori, entre min i max caràcters i amb el
     * patró indicat.
     *
     * @return missatge d'error o null si és vàlid
     */
    private String validarText(String valor, String camp, int min, int max, Pattern patro) {
        if (valor == null || valor.isBlank()) {
            return camp + ": és obligatori.";
        }
        String net = valor.trim();
        if (net.length() < min || net.length() > max) {
            return camp + ": ha de tenir entre " + min + " i " + max + " caràcters.";
        }
        if (!patro.matcher(net).matches()) {
            return camp + ": conté caràcters no permesos.";
        }
        return null;
    }

    /**
     * Comprova que la contrasenya i la seva confirmació (camp "Confirma la
     * contrasenya" del registre) són iguals. Evita que l'usuari s'equivoqui en
     * escriure-la. Si alguna és null retorna false.
     *
     * @param password contrasenya
     * @param confirmPassword repetició de la contrasenya
     * @return true si són iguals i cap és null
     */
    public boolean contrasenyesCoincideixen(String password, String confirmPassword) {
        return password != null && confirmPassword != null && password.equals(confirmPassword);
    }

    // --- NUEVO MÉTODO PARA LA LÓGICA DE VALIDACIÓN ---
    /**
     * Comprueba si el cliente está en estado pendiente para ser procesado.
     */
    public boolean potSerValidatORebutjat(Client client) {
        // Solo se puede validar o rechazar a un cliente que exista y esté en estado Pendent.
        // Evita validar dos veces o cambiar el estado de uno ya validado o rechazado
        return client != null && client.getEstatValidacio() == Enums.EstatValidacio.Pendent;
    }

    /**
     * Valida la data de naixement: és opcional, però si hi és no pot ser
     * futura.
     *
     * @param dataNaixement data de naixement (pot ser null)
     * @return true si és null o no és posterior a avui
     */
    public boolean isDataNaixementValida(LocalDate dataNaixement) {
        return dataNaixement == null || !dataNaixement.isAfter(LocalDate.now());
    }

    /**
     * Comprova la majoria d'edat (18 anys o més). Si no hi ha data es considera
     * vàlid.
     *
     * @param dataNaixement data de naixement (pot ser null)
     * @return true si és null o té 18 anys o més
     */
    public boolean isMajorDEdat(LocalDate dataNaixement) {
        return dataNaixement == null
                || Period.between(dataNaixement, LocalDate.now()).getYears() >= 18;
    }

    /**
     * Valida el telèfon complet, que és opcional. Ha de tenir el format exacte
     * "+34 612345678": prefix amb +, un sol espai i només dígits.
     *
     * @param telefonComplet telèfon amb prefix (pot ser null)
     * @return true si és null o té el format correcte
     */
    public boolean isTelefonValid(String telefonComplet) {
        return telefonComplet == null || telefonComplet.matches("^\\+\\d{1,3} \\d{6,12}$");
    }

    /**
     * Deixa el telèfon en el format "+prefix número" (sense espais ni guions al
     * número), per al cas d'actualitzar dades.
     *
     * @param telefon telèfon introduït (pot ser null o buit)
     * @return telèfon normalitzat, o null si és buit
     */
    public String normalitzarTelefon(String telefon) {
        if (telefon == null || telefon.isBlank()) {
            return null;
        }
        Matcher m = PATRO_TELEFON.matcher(telefon.trim());
        return m.matches()
                ? m.group(1) + " " + m.group(2).replaceAll("[\\s-]", "")
                : telefon.trim();
    }

    /**
     * Obté la població d'una adreça guardada com "carrer, codiPostal ciutat":
     * és el que hi ha després de l'última coma, sense el codi postal. Serveix
     * per filtrar clients per població.
     *
     * @param adreca adreça completa (pot ser null)
     * @return la població, o null si no es pot saber
     */
    public String extreurPoblacio(String adreca) {
        if (adreca == null) {
            return null;
        }
        int coma = adreca.lastIndexOf(',');
        if (coma < 0) {
            return null;
        }
        String resta = adreca.substring(coma + 1).trim();
        // Quita el primer trozo si lleva números (es el código postal)
        String senseCodi = resta.replaceFirst("^\\S*\\d\\S*(\\s+|$)", "");
        return senseCodi.isBlank() ? null : senseCodi;
    }

    /**
     * Valida un fitxer de documentació (present, fins a 5 MB i JPG, PNG o PDF)
     * amb un missatge genèric.
     *
     * @param fitxer fitxer pujat
     * @return el missatge d'error, o null si és vàlid
     */
    public String validarFitxer(MultipartFile fitxer) {
        return validarFitxer(fitxer, "fitxer");
    }

    /**
     * Valida un fitxer de documentació (present, fins a 5 MB i JPG, PNG o PDF)
     * i anomena el fitxer concret als missatges (p. ex. "document
     * d'identitat"). És l'única validació de fitxers de documentació: la
     * reutilitzen DocumentacioService i ClientService.
     *
     * @param fitxer fitxer pujat
     * @param nom nom del fitxer per als missatges d'error
     * @return el missatge d'error, o null si és vàlid
     */
    public String validarFitxer(MultipartFile fitxer, String nom) {
        if (!isFitxerPresent(fitxer)) {
            return "Cal adjuntar el " + nom + ".";
        }
        if (!isFitxerMidaValida(fitxer)) {
            return "El " + nom + " no pot superar els 5 MB.";
        }
        if (!isFitxerTipusValid(fitxer)) {
            return "El " + nom + " ha de ser JPG, PNG o PDF.";
        }
        // RN53: el contingut ha de ser realment del tipus que diu el navegador
        if (!isFitxerContingutValid(fitxer)) {
            return "El " + nom + " no és un JPG, PNG o PDF vàlid: el contingut no coincideix amb el tipus del fitxer.";
        }
        return null;
    }

}
