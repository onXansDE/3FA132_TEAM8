package de.fentacore.endpoints;

import de.fentacore.config.DatabaseConfig;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/setupDB")
public class DbSetup {

    @DELETE
    public Response resetDatabase() {
        try {
            DatabaseConfig.deleteTables();
            DatabaseConfig.createTables();

            return Response.status(Response.Status.OK)
                    .entity("Successfully reset database.")
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error resetting database.")
                    .build();
        }
    }
}
