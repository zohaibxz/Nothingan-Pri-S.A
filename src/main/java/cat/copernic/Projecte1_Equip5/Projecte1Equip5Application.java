package cat.copernic.Projecte1_Equip5;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

//Per arrecncar la web guardant dades posar com a comentari
/*@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class, 
    HibernateJpaAutoConfiguration.class
})
*/
@SpringBootApplication
public class Projecte1Equip5Application {

	public static void main(String[] args) {
		SpringApplication.run(Projecte1Equip5Application.class, args);
	}

}   