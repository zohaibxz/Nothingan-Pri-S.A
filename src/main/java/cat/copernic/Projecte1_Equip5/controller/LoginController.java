
package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.entity.Agent;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Usuari;
import cat.copernic.Projecte1_Equip5.repository.UsuariRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class LoginController {

    @Autowired
    private UsuariRepository usuariRepository;

    @GetMapping({"/", "/login"})
    public String mostrarLogin() {
        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String email, 
                                @RequestParam String password, 
                                HttpSession session, 
                                Model model) {
        
        Optional<Usuari> usuariOpt = usuariRepository.findByEmail(email);

        if (usuariOpt.isPresent() && usuariOpt.get().getPasswordHash().equals(password)) {
            Usuari usuari = usuariOpt.get();
            
            String rol = "client";
            if (usuari instanceof Agent agent) {
                if (agent.getRol() == Enums.Rol.Administrador) {
                    rol = "administrador";
                } else {
                    rol = "agent";
                }
            }

            session.setAttribute("usuarioLogueado", usuari);
            session.setAttribute("rol", rol);

            return "redirect:/dashboard?rol=" + rol;
        }

        model.addAttribute("error", "Email o contraseña incorrectos");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
