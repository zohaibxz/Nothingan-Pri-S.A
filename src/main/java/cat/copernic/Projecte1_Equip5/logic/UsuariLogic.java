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
 * Capa de lógica: reglas de validación de negocio, sin acceso a la BD ni a la web.
 * Son métodos sencillos que reciben un dato y devuelven true/false. Los usa ClientService.registrar
 * (y validarClient/rebutjarClient) y, al no depender de nada más, son fáciles de probar con tests.
 */
// @Component: Spring la registra como componente genérico (ni controlador, ni servicio, ni repositorio),
// para poder inyectarla en otras clases, como hace ClientService en su constructor
@Component
public class UsuariLogic {
    
    private static final Pattern PATRO_TELEFON = Pattern.compile("^(\\+\\d{1,3})\\s+([\\d\\s-]+)$"); 
    // para validat el telefon 
    private static final long MIDA_MAXIMA = 5 * 1024 * 1024; // 5 MB // variable añadido para validar tamaño archivos 
    private static final Set<String> TIPUS_PERMESOS  // // variable añadido para validar tamaño archivos 
            = Set.of("image/jpeg", "image/png", "application/pdf");
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
 
    // Lletres oficials del DNI/NIE: la posició és (número mod 23)
    private static final String LLETRES_DNI = "TRWAGMYFPDXBNJZSQVHLCKE";

    /**
     * Comprova només el FORMAT d'un DNI o NIE espanyol:
     *   DNI: 8 xifres + 1 lletra (ex. 12345678Z)
     *   NIE: X, Y o Z + 7 xifres + 1 lletra (ex. X1234567L)
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
     * es divideix el número entre 23 i el RESTA indica la lletra dins de LLETRES_DNI.
     * En un NIE, la X/Y/Z inicial es canvia per 0/1/2 abans de dividir.
     * Si el format no és vàlid retorna false.
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
     * Comprueba que la contraseña y su confirmación (campo "Confirma la contrasenya" del registro) son iguales.
     * Evita que el usuario se equivoque al escribirla. Si alguna es null devuelve false.
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
 
    // Fecha de nacimiento: opcional, pero si viene no puede ser futura
    public boolean isDataNaixementValida(LocalDate dataNaixement) {
        return dataNaixement == null || !dataNaixement.isAfter(LocalDate.now());
    }

    // Solo si tus RN exigen mayoría de edad
    public boolean isMajorDEdat(LocalDate dataNaixement) {
        return dataNaixement == null
                || Period.between(dataNaixement, LocalDate.now()).getYears() >= 18;
    }

    // Teléfono: opcional; formato "+34 612345678" (el que monta el controlador)
    // solo responde sí o no. Su regex exige el formato exacto +34 612345678: prefijo, un único espacio y solo dígitos.
    public boolean isTelefonValid(String telefonComplet) {
        return telefonComplet == null || telefonComplet.matches("^\\+\\d{1,3} \\d{6,12}$");
    }
    
    // esto es para cuando se quiera actualizar datos 
    // transforma el texto: lo deja en ese formato exacto, y devuelve null si viene vacío.
    public String normalitzarTelefon(String telefon) {
        if (telefon == null || telefon.isBlank()) {
            return null;
        }
        Matcher m = PATRO_TELEFON.matcher(telefon.trim());
        return m.matches()
                ? m.group(1) + " " + m.group(2).replaceAll("[\\s-]", "")
                : telefon.trim();
    }
    
    // Población de un cliente, para poder filtrar por ella en la lista de clientes.
    // No hay columna de población: ClientController guarda la dirección como "carrer, codiPostal ciutat"
    // (p. ej. "Carrer de Mallorca 123, 3r 2a, 08036 Barcelona"). La población es lo que va
    // después de la ÚLTIMA coma, sin el código postal del principio ("08036 Barcelona" -> "Barcelona").
    // Devuelve null si no se puede saber (dirección sin coma o sin población), y así no
    // coincide con ningún filtro en vez de coincidir con el nombre de la calle.
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

    // PARA TEMA DE CRUD DOCUMENTS Y HACER RESET PASSWORD 
    // Valida un fichero subido con las tres reglas que ya existen.
    // Devuelve el mensaje de error, o null si es correcto.
    public String validarFitxer(MultipartFile fitxer) {
        return validarFitxer(fitxer, "fitxer");
    }

    // Igual que la anterior, pero el mensaje nombra el fichero concreto
    // (p. ex. "document d'identitat"). Es la ÚNICA validación de ficheros del
    // proyecto: DocumentacioService y ClientService la reutilizan.
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