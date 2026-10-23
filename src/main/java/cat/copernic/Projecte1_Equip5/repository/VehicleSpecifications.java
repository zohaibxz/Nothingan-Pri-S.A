/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cat.copernic.Projecte1_Equip5.repository;

import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Reserva;
import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Aquesta classe serveix per construir filtres de cerca dinàmics i combinables a la base de dades mitjançant JPA Criteria API.
 * 
 * Components de la consulta (root, query, cb):
 * - root (Root<Vehicle>): Representa l'entitat principal de la consulta (la taula 'Vehicle') i permet accedir als seus camps (ex: root.get("matricula")).
 * - query (CriteriaQuery<?>): Representa l'estructura global de la consulta SQL i permet afegir subconsultes, ordenacions o clàusules extres.
 * - cb (CriteriaBuilder): És el constructor d'operadors lògics i condicions (ex: cb.like(), cb.equal(), cb.and(), cb.not()).
 */

public class VehicleSpecifications {

    // Construeix dinàmicament una consulta amb múltiples filtres opcionals per cercar vehicles segons dades i disponibilitat de dates.
    public static Specification<Vehicle> filtrar(
            String matricula,
            String marca,
            String model,
            Integer anyMatriculacio,
            Enums.CategoriaVehicle categoria,
            LocalDate dataInici,
            LocalDate dataFi) {

        return (root, query, cb) -> {

            List<Predicate> filtres = new ArrayList<>();

            // Filtrar per matrícula
            if (matricula != null && !matricula.isBlank()) {
                filtres.add(cb.like(
                        cb.lower(root.<String>get("matricula")),
                        "%" + matricula.trim().toLowerCase(Locale.ROOT) + "%"
                ));
            }

            // Filtrar per marca
            if (marca != null && !marca.isBlank()) {
                filtres.add(cb.like(
                        cb.lower(root.<String>get("marca")),
                        "%" + marca.trim().toLowerCase(Locale.ROOT) + "%"
                ));
            }

            // Filtrar per model
            if (model != null && !model.isBlank()) {
                filtres.add(cb.like(
                        cb.lower(root.<String>get("model")),
                        "%" + model.trim().toLowerCase(Locale.ROOT) + "%"
                ));
            }

            // Filtrar per any de matriculació
            if (anyMatriculacio != null) {
                filtres.add(cb.equal(
                        root.get("any_matriculacio"),
                        anyMatriculacio
                ));
            }

            // Filtrar per categoria, si s'ha seleccionat
            if (categoria != null) {
                filtres.add(cb.equal(
                        root.get("categoria"),
                        categoria
                ));
            }

            // Filtrar per disponibilitat durant un període
            if (dataInici != null && dataFi != null) {

                // Els vehicles inactius no estan disponibles
                filtres.add(cb.notEqual(
                        root.get("estat"),
                        Enums.EstatVehicle.Inactiu
                ));

                // Cercar reserves que coincideixin amb el període
                Subquery<Long> subconsulta =
                        query.subquery(Long.class);

                Root<Reserva> reserva =
                        subconsulta.from(Reserva.class);

                subconsulta.select(
                        reserva.<Long>get("idReserva")
                );

                subconsulta.where(
                        cb.equal(
                                reserva.get("vehicle"),
                                root
                        ),

                        cb.notEqual(
                                reserva.get("estat"),
                                Enums.EstatReserva.Anul_lada
                        ),

                        cb.lessThan(
                                reserva.<LocalDate>get("dataInici"),
                                dataFi
                        ),

                        cb.greaterThan(
                                reserva.<LocalDate>get("dataFi"),
                                dataInici
                        )
                );

                // Només vehicles sense reserves coincidents
                filtres.add(cb.not(cb.exists(subconsulta)));
            }

            // Aplicar tots els filtres seleccionats alhora
            return cb.and(
                    filtres.toArray(new Predicate[0])
            );
        };
    }
}
