package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.document.VehicleArxiu;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import cat.copernic.Projecte1_Equip5.logic.VehicleLogic;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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

/**
 * Controlador encarregat de gestionar totes les operacions relacionades amb els
 * vehicles de l'aplicació.
 *
 * Gestiona les peticions HTTP per donar d'alta, modificar, eliminar, consultar
 * i canviar l'estat dels vehicles. També permet cercar i filtrar vehicles,
 * consultar-ne els detalls i gestionar les seves fotografies i documents.
 *
 * S'encarrega de rebre les dades dels formularis, comprovar les validacions
 * bàsiques i comunicar-se amb la capa de lògica (VehicleLogic) per executar les
 * operacions corresponents.
 *
 * @author Equip 5
 * @version 1.0
 */
// Controlador web de Spring, rep peticions HTTP
@Controller
public class VehicleController {

    /**
     * Crea el controlador dels vehicles.
     */
    public VehicleController() {
    }

    // Injecta automaticament la classe, aixi es poden cridar als metodes sense crear l'objecte manualment
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

    /**
     * Pàgina principal de gestió de vehicles, amb cerca, filtres,
     * disponibilitat per dates, paginació i indicadors per estat.
     *
     * @param q cerca ràpida per text
     * @param categoria filtre per categoria
     * @param fMatricula filtre per matrícula
     * @param fMarca filtre per marca
     * @param fModel filtre per model
     * @param fAny filtre per any de matriculació
     * @param dataInici inici del període de disponibilitat (opcional)
     * @param dataFi fi del període de disponibilitat (opcional)
     * @param page número de pàgina (comença a 0)
     * @param model model de la vista
     * @return plantilla del llistat de vehicles
     */
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

        Page<Vehicle> pagina;

        try {
            pagina = vehicleLogic.cercaFiltrada(
                    q, fMatricula, fMarca, fModel, fAny, categoriaEnum,
                    dataInici, dataFi, page, 10);

        } catch (IllegalArgumentException e) {

            model.addAttribute("error", e.getMessage());

            pagina = vehicleLogic.cercaFiltrada(
                    null, null, null, null, null, null,
                    null, null, 0, 10);
        }

        long total = vehicleLogic.countVehicles();
        long llogats = vehicleLogic.countLlogats();
        long inactius = vehicleLogic.countPerEstat(Enums.EstatVehicle.Inactiu);
        long disponibles = total - llogats - inactius;

        model.addAttribute("pagina", pagina);
        model.addAttribute("matriculesLlogades", vehicleLogic.matriculesLlogades());
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

    /**
     * Afegeix a la redirecció els filtres actius perquè, després d'una acció,
     * el llistat es mostri tal com l'usuari el tenia.
     *
     * @param ra atributs de la redirecció
     * @param page pàgina actual
     * @param q cerca ràpida per text
     * @param categoria filtre per categoria
     * @param fMatricula filtre per matrícula
     * @param fMarca filtre per marca
     * @param fModel filtre per model
     * @param fAny filtre per any de matriculació
     * @param dataInici inici del període de disponibilitat (opcional)
     * @param dataFi fi del període de disponibilitat (opcional)
     */
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

    /**
     * Calcula un percentatge arrodonit sense dividir per zero.
     *
     * @param part part del total
     * @param total total
     * @return percentatge entre 0 i 100 (0 si el total és 0)
     */
    private long pct(long part, long total) {
        return total == 0 ? 0 : Math.round(part * 100.0 / total);
    }

    /**
     * Mostra el formulari d'alta d'un vehicle nou, amb la data d'alta d'avui.
     *
     * @param model model de la vista
     * @return plantilla del formulari
     */
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

