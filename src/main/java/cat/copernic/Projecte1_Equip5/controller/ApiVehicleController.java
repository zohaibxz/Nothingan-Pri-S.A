/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package cat.copernic.Projecte1_Equip5.controller;

import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import cat.copernic.Projecte1_Equip5.logic.VehicleLogic;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/vehicles")
public class ApiVehicleController {

    Logger logger = LoggerFactory.getLogger(ApiVehicleController.class);

    @Autowired
    private VehicleLogic vehicleLogic;

    @GetMapping("/read/all")
    public ResponseEntity<List<Vehicle>> findAll() {

        List<Vehicle> llista;
        ResponseEntity<List<Vehicle>> response;

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-store");

        try {
            logger.debug("Reading all vehicles...");
            llista = vehicleLogic.findAllVehicles();
            response = new ResponseEntity<>(llista, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error reading all vehicles: " + e);
            response = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    @GetMapping("/read/byId/{vehicleId}")
    public ResponseEntity<Vehicle> byId(@PathVariable String vehicleId) {

        Vehicle v;
        ResponseEntity<Vehicle> response;

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-store");

        try {
            logger.debug("Reading vehicle matricula: " + vehicleId);
            v = vehicleLogic.getVehicleById(vehicleId);

            if (v == null) {
                response = new ResponseEntity<>(headers, HttpStatus.NOT_FOUND);
            } else {
                response = new ResponseEntity<>(v, headers, HttpStatus.OK);
            }

        } catch (Exception e) {
            logger.error("Error reading vehicle matricula:" + vehicleId + " : " + e);
            response = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    @PostMapping("/create")
    public ResponseEntity<String> save(@RequestBody Vehicle vehicle) {

        ResponseEntity<String> response;
        String vehicleId;

        try {
            if (vehicle == null) {
                logger.error("No data received!!");
                response = ResponseEntity.notFound().build();
            } else {
                vehicleId = vehicleLogic.saveVehicle(vehicle);
                response = new ResponseEntity<>(vehicleId, HttpStatus.CREATED);
                logger.debug("Created vehicle: " + vehicleId);
            }
        } catch (Exception e) {
            logger.error("Error creating vehicle: " + e);
            response = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    @PutMapping("/update")
    public ResponseEntity<Void> update(@RequestBody Vehicle vehicle) {

        ResponseEntity<Void> response;

        try {
            if (vehicle != null) {
                if (vehicleLogic.existsById(vehicle.getMatricula())) {
                    logger.debug("Updating vehicle: " + vehicle);
                    vehicleLogic.saveVehicle(vehicle);
                    response = ResponseEntity.ok().build();
                } else {
                    logger.error("Not found: " + vehicle);
                    response = ResponseEntity.notFound().build();
                }
            } else {
                logger.error("No data received!!");
                response = ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            logger.error("Error updating vehicle: " + e);
            response = ResponseEntity.internalServerError().build();
        }

        return response;
    }

    @DeleteMapping("/delete/{vehicleId}")
    public ResponseEntity<Void> deleteById(@PathVariable String vehicleId) {

        ResponseEntity<Void> response;

        try {
            if (vehicleId != null) {
                if (vehicleLogic.existsById(vehicleId)) {
                    vehicleLogic.deleteVehicleById(vehicleId);
                    response = ResponseEntity.noContent().build();
                    logger.debug("Deleted vehicle: " + vehicleId);
                } else {
                    logger.error("Not found: " + vehicleId);
                    response = ResponseEntity.notFound().build();
                }
            } else {
                logger.error("No data received!!");
                response = ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            logger.error("Error deleting vehicle matricula:" + vehicleId + " : " + e);
            response = ResponseEntity.internalServerError().build();
        }

        return response;
    }
}