package cat.copernic.Projecte1_Equip5.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// añadido para el tema del campo actiu para poder dar de baja column default true..
import org.hibernate.annotations.ColumnDefault;

/**
 * Clase base abstracta de todos los usuarios de la aplicación (clientes,
 * agentes y administradores). Se guarda en la tabla {@code usuaris} y las
 * clases hijas ({@link Client} y, por herencia de esta, {@link Agent}) añaden
 * sus datos en tablas propias unidas por el DNI (herencia {@code JOINED}).
 *
 * Es abstracta: siempre se crea un {@code Client} o un {@code Agent}. El campo
 * {@code actiu} permite dar de baja a un usuario sin borrarlo de la base de
 * datos; empieza siempre en {@code true} y por eso no se pasa por constructor.
 */
@Entity
@Table(name = "usuaris")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Usuari {

    @Id
    @Column(name = "dni_usuari", length = 10) // no hace falta poner unique = true porque el Dni es clave primaria y 
    // luego en validaciones no nos ahorramos nada por poner unique = true
    private String dniUsuari;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "data_creacio", nullable = false, updatable = false)
    private LocalDateTime dataCreacio;

    @Column(name = "nom", nullable = false, length = 50)
    private String nom;

    @Column(name = "cognoms", nullable = false, length = 100)
    private String cognoms;

    @Column(name = "actiu", nullable = false)
    // @@ColumnDefault("true") si fuera la primera que ejecutaramso el proyecto y la base de datos estuviera vacia no haria falta eso ... 
    @ColumnDefault("true")
    /* Por qué = true y además @ColumnDefault("true"): 
    el = true cubre los objetos nuevos creados desde Java, y por eso no tienes 
    que tocar ningún constructor. Pero tu base de datos ya tiene filas y usas ddl-auto=update. 
    Si Hibernate añade una columna NOT NULL sin valor por defecto, MySQL pone 0 (false) a todas las filas existentes. 
    Entonces todos tus usuarios quedarían dados de baja y nadie podría entrar. El @ColumnDefault hace que las filas antiguas queden como activas.*/
    private boolean actiu = true;

    // no hace falta añadir el cmapo actiu en el cosntrcutor porque luego habrai que añadir para todoas las clases hijas
    // y ademas actiu seimpre es true asi que no hace añadir a los campos hijas etc..
    // Constructores
    /**
     * Crea un usuario con todos sus datos, incluido el DNI (clave primaria). El
     * campo {@code actiu} no se pasa: el usuario nace activo.
     *
     * @param dniUsuari DNI del usuario (clave primaria)
     * @param email email del usuario (único; se usa para el login)
     * @param passwordHash contraseña ya codificada (hash), nunca en texto plano
     * @param dataCreacio fecha y hora de alta del usuario
     * @param nom nombre del usuario
     * @param cognoms apellidos del usuario
     */
    public Usuari(String dniUsuari, String email, String passwordHash, LocalDateTime dataCreacio, String nom, String cognoms) {
        this.dniUsuari = dniUsuari;
        this.email = email;
        this.passwordHash = passwordHash;
        this.dataCreacio = dataCreacio;
        this.nom = nom;
        this.cognoms = cognoms;
    }

    /**
     * Crea un usuario sin DNI, para casos en que la clave primaria se asigna
     * después (por ejemplo, al rellenar el formulario de alta).
     *
     * @param email email del usuario (único; se usa para el login)
     * @param passwordHash contraseña ya codificada (hash), nunca en texto plano
     * @param dataCreacio fecha y hora de alta del usuario
     * @param nom nombre del usuario
     * @param cognoms apellidos del usuario
     */
    public Usuari(String email, String passwordHash, LocalDateTime dataCreacio, String nom, String cognoms) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.dataCreacio = dataCreacio;
        this.nom = nom;
        this.cognoms = cognoms;
    }

    /**
     * Constructor vacío, necesario para JPA y para que Spring pueda rellenar el
     * objeto con los datos de un formulario.
     */
    public Usuari() {
    }

    // Getters y Setters
    /**
     * Devuelve el DNI del usuario, que es la clave primaria de la tabla.
     *
     * @return DNI del usuario
     */
    public String getDniUsuari() {
        return dniUsuari;
    }

    /**
     * Establece el DNI del usuario, que es la clave primaria de la tabla.
     *
     * @param dniUsuari DNI del usuario
     */
    public void setDniUsuari(String dniUsuari) {
        this.dniUsuari = dniUsuari;
    }

    /**
     * Devuelve el email del usuario, único en toda la aplicación; se usa como
     * nombre de usuario en el login.
     *
     * @return email del usuario
     */
    public String getEmail() {
        return email;
    }

    /**
     * Establece el email del usuario, único en toda la aplicación; se usa como
     * nombre de usuario en el login.
     *
     * @param email email del usuario
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Devuelve la contraseña del usuario codificada (hash); nunca se guarda en
     * texto plano.
     *
     * @return contraseña codificada (hash)
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Establece la contraseña del usuario codificada (hash); nunca se guarda en
     * texto plano.
     *
     * @param passwordHash contraseña codificada (hash)
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Devuelve la fecha y hora en que se dio de alta al usuario.
     *
     * @return fecha y hora de alta
     */
    public LocalDateTime getDataCreacio() {
        return dataCreacio;
    }

    /**
     * Establece la fecha y hora en que se dio de alta al usuario.
     *
     * @param dataCreacio fecha y hora de alta
     */
    public void setDataCreacio(LocalDateTime dataCreacio) {
        this.dataCreacio = dataCreacio;
    }

    /**
     * Devuelve el nombre del usuario.
     *
     * @return nombre del usuario
     */
    public String getNom() {
        return nom;
    }

    /**
     * Establece el nombre del usuario.
     *
     * @param nom nombre del usuario
     */
    public void setNom(String nom) {
        this.nom = nom;
    }

    /**
     * Devuelve los apellidos del usuario.
     *
     * @return apellidos del usuario
     */
    public String getCognoms() {
        return cognoms;
    }

    /**
     * Establece los apellidos del usuario.
     *
     * @param cognoms apellidos del usuario
     */
    public void setCognoms(String cognoms) {
        this.cognoms = cognoms;
    }

    /**
     * Establece si el usuario está activo; {@code false} significa que está
     * dado de baja.
     *
     * @param actiu {@code true} si el usuario está activo, {@code false} si
     * está dado de baja
     */
    public void setActiu(boolean actiu) {
        this.actiu = actiu;
    }

    /**
     * Devuelve si el usuario está activo; {@code false} significa que está dado
     * de baja.
     *
     * @return {@code true} si el usuario está activo, {@code false} si está
     * dado de baja
     */
    public boolean isActiu() {
        return actiu;
    }
}
