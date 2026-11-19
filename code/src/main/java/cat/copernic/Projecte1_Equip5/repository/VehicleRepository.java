package cat.copernic.Projecte1_Equip5.repository;

import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repositori JPA dels vehicles (MySQL), amb la matrícula com a clau primària.
 * Amb {@link JpaSpecificationExecutor} permet cerques dinàmiques amb filtres
 * opcionals construïts a {@link VehicleSpecifications}.
 */
@Repository
public interface VehicleRepository extends
        JpaRepository<Vehicle, String>,
        JpaSpecificationExecutor<Vehicle> {

    /**
     * Compta els vehicles segons el seu estat (p. ex. disponibles, inactius).
     *
     * @param estat estat del vehicle
     * @return nombre de vehicles en aquest estat
     */
    long countByEstat(Enums.EstatVehicle estat);
}
