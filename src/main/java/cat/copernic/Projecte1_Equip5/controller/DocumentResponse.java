package cat.copernic.Projecte1_Equip5.controller;
 
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
 
/**
 * Construye la respuesta HTTP para devolver un documento guardado en MongoDB.
 * No es un bean de Spring: es una clase de utilidad con un método estático,
 * compartida por PerfilClientController y RevisaClientController para no repetir la lógica.
 */
public final class DocumentResponse {
 // añadido para el tema de como client modificar sus documentos... 
    
    // Tipos que se muestran en el navegador; cualquier otro se fuerza a descarga
    private static final Set<String> TIPUS_VISIBLES = Set.of("image/jpeg", "image/png", "application/pdf");
 
    private DocumentResponse() {
    }
 
    public static ResponseEntity<byte[]> de(byte[] bytes, String contentType) {
        // Sin contenido: 404
        if (bytes == null || bytes.length == 0) {
            return ResponseEntity.notFound().build();
        }
        // Tipo desconocido o no permitido: descarga, nunca se interpreta en el navegador
        if (contentType == null || !TIPUS_VISIBLES.contains(contentType)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment")
                    .header("X-Content-Type-Options", "nosniff")
                    .body(bytes);
        }
        // JPG, PNG o PDF: se muestra en línea
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header("X-Content-Type-Options", "nosniff")
                .body(bytes);
    }
}
 
