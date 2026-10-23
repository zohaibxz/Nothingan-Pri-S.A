
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cat.copernic.Projecte1_Equip5.entity;

/**
 *
 * @author carle
 */
public class Enums {
    
    //Vehicles
        public enum EstatVehicle { Disponible, Llogat, Inactiu};
        
        public enum CategoriaVehicle { Turisme, SUV, Furgoneta, Esportiu, 
                                                                    Elèctric };
        public enum Combustible { Gasolina, Dièsel, Elèctric, Híbrid };

        public enum Transmissio { Manual, Automàtic };
        
    //Reserbes
        public enum EstatReserva { Activa, Lliurada, Finalitzada, Anul_lada};
        
    //Usuaris
        public enum EstatValidacio { Pendent, Validat, Rebutjat };
        
        public enum Rol { Agent, Administrador };

        
}
