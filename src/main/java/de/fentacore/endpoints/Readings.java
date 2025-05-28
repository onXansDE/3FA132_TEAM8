package de.fentacore.endpoints;

import de.fentacore.dao.ReadingDAO;
import de.fentacore.interfaces.IReading;
import de.fentacore.model.Reading;
import de.fentacore.model.ReadingRequest;
import de.fentacore.model.ReadingsResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("readings")
public class Readings {

    @GET
    @Path("{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReadingById(@PathParam("uuid") String uuidString) {
        try {
            UUID uuid = UUID.fromString(uuidString);
            ReadingDAO readingDAO = new ReadingDAO();
            IReading reading = readingDAO.findById(uuid);
            
            if (reading == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Reading with ID " + uuid + " not found\"}")
                        .build();
            }
            
            // Return reading wrapped in ReadingRequest format to match JSON Schema Reading
            ReadingRequest response = new ReadingRequest((Reading) reading);
            return Response.ok(response).build();
            
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Invalid UUID format: " + uuidString + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Error retrieving reading: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReadings(@QueryParam("customer") String customerUuid,
                               @QueryParam("start") String startDate,
                               @QueryParam("end") String endDate,
                               @QueryParam("kindOfMeter") String kindOfMeter) {
        try {
            ReadingDAO readingDAO = new ReadingDAO();
            List<IReading> readings;
            
            // If customer parameter is provided, filter by customer
            if (customerUuid != null && !customerUuid.trim().isEmpty()) {
                try {
                    UUID customerId = UUID.fromString(customerUuid);
                    readings = readingDAO.findByCustomerId(customerId);
                } catch (IllegalArgumentException e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("{\"error\": \"Invalid customer UUID format: " + customerUuid + "\"}")
                            .build();
                }
            } else {
                // Get all readings
                readings = readingDAO.findAll();
            }
            
            // Filter by date range if provided
            if (startDate != null && !startDate.trim().isEmpty()) {
                try {
                    LocalDate start = LocalDate.parse(startDate);
                    readings = readings.stream()
                            .filter(reading -> reading.getDateOfReading() != null && 
                                    !reading.getDateOfReading().isBefore(start))
                            .collect(Collectors.toList());
                } catch (DateTimeParseException e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("{\"error\": \"Invalid start date format. Expected yyyy-MM-dd: " + startDate + "\"}")
                            .build();
                }
            }
            
            if (endDate != null && !endDate.trim().isEmpty()) {
                try {
                    LocalDate end = LocalDate.parse(endDate);
                    readings = readings.stream()
                            .filter(reading -> reading.getDateOfReading() != null && 
                                    !reading.getDateOfReading().isAfter(end))
                            .collect(Collectors.toList());
                } catch (DateTimeParseException e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("{\"error\": \"Invalid end date format. Expected yyyy-MM-dd: " + endDate + "\"}")
                            .build();
                }
            }
            
            // Filter by kindOfMeter if provided
            if (kindOfMeter != null && !kindOfMeter.trim().isEmpty()) {
                try {
                    IReading.KindOfMeter meterType = IReading.KindOfMeter.valueOf(kindOfMeter.toUpperCase());
                    readings = readings.stream()
                            .filter(reading -> reading.getKindOfMeter() == meterType)
                            .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("{\"error\": \"Invalid kindOfMeter. Valid values: HEIZUNG, STROM, WASSER, UNBEKANNT\"}")
                            .build();
                }
            }
            
            // Return readings wrapped in ReadingsResponse format to match JSON Schema Readings
            ReadingsResponse response = new ReadingsResponse(readings);
            return Response.ok(response).build();
            
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Error retrieving readings: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createReading(ReadingRequest readingRequest) {
        try {
            // Validate request
            if (readingRequest == null || readingRequest.getReading() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Invalid JSON format or missing reading object\"}")
                        .build();
            }
            
            Reading reading = readingRequest.getReading();
            
            // Validate required fields according to JSON schema
            if (reading.getCustomer() == null ||
                reading.getDateOfReading() == null ||
                reading.getMeterId() == null || reading.getMeterId().trim().isEmpty() ||
                reading.getSubstitute() == null ||
                reading.getMeterCount() == null ||
                reading.getKindOfMeter() == null) {
                
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Missing required fields: customer, dateOfReading, meterId, substitute, meterCount, kindOfMeter\"}")
                        .build();
            }
            
            // Generate UUID if not provided (as per requirement)
            if (reading.getId() == null) {
                reading.setId(UUID.randomUUID());
            }
            
            // Create reading using DAO
            ReadingDAO readingDAO = new ReadingDAO();
            IReading createdReading = readingDAO.create(reading);
            
            if (createdReading != null) {
                // Return 201 Created with the created reading wrapped in ReadingRequest format
                ReadingRequest response = new ReadingRequest((Reading) createdReading);
                return Response.status(Response.Status.CREATED)
                        .entity(response)
                        .build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Failed to create reading\"}")
                        .build();
            }
            
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Error processing request: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateReading(ReadingRequest readingRequest) {
        try {
            // Validate request
            if (readingRequest == null || readingRequest.getReading() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Invalid JSON format or missing reading object\"}")
                        .build();
            }
            
            Reading reading = readingRequest.getReading();
            
            // Validate required fields according to JSON schema
            if (reading.getId() == null ||
                reading.getCustomer() == null ||
                reading.getDateOfReading() == null ||
                reading.getMeterId() == null || reading.getMeterId().trim().isEmpty() ||
                reading.getSubstitute() == null ||
                reading.getMeterCount() == null ||
                reading.getKindOfMeter() == null) {
                
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Missing required fields: id, customer, dateOfReading, meterId, substitute, meterCount, kindOfMeter\"}")
                        .build();
            }
            
            ReadingDAO readingDAO = new ReadingDAO();
            
            // Check if reading exists
            IReading existingReading = readingDAO.findById(reading.getId());
            if (existingReading == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Reading with ID " + reading.getId() + " not found\"}")
                        .build();
            }
            
            // Update reading using DAO
            boolean updated = readingDAO.update(reading);
            
            if (updated) {
                // Return 200 OK with the updated reading wrapped in ReadingRequest format
                ReadingRequest response = new ReadingRequest(reading);
                return Response.status(Response.Status.OK)
                        .entity(response)
                        .build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Failed to update reading\"}")
                        .build();
            }
            
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Error processing request: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @DELETE
    @Path("{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteReading(@PathParam("uuid") String uuidString) {
        try {
            UUID uuid = UUID.fromString(uuidString);
            ReadingDAO readingDAO = new ReadingDAO();
            
            // First, get the reading to return it in the response
            IReading reading = readingDAO.findById(uuid);
            if (reading == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Reading with ID " + uuid + " not found\"}")
                        .build();
            }
            
            // Delete the reading
            boolean deleted = readingDAO.delete(uuid);
            
            if (deleted) {
                // Return 200 OK with the deleted reading wrapped in ReadingRequest format
                ReadingRequest response = new ReadingRequest((Reading) reading);
                return Response.status(Response.Status.OK)
                        .entity(response)
                        .build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Failed to delete reading\"}")
                        .build();
            }
            
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Invalid UUID format: " + uuidString + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Error deleting reading: " + e.getMessage() + "\"}")
                    .build();
        }
    }
} 