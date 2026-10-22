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
import java.util.Locale;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class VehicleController {

    @GetMapping("/vehicles/alta")
    public String formulariAlta(Model model) {
        Vehicle vehicle = new Vehicle();
        vehicle.setData_alta(LocalDate.now());
        model.addAttribute("vehicle", vehicle);
        return "vehicles/alta";
    }

    @PostMapping("/vehicles/alta")
    public String enviar(@ModelAttribute("vehicle") Vehicle vehicle, Model model) {

    // Comprovar la matrícula
    String matricula = vehicle.getMatricula();

    if (matricula == null || matricula.isBlank()) {
        model.addAttribute("error", "La matrícula és obligatòria.");
        return "vehicles/alta";
    }

    // Eliminar espais i convertir la matrícula a majúscules
    matricula = matricula.trim().toUpperCase(Locale.ROOT);
    vehicle.setMatricula(matricula);

    // Comprovar que no existeixi un vehicle amb aquesta matrícula
    if (vehicleLogic.existsById(matricula)) {
        model.addAttribute("error", "Ja existeix un vehicle amb aquesta matrícula.");
        return "vehicles/alta";
    }

    // Comprovar la marca
    if (vehicle.getMarca() == null || vehicle.getMarca().isBlank()) {
        model.addAttribute("error", "La marca és obligatòria.");
        return "vehicles/alta";
    }

    // Comprovar el model
    if (vehicle.getModel() == null || vehicle.getModel().isBlank()) {
        model.addAttribute("error", "El model és obligatori.");
        return "vehicles/alta";
    }

    // Comprovar la categoria
    if (vehicle.getCategoria() == null) {
        model.addAttribute("error", "Has de seleccionar una categoria.");
        return "vehicles/alta";
    }

    // Comprovar l'any de matriculació
    int anyActual = Year.now().getValue();

    if (vehicle.getAny_matriculacio() == null
        || vehicle.getAny_matriculacio() < 1900
        || vehicle.getAny_matriculacio() > Year.now().getValue()) {

    model.addAttribute("error", "L'any de matrícula no és vàlid.");
    return "vehicles/alta";
}

    // Comprovar el preu diari
    if (vehicle.getPreu_diari() == null || vehicle.getPreu_diari() < 0) {
        model.addAttribute("error", "El preu diari no pot ser negatiu ni buit.");
        return "vehicles/alta";
    }

    // Comprovar la data d'alta
    if (vehicle.getData_alta() == null) {
        vehicle.setData_alta(LocalDate.now());
    } else if (vehicle.getData_alta().isAfter(LocalDate.now())) {
        model.addAttribute("error", "La data d'alta no pot ser futura.");
        return "vehicles/alta";
    }

    // Netejar els espais sobrants
    vehicle.setMarca(vehicle.getMarca().trim());
    vehicle.setModel(vehicle.getModel().trim());

    // Un vehicle nou comença com a disponible
    vehicle.setEstat(Enums.EstatVehicle.Disponible);

    // Desar el vehicle
    try {
        vehicleLogic.saveVehicle(vehicle);
    } catch (DataIntegrityViolationException e) {
        model.addAttribute("error", "No s'ha pogut desar el vehicle. Revisa les dades.");
        return "vehicles/alta";
    }

    // Mostrar la pantalla de confirmació
    return "vehicles/acceptat";
}

    // Perquè el botó "Cancel·lar" no doni 404
    @GetMapping("/vehicles")
    public String llistat() {
        return "redirect:/vehicles/alta";
    }
}
