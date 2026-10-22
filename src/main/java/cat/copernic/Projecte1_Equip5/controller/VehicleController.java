package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import cat.copernic.Projecte1_Equip5.logic.VehicleLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;
import java.time.Year;
import java.util.Locale;

@Controller
public class VehicleController {

    @Autowired
    private VehicleLogic vehicleLogic;

    /**
     * Mètode auxiliar per recarregar les dades necessàries al formulari d'alta
     * quan hi ha un error de validació.
     */
    private void carregarDadesFormulari(Model model) {
        model.addAttribute("categories", Enums.CategoriaVehicle.values());
        model.addAttribute("maxAnyMatriculacio", LocalDate.now().getYear());
    }

    // 1. Pàgina principal / Gestió de vehicles
    @GetMapping({"/", "/vehicles", "/vehicles/"})
    public String llistat() {
        return "vehicles/gestio";
    }

    // 2. Mostrar el formulari d'alta
    @GetMapping("/vehicles/alta")
    public String formulariAlta(Model model) {
        Vehicle vehicle = new Vehicle();
        vehicle.setData_alta(LocalDate.now());

        model.addAttribute("vehicle", vehicle);
        carregarDadesFormulari(model);

        return "vehicles/alta";
    }

    // 3. Processar el formulari d'alta
    @PostMapping("/vehicles/alta")
    public String enviar(@ModelAttribute("vehicle") Vehicle vehicle, Model model) {

        // Comprovar la matrícula
        String matricula = vehicle.getMatricula();

        if (matricula == null || matricula.isBlank()) {
            model.addAttribute("error", "La matrícula és obligatòria.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        // Eliminar espais i convertir la matrícula a majúscules
        matricula = matricula.trim().toUpperCase(Locale.ROOT);
        vehicle.setMatricula(matricula);

        // Comprovar que no existeixi un vehicle amb aquesta matrícula
        if (vehicleLogic.existsById(matricula)) {
            model.addAttribute("error", "Ja existeix un vehicle amb aquesta matrícula.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        // Comprovar la marca
        if (vehicle.getMarca() == null || vehicle.getMarca().isBlank()) {
            model.addAttribute("error", "La marca és obligatòria.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        // Comprovar el model
        if (vehicle.getModel() == null || vehicle.getModel().isBlank()) {
            model.addAttribute("error", "El model és obligatori.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        // Comprovar la categoria
        if (vehicle.getCategoria() == null) {
            model.addAttribute("error", "Has de seleccionar una categoria.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        // Comprovar l'any de matriculació
        if (vehicle.getAny_matriculacio() == null
                || vehicle.getAny_matriculacio() < 1900
                || vehicle.getAny_matriculacio() > Year.now().getValue()) {

            model.addAttribute("error", "L'any de matrícula no és vàlid.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        // Comprovar el preu diari
        if (vehicle.getPreu_diari() == null || vehicle.getPreu_diari() < 0) {
            model.addAttribute("error", "El preu diari no pot ser negatiu ni buit.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        // Comprovar la data d'alta
        if (vehicle.getData_alta() == null) {
            vehicle.setData_alta(LocalDate.now());
        } else if (vehicle.getData_alta().isAfter(LocalDate.now())) {
            model.addAttribute("error", "La data d'alta no pot ser futura.");
            carregarDadesFormulari(model);
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
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        // Mostrar la pantalla de confirmació
        return "vehicles/acceptat";
    }
}