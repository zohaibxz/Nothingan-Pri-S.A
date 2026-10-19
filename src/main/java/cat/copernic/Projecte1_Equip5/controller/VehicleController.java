/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cat.copernic.Projecte1_equip5.controller;

import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;

@Controller
public class VehicleController {

    @GetMapping("/vehicles/alta")
    public String formulariAlta(Model model) {
        Vehicle vehicle = new Vehicle();
        vehicle.setData_alta(LocalDate.now());
        model.addAttribute("vehicle", vehicle);
        return "vehicles/alta";
    }

    // No guarda res: al enviar, simplement torna al formulari
    @PostMapping("/vehicles/alta")
    public String enviar() {
        return "redirect:/vehicles/alta";
    }

    // Perquè el botó "Cancel·lar" no doni 404
    @GetMapping("/vehicles")
    public String llistat() {
        return "redirect:/vehicles/alta";
    }
}