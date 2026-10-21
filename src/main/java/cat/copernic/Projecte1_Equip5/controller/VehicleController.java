/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cat.copernic.Projecte1_equip5.controller;

import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import cat.copernic.Projecte1_Equip5.logic.VehicleLogic;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class VehicleController {
 
    private static final int MIDA_PAGINA = 8;
 
    @Autowired
    private VehicleLogic vehicleLogic;
 
    // Alta de vehicle
    @GetMapping("/vehicles/alta")
    public String formulariAlta(Model model) {
        Vehicle vehicle = new Vehicle();
        vehicle.setData_alta(LocalDate.now());
        model.addAttribute("vehicle", vehicle);
        return "vehicles/alta";
    }
 
    @PostMapping("/vehicles/alta")
    public String enviar(@ModelAttribute("vehicle") Vehicle vehicle) {
        vehicleLogic.saveVehicle(vehicle);
        return "vehicles/acceptat";
    }
 
    // Llistat: cerca + filtre de categoria + paginació + KPIs
    @GetMapping("/vehicles")
    public String llistat(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(required = false) String q,
                          @RequestParam(required = false) String categoria,
                          HttpSession session,
                          Model model) {
 
        Enums.CategoriaVehicle cat = parseCategoria(categoria);
        Page<Vehicle> pagina = vehicleLogic.cerca(q, cat, Math.max(page, 0), MIDA_PAGINA);
 
        long total       = vehicleLogic.countVehicles();
        long disponibles = vehicleLogic.countPerEstat(Enums.EstatVehicle.Disponible);
        long llogats     = vehicleLogic.countPerEstat(Enums.EstatVehicle.Llogat);
        long inactius    = vehicleLogic.countPerEstat(Enums.EstatVehicle.Inactiu);
 
        model.addAttribute("pagina", pagina);
        model.addAttribute("q", q);
        model.addAttribute("categoria", categoria);
        model.addAttribute("estats", Enums.EstatVehicle.values());
 
        model.addAttribute("total", total);
        model.addAttribute("disponibles", disponibles);
        model.addAttribute("llogats", llogats);
        model.addAttribute("inactius", inactius);
        model.addAttribute("pctDisponibles", pct(disponibles, total));
        model.addAttribute("pctLlogats", pct(llogats, total));
        model.addAttribute("pctInactius", pct(inactius, total));
 
        // Nom i rol de l'usuari connectat (Client / Agent / Administrador).
        // Aquí els llegeixo de la sessió: posa-hi els atributs en fer el login.
        model.addAttribute("nomUsuari", session.getAttribute("nomUsuari"));
        model.addAttribute("rolUsuari", session.getAttribute("rolUsuari"));
 
        return "vehicles/gestio";
    }
 
    // Canvi d'estat (menú ··· de la taula)
    @PostMapping("/vehicles/{matricula}/estat")
    public String canviaEstat(@PathVariable("matricula") String matricula,
                              @RequestParam Enums.EstatVehicle estat,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(required = false) String q,
                              @RequestParam(required = false) String categoria,
                              RedirectAttributes ra) {
 
        vehicleLogic.canviaEstat(matricula, estat);
 
        // Tornem a la mateixa pàgina i amb els mateixos filtres
        ra.addAttribute("page", page);
        if (q != null && !q.isBlank()) ra.addAttribute("q", q);
        if (categoria != null && !categoria.isBlank()) ra.addAttribute("categoria", categoria);
        return "redirect:/vehicles";
    }
 
    // Eliminació (la plantilla l'envia per POST; substitueix el GET /vehicles/baixa/{matricula})
    // Si el vehicle té reserves no es pot esborrar: es deixa com a Inactiu.
    @PostMapping("/vehicles/{matricula}/elimina")
    public String elimina(@PathVariable("matricula") String matricula) {
        try {
            vehicleLogic.deleteVehicleById(matricula);
        } catch (DataIntegrityViolationException e) {
            vehicleLogic.canviaEstat(matricula, Enums.EstatVehicle.Inactiu);
        }
        return "redirect:/vehicles";
    }
 
    // Auxiliars
    private Enums.CategoriaVehicle parseCategoria(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return Enums.CategoriaVehicle.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return null;   // valor desconegut: sense filtre
        }
    }
        //Percentatge de vehicles en cada estat
    private long pct(long part, long total) {
        return total == 0 ? 0 : Math.round(100.0 * part / total);
    }
}