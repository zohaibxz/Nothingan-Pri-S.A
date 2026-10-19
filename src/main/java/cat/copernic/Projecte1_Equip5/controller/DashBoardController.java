package cat.copernic.Projecte1_Equip5.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashBoardController {

    @GetMapping("/dashboard")
    public String direccionarSegunRol(@RequestParam(name = "rol", defaultValue = "client") String rol) {
        if ("administrador".equalsIgnoreCase(rol)) {
            return "administrador/dashboard";
        } else if ("agent".equalsIgnoreCase(rol)) {
            return "agent/dashboard";
        } else {
            return "client/dashboard";
        }
    }
}