    /**
     * Processa l'alta o la modificació d'un vehicle: valida les dades, desa el
     * vehicle i després la foto principal, les fotos i els documents privats.
     * Si falla alguna validació torna al formulari amb el missatge i no es desa
     * res.
     *
     * @param vehicle dades del formulari
     * @param model model de la vista
     * @param fotoPrincipal foto principal (opcional)
     * @param fotos fotos addicionals (opcional)
     * @param documents documents privats (opcional)
     * @param editant true si és una modificació i false si és una alta
     * @param redirectAttributes atributs per a la redirecció
     * @return redirecció a la confirmació, o el formulari amb l'error
     */
    @PostMapping("/vehicles/alta")
    public String enviar(
            @ModelAttribute("vehicle") Vehicle vehicle,
            Model model,
            @RequestParam(name = "fotoPrincipal", required = false) MultipartFile fotoPrincipal,
            @RequestParam(name = "fotos", required = false) MultipartFile[] fotos,
            @RequestParam(name = "documents", required = false) MultipartFile[] documents,
            @RequestParam(name = "editant", defaultValue = "false") boolean editant,
            RedirectAttributes redirectAttributes) {

        model.addAttribute("editant", editant);
        model.addAttribute("fotoPrincipalActual", null);
        model.addAttribute("fotosActuals", java.util.List.of());
        model.addAttribute("documentsActuals", java.util.List.of());
        model.addAttribute("arxiusActuals", java.util.List.of());

        if (editant && vehicle.getMatricula() != null) {
            List<VehicleArxiu> arxiusFormulari
                    = vehicleLogic.arxiusDe(vehicle.getMatricula());

            model.addAttribute("arxiusActuals", arxiusFormulari);
            model.addAttribute("fotosActuals", arxiusFormulari.stream()
                    .filter(a -> a.getTipus() == VehicleArxiu.Tipus.FOTO)
                    .toList());
            model.addAttribute("documentsActuals", arxiusFormulari.stream()
                    .filter(a -> a.getTipus() == VehicleArxiu.Tipus.DOCUMENT)
                    .toList());
            model.addAttribute("fotoPrincipalActual",
                    vehicleLogic.fotoPrincipalDe(vehicle.getMatricula()).orElse(null));
        }

        // 1. Validació mínima: camps obligatoris no nuls ni en blanc.
        String errorObligatoris = validarObligatoris(vehicle, editant);
        if (errorObligatoris != null) {
            return formulariAmbError(model, errorObligatoris);
        }

        // 2. Normalitzar dades (abans que la lògica les validi).
        String matricula = vehicle.getMatricula().trim().toUpperCase(Locale.ROOT);
        vehicle.setMatricula(matricula);
        vehicle.setMarca(vehicle.getMarca().trim());
        vehicle.setModel(vehicle.getModel().trim());
        vehicle.setColor(vehicle.getColor().trim());

        if (vehicle.getComentaris_privats() != null) {
            String comentaris = vehicle.getComentaris_privats().trim();
            vehicle.setComentaris_privats(comentaris.isEmpty() ? null : comentaris);
        }

        // 3. Alta o modificació: conservar dades que no es poden editar.
        if (editant) {
            Vehicle vehicleOriginal = vehicleLogic.getVehicleById(matricula);

            if (vehicleOriginal == null) {
                return formulariAmbError(model,
                        "No s'ha trobat el vehicle que vols modificar.");
            }

            vehicle.setMatricula(vehicleOriginal.getMatricula());
            vehicle.setData_alta(vehicleOriginal.getData_alta());
            vehicle.setEstat(vehicleOriginal.getEstat());
        } else {
            vehicle.setEstat(Enums.EstatVehicle.Disponible);

            if (fotoPrincipal == null || fotoPrincipal.isEmpty()) {
                return formulariAmbError(model,
                        "Has d'afegir una foto principal del vehicle.");
            }
        }

        // 4. Validacions i desat (tot el que no sigui "buit" es valida a la lògica).
        try {
            if (!editant) {
                vehicleLogic.comprovarMatriculaNova(matricula);
            }

            String errorFitxers = vehicleLogic.validarFotoPrincipal(fotoPrincipal);
            if (errorFitxers == null) {
                errorFitxers = vehicleLogic.validarArxius(matricula, fotos, documents);
            }
            if (errorFitxers != null) {
                return formulariAmbError(model, errorFitxers);
            }

            vehicleLogic.saveVehicle(vehicle);

        } catch (IllegalArgumentException e) {
            return formulariAmbError(model, e.getMessage());
        } catch (DataIntegrityViolationException e) {
            return formulariAmbError(model,
                    "No s'ha pogut desar el vehicle. Revisa les dades.");
        }

        // 5. Desar fitxers.
        try {
            vehicleLogic.desarFotoPrincipal(matricula, fotoPrincipal);
            vehicleLogic.desarArxius(matricula, fotos, documents);
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("missatge",
                    "El vehicle s'ha desat, però no s'han pogut desar els fitxers.");
        }

        if (editant) {
            redirectAttributes.addFlashAttribute("missatge",
                    "Les dades del vehicle s'han modificat correctament.");
            return "redirect:/vehicles";
        }

        redirectAttributes.addAttribute("matricula", matricula);
        return "redirect:/vehicles/acceptat/{matricula}";
    }

