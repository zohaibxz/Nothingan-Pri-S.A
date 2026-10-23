/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cat.copernic.Projecte1_Equip5.repository;

import cat.copernic.Projecte1_Equip5.document.VehicleArxiu;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleArxiuRepository extends MongoRepository<VehicleArxiu, String> {

    List<VehicleArxiu> findByMatriculaOrderByDataPujadaAsc(String matricula);

    void deleteByMatricula(String matricula);
}