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
@ControllerAdvice
public class NomUsuaris {

    private final UsuariRepository usuariRepository;

    public NomUsuaris(UsuariRepository usuariRepository) {
        this.usuariRepository = usuariRepository;
    }

    @ModelAttribute("nomUsuari")
    public String nomUsuari(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return usuariRepository.findByEmail(authentication.getName())
                .map(this::nomComplet)
                .orElse("Usuari");
    }

    @ModelAttribute("rolUsuari")
    public String rolUsuari(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
        boolean agent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGENT"));
        return admin ? "Administrador" : (agent ? "Agent" : "Client");
    }

    private String nomComplet(Usuari u) {
        String nom = u.getNom() == null ? "" : u.getNom().trim();
        String cognoms = u.getCognoms() == null ? "" : u.getCognoms().trim();
        String complet = (nom + " " + cognoms).trim();
        return complet.isEmpty() ? "Usuari" : complet;
    }
}