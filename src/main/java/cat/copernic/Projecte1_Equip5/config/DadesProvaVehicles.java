package cat.copernic.Projecte1_Equip5.config;

import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import cat.copernic.Projecte1_Equip5.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Carrega uns quants vehicles de prova en arrencar l'aplicació, només si la
 * taula de vehicles està buida. No crea reserves. Es pot desactivar amb la
 * propietat {@code app.dades-prova=false}.
 */
@Component
@ConditionalOnProperty(name = "app.dades-prova", havingValue = "true", matchIfMissing = true)
public class DadesProvaVehicles implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DadesProvaVehicles.class);

    private final VehicleRepository vehicleRepository;

    /**
     * Constructor amb injecció del repositori de vehicles.
     *
     * @param vehicleRepository repositori on es desen els vehicles de prova
     */
    public DadesProvaVehicles(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    /**
     * Desa els vehicles de prova si encara no n'hi ha cap a la base de dades.
     *
     * @param args arguments de línia d'ordres (no s'utilitzen)
     */
    @Override
    public void run(String... args) {
        if (vehicleRepository.count() > 0) {
            return;
        }

        Vehicle inactiu = vehicle("2468HJK", "Peugeot", Enums.CategoriaVehicle.Turisme, "208", 2019,
                7.0, Enums.Combustible.Gasolina, 100, "Vermell", 5, Enums.Transmissio.Manual,
                200, 20, "200.00", "Pendent de passar l'ITV.");
        inactiu.setEstat(Enums.EstatVehicle.Inactiu);

        List<Vehicle> vehicles = List.of(
                vehicle("1234BCD", "Seat", Enums.CategoriaVehicle.Turisme, "Ibiza", 2022,
                        8.5, Enums.Combustible.Gasolina, 95, "Blanc", 5, Enums.Transmissio.Manual,
                        300, 30, "250.00", "Vehicle de prova."),
                vehicle("2345FGH", "Volkswagen", Enums.CategoriaVehicle.Turisme, "Golf", 2021,
                        10.0, Enums.Combustible.Dièsel, 115, "Gris", 5, Enums.Transmissio.Manual,
                        250, 14, "300.00", null),
                vehicle("3456JKL", "Toyota", Enums.CategoriaVehicle.SUV, "RAV4", 2023,
                        14.0, Enums.Combustible.Híbrid, 222, "Negre", 5, Enums.Transmissio.Automàtic,
                        null, 30, "500.00", null),
                vehicle("4567MNP", "Tesla", Enums.CategoriaVehicle.Elèctric, "Model 3", 2024,
                        18.0, Enums.Combustible.Elèctric, 283, "Blanc", 5, Enums.Transmissio.Automàtic,
                        null, 15, "600.00", "Càrrega al garatge de l'oficina."),
                vehicle("5678RST", "Ford", Enums.CategoriaVehicle.Furgoneta, "Transit", 2020,
                        12.0, Enums.Combustible.Dièsel, 130, "Blanc", 3, Enums.Transmissio.Manual,
                        400, 30, "400.00", null),
                vehicle("6789VWX", "Porsche", Enums.CategoriaVehicle.Esportiu, "911", 2022,
                        45.0, Enums.Combustible.Gasolina, 385, "Vermell", 2, Enums.Transmissio.Automàtic,
                        150, 7, "2000.00", "Només clients premium."),
                vehicle("7890BCF", "Renault", Enums.CategoriaVehicle.Elèctric, "Zoe", 2021,
                        9.0, Enums.Combustible.Elèctric, 109, "Blau", 5, Enums.Transmissio.Automàtic,
                        200, 20, "250.00", null),
                vehicle("1357DFG", "Hyundai", Enums.CategoriaVehicle.SUV, "Tucson", 2023,
                        13.0, Enums.Combustible.Híbrid, 150, "Gris", 5, Enums.Transmissio.Automàtic,
                        null, 30, "450.00", null),
                vehicle("9753LMN", "Mercedes-Benz", Enums.CategoriaVehicle.Furgoneta, "Vito", 2022,
                        16.0, Enums.Combustible.Dièsel, 136, "Negre", 9, Enums.Transmissio.Automàtic,
                        300, 30, "500.00", null),
                inactiu);

        vehicleRepository.saveAll(vehicles);
        LOG.info("Carregats {} vehicles de prova", vehicles.size());
    }

    /**
     * Crea un vehicle de prova amb l'estat Disponible, el mínim d'1 dia i la
     * data d'alta fixada fa 30 dies.
     */
    private static Vehicle vehicle(String matricula, String marca, Enums.CategoriaVehicle categoria,
            String model, int anyMatriculacio, double preuHores, Enums.Combustible combustible,
            int potencia, String color, int places, Enums.Transmissio transmissio,
            Integer limitKm, int maxDies, String fianca, String comentaris) {
        return new Vehicle(matricula, marca, categoria, model, anyMatriculacio, preuHores,
                LocalDate.now().minusDays(30), combustible, potencia, color, places, transmissio,
                limitKm, maxDies, new BigDecimal(fianca), comentaris);
    }
}