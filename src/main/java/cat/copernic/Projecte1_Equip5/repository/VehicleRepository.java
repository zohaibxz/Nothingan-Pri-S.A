package cat.copernic.Projecte1_Equip5.repository;

import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepository extends
        JpaRepository<Vehicle, String>,
        JpaSpecificationExecutor<Vehicle> {

    // Comptar els vehicles segons el seu estat
    long countByEstat(Enums.EstatVehicle estat);
}