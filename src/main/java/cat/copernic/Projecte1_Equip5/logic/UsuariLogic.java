package cat.copernic.Projecte1_Equip5.logic;
        
import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import java.time.LocalDate;
import java.time.Period;
// añadido los 2 imports de abajo para el tema de validar imageens que cumpla con el tamaño por ejemplo 
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;
 
import org.springframework.stereotype.Component;
// Librería Apache Commons Validator: trae ya hecho el algoritmo de Luhn (LuhnCheckDigit),
// así no hay que escribirlo a mano. Por eso hay que añadir la dependencia commons-validator en el pom.xml.
// (No confundir con la anotación @LuhnCheck de la entidad Client, que viene de Hibernate Validator)
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;

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
 
    /**
     * Valida el formato básico de un DNI o NIE español.
     */
    public boolean isDniValid(String dni) {
        // null o solo espacios -> no válido
        if (dni == null || dni.trim().isEmpty()) {
            return false;
        }
        // Expresión regular para 8 números y 1 letra (o NIE con X, Y, Z)
        // Se pasa a mayúsculas para aceptar también "12345678a". Desglose de la expresión:
        //   ^          inicio del texto
        //   [XYZ]?     opcionalmente una X, Y o Z al principio (los NIE)
        //   \\d{7,8}   entre 7 y 8 dígitos
        //   [A-Z]      una letra final
        //   $          fin del texto
        // Solo comprueba el FORMATO: no verifica que la letra sea la correcta para ese número
        return dni.toUpperCase().matches("^[XYZ]?\\d{7,8}[A-Z]$");
    }
 
    /**
     * Valida la longitud y complejidad de la contraseña.
     */
    public boolean isPasswordSecure(String password) {
        // No nula y con al menos 6 caracteres (no exige mayúsculas, números ni símbolos)
        return password != null && password.length() >= 6;
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
 
    // Quita espacios y guiones del número de tarjeta: "4111 1111-1111 1111" -> "4111111111111111"
    // replaceAll con la expresión [\\s-]: cualquier espacio en blanco (\s) o guion (-) se sustituye por nada.
    // Es el valor que se guarda en la BD (targetaNeta en ClientService)
    public String normalitzarTargeta(String targeta) {
        return targeta == null ? null : targeta.replaceAll("[\\s-]", "");
    }
 
    /**
     * Solo dígitos, espacios y guiones.
     */
    public boolean isTargetaFormatValid(String targeta) {
        // [\\d\\s-]+ : uno o más caracteres que sean dígito, espacio o guion.
        // matches exige que TODO el texto cumpla la expresión (no basta con una parte)
        return targeta != null && targeta.matches("[\\d\\s-]+");
    }
 
    /**
     * Entre 13 y 19 dígitos (sin contar espacios ni guiones).
     */
    public boolean isTargetaLongitudValida(String targeta) {
        // Primero se limpian espacios y guiones, y luego se exige entre 13 y 19 dígitos (\\d{13,19})
        return targeta != null && normalitzarTargeta(targeta).matches("\\d{13,19}");
    }
 
    /**
     * Algoritmo de Luhn.
     */
    public boolean isTargetaLuhnValida(String targeta) {
        // El algoritmo de Luhn comprueba el último dígito del número como control (dígito de verificación)
        // para detectar errores de escritura. Lo calcula la librería de Apache sobre el número ya limpio.
        // No comprueba que la tarjeta exista ni tenga saldo, solo que el número sea coherente
        return targeta != null
                && LuhnCheckDigit.LUHN_CHECK_DIGIT.isValid(normalitzarTargeta(targeta));
    }
 
    /**
     * Valida las tres reglas a la vez (útil para tests).
     */
    public boolean isTargetaValida(String targeta) {
        // && encadena las tres comprobaciones: formato, longitud y Luhn.
        // Si una falla, devuelve false y no se evalúan las siguientes (cortocircuito).
        // ClientService.registrar las llama por separado para dar un mensaje de error distinto en cada caso
        return isTargetaFormatValid(targeta)
                && isTargetaLongitudValida(targeta)
                && isTargetaLuhnValida(targeta);
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
    
    // PARA TEMA DE CRUD DOCUMENTS Y HACER RESET PASSWORD 
    // Tarjeta enmascarada para mostrarla: "•••• •••• •••• 1234" ("—" si no hay datos)
    public String mascararTargeta(String targeta) {
        return (targeta != null && targeta.length() >= 4)
                ? "•••• •••• •••• " + targeta.substring(targeta.length() - 4)
                : "—";
    }

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
 
 