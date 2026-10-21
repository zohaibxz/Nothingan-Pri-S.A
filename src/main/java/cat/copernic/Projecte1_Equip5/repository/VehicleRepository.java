/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cat.copernic.Projecte1_Equip5.repository;

import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;



@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    
    // Cerca per matrícula, marca o model (sense distingir majúscules), sense filtre de categoria
    Page<Vehicle> findByMatriculaContainingIgnoreCaseOrMarcaContainingIgnoreCaseOrModelContainingIgnoreCase(
            String matricula, String marca, String model, Pageable pageable);
 
    // El mateix, però només d'una categoria: (categoria I matrícula) O (categoria I marca) O (categoria I model)
    Page<Vehicle> findByCategoriaAndMatriculaContainingIgnoreCaseOrCategoriaAndMarcaContainingIgnoreCaseOrCategoriaAndModelContainingIgnoreCase(
            Enums.CategoriaVehicle c1, String matricula,
            Enums.CategoriaVehicle c2, String marca,
            Enums.CategoriaVehicle c3, String model,
            Pageable pageable);
 
    // Per als KPIs
    long countByEstat(Enums.EstatVehicle estat);
}