    /**
     * Validació mínima: només camps obligatoris nuls o en blanc.
     */
    private String validarObligatoris(Vehicle v, boolean editant) {
        if (v.getMatricula() == null || v.getMatricula().isBlank()) {
            return "La matrícula és obligatòria.";
        }
        if (v.getMarca() == null || v.getMarca().isBlank()) {
            return "La marca és obligatòria.";
        }
        if (v.getModel() == null || v.getModel().isBlank()) {
            return "El model és obligatori.";
        }
        if (v.getCategoria() == null) {
            return "Has de seleccionar una categoria.";
        }
        if (v.getAny_matriculacio() == null) {
            return "Has d'introduir l'any de matriculació.";
        }
        // En una edició la data d'alta es recupera de la BD.
        if (!editant && v.getData_alta() == null) {
            return "Has d'introduir la data d'alta.";
        }
        if (v.getPreu_hores() == null) {
            return "Has d'introduir el preu per hores.";
        }
        if (v.getCombustible() == null) {
            return "Has de seleccionar el tipus de combustible/motor.";
        }
        if (v.getPotencia() == null) {
            return "Has d'introduir la potència.";
        }
        if (v.getColor() == null || v.getColor().isBlank()) {
            return "El color és obligatori.";
        }
        if (v.getPlaces() == null) {
            return "Has d'introduir el nombre de places.";
        }
        if (v.getTransmissio() == null) {
            return "Has de seleccionar la transmissió.";
        }
        if (v.getMin_dies() == null) {
            return "Has d'introduir el mínim de dies de lloguer.";
        }
        if (v.getMax_dies() == null) {
            return "Has d'introduir el màxim de dies de lloguer.";
        }
        if (v.getFianca() == null) {
            return "Has d'introduir la fiança.";
        }
        return null;
    }

    /**
     * Torna a mostrar el formulari d'alta amb un missatge d'error.
     *
     * @param model model de la vista
     * @param missatge error a mostrar
     * @return plantilla del formulari
     */
    private String formulariAmbError(Model model, String missatge) {
        model.addAttribute("error", missatge);
        carregarDadesFormulari(model);
        return "vehicles/alta";
    }

    /**
     * Pàgina de confirmació després d'una alta o modificació.
     *
     * @param matricula matrícula del vehicle desat
     * @param model model de la vista
     * @return plantilla de confirmació, o redirecció al llistat si no existeix
     */
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

