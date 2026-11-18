package cat.copernic.Projecte1_Equip5;

import cat.copernic.Projecte1_Equip5.entity.*;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Clase principal de la aplicación Spring Boot del proyecto. Arranca el
 * contexto de Spring (controladores, servicios, repositorios y seguridad) y, al
 * iniciarse, crea tres usuarios de prueba si la base de datos está vacía.
 */
@SpringBootApplication
public class Projecte1Equip5Application {

    /**
     * Crea la classe principal de l'aplicació; Spring Boot l'arrenca des de {@code main}.
     */
    public Projecte1Equip5Application() {
    }

    /**
     * Punto de entrada de la aplicación: lanza Spring Boot.
     *
     * @param args argumentos de línea de comandos (no se usan)
     */
    public static void main(String[] args) {
        SpringApplication.run(Projecte1Equip5Application.class, args);
    }

    /**
     * Carga datos iniciales al arrancar. Solo actúa si no existe ningún usuario
     * en la BD: crea un cliente, un agente y un administrador de prueba, todos
     * con una contraseña que cumple la política RN52 (mínimo 8 caracteres, con
     * letra y número), guardada codificada (hash). Por defecto es "Prova1234",
     * la misma que usan los usuarios de DadesProva; se puede cambiar con la
     * variable de entorno ADMIN_PASSWORD_INICIAL.
     *
     * @param usuariRepository repositorio para comprobar si hay usuarios y
     * guardar los de prueba
     * @param passwordEncoder codificador con el que se genera el hash de la
     * contraseña
     * @param passwordInicial contraseña inicial de los tres usuarios
     * @return tarea que Spring ejecuta una vez arrancada la aplicación
     */
    @Bean
    @Order(1)
    CommandLineRunner initDatabase(UsuariRepository usuariRepository, PasswordEncoder passwordEncoder,
            @Value("${ADMIN_PASSWORD_INICIAL:Prova1234}") String passwordInicial) {
        return args -> {
            if (usuariRepository.count() == 0) {
                String hash = passwordEncoder.encode(passwordInicial);

                Client client = new Client(
                        "11111111A", "client@empresa.com", hash, LocalDateTime.now(),
                        "Joan", "Pérez", LocalDate.now().plusYears(5), "B",
                        LocalDate.now().plusYears(5), "Carrer Major 1",
                        "Espanyola", Enums.EstatValidacio.Validat, null, false, null
                );
                usuariRepository.save(client);

                Agent agent = new Agent(
                        "22222222B", "agent@empresa.com", hash, LocalDateTime.now(),
                        "Carlos", "García", LocalDate.now().plusYears(5), "B",
                        LocalDate.now().plusYears(5), "Carrer Agent 2",
                        "Espanyola", Enums.EstatValidacio.Validat, null, false, null, Enums.Rol.Agent
                );
                usuariRepository.save(agent);

                Agent admin = new Agent(
                        "33333333C", "admin@empresa.com", hash, LocalDateTime.now(),
                        "Admin", "Jefe", LocalDate.now().plusYears(5), "B",
                        LocalDate.now().plusYears(5), "Carrer Admin 3",
                        "Espanyola", Enums.EstatValidacio.Validat, null, false, null, Enums.Rol.Administrador
                );
                usuariRepository.save(admin);
            }
        };
    }
}
