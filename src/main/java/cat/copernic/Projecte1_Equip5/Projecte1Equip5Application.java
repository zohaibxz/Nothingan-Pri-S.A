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


@SpringBootApplication
public class Projecte1Equip5Application {

    public static void main(String[] args) {
        SpringApplication.run(Projecte1Equip5Application.class, args);
    }

    @Bean
    CommandLineRunner initDatabase(UsuariRepository usuariRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (usuariRepository.count() == 0) {
                String hash = passwordEncoder.encode("123456");

                Client client = new Client(
                    "11111111A", "client@empresa.com", hash, LocalDateTime.now(),
                    "Joan", "Pérez", LocalDate.now().plusYears(5), "B",
                    LocalDate.now().plusYears(5), "4532000000000000", "Carrer Major 1",
                    "Espanyola", Enums.EstatValidacio.Validat, null, false, null
                );
                usuariRepository.save(client);

                Agent agent = new Agent(
                    "22222222B", "agent@empresa.com", hash, LocalDateTime.now(),
                    "Carlos", "García", LocalDate.now().plusYears(5), "B",
                    LocalDate.now().plusYears(5), "4532000000000001", "Carrer Agent 2",
                    "Espanyola", Enums.EstatValidacio.Validat, null, false, null, Enums.Rol.Agent
                );
                usuariRepository.save(agent);

                Agent admin = new Agent(
                    "33333333C", "admin@empresa.com", hash, LocalDateTime.now(),
                    "Admin", "Jefe", LocalDate.now().plusYears(5), "B",
                    LocalDate.now().plusYears(5), "4532000000000002", "Carrer Admin 3",
                    "Espanyola", Enums.EstatValidacio.Validat, null, false, null, Enums.Rol.Administrador
                );
                usuariRepository.save(admin);
            }
        };
    }
}