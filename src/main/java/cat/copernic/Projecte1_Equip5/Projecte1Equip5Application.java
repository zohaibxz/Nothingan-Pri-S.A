package cat.copernic.Projecte1_Equip5;

import cat.copernic.Projecte1_Equip5.entity.*;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Clase principal de la aplicación Spring Boot del proyecto. Arranca el
 * contexto de Spring (controladores, servicios, repositorios y seguridad) y, al
 * iniciarse, crea tres usuarios de prueba si la base de datos está vacía.
 */
@SpringBootApplication
public class Projecte1Equip5Application {

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
     * con la contraseña "123456" guardada codificada (hash).
     *
     * @param usuariRepository repositorio para comprobar si hay usuarios y
     * guardar los de prueba
     * @param passwordEncoder codificador con el que se genera el hash de la
     * contraseña
     * @return tarea que Spring ejecuta una vez arrancada la aplicación
     */
    @Bean
    CommandLineRunner initDatabase(UsuariRepository usuariRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (usuariRepository.count() == 0) {
                String hash = passwordEncoder.encode("123456");

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
