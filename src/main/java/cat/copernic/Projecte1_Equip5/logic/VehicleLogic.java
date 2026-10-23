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
import java.io.IOException;
import java.time.LocalDate;
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

@Service
public class VehicleLogic {

    @Autowired
    private VehicleRepository vehicleRepository;
    
      @Autowired
    private VehicleArxiuRepository arxiuRepository;

    // Fitxers privats: màxim 5 MB per fitxer. Fotos jpg/png; documents pdf/text.
    public static final long MAX_BYTES_ARXIU = 5L * 1024 * 1024;
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

    // Dona d'alta un vehicle nou. No actualitza un vehicle existent amb la mateixa matrícula.
    public String altaVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            throw new IllegalArgumentException("No s'han rebut les dades del vehicle.");
        }

        if (vehicle.getMatricula() == null || vehicle.getMatricula().isBlank()) {
            throw new IllegalArgumentException("La matrícula és obligatòria.");
        }
        if (vehicle.getMarca() == null || vehicle.getMarca().isBlank()) {
            throw new IllegalArgumentException("La marca és obligatòria.");
        }
        if (vehicle.getModel() == null || vehicle.getModel().isBlank()) {
            throw new IllegalArgumentException("El model és obligatori.");
        }
        if (vehicle.getCategoria() == null) {
            throw new IllegalArgumentException("Has de seleccionar una categoria.");
        }
        if (vehicle.getAny_matriculacio() == null
                || vehicle.getAny_matriculacio() < 1900
                || vehicle.getAny_matriculacio() > LocalDate.now().getYear() + 1) {
            throw new IllegalArgumentException("L'any de matriculació no és vàlid.");
        }
        if (vehicle.getPreu_hores() == null || vehicle.getPreu_hores() < 0) {
            throw new IllegalArgumentException("El preu per hores no pot ser negatiu ni estar buit.");
        }

        // Normalitzem la matrícula per evitar diferències entre minúscules i majúscules.
        String matricula = vehicle.getMatricula().trim().toUpperCase(Locale.ROOT);
        if (vehicleRepository.existsById(matricula)) {
            throw new IllegalArgumentException("Ja existeix un vehicle amb la matrícula " + matricula + ".");
        }

        vehicle.setMatricula(matricula);
        vehicle.setMarca(vehicle.getMarca().trim());
        vehicle.setModel(vehicle.getModel().trim());

        if (vehicle.getEstat() == null) {
            vehicle.setEstat(Enums.EstatVehicle.Disponible);
        }
        if (vehicle.getData_alta() == null) {
            vehicle.setData_alta(LocalDate.now());
        }

        Vehicle vehicleGuardat = vehicleRepository.save(vehicle);
        return vehicleGuardat.getMatricula();
    }

    // Desa o actualitza un vehicle; es manté per a l'API REST existent.
    public String saveVehicle(Vehicle vehicle) {
        Vehicle vehicleGuardat = vehicleRepository.save(vehicle);
        return vehicleGuardat.getMatricula();
    }

    // Comprova si existeix un vehicle per matrícula
    public boolean existsById(String matricula) {
        if (matricula == null) {
            return false;
        }
        return vehicleRepository.existsById(matricula);
    }

    // Elimina un vehicle per matrícula (i, un cop eliminat, els seus fitxers privats).
    // Si la BD rebutja l'eliminació (reserves associades) llença l'excepció abans
    // d'esborrar els fitxers, de manera que no es perd res.
    public void deleteVehicleById(String matricula) {
        vehicleRepository.deleteById(matricula);
        arxiuRepository.deleteByMatricula(matricula);
    }

    // ---------- Fitxers privats (fotos i documents) ----------

    // Valida tots els fitxers abans de desar res. Retorna el missatge d'error o null si tot és correcte.
    public String validarArxius(MultipartFile[] fotos, MultipartFile[] documents) {
        try {
            if (fotos != null) {
                for (MultipartFile f : fotos) {
                    String error = validarArxiu(f, TIPUS_FOTO, "Les fotos han de ser en format JPG o PNG");
                    if (error != null) {
                        return error;
                    }
                }
            }
            if (documents != null) {
                for (MultipartFile f : documents) {
                    String error = validarArxiu(f, TIPUS_DOCUMENT, "Els documents han de ser en format PDF o text (.txt)");
                    if (error != null) {
                        return error;
                    }
                }
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
    
    public Page<Vehicle> cerca(String q, Enums.CategoriaVehicle categoria, int page, int size) {
        String t = (q == null) ? "" : q.trim();
        Pageable pageable = PageRequest.of(page, size, Sort.by("matricula"));
 
        if (categoria == null) {
            return vehicleRepository
                .findByMatriculaContainingIgnoreCaseOrMarcaContainingIgnoreCaseOrModelContainingIgnoreCase(
                    t, t, t, pageable);
        }
        return vehicleRepository
            .findByCategoriaAndMatriculaContainingIgnoreCaseOrCategoriaAndMarcaContainingIgnoreCaseOrCategoriaAndModelContainingIgnoreCase(
                categoria, t, categoria, t, categoria, t, pageable);
    }
 
    public long countVehicles() {
        return vehicleRepository.count();
    }
 
    public long countPerEstat(Enums.EstatVehicle estat) {
        return vehicleRepository.countByEstat(estat);
    }
    
    public Page<Vehicle> cercaFiltrada(
        String matricula,
        String marca,
        String model,
        Integer anyMatriculacio,
        Enums.CategoriaVehicle categoria,
        LocalDate dataInici,
        LocalDate dataFi,
        int page,
        int size) {

    Pageable pageable = PageRequest.of(
            Math.max(page, 0),
            size,
            Sort.by("matricula")
    );

    return vehicleRepository.findAll(
            VehicleSpecifications.filtrar(
                    matricula,
                    marca,
                    model,
                    anyMatriculacio,
                    categoria,
                    dataInici,
                    dataFi
            ),
            pageable
    );
}
 
    // Actualitza l'estat d'un vehicle existent a la base de dades.
    // El transactional garanteix la integritat de les dades a la base de dades durant el canvi d'estat.
    @Transactional
public void canviaEstat(String matricula, Enums.EstatVehicle estat) {
    Vehicle v = vehicleRepository.findById(matricula)
            .orElseThrow(() ->
                    new IllegalArgumentException(
                            "Vehicle inexistent: " + matricula));

    v.setEstat(estat);
    vehicleRepository.save(v);
}
}