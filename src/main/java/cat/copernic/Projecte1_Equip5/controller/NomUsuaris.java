/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.entity.Usuari;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Afegeix a TOTES les vistes el nom i el rol de l'usuari que ha iniciat sessió
 * (variables nomUsuari i rolUsuari), per mostrar-los a la capçalera.
 */
// @ControllerAdvice: clase "global" para todos los controladores. Sus métodos con @ModelAttribute
// se ejecutan ANTES de cada petición y meten sus resultados en el Model de la vista,
// así no hay que repetir este código en cada controlador.

//e dices a Spring: "Aplica los métodos de esta clase a absolutamente todos los @Controller de la aplicación". Se ejecuta automáticamente antes de renderizar cualquier vista.
@ControllerAdvice
// Esta clase, NomUsuaris, es un componente global de Thymeleaf y Spring MVC que sirve para evitar repetir código en todos tus controladores.
public class NomUsuaris {
    /* Su función principal es inyectar automáticamente el nombre y el rol del usuario autenticado en el modelo (Model) de TODAS las plantillas HTML,
    para que puedan mostrarse en la cabecera (header) o menú de navegación sin importar en qué página estés.*/
    
    
    // Repositorio para buscar al usuario logueado en la BD
    private final UsuariRepository usuariRepository;
 
    // Inyección por constructor: Spring pasa el repositorio automáticamente
    public NomUsuaris(UsuariRepository usuariRepository) {
        this.usuariRepository = usuariRepository;
    }
 
    // @ModelAttribute("nomUsuari"): el valor que devuelve este método se añade al Model
    // con el nombre "nomUsuari". La plantilla lo lee con ${nomUsuari}.
    // Authentication lo inyecta Spring Security (datos del usuario logueado).
    @ModelAttribute("nomUsuari")
    public String nomUsuari(Authentication authentication) {
        // Sin sesión, o con usuario anónimo (visitante no logueado): no hay nombre que mostrar.
        // Spring Security usa AnonymousAuthenticationToken para los visitantes sin login.
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        // Se busca al usuario por email (authentication.getName()). findByEmail devuelve un Optional:
        // .map(this::nomComplet) -> si existe, lo transforma en su nombre completo
        // .orElse("Usuari")      -> si no existe en la BD, texto por defecto
        return usuariRepository.findByEmail(authentication.getName())
                .map(this::nomComplet)
                .orElse("Usuari");
    }
 
    // Igual que el anterior, pero el valor se guarda en el Model como "rolUsuari" (${rolUsuari} en la plantilla)
    @ModelAttribute("rolUsuari")
    public String rolUsuari(Authentication authentication) {
        // Sin sesión o anónimo: no hay rol
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        // ¿Alguna de las authorities del usuario es ROLE_ADMINISTRADOR? (stream + anyMatch)
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
        // ¿Alguna es ROLE_AGENT?
        boolean agent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGENT"));
        // Operador ternario anidado: si es admin -> "Administrador"; si no, si es agent -> "Agent"; si no -> "Client"
        return admin ? "Administrador" : (agent ? "Agent" : "Client");
    }
 
    // Método auxiliar: junta nombre y apellidos en un solo texto (ej. "Marta Soler")
    private String nomComplet(Usuari u) {
        // Los null se convierten en texto vacío y se quitan los espacios sobrantes
        String nom = u.getNom() == null ? "" : u.getNom().trim();
        String cognoms = u.getCognoms() == null ? "" : u.getCognoms().trim();
        // Se concatenan con un espacio y se limpia (si falta uno de los dos, no quedan espacios de más)
        String complet = (nom + " " + cognoms).trim();
        // Si no hay ni nombre ni apellidos, "Usuari"
        return complet.isEmpty() ? "Usuari" : complet;
    }
}
 