/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cat.copernic.Projecte1_Equip5.logic;

import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import cat.copernic.Projecte1_Equip5.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
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

    // Desa o actualitza un vehicle i retorna la seva matrícula (String)
    public String saveVehicle(Vehicle vehicle) {
        Vehicle vehicleGuardat = vehicleRepository.save(vehicle);
        return vehicleGuardat.getMatricula(); // Canvia per getId() si el teu getter es diu així
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
                .orElseThrow(() -> new IllegalArgumentException("Vehicle inexistent: " + matricula));
        v.setEstat(estat);
        vehicleRepository.save(v);
    }
}