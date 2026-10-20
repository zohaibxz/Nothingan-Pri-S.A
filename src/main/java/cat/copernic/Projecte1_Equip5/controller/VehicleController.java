/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cat.copernic.Projecte1_equip5.controller;

import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import cat.copernic.Projecte1_Equip5.logic.VehicleLogic;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class VehicleController {

    @Autowired
    private VehicleLogic vehicleLogic;

    // Formulari d'alta de vehicle
    @GetMapping("/vehicles/alta")
    public String formulariAlta(Model model) {
        Vehicle vehicle = new Vehicle();
        vehicle.setData_alta(LocalDate.now());
        model.addAttribute("vehicle", vehicle);
        return "vehicles/alta";
    }

    // Processar l'alta del vehicle
    @PostMapping("/vehicles/alta")
    public String enviar(@ModelAttribute("vehicle") Vehicle vehicle) {
        vehicleLogic.saveVehicle(vehicle);
        return "vehicles/acceptat";
    }

    // Baixa directa (eliminació de la base de dades per matrícula)
    @GetMapping("/vehicles/baixa/{matricula}")
    public String baixaVehicle(@PathVariable("matricula") String matricula) {
        vehicleLogic.deleteVehicleById(matricula);
        return "redirect:/vehicles";
    }

    // Llistat general de vehicles
    @GetMapping("/vehicles")
    public String llistat(Model model) {
        model.addAttribute("vehicles", vehicleLogic.findAllVehicles());
        return "vehicles/llistat";
    }
}