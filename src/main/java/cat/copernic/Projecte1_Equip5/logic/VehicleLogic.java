/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cat.copernic.Projecte1_Equip5.logic;

import cat.copernic.Projecte1_Equip5.document.VehicleArxiu;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import cat.copernic.Projecte1_Equip5.repository.VehicleArxiuRepository;
import cat.copernic.Projecte1_Equip5.repository.VehicleRepository;
import cat.copernic.Projecte1_Equip5.repository.VehicleSpecifications;
import cat.copernic.Projecte1_Equip5.repository.ReservaRepository;
import java.io.IOException;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

// Indica capa lògica de negoci
@Service

public class VehicleLogic {

    // Injecció de dependències
    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private VehicleArxiuRepository arxiuRepository;

    // Fitxers privats: màxim 5 MB per fitxer. Fotos jpg/png; documents pdf/text.
    public static final long MAX_BYTES_ARXIU = 5L * 1024 * 1024;

    private static final int MAX_FOTOS = 10;
    private static final int MAX_DOCUMENTS = 5;
    private static final long MAX_BYTES_TOTAL = 25L * 1024 * 1024;
    private static final Set<String> TIPUS_FOTO = Set.of("image/jpeg", "image/png");
    private static final Set<String> TIPUS_DOCUMENT = Set.of("application/pdf", "text/plain");

    // Obté tots els vehicles
    public List<Vehicle> findAllVehicles() {
        return vehicleRepository.findAll();
    }

    // Obté un vehicle per la seva matrícula (String)
    public Vehicle getVehicleById(String matricula) {
        Optional<Vehicle> vehicle = vehicleRepository.findById(matricula);
        return vehicle.orElse(null);
    }

