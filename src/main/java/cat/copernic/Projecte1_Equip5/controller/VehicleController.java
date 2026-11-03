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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;

@Controller
public class VehicleController {

    @Autowired
    private VehicleLogic vehicleLogic;

    /**
     * Carrega les dades necessàries per als formularis.
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
            @RequestParam(required = false) String fMatricula,
            @RequestParam(required = false) String fMarca,
            @RequestParam(required = false) String fModel,
            @RequestParam(required = false) Integer fAny,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInici,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFi,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        Enums.CategoriaVehicle categoriaEnum = null;

        if (categoria != null && !categoria.isBlank()) {
            try {
                categoriaEnum = Enums.CategoriaVehicle.valueOf(categoria);
            } catch (IllegalArgumentException e) {
                categoria = null;
            }
        }

        // Validació del rang de dates (disponibilitat entre data mínima i màxima).
        String errorFiltre = null;
        if ((dataInici == null) != (dataFi == null)) {
            errorFiltre = "Per filtrar per disponibilitat cal indicar la data mínima i la data màxima.";
            dataInici = null;
            dataFi = null;
        } else if (dataInici != null && !dataFi.isAfter(dataInici)) {
            errorFiltre = "La data màxima ha de ser posterior a la data mínima.";
            dataInici = null;
            dataFi = null;
        }

        Page<Vehicle> pagina = vehicleLogic.cercaFiltrada(
                q, fMatricula, fMarca, fModel, fAny, categoriaEnum,
                dataInici, dataFi, Math.max(page, 0), 10);

        long total = vehicleLogic.countVehicles();
        long disponibles = vehicleLogic.countPerEstat(Enums.EstatVehicle.Disponible);
        long llogats = vehicleLogic.countPerEstat(Enums.EstatVehicle.Llogat);
        long inactius = vehicleLogic.countPerEstat(Enums.EstatVehicle.Inactiu);

        model.addAttribute("pagina", pagina);
        model.addAttribute("q", q);
        model.addAttribute("categoria", categoria);
        model.addAttribute("fMatricula", fMatricula);
        model.addAttribute("fMarca", fMarca);
        model.addAttribute("fModel", fModel);
        model.addAttribute("fAny", fAny);
        model.addAttribute("dataInici", dataInici);
        model.addAttribute("dataFi", dataFi);
        model.addAttribute("filtresAvancatsActius",
                errorFiltre != null
                || (fMatricula != null && !fMatricula.isBlank())
                || (fMarca != null && !fMarca.isBlank())
                || (fModel != null && !fModel.isBlank())
                || fAny != null || dataInici != null);
        if (errorFiltre != null) {
            model.addAttribute("error", errorFiltre);
        }
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

    // Conserva els filtres en redirigir al llistat després d'una acció.
    private void conservaFiltres(RedirectAttributes ra, int page, String q, String categoria,
            String fMatricula, String fMarca, String fModel, Integer fAny,
            LocalDate dataInici, LocalDate dataFi) {
        ra.addAttribute("page", page);
        if (q != null && !q.isBlank()) {
            ra.addAttribute("q", q);
        }
        if (categoria != null && !categoria.isBlank()) {
            ra.addAttribute("categoria", categoria);
        }
        if (fMatricula != null && !fMatricula.isBlank()) {
            ra.addAttribute("fMatricula", fMatricula);
        }
        if (fMarca != null && !fMarca.isBlank()) {
            ra.addAttribute("fMarca", fMarca);
        }
        if (fModel != null && !fModel.isBlank()) {
            ra.addAttribute("fModel", fModel);
        }
        if (fAny != null) {
            ra.addAttribute("fAny", fAny);
        }
        if (dataInici != null) {
            ra.addAttribute("dataInici", dataInici);
        }
        if (dataFi != null) {
            ra.addAttribute("dataFi", dataFi);
        }
    }

    // Calcula el percentatge sense dividir per zero.
    private long pct(long part, long total) {
        return total == 0 ? 0 : Math.round(part * 100.0 / total);
    }

    // 2. Formulari d'alta
    @GetMapping("/vehicles/alta")
    public String formulariAlta(Model model) {
        Vehicle vehicle = new Vehicle();

        // Data inicial per al formulari d'alta.
        vehicle.setData_alta(LocalDate.now());

        model.addAttribute("vehicle", vehicle);
        model.addAttribute("editant", false);

        carregarDadesFormulari(model);

        return "vehicles/alta";
    }

    // 3. Processar l'alta o la modificació
    @PostMapping("/vehicles/alta")
    public String enviar(
            @ModelAttribute("vehicle") Vehicle vehicle,
            Model model,
            @RequestParam(name = "fotos", required = false) MultipartFile[] fotos,
            @RequestParam(name = "documents", required = false) MultipartFile[] documents,
            @RequestParam(name = "editant", defaultValue = "false") boolean editant,
            RedirectAttributes redirectAttributes) {

        model.addAttribute("editant", editant);

        // Comprovar la matrícula.
        String matricula = vehicle.getMatricula();

        if (matricula == null || matricula.isBlank()) {
            model.addAttribute("error", "La matrícula és obligatòria.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        matricula = matricula.trim().toUpperCase(Locale.ROOT);
        vehicle.setMatricula(matricula);

        // Comprovar si és una alta o una modificació.
        Vehicle vehicleOriginal = null;

        if (editant) {

            // En una modificació, el vehicle ja ha d'existir.
            vehicleOriginal = vehicleLogic.getVehicleById(matricula);

            if (vehicleOriginal == null) {
                model.addAttribute(
                        "error",
                        "No s'ha trobat el vehicle que vols modificar.");
                carregarDadesFormulari(model);
                return "vehicles/alta";
            }

            // Conservar la matrícula i la data d'alta originals.
            vehicle.setMatricula(vehicleOriginal.getMatricula());
            vehicle.setData_alta(vehicleOriginal.getData_alta());

            // Conservar l'estat actual del vehicle.
            vehicle.setEstat(vehicleOriginal.getEstat());

        } else {

            // En una alta nova no es pot repetir la matrícula.
            if (vehicleLogic.existsById(matricula)) {
                model.addAttribute(
                        "error",
                        "Ja existeix un vehicle amb aquesta matrícula.");
                carregarDadesFormulari(model);
                return "vehicles/alta";
            }

            // La data d'alta només es valida en crear el vehicle.
            if (vehicle.getData_alta() == null) {
                model.addAttribute(
                        "error",
                        "Has d'introduir la data d'alta.");
                carregarDadesFormulari(model);
                return "vehicles/alta";
            }

            if (vehicle.getData_alta().isAfter(LocalDate.now())) {
                model.addAttribute(
                        "error",
                        "La data d'alta no pot ser futura.");
                carregarDadesFormulari(model);
                return "vehicles/alta";
            }

            // Un vehicle nou comença com a disponible.
            vehicle.setEstat(Enums.EstatVehicle.Disponible);
        }

        // Comprovar la marca.
        if (vehicle.getMarca() == null
                || vehicle.getMarca().isBlank()) {
            model.addAttribute("error", "La marca és obligatòria.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        // Comprovar el model.
        if (vehicle.getModel() == null
                || vehicle.getModel().isBlank()) {
            model.addAttribute("error", "El model és obligatori.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        // Comprovar la categoria.
        if (vehicle.getCategoria() == null) {
            model.addAttribute(
                    "error", "Has de seleccionar una categoria.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        // Comprovar l'any de matriculació.
        if (vehicle.getAny_matriculacio() == null
                || vehicle.getAny_matriculacio() < 1900
                || vehicle.getAny_matriculacio() > Year.now().getValue()) {
            model.addAttribute(
                    "error", "L'any de matrícula no és vàlid.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        // Comprovar el preu per hores.
        if (vehicle.getPreu_hores() == null
                || vehicle.getPreu_hores() < 0) {
            model.addAttribute(
                    "error",
                    "El preu per hores no pot ser negatiu ni buit.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        // La data no es torna a validar ni a modificar en una edició.
        // Ja s'ha recuperat de la base de dades anteriorment.
        // Característiques tècniques.
        if (vehicle.getCombustible() == null) {
            model.addAttribute(
                    "error",
                    "Has de seleccionar el tipus de combustible/motor.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        if (vehicle.getPotencia() == null
                || vehicle.getPotencia() <= 0) {
            model.addAttribute(
                    "error",
                    "La potència ha de ser un nombre positiu (CV).");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        if (vehicle.getColor() == null
                || vehicle.getColor().isBlank()) {
            model.addAttribute("error", "El color és obligatori.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        if (vehicle.getPlaces() == null
                || vehicle.getPlaces() < 1
                || vehicle.getPlaces() > 20) {
            model.addAttribute(
                    "error",
                    "El nombre de places ha d'estar entre 1 i 20.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        if (vehicle.getTransmissio() == null) {
            model.addAttribute(
                    "error", "Has de seleccionar la transmissió.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        // Condicions de lloguer.
        if (vehicle.getLimit_km() != null
                && vehicle.getLimit_km() <= 0) {
            model.addAttribute(
                    "error",
                    "El límit de quilometratge ha de ser positiu "
                    + "o deixa'l buit si no n'hi ha.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        if (vehicle.getMin_dies() == null
                || vehicle.getMin_dies() < 1) {
            model.addAttribute(
                    "error",
                    "El mínim de dies de lloguer ha de ser "
                    + "com a mínim 1.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        if (vehicle.getMax_dies() == null
                || vehicle.getMax_dies() < vehicle.getMin_dies()) {
            model.addAttribute(
                    "error",
                    "El màxim de dies de lloguer no pot ser "
                    + "inferior al mínim.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        if (vehicle.getFianca() == null
                || vehicle.getFianca().signum() < 0
                || vehicle.getFianca().compareTo(new java.math.BigDecimal("100000")) > 0) {
            model.addAttribute(
                    "error",
                    "La fiança és obligatòria i ha d'estar entre 0 i 100.000 €.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        // Validar els fitxers.
        String errorArxius = vehicleLogic.validarArxius(
                fotos, documents);

        if (errorArxius != null) {
            model.addAttribute("error", errorArxius);
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        // Netejar els espais sobrants.
        vehicle.setMarca(vehicle.getMarca().trim());
        vehicle.setModel(vehicle.getModel().trim());
        vehicle.setColor(vehicle.getColor().trim());

        if (vehicle.getComentaris_privats() != null) {
            String comentaris = vehicle.getComentaris_privats().trim();

            vehicle.setComentaris_privats(
                    comentaris.isEmpty() ? null : comentaris);
        }

        // Desar el vehicle.
        try {
            vehicleLogic.saveVehicle(vehicle);
        } catch (IllegalArgumentException e) {
            // Error de la validació de la lògica: es mostra al formulari.
            model.addAttribute("error", e.getMessage());
            carregarDadesFormulari(model);
            return "vehicles/alta";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute(
                    "error",
                    "No s'ha pogut desar el vehicle. Revisa les dades.");
            carregarDadesFormulari(model);
            return "vehicles/alta";
        }

        // Desar les fotos i els documents.
        try {
            vehicleLogic.desarArxius(
                    vehicle.getMatricula(), fotos, documents);
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute(
                    "missatge",
                    "El vehicle s'ha desat, però no s'han pogut "
                    + "desar els fitxers.");
        }

        // Si és una modificació, tornar al llistat.
        if (editant) {
            redirectAttributes.addFlashAttribute(
                    "missatge",
                    "Les dades del vehicle s'han modificat correctament.");
            return "redirect:/vehicles";
        }

        // Si és una alta nova, mostrar la confirmació.
        redirectAttributes.addAttribute(
                "matricula", vehicle.getMatricula());

        return "redirect:/vehicles/acceptat/{matricula}";
    }

    // 4. Pàgina de confirmació d'alta.
    @GetMapping("/vehicles/acceptat/{matricula}")
    public String acceptat(
            @PathVariable String matricula,
            Model model) {

        Vehicle vehicle = vehicleLogic.getVehicleById(matricula);

        if (vehicle == null) {
            return "redirect:/vehicles";
        }

        model.addAttribute("vehicle", vehicle);

        return "vehicles/acceptat";
    }

    // Comprovar si l'usuari és personal.
        private boolean esPersonal(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGENT")
                        || a.getAuthority().equals("ROLE_ADMINISTRADOR"));
    }

    // 4b. Detall del vehicle.
    @GetMapping("/vehicles/{matricula}")
    public String detall(
            @PathVariable String matricula,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoria,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        Vehicle vehicle = vehicleLogic.getVehicleById(matricula);

        if (vehicle == null) {
            redirectAttributes.addFlashAttribute(
                    "missatge",
                    "No s'ha trobat el vehicle " + matricula + ".");
            return "redirect:/vehicles";
        }

        model.addAttribute("vehicle", vehicle);

        // Conservar els filtres per tornar al llistat.
        model.addAttribute("page", page);
        model.addAttribute("q", q);
        model.addAttribute("categoria", categoria);

        // Les dades privades només es mostren al personal.
        boolean personal = esPersonal(authentication);
        model.addAttribute("esPersonal", personal);

        if (personal) {
            List<VehicleArxiu> arxius
                    = vehicleLogic.arxiusDe(matricula);

            model.addAttribute(
                    "fotos",
                    arxius.stream()
                            .filter(a -> a.getTipus()
                                    == VehicleArxiu.Tipus.FOTO)
                            .toList());

            model.addAttribute(
                    "documents",
                    arxius.stream()
                            .filter(a -> a.getTipus()
                                    == VehicleArxiu.Tipus.DOCUMENT)
                            .toList());
        }

        return "vehicles/detall";
    }

    // 4c. Obrir un fitxer privat.
    @GetMapping("/vehicles/{matricula}/arxius/{id}")
    public ResponseEntity<byte[]> arxiu(
            @PathVariable String matricula,
            @PathVariable String id,
            Authentication authentication) {

        if (!esPersonal(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        VehicleArxiu arxiu = vehicleLogic.getArxiu(id).orElse(null);

        if (arxiu == null
                || !matricula.equals(arxiu.getMatricula())) {
            return ResponseEntity.notFound().build();
        }

        MediaType tipus = "text/plain".equals(arxiu.getContentType())
                ? new MediaType("text", "plain", StandardCharsets.UTF_8)
                : MediaType.parseMediaType(arxiu.getContentType());

        return ResponseEntity.ok()
                .contentType(tipus)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(
                                        arxiu.getNom(),
                                        StandardCharsets.UTF_8)
                                .build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(arxiu.getDades());
    }

    // 5. Canviar l'estat d'un vehicle.
    @PostMapping("/vehicles/{matricula}/estat")
    public String canviaEstat(
            @PathVariable String matricula,
            @RequestParam Enums.EstatVehicle estat,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String fMatricula,
            @RequestParam(required = false) String fMarca,
            @RequestParam(required = false) String fModel,
            @RequestParam(required = false) Integer fAny,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInici,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFi,
            RedirectAttributes redirectAttributes) {

        try {
            vehicleLogic.canviaEstat(matricula, estat);

            redirectAttributes.addFlashAttribute(
                    "missatge",
                    "El vehicle " + matricula
                    + " ara està " + estat.name() + ".");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "error", e.getMessage());
        }

        conservaFiltres(redirectAttributes, page, q, categoria,
                fMatricula, fMarca, fModel, fAny, dataInici, dataFi);

        return "redirect:/vehicles";
    }

        // 6. Mostrar el formulari amb les dades actuals del vehicle (i els seus fitxers).
    @GetMapping("/vehicles/editar/{matricula}")
    public String editarVehicle(@PathVariable String matricula, Model model) {

        Vehicle vehicle = vehicleLogic.getVehicleById(matricula);

        if (vehicle == null) {
            return "redirect:/vehicles";
        }

        List<VehicleArxiu> arxius = vehicleLogic.arxiusDe(matricula);

        model.addAttribute("vehicle", vehicle);
        model.addAttribute("editant", true);
        model.addAttribute("arxiusActuals", arxius);
        model.addAttribute("fotosActuals", arxius.stream()
                .filter(a -> a.getTipus() == VehicleArxiu.Tipus.FOTO).toList());
        model.addAttribute("documentsActuals", arxius.stream()
                .filter(a -> a.getTipus() == VehicleArxiu.Tipus.DOCUMENT).toList());
        carregarDadesFormulari(model);

        return "vehicles/alta";
    }

    // 6b. Eliminar un fitxer privat concret des de l'edició.
    @PostMapping("/vehicles/{matricula}/arxius/{id}/eliminar")
    public String eliminarArxiu(@PathVariable String matricula,
                                @PathVariable String id,
                                RedirectAttributes redirectAttributes) {
        try {
            vehicleLogic.eliminarArxiu(matricula, id);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/vehicles/editar/" + matricula;
    }

    // 7. Eliminar un vehicle.
    @PostMapping("/vehicles/{matricula}/eliminar")
    public String eliminarVehicle(
            @PathVariable String matricula,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String fMatricula,
            @RequestParam(required = false) String fMarca,
            @RequestParam(required = false) String fModel,
            @RequestParam(required = false) Integer fAny,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInici,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFi,
            RedirectAttributes redirectAttributes) {

        try {
            vehicleLogic.deleteVehicleById(matricula);

            redirectAttributes.addFlashAttribute(
                    "missatge",
                    "El vehicle " + matricula
                    + " s'ha eliminat correctament.");

        } catch (IllegalArgumentException e) {
            // Vehicle llogat: no es pot eliminar.
            redirectAttributes.addFlashAttribute("error", e.getMessage());

        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "No es pot eliminar el vehicle " + matricula
                    + " perquè té reserves associades. "
                    + "Pots canviar el seu estat a Inactiu.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Error en eliminar el vehicle: " + e.getMessage());
        }

        conservaFiltres(redirectAttributes, page, q, categoria,
                fMatricula, fMarca, fModel, fAny, dataInici, dataFi);

        return "redirect:/vehicles";
    }
}
