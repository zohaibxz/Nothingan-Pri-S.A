package cat.copernic.Projecte1_Equip5.logic;
        
import java.time.LocalDate;
import org.springframework.stereotype.Component;

import org.springframework.stereotype.Component;

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
}
