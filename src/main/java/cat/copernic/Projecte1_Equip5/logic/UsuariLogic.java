package cat.copernic.Projecte1_Equip5.logic;
        
import cat.copernic.Projecte1_Equip5.entity.Client;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import java.time.LocalDate;

import org.springframework.stereotype.Component;
// dependencia para @LuhnCheck para importar el algoritmo y no tener que escribirlo a mano 
//en este capa logica y he tenido que añadir la dependencia en pom
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;


@Component
public class UsuariLogic {

    /**
     * Valida si la fecha del documento o carné no está caducada.
     */
    public boolean isDocumentValid(LocalDate dataCaducitat) {
        if (dataCaducitat == null) {
            return false;
        }
        return dataCaducitat.isAfter(LocalDate.now());
    }

    /**
     * Valida el formato básico de un DNI o NIE español.
     */
    public boolean isDniValid(String dni) {
        if (dni == null || dni.trim().isEmpty()) {
            return false;
        }
        // Expresión regular para 8 números y 1 letra (o NIE con X, Y, Z)
        return dni.toUpperCase().matches("^[XYZ]?\\d{7,8}[A-Z]$");
    }

    /**
     * Valida la longitud y complejidad de la contraseña.
     */
    public boolean isPasswordSecure(String password) {
        return password != null && password.length() >= 6;
    }
    
    // --- NUEVO MÉTODO PARA LA LÓGICA DE VALIDACIÓN ---
    /**
     * Comprueba si el cliente está en estado pendiente para ser procesado.
     */
    public boolean potSerValidatORebutjat(Client client) {
        return client != null && client.getEstatValidacio() == Enums.EstatValidacio.Pendent;
    }
    
    public String normalitzarTargeta(String targeta) {
        return targeta == null ? null : targeta.replaceAll("[\\s-]", "");
    }
    /**
     * Solo dígitos, espacios y guiones.
     */
    public boolean isTargetaFormatValid(String targeta) {
        return targeta != null && targeta.matches("[\\d\\s-]+");
    }

    /**
     * Entre 13 y 19 dígitos (sin contar espacios ni guiones).
     */
    public boolean isTargetaLongitudValida(String targeta) {
        return targeta != null && normalitzarTargeta(targeta).matches("\\d{13,19}");
    }

    /**
     * Algoritmo de Luhn.
     */
    public boolean isTargetaLuhnValida(String targeta) {
        return targeta != null
                && LuhnCheckDigit.LUHN_CHECK_DIGIT.isValid(normalitzarTargeta(targeta));
    }

    /**
     * Valida las tres reglas a la vez (útil para tests).
     */
    public boolean isTargetaValida(String targeta) {
        return isTargetaFormatValid(targeta)
                && isTargetaLongitudValida(targeta)
                && isTargetaLuhnValida(targeta);
    }
}