    /**
     * Indica si l'usuari és personal de l'empresa (agent o administrador).
     *
     * @param authentication usuari autenticat
     * @return true si és agent o administrador
     */
    private boolean esPersonal(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGENT")
                || a.getAuthority().equals("ROLE_ADMINISTRADOR"));
    }

    /**
     * Detall d'un vehicle per al personal, amb les seves dades privades i
     * fitxers.
     *
     * @param matricula matrícula del vehicle
     * @param page pàgina del llistat d'on es ve (per tornar-hi)
     * @param q cerca activa al llistat
     * @param categoria categoria activa al llistat
     * @param authentication usuari autenticat
     * @param model model de la vista
     * @param redirectAttributes atributs per mostrar errors
     * @return plantilla del detall, o redirecció al llistat si no existeix
     */
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
        model.addAttribute("estatVista", vehicleLogic.estatVista(vehicle));

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

    /**
     * Obre un fitxer privat d'un vehicle. Només el personal hi pot accedir.
     *
     * @param matricula matrícula del vehicle
     * @param id identificador del fitxer
     * @param authentication usuari autenticat
     * @return el fitxer, 403 si no és personal o 404 si no existeix
     */
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

        // MediaType - Indica al navegador quin tipus de contingut esta rebent
        MediaType tipus = "text/plain".equals(arxiu.getContentType())
                ? new MediaType("text", "plain", StandardCharsets.UTF_8)
                : MediaType.parseMediaType(arxiu.getContentType());

        return ResponseEntity.ok()
                .contentType(tipus)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        // El navegador ha de mostrar el fitxer direcmant, enlloc de descarregarlo 
                        ContentDisposition.inline()
                                .filename(
                                        arxiu.getNom(),
                                        StandardCharsets.UTF_8)
                                .build().toString())
                // Capçelera de seguretat que no interpreti el document com unalter tipus
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(arxiu.getDades());
    }

    /**
     * Canvia l'estat d'un vehicle (Disponible o Inactiu). L'estat Llogat es
     * gestiona sol amb les reserves i no es pot canviar a mà.
     *
     * @param matricula matrícula del vehicle
     * @param estat nou estat
     * @param page pàgina del llistat
     * @param q cerca ràpida per text
     * @param categoria filtre per categoria
     * @param fMatricula filtre per matrícula
     * @param fMarca filtre per marca
     * @param fModel filtre per model
     * @param fAny filtre per any de matriculació
     * @param dataInici inici del període de disponibilitat (opcional)
     * @param dataFi fi del període de disponibilitat (opcional)
     * @param redirectAttributes atributs per mostrar el resultat
     * @return redirecció al llistat amb els filtres conservats
     */
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

    /**
     * Mostra el formulari de modificació amb les dades i els fitxers actuals.
     *
     * @param matricula matrícula del vehicle
     * @param model model de la vista
     * @return plantilla del formulari, o redirecció al llistat si no existeix
     */
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
        model.addAttribute("fotoPrincipalActual",
                vehicleLogic.fotoPrincipalDe(matricula).orElse(null));

        return "vehicles/alta";
    }

    /**
     * Elimina un fitxer privat concret des de la pantalla d'edició.
     *
     * @param matricula matrícula del vehicle
     * @param id identificador del fitxer
     * @param redirectAttributes atributs per mostrar errors
     * @return redirecció a la pantalla d'edició
     */
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

    /**
     * Elimina un vehicle. No es permet si està llogat o si té reserves
     * associades.
     *
     * @param matricula matrícula del vehicle
     * @param page pàgina del llistat
     * @param q cerca ràpida per text
     * @param categoria filtre per categoria
     * @param fMatricula filtre per matrícula
     * @param fMarca filtre per marca
     * @param fModel filtre per model
     * @param fAny filtre per any de matriculació
     * @param dataInici inici del període de disponibilitat (opcional)
     * @param dataFi fi del període de disponibilitat (opcional)
     * @param redirectAttributes atributs per mostrar el resultat
     * @return redirecció al llistat amb els filtres conservats
     */
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
                    "No s'ha pogut eliminar el vehicle " + matricula + ". Torna-ho a provar més tard.");
        }

        conservaFiltres(redirectAttributes, page, q, categoria,
                fMatricula, fMarca, fModel, fAny, dataInici, dataFi);

        return "redirect:/vehicles";
    }
}
