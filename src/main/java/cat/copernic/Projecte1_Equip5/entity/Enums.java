
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cat.copernic.Projecte1_Equip5.entity;

/**
 * Contenidor dels enumerats (llistes de valors fixos) que comparteixen les
 * entitats del projecte: estat i característiques dels vehicles, estat de les
 * reserves i estat i rol dels usuaris. Les entitats els guarden a la base de
 * dades com a text ({@code EnumType.STRING}), no com a número.
 *
 */
public class Enums {

    // Vehicles
    /**
     * Estat d'un vehicle dins del catàleg.
     */
    public enum EstatVehicle {
        /**
         * Es pot reservar.
         */
        Disponible,
        /**
         * Està llogat ara mateix.
         */
        Llogat,
        /**
         * Donat de baixa: no apareix com a disponible ni es pot reservar.
         */
        Inactiu
    }

    /**
     * Categoria d'un vehicle, per a la cerca i el filtratge del catàleg.
     */
    public enum CategoriaVehicle {
        /**
         * Cotxe de turisme.
         */
        Turisme,
        /**
         * Tot terreny o crossover.
         */
        SUV,
        /**
         * Furgoneta.
         */
        Furgoneta,
        /**
         * Cotxe esportiu.
         */
        Esportiu,
        /**
         * Vehicle elèctric.
         */
        Elèctric
    }

    /**
     * Tipus de combustible d'un vehicle.
     */
    public enum Combustible {
        /**
         * Motor de gasolina.
         */
        Gasolina,
        /**
         * Motor dièsel.
         */
        Dièsel,
        /**
         * Motor elèctric.
         */
        Elèctric,
        /**
         * Motor híbrid.
         */
        Híbrid
    }

    /**
     * Tipus de transmissió (caixa de canvis) d'un vehicle.
     */
    public enum Transmissio {
        /**
         * Caixa de canvis manual.
         */
        Manual,
        /**
         * Caixa de canvis automàtica.
         */
        Automàtic
    }

    // Reserves
    /**
     * Estat d'una reserva al llarg del seu cicle de vida: Activa, després
     * Lliurada quan el client recull el vehicle i Finalitzada quan el torna, o
     * Anul·lada si es cancel·la abans.
     */
    public enum EstatReserva {
        /**
         * Reserva confirmada; el vehicle encara no s'ha lliurat.
         */
        Activa,
        /**
         * El client ha recollit el vehicle i encara no l'ha tornat.
         */
        Lliurada,
        /**
         * El vehicle s'ha tornat i la reserva ha acabat.
         */
        Finalitzada,
        /**
         * Reserva anul·lada abans del lliurament.
         */
        Anul_lada
    }

    // Usuaris
    /**
     * Estat de validació d'un client: un client nou queda Pendent fins que un
     * agent o administrador el valida o el rebutja.
     */
    public enum EstatValidacio {
        /**
         * Encara no revisat per cap agent.
         */
        Pendent,
        /**
         * Revisat i acceptat.
         */
        Validat,
        /**
         * Revisat i rebutjat.
         */
        Rebutjat
    }

    /**
     * Rol d'un empleat de l'empresa. Es tradueix al rol de seguretat amb
     * {@link AuthorityType#of(Usuari)}.
     */
    public enum Rol {
        /**
         * Agent: gestiona clients, reserves i vehicles.
         */
        Agent,
        /**
         * Administrador: té més permisos que l'agent.
         */
        Administrador
    }

}
