package cat.copernic.Projecte1_Equip5.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Empleado de la empresa: agente o administrador. Extiende {@link Client}, así
 * que comparte con él todos los datos personales y de validación, y añade el
 * campo {@code rol}, que distingue a un {@code Agent} de un
 * {@code Administrador} (este último tiene más permisos). Spring Security
 * traduce ese rol con {@link AuthorityType#of(Usuari)}.
 */
@Entity
@Table(name = "agents")
@PrimaryKeyJoinColumn(name = "id_agent")
public class Agent extends Client {

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private Enums.Rol rol;

    // 1. Buit
    /**
     * Constructor vacío, necesario para JPA y para rellenar el objeto desde un
     * formulario.
     */
    public Agent() {
        super();
    }

    // 2. Amb tots els atributs (amb DNI)
    /**
     * Crea un agente con todos sus atributos, incluido el DNI (clave primaria)
     * y el rol.
     *
     * @param dniUsuari DNI del usuario (clave primaria)
     * @param email email del usuario (único; se usa para el login)
     * @param passwordHash contraseña ya codificada (hash), nunca en texto plano
     * @param dataCreacio fecha y hora de alta del usuario
     * @param nom nombre del usuario
     * @param cognoms apellidos del usuario
     * @param dataCaducitatDni fecha de caducidad del DNI
     * @param tipusLlicencia tipo de carnet de conducir (por ejemplo, B)
     * @param dataCaducitatLlicencia fecha de caducidad del carnet de conducir
     * @param adreca dirección postal del cliente
     * @param nacionalitat nacionalidad del cliente
     * @param estatValidacio estado de validación (Pendent, Validat o Rebutjat)
     * @param agentValidador agente que validó al cliente, o {@code null} si aún
     * no está validado
     * @param premium si el cliente es premium
     * @param descomptePremium descuento que tiene el cliente premium, o
     * {@code null} si no tiene
     * @param rol rol del empleado (Agent o Administrador)
     */
    public Agent(String dniUsuari, String email, String passwordHash, LocalDateTime dataCreacio,
            String nom, String cognoms, LocalDate dataCaducitatDni, String tipusLlicencia,
            LocalDate dataCaducitatLlicencia, String adreca,
            String nacionalitat, Enums.EstatValidacio estatValidacio, Agent agentValidador,
            Boolean premium, BigDecimal descomptePremium, Enums.Rol rol) {
        super(dniUsuari, email, passwordHash, dataCreacio, nom, cognoms, dataCaducitatDni,
                tipusLlicencia, dataCaducitatLlicencia, adreca, nacionalitat,
                estatValidacio, agentValidador, premium, descomptePremium);
        this.rol = rol;
    }

    // 3. Sense clau primària (sense DNI)
    /**
     * Crea un agente sin DNI, para cuando la clave primaria se asigna después.
     *
     * @param email email del usuario (único; se usa para el login)
     * @param passwordHash contraseña ya codificada (hash), nunca en texto plano
     * @param dataCreacio fecha y hora de alta del usuario
     * @param nom nombre del usuario
     * @param cognoms apellidos del usuario
     * @param dataCaducitatDni fecha de caducidad del DNI
     * @param tipusLlicencia tipo de carnet de conducir (por ejemplo, B)
     * @param dataCaducitatLlicencia fecha de caducidad del carnet de conducir
     * @param adreca dirección postal del cliente
     * @param nacionalitat nacionalidad del cliente
     * @param estatValidacio estado de validación (Pendent, Validat o Rebutjat)
     * @param agentValidador agente que validó al cliente, o {@code null} si aún
     * no está validado
     * @param premium si el cliente es premium
     * @param descomptePremium descuento que tiene el cliente premium, o
     * {@code null} si no tiene
     * @param rol rol del empleado (Agent o Administrador)
     */
    public Agent(String email, String passwordHash, LocalDateTime dataCreacio,
            String nom, String cognoms, LocalDate dataCaducitatDni, String tipusLlicencia,
            LocalDate dataCaducitatLlicencia, String adreca,
            String nacionalitat, Enums.EstatValidacio estatValidacio, Agent agentValidador,
            Boolean premium, BigDecimal descomptePremium, Enums.Rol rol) {
        super(email, passwordHash, dataCreacio, nom, cognoms, dataCaducitatDni,
                tipusLlicencia, dataCaducitatLlicencia, adreca, nacionalitat,
                estatValidacio, agentValidador, premium, descomptePremium);
        this.rol = rol;
    }

    // Getters y Setters
    /**
     * Devuelve el rol del empleado: {@code Agent} o {@code Administrador}.
     *
     * @return rol del empleado
     */
    public Enums.Rol getRol() {
        return rol;
    }

    /**
     * Establece el rol del empleado: {@code Agent} o {@code Administrador}.
     *
     * @param rol rol del empleado
     */
    public void setRol(Enums.Rol rol) {
        this.rol = rol;
    }

}
