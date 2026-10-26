package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.document.VehicleArxiu;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import cat.copernic.Projecte1_Equip5.logic.VehicleLogic;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        model.addAttribute("combustibles", Enums.Combustible.values());
        model.addAttribute("transmissions", Enums.Transmissio.values());
        model.addAttribute("maxAnyMatriculacio", LocalDate.now().getYear());
    }

    // 1. Pàgina principal / Gestió de vehicles
    @GetMapping({"/vehicles", "/vehicles/"})
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
                         @RequestParam(name = "fotos", required = false) MultipartFile[] fotos,
                         @RequestParam(name = "documents", required = false) MultipartFile[] documents,
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

        // Comprovar el preu hores
        if (vehicle.getPreu_hores() == null || vehicle.getPreu_hores() < 0) {
            model.addAttribute("error", "El preu per hores no pot ser negatiu ni buit.");
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

        // ---- Característiques tècniques ----
        if (vehicle.getCombustible() == null) {
            model.addAttribute("error", "Has de seleccionar el tipus de combustible/motor.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }
        if (vehicle.getPotencia() == null || vehicle.getPotencia() <= 0) {
            model.addAttribute("error", "La potència ha de ser un nombre positiu (CV).");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }
        if (vehicle.getColor() == null || vehicle.getColor().isBlank()) {
            model.addAttribute("error", "El color és obligatori.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }
        if (vehicle.getPlaces() == null || vehicle.getPlaces() < 1) {
            model.addAttribute("error", "El nombre de places ha de ser com a mínim 1.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }
        if (vehicle.getTransmissio() == null) {
            model.addAttribute("error", "Has de seleccionar la transmissió.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        // ---- Condicions de lloguer ----
        // Límit de quilometratge: opcional (buit = sense límit), però si s'indica ha de ser positiu
        if (vehicle.getLimit_km() != null && vehicle.getLimit_km() <= 0) {
            model.addAttribute("error", "El límit de quilometratge ha de ser positiu (o deixa'l buit si no n'hi ha).");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }
        // El mínim de dies de lloguer és 1 dia
        if (vehicle.getMin_dies() == null || vehicle.getMin_dies() < 1) {
            model.addAttribute("error", "El mínim de dies de lloguer ha de ser com a mínim 1.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }
        if (vehicle.getMax_dies() == null || vehicle.getMax_dies() < vehicle.getMin_dies()) {
            model.addAttribute("error", "El màxim de dies de lloguer no pot ser inferior al mínim.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }
        if (vehicle.getFianca() == null || vehicle.getFianca().signum() < 0) {
            model.addAttribute("error", "La fiança estàndard no pot ser negativa ni estar buida.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        // ---- Dades privades: fitxers (es validen TOTS abans de desar el vehicle) ----
        String errorArxius = vehicleLogic.validarArxius(fotos, documents);
        if (errorArxius != null) {
            model.addAttribute("error", errorArxius);
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        // Netejar els espais sobrants
        vehicle.setMarca(vehicle.getMarca().trim());
        vehicle.setModel(vehicle.getModel().trim());
        vehicle.setColor(vehicle.getColor().trim());
        if (vehicle.getComentaris_privats() != null) {
            String c = vehicle.getComentaris_privats().trim();
            vehicle.setComentaris_privats(c.isEmpty() ? null : c);
        }

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

        // Desar les fotos i els documents privats
        try {
            vehicleLogic.desarArxius(vehicle.getMatricula(), fotos, documents);
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("missatge",
                    "El vehicle s'ha desat, però no s'han pogut desar els fitxers.");
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

    // Només el personal (agent / administrador) pot veure les dades privades.
    // El rol es desa a la sessió en fer login (LoginController).
    private boolean esPersonal(HttpSession session) {
        Object rol = session.getAttribute("rol");
        return "agent".equals(rol) || "administrador".equals(rol);
    }

    // 4b. Detall del vehicle (icona de l'ull de la gestió de vehicles)
    @GetMapping("/vehicles/{matricula}")
    public String detall(@PathVariable String matricula,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(required = false) String q,
                         @RequestParam(required = false) String categoria,
                         HttpSession session,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Vehicle vehicle = vehicleLogic.getVehicleById(matricula);
        if (vehicle == null) {
            redirectAttributes.addFlashAttribute("missatge", "No s'ha trobat el vehicle " + matricula + ".");
            return "redirect:/vehicles";
        }
        model.addAttribute("vehicle", vehicle);

        // Per tornar a la mateixa pàgina i amb els mateixos filtres
        model.addAttribute("page", page);
        model.addAttribute("q", q);
        model.addAttribute("categoria", categoria);

        // Dades privades: només es carreguen al model si l'usuari és personal
        boolean personal = esPersonal(session);
        model.addAttribute("esPersonal", personal);
        if (personal) {
            List<VehicleArxiu> arxius = vehicleLogic.arxiusDe(matricula);
            model.addAttribute("fotos", arxius.stream()
                    .filter(a -> a.getTipus() == VehicleArxiu.Tipus.FOTO).toList());
            model.addAttribute("documents", arxius.stream()
                    .filter(a -> a.getTipus() == VehicleArxiu.Tipus.DOCUMENT).toList());
        }
        return "vehicles/detall";
    }

    // 4c. Descàrrega / visualització d'un fitxer privat (només personal)
    @GetMapping("/vehicles/{matricula}/arxius/{id}")
    public ResponseEntity<byte[]> arxiu(@PathVariable String matricula,
                                        @PathVariable String id,
                                        HttpSession session) {
        if (!esPersonal(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        VehicleArxiu a = vehicleLogic.getArxiu(id).orElse(null);
        if (a == null || !matricula.equals(a.getMatricula())) {
            return ResponseEntity.notFound().build();
        }
        MediaType tipus = "text/plain".equals(a.getContentType())
                ? new MediaType("text", "plain", StandardCharsets.UTF_8)
                : MediaType.parseMediaType(a.getContentType());
        return ResponseEntity.ok()
                .contentType(tipus)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(a.getNom(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(a.getDades());
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
    
    // Elimina un vehicle per la seva matrícula i torna a la mateixa pàgina mantenint els filtres.
    @PostMapping("/vehicles/{matricula}/eliminar")
    public String eliminarVehicle(@PathVariable String matricula,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(required = false) String q,
                                  @RequestParam(required = false) String categoria,
                                  RedirectAttributes redirectAttributes) {
        try {
            vehicleLogic.deleteVehicleById(matricula);
            redirectAttributes.addFlashAttribute("missatge", "El vehicle " + matricula + " s'ha eliminat correctament.");
        } catch (DataIntegrityViolationException e) {
            // Si el vehicle té reserves associades, no es pot eliminar per la clau forana de la BD
            redirectAttributes.addFlashAttribute("error", "No es pot eliminar el vehicle " + matricula + " perquè té reserves associades. Pots canviar el seu estat a Inactiu.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error en eliminar el vehicle: " + e.getMessage());
        }

        // Torna a la mateixa pàgina i amb els mateixos filtres
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