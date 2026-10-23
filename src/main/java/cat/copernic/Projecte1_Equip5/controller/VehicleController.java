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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;

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
    public String llistat(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        // La categoria arriba com a text; si és buida o no vàlida, no es filtra
        Enums.CategoriaVehicle categoriaEnum = null;
        if (categoria != null && !categoria.isBlank()) {
            try {
                categoriaEnum = Enums.CategoriaVehicle.valueOf(categoria);
            } catch (IllegalArgumentException e) {
                categoria = null;
            }
        }

        Page<Vehicle> pagina = vehicleLogic.cerca(q, categoriaEnum, Math.max(page, 0), 10);

        long total = vehicleLogic.countVehicles();
        long disponibles = vehicleLogic.countPerEstat(Enums.EstatVehicle.Disponible);
        long llogats = vehicleLogic.countPerEstat(Enums.EstatVehicle.Llogat);
        long inactius = vehicleLogic.countPerEstat(Enums.EstatVehicle.Inactiu);

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

        return "vehicles/gestio";
    }

    // Percentatge enter sobre el total (evita dividir per zero)
    private long pct(long part, long total) {
        return total == 0 ? 0 : Math.round(part * 100.0 / total);
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
    public String enviar(@ModelAttribute("vehicle") Vehicle vehicle, Model model,
                         RedirectAttributes redirectAttributes) {

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
                || vehicle.getAny_matriculacio() < 1800
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

        // Redirect a la pàgina de confirmació (evita reenviar el formulari en refrescar)
        redirectAttributes.addAttribute("matricula", vehicle.getMatricula());
        return "redirect:/vehicles/acceptat/{matricula}";
    }

    // 4. Pàgina de confirmació de vehicle acceptat
    @GetMapping("/vehicles/acceptat/{matricula}")
    public String acceptat(@PathVariable String matricula, Model model) {
        Vehicle vehicle = vehicleLogic.getVehicleById(matricula);
        if (vehicle == null) {
            return "redirect:/vehicles";
        }
        model.addAttribute("vehicle", vehicle);
        return "vehicles/acceptat";
    }

    // 5. Canviar l'estat d'un vehicle (menú "···" de la gestió de vehicles)
    @PostMapping("/vehicles/{matricula}/estat")
    public String canviaEstat(@PathVariable String matricula,
                              @RequestParam Enums.EstatVehicle estat,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(required = false) String q,
                              @RequestParam(required = false) String categoria,
                              RedirectAttributes redirectAttributes) {
        try {
            vehicleLogic.canviaEstat(matricula, estat);
            redirectAttributes.addFlashAttribute("missatge",
                    "El vehicle " + matricula + " ara està " + estat.name() + ".");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("missatge", e.getMessage());
        }

        // Tornar a la mateixa pàgina i amb els mateixos filtres
        redirectAttributes.addAttribute("page", page);
        if (q != null && !q.isBlank()) {
            redirectAttributes.addAttribute("q", q);
        }
        if (categoria != null && !categoria.isBlank()) {
            redirectAttributes.addAttribute("categoria", categoria);
        }
        return "redirect:/vehicles";
    }
}