/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import cat.copernic.Projecte1_Equip5.logic.VehicleLogic;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;
import java.time.Year;
import java.util.Locale;
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

    // Comprovar l'any de fabricació
    int anyActual = Year.now().getValue();

    if (vehicle.getAny_fabricacio() == null
            || vehicle.getAny_fabricacio() < 1900
            || vehicle.getAny_fabricacio() > anyActual) {
        model.addAttribute("error", "L'any de fabricació no és vàlid.");
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
