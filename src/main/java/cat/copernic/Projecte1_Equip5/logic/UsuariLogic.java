package cat.copernic.Projecte1_Equip5.logic;

import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
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
     * Valida la longitud y complejidad de la contraseña.
     */
    public boolean isPasswordSecure(String password) {
        // No nula y con al menos 6 caracteres (no exige mayúsculas, números ni símbolos)
        return password != null && password.length() >= 6;
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
        return null;
    }

}