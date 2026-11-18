package cat.copernic.Projecte1_Equip5.entity;

/**
 * Roles de seguridad de la aplicación. Spring Security los usa para decidir a
 * qué URLs puede acceder cada usuario.
 *
 * No es una tabla de la base de datos: el rol del agente se guarda en
 * {@link Enums.Rol}, y esta clase solo lo traduce al rol de seguridad.
 */
public enum AuthorityType {

    /**
     * Cliente normal: no tiene rol guardado en la BD.
     */
    CLIENT,
    
    /**
     * Agente con rol Agent.
     */
    AGENT,
    
    /**
     * Agente con rol Administrador.
     */
    ADMINISTRADOR;

    /**
     * Determina el rol de seguridad de un usuario según su tipo. Un Agent usa
     * el rol que tiene guardado; cualquier otro usuario es CLIENT.
     *
     * @param u usuario autenticado (Client o Agent)
     * @return el rol de seguridad que le corresponde
     */
    public static AuthorityType of(Usuari u) {

        // ¿El usuario es un Agent? Se pregunta primero por Agent porque
        // Agent extiende Client: si se preguntara antes por Client, los
        // agentes también entrarían ahí y recibirían CLIENT por error.
        if (u instanceof Agent agent) {

            // Se mira el rol guardado en la BD (Enums.Rol) y se traduce
            // al rol de seguridad equivalente.
            return switch (agent.getRol()) {
                case Administrador ->
                    ADMINISTRADOR;
                case Agent ->
                    AGENT;
            };
        }

        // Si no es un Agent, es un cliente normal.
        return CLIENT;
    }
}
