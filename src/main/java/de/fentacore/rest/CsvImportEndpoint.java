package de.fentacore.rest;

import de.fentacore.utils.CSVImporter;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.BufferedReader;
import java.io.StringReader;

@Path("/import")
public class CsvImportEndpoint {

    private final CSVImporter csvImporter = new CSVImporter();

    @POST
    @Path("/customers")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importCustomers(String csvContent) {
        try {
            if (csvContent == null || csvContent.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"CSV content is required\"}")
                    .build();
            }

            int importedCount = csvImporter.importCustomersFromString(csvContent);
            
            return Response.ok()
                .entity("{\"message\": \"Successfully imported " + importedCount + " customers\", \"count\": " + importedCount + "}")
                .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"Import failed: " + e.getMessage() + "\"}")
                .build();
        }
    }

    @POST
    @Path("/readings")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importReadings(String csvContent) {
        try {
            if (csvContent == null || csvContent.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"CSV content is required\"}")
                    .build();
            }

            int importedCount = csvImporter.importReadingsFromString(csvContent);
            
            return Response.ok()
                .entity("{\"message\": \"Successfully imported " + importedCount + " readings\", \"count\": " + importedCount + "}")
                .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"Import failed: " + e.getMessage() + "\"}")
                .build();
        }
    }
} 