    // Desa o actualitza un vehicle (alta i modificació). Valida les dades abans de desar.
    public String saveVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            throw new IllegalArgumentException("No s'han rebut les dades del vehicle.");
        }

        normalitzarVehicle(vehicle);
        validarVehicle(vehicle);

        return vehicleRepository.save(vehicle).getMatricula();
    }

    // ---------- Validació de les dades del vehicle ----------
    // Matrícula espanyola moderna: 4 números + 3 lletres (p. ex. 1234ABC).
    private static final Pattern PATRO_MATRICULA
            = Pattern.compile("^[0-9]{4}[BCDFGHJKLMNPRSTVWXYZ]{3}$");

    // Color: només lletres, espais i guions (p. ex. "Gris metal-litzat").
    private static final Pattern PATRO_COLOR = Pattern.compile("^\\p{L}[\\p{L} \\-]*$");

    //Marca i model: Primer digit pot ser lletra o número, 
    private static final Pattern PATRO_MARCA_MODEL
            = Pattern.compile("^[\\p{L}\\p{N}][\\p{L}\\p{N} .'-]*$");
    public static final int ANY_MINIM_MATRICULACIO = 2001;
    private static final int MAX_DIES_LLOGUER = 365;

    //Places
    public static final int MIN_PLACES = 1;
    public static final int MAX_PLACES = 20;

    // Neteja espais i posa la matrícula en majúscules (i sense espais ni guions).
    private void normalitzarVehicle(Vehicle v) {
        if (v.getMatricula() != null) {
            v.setMatricula(v.getMatricula().replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT));
        }
        if (v.getMarca() != null) {
            v.setMarca(v.getMarca().trim());
        }
        if (v.getModel() != null) {
            v.setModel(v.getModel().trim());
        }
        if (v.getColor() != null) {
            v.setColor(v.getColor().trim());
        }
        // Si els comentaris estan buits despres de treure espais es guarden com a null
        if (v.getComentaris_privats() != null) {
            String c = v.getComentaris_privats().trim();
            v.setComentaris_privats(c.isEmpty() ? null : c);
        }
    }

    // Validació completa (segona barrera, després de la del controlador).
    // Llança IllegalArgumentException amb un missatge entenedor per a l'usuari.
    /**
     * Per a altes noves: la matrícula no pot estar repetida.
     */
    public void comprovarMatriculaNova(String matricula) {
        if (existsById(matricula)) {
            throw new IllegalArgumentException("Ja existeix un vehicle amb aquesta matrícula.");
        }
    }

    /**
     * Validació de format, rangs i regles entre camps. Els camps obligatoris ja
     * arriben no nuls des del controlador.
     */
    private void validarVehicle(Vehicle v) {

        // Matrícula
        if (!PATRO_MATRICULA.matcher(v.getMatricula()).matches()) {
            throw new IllegalArgumentException(
                    "La matrícula ha de tenir 4 números seguits de 3 lletres no vocals ni Q i Ñ (ex. 1234BCD).");
        }

        // Marca i model
        if (v.getMarca().length() < 2 || v.getMarca().length() > 50
                || !PATRO_MARCA_MODEL.matcher(v.getMarca()).matches()) {
            throw new IllegalArgumentException(
                    "La marca ha de tenir entre 2 i 50 caràcters i no pot contenir símbols no permesos.");
        }
        if (v.getModel().length() > 50
                || !PATRO_MARCA_MODEL.matcher(v.getModel()).matches()) {
            throw new IllegalArgumentException(
                    "El model no pot superar els 50 caràcters ni contenir símbols no permesos.");
        }

        // Any de matriculació
        int anyActual = LocalDate.now().getYear();
        if (v.getAny_matriculacio() < ANY_MINIM_MATRICULACIO
                || v.getAny_matriculacio() > anyActual) {
            throw new IllegalArgumentException(
                    "L'any de matriculació ha d'estar entre " + ANY_MINIM_MATRICULACIO + " i " + anyActual + ".");
        }

        // Data d'alta
        if (v.getData_alta().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La data d'alta no pot ser futura.");
        }
        if (v.getData_alta().getYear() < v.getAny_matriculacio()) {
            throw new IllegalArgumentException(
                    "La data d'alta no pot ser anterior a l'any de matriculació.");
        }

        // Preu per hores
        if (v.getPreu_hores().isNaN() || v.getPreu_hores().isInfinite()
                || v.getPreu_hores() <= 0 || v.getPreu_hores() > 1000) {
            throw new IllegalArgumentException(
                    "El preu per hores ha de ser un nombre major que 0 i com a màxim 1000 €.");
        }

        // Característiques tècniques
        if (v.getPotencia() < 1 || v.getPotencia() > 2000) {
            throw new IllegalArgumentException("La potència ha d'estar entre 1 i 2000 CV.");
        }
        if (v.getColor().length() > 30 || !PATRO_COLOR.matcher(v.getColor()).matches()) {
            throw new IllegalArgumentException(
                    "El color només pot contenir lletres, espais i guions (màxim 30 caràcters).");
        }
        if (v.getPlaces() < MIN_PLACES || v.getPlaces() > MAX_PLACES) {
            throw new IllegalArgumentException("El nombre de places ha d'estar entre 1 i 20.");
        }

        // Condicions de lloguer
        if (v.getLimit_km() != null
                && (v.getLimit_km() <= 0 || v.getLimit_km() > 1000000)) {
            throw new IllegalArgumentException(
                    "El límit de quilometratge ha d'estar entre 1 i 1.000.000 km.");
        }
        if (v.getMin_dies() < 1) {
            throw new IllegalArgumentException("El mínim de dies de lloguer ha de ser com a mínim 1.");
        }
        if (v.getMax_dies() < v.getMin_dies()) {
            throw new IllegalArgumentException(
                    "El màxim de dies de lloguer no pot ser inferior al mínim.");
        }
        if (v.getMax_dies() > MAX_DIES_LLOGUER) {
            throw new IllegalArgumentException(
                    "El màxim de dies de lloguer no pot superar els " + MAX_DIES_LLOGUER + ".");
        }

        BigDecimal fianca = v.getFianca();
        if (fianca.signum() < 0) {
            throw new IllegalArgumentException("La fiança no pot ser negativa.");
        }
        if (fianca.compareTo(new BigDecimal("100000")) > 0) {
            throw new IllegalArgumentException("La fiança no pot superar els 100.000 €.");
        }
        if (fianca.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("La fiança com a màxim pot tenir 2 decimals.");
        }

        // Comentaris privats
        if (v.getComentaris_privats() != null && v.getComentaris_privats().length() > 2000) {
            throw new IllegalArgumentException("Els comentaris no poden superar els 2000 caràcters.");
        }
    }

    // Comprova si existeix un vehicle per matrícula
    public boolean existsById(String matricula) {
        if (matricula == null) {
            return false;
        }
        return vehicleRepository.existsById(matricula);
    }

    // Un vehicle es considera llogat si el seu estat és Llogat o si té una reserva
    // Lliurada (el client l'ha recollit i encara no l'ha retornat).
    public boolean estaLlogat(String matricula) {
        if (matricula == null) {
            return false;
        }
        Vehicle v = vehicleRepository.findById(matricula).orElse(null);
        if (v != null && v.getEstat() == Enums.EstatVehicle.Llogat) {
            return true;
        }
        return reservaRepository.existsByVehicle_MatriculaAndEstat(
                matricula, Enums.EstatReserva.Lliurada);
    }

    // Elimina un vehicle per matrícula (i, un cop eliminat, els seus fitxers privats).
    // Si la BD rebutja l'eliminació (reserves associades) llença l'excepció abans
    // d'esborrar els fitxers, de manera que no es perd res.
    public void deleteVehicleById(String matricula) {
        if (estaLlogat(matricula)) {
            throw new IllegalArgumentException("No es pot eliminar el vehicle " + matricula
                    + " perquè està llogat actualment.");
        }
        vehicleRepository.deleteById(matricula);
        arxiuRepository.deleteByMatricula(matricula);
    }

    // ---------- Fitxers privats (fotos i documents) ----------
    // Valida tots els fitxers abans de desar res. Retorna el missatge d'error o null si tot és correcte.
    // MultipartFile - És una interfície de Spring que representa un fitxer rebut des d'un formulari HTML.
    public String validarArxius(String matricula, MultipartFile[] fotos, MultipartFile[] documents) {

        try {
            int nombreFotos = 0;
            int nombreDocuments = 0;
            long midaTotal = 0;

            // Comptar els fitxers que ja existeixen a MongoDB.
            if (matricula != null) {
                nombreFotos = (int) arxiuRepository.countByMatriculaAndTipus(
                        matricula, VehicleArxiu.Tipus.FOTO);

                nombreDocuments = (int) arxiuRepository.countByMatriculaAndTipus(
                        matricula, VehicleArxiu.Tipus.DOCUMENT);
            }

            // Validar fotografies.
            if (fotos != null) {
                for (MultipartFile f : fotos) {

                    if (f == null || f.isEmpty()) {
                        continue;
                    }

                    nombreFotos++;
                    midaTotal += f.getSize();

                    if (nombreFotos > MAX_FOTOS) {
                        return "Un vehicle no pot tenir més de 10 fotografies.";
                    }

                    String error = validarArxiu(
                            f, TIPUS_FOTO,
                            "Les fotos han de ser en format JPG o PNG");

                    if (error != null) {
                        return error;
                    }
                }
            }

            // Validar documents.
            if (documents != null) {
                for (MultipartFile f : documents) {

                    if (f == null || f.isEmpty()) {
                        continue;
                    }

                    nombreDocuments++;
                    midaTotal += f.getSize();

                    if (nombreDocuments > MAX_DOCUMENTS) {
                        return "Un vehicle no pot tenir més de 5 documents.";
                    }

                    String error = validarArxiu(
                            f, TIPUS_DOCUMENT,
                            "Els documents han de ser en format PDF o text (.txt)");

                    if (error != null) {
                        return error;
                    }
                }
            }

            // Validar mida total.
            if (midaTotal > MAX_BYTES_TOTAL) {
                return "La mida total dels fitxers no pot superar els 25 MB.";
            }

        } catch (IOException e) {
            return "No s'ha pogut llegir un dels fitxers pujats.";
        }

        return null;
    }

    private String validarArxiu(MultipartFile f, Set<String> permesos, String missatgeTipus) throws IOException {
        if (f == null || f.isEmpty()) {
            return null; // casella sense fitxer
        }
        String nom = f.getOriginalFilename() == null ? "fitxer" : f.getOriginalFilename();
        if (f.getContentType() == null || !permesos.contains(f.getContentType())) {
            return missatgeTipus + " (" + nom + ").";
        }
        if (f.getSize() > MAX_BYTES_ARXIU) {
            return "El fitxer " + nom + " supera el màxim de 5 MB.";
        }
        // No ens fiem només del Content-Type que envia el navegador: comprovem el contingut real.
        if (!contingutCoincideix(f.getContentType(), f.getBytes())) {
            return "El contingut del fitxer " + nom + " no coincideix amb el seu format.";
        }
        return null;
    }

    private boolean contingutCoincideix(String tipus, byte[] b) {
        switch (tipus) {
            case "image/jpeg":
                return b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
            case "image/png":
                return b.length > 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
            case "application/pdf":
                return b.length > 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F';
            case "text/plain":
                for (byte x : b) {
                    if (x == 0) {
                        return false; // un text no pot tenir bytes nuls
                    }
                }
                return true;
            default:
                return false;
        }
    }

    // Valida la foto principal del vehicle.
    public String validarFotoPrincipal(MultipartFile foto) {
        try {
            return validarArxiu(foto, TIPUS_FOTO,
                    "La foto principal ha de ser en format JPG o PNG");
        } catch (IOException e) {
            return "No s'ha pogut llegir la foto principal.";
        }
    }

