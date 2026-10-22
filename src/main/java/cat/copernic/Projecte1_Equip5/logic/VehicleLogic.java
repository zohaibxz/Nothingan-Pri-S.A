/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cat.copernic.Projecte1_Equip5.logic;

import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import cat.copernic.Projecte1_Equip5.repository.VehicleRepository;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleLogic {

    @Autowired
    private VehicleRepository vehicleRepository;

    // Obté tots els vehicles
    public List<Vehicle> findAllVehicles() {
        return vehicleRepository.findAll();
    }

    // Obté un vehicle per la seva matrícula (String)
    public Vehicle getVehicleById(String matricula) {
        Optional<Vehicle> vehicle = vehicleRepository.findById(matricula);
        return vehicle.orElse(null);
    }

    // Dona d'alta un vehicle nou. No actualitza un vehicle existent amb la mateixa matrícula.
    public String altaVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            throw new IllegalArgumentException("No s'han rebut les dades del vehicle.");
        }

        if (vehicle.getMatricula() == null || vehicle.getMatricula().isBlank()) {
            throw new IllegalArgumentException("La matrícula és obligatòria.");
        }
        if (vehicle.getMarca() == null || vehicle.getMarca().isBlank()) {
            throw new IllegalArgumentException("La marca és obligatòria.");
        }
        if (vehicle.getModel() == null || vehicle.getModel().isBlank()) {
            throw new IllegalArgumentException("El model és obligatori.");
        }
        if (vehicle.getCategoria() == null) {
            throw new IllegalArgumentException("Has de seleccionar una categoria.");
        }
        if (vehicle.getAny_matriculacio() == null
                || vehicle.getAny_matriculacio() < 1900
                || vehicle.getAny_matriculacio() > LocalDate.now().getYear() + 1) {
            throw new IllegalArgumentException("L'any de matriculació no és vàlid.");
        }
        if (vehicle.getPreu_diari() == null || vehicle.getPreu_diari() < 0) {
            throw new IllegalArgumentException("El preu diari no pot ser negatiu ni estar buit.");
        }

        // Normalitzem la matrícula per evitar diferències entre minúscules i majúscules.
        String matricula = vehicle.getMatricula().trim().toUpperCase(Locale.ROOT);
        if (vehicleRepository.existsById(matricula)) {
            throw new IllegalArgumentException("Ja existeix un vehicle amb la matrícula " + matricula + ".");
        }

        vehicle.setMatricula(matricula);
        vehicle.setMarca(vehicle.getMarca().trim());
        vehicle.setModel(vehicle.getModel().trim());

        if (vehicle.getEstat() == null) {
            vehicle.setEstat(Enums.EstatVehicle.Disponible);
        }
        if (vehicle.getData_alta() == null) {
            vehicle.setData_alta(LocalDate.now());
        }

        Vehicle vehicleGuardat = vehicleRepository.save(vehicle);
        return vehicleGuardat.getMatricula();
    }

    // Desa o actualitza un vehicle; es manté per a l'API REST existent.
    public String saveVehicle(Vehicle vehicle) {
        Vehicle vehicleGuardat = vehicleRepository.save(vehicle);
        return vehicleGuardat.getMatricula();
    }

    // Comprova si existeix un vehicle per matrícula
    public boolean existsById(String matricula) {
        if (matricula == null) {
            return false;
        }
        return vehicleRepository.existsById(matricula);
    }

    // Elimina un vehicle per matrícula
    public void deleteVehicleById(String matricula) {
        vehicleRepository.deleteById(matricula);
    }
    
    public Page<Vehicle> cerca(String q, Enums.CategoriaVehicle categoria, int page, int size) {
        String t = (q == null) ? "" : q.trim();
        Pageable pageable = PageRequest.of(page, size, Sort.by("matricula"));
 
        if (categoria == null) {
            return vehicleRepository
                .findByMatriculaContainingIgnoreCaseOrMarcaContainingIgnoreCaseOrModelContainingIgnoreCase(
                    t, t, t, pageable);
        }
        return vehicleRepository
            .findByCategoriaAndMatriculaContainingIgnoreCaseOrCategoriaAndMarcaContainingIgnoreCaseOrCategoriaAndModelContainingIgnoreCase(
                categoria, t, categoria, t, categoria, t, pageable);
    }
 
    public long countVehicles() {
        return vehicleRepository.count();
    }
 
    public long countPerEstat(Enums.EstatVehicle estat) {
        return vehicleRepository.countByEstat(estat);
    }
 
    @Transactional
public void canviaEstat(String matricula, Enums.EstatVehicle estat) {
    Vehicle v = vehicleRepository.findById(matricula)
            .orElseThrow(() ->
                    new IllegalArgumentException(
                            "Vehicle inexistent: " + matricula));

    v.setEstat(estat);
    vehicleRepository.save(v);
}
}