// Desa o substitueix la foto principal.
    public void desarFotoPrincipal(String matricula, MultipartFile foto) throws IOException {

        if (foto == null || foto.isEmpty()) {
            return;
        }

        // Eliminar la foto principal anterior.
        arxiuRepository.findFirstByMatriculaAndTipusAndPrincipalTrue(
                matricula, VehicleArxiu.Tipus.FOTO).ifPresent(anterior -> {
                    arxiuRepository.deleteById(anterior.getId());
                });

        // Crear la nova fotografia principal.
        VehicleArxiu arxiu = new VehicleArxiu(
                matricula,
                VehicleArxiu.Tipus.FOTO,
                foto.getOriginalFilename(),
                foto.getContentType(),
                foto.getSize(),
                foto.getBytes()
        );

        arxiu.setPrincipal(true);
        arxiuRepository.save(arxiu);
    }

// Recuperar la fotografia principal.
    public Optional<VehicleArxiu> fotoPrincipalDe(String matricula) {
        return arxiuRepository.findFirstByMatriculaAndTipusAndPrincipalTrue(
                matricula, VehicleArxiu.Tipus.FOTO);
    }

// Recuperar totes les fotografies, posant primer la principal.
    public List<VehicleArxiu> fotosDe(String matricula) {
        return arxiuRepository.findByMatriculaAndTipusOrderByDataPujadaAsc(
                matricula, VehicleArxiu.Tipus.FOTO).stream()
                .sorted(java.util.Comparator.comparing(
                        VehicleArxiu::isPrincipal).reversed())
                .toList();
    }

    // Desa els fitxers (ja validats) d'un vehicle a MongoDB.
    public void desarArxius(String matricula, MultipartFile[] fotos, MultipartFile[] documents) throws IOException {
        desarLlista(matricula, fotos, VehicleArxiu.Tipus.FOTO);
        desarLlista(matricula, documents, VehicleArxiu.Tipus.DOCUMENT);
    }

    private void desarLlista(String matricula, MultipartFile[] fitxers, VehicleArxiu.Tipus tipus) throws IOException {
        if (fitxers == null) {
            return;
        }
        for (MultipartFile f : fitxers) {
            if (f == null || f.isEmpty()) {
                continue;
            }
            arxiuRepository.save(new VehicleArxiu(matricula, tipus,
                    f.getOriginalFilename(), f.getContentType(), f.getSize(), f.getBytes()));
        }
    }

    public List<VehicleArxiu> arxiusDe(String matricula) {
        return arxiuRepository.findByMatriculaOrderByDataPujadaAsc(matricula);
    }

    public Optional<VehicleArxiu> getArxiu(String id) {
        return arxiuRepository.findById(id);
    }

    public void eliminarArxiu(String matricula, String id) {
        VehicleArxiu arxiu = arxiuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El fitxer no existeix."));
        if (!matricula.equals(arxiu.getMatricula())) {
            throw new IllegalArgumentException("El fitxer no pertany a aquest vehicle.");
        }
        arxiuRepository.deleteById(id);
    }

    public long countVehicles() {
        return vehicleRepository.count();
    }

    public long countPerEstat(Enums.EstatVehicle estat) {
        return vehicleRepository.countByEstat(estat);
    }

    public Page<Vehicle> cercaFiltrada(
            String q,
            String matricula,
            String marca,
            String model,
            Integer anyMatriculacio,
            Enums.CategoriaVehicle categoria,
            LocalDate dataInici,
            LocalDate dataFi,
            int page,
            int size) {

        // Validar la longitud dels filtres.
        validarTextCerca(q, 100, "cerca ràpida");
        validarTextCerca(matricula, 20, "matrícula");
        validarTextCerca(marca, 50, "marca");
        validarTextCerca(model, 50, "model");

        // Limitar la paginació.
        page = Math.max(0, Math.min(page, 10000));
        size = Math.max(1, Math.min(size, 50));

        // Validar l'any de matriculació.
        if (anyMatriculacio != null
                && (anyMatriculacio < ANY_MINIM_MATRICULACIO
                || anyMatriculacio > LocalDate.now().getYear())) {
            throw new IllegalArgumentException(
                    "L'any de matriculació no és vàlid.");
        }

        // Validar les dates.
        if (dataInici != null && dataFi != null
                && !dataFi.isAfter(dataInici)) {
            throw new IllegalArgumentException(
                    "La data màxima ha de ser posterior a la data mínima.");
        }

        Pageable pageable = PageRequest.of(
                page, size, Sort.by("matricula"));

        return vehicleRepository.findAll(
                VehicleSpecifications.filtrar(
                        matricula, marca, model, anyMatriculacio,
                        categoria, dataInici, dataFi)
                        .and(VehicleSpecifications.cercaRapida(q)),
                pageable);
    }

    // Actualitza l'estat d'un vehicle existent a la base de dades.
    // El transactional garanteix la integritat de les dades a la base de dades durant el canvi d'estat.
    // Donant oportunitat a desfer canvis si provoca rollback
    @Transactional
    public void canviaEstat(String matricula, Enums.EstatVehicle estat) {

        Vehicle v = vehicleRepository.findById(matricula)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Vehicle inexistent: " + matricula));

        if (estat == null) {
            throw new IllegalArgumentException("Has de seleccionar un estat.");
        }

        // Des d'aquest mètode només es pot activar o desactivar.
        // L'estat Llogat es gestiona mitjançant les reserves.
        if (estat != Enums.EstatVehicle.Disponible
                && estat != Enums.EstatVehicle.Inactiu) {
            throw new IllegalArgumentException(
                    "No es pot assignar manualment aquest estat al vehicle.");
        }

        // No es pot desactivar un vehicle que està llogat.
        if (estat == Enums.EstatVehicle.Inactiu && estaLlogat(matricula)) {
            throw new IllegalArgumentException(
                    "No es pot donar de baixa un vehicle que està llogat.");
        }

        v.setEstat(estat);
        vehicleRepository.save(v);
    }

    private void validarTextCerca(String text, int maxim, String camp) {

        if (text != null && text.length() > maxim) {
            throw new IllegalArgumentException(
                    "El camp " + camp + " no pot superar els "
                    + maxim + " caràcters.");
        }
    }
}
