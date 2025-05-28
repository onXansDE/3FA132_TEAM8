package de.fentacore.endpoints;

import de.fentacore.config.DatabaseConfig;
import de.fentacore.dao.CustomerDAO;
import de.fentacore.dao.ReadingDAO;
import de.fentacore.model.Customer;
import de.fentacore.model.CustomerRequest;
import de.fentacore.model.CustomersResponse;
import de.fentacore.model.CustomerWithReadingsResponse;
import de.fentacore.interfaces.ICustomer;
import de.fentacore.interfaces.IReading;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("customers")
public class Customers {

    @GET
    @Produces("application/json")
    public Response getCustomers() {
        CustomerDAO customers = new CustomerDAO();
        List<ICustomer> customerList = customers.findAll();
        CustomersResponse response = new CustomersResponse(customerList);
        return Response.ok(response).build();
    }

    @GET
    @Path("{uuid}")
    @Produces("application/json")
    public Response getCustomerById(@PathParam("uuid") String uuidString) {
        try {
            UUID uuid = UUID.fromString(uuidString);
            CustomerDAO customerDAO = new CustomerDAO();
            ICustomer customer = customerDAO.findById(uuid);
            
            if (customer == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Customer with ID " + uuid + " not found\"}")
                        .build();
            }
            
            // Return customer wrapped in CustomerRequest format to match JSON Schema Nr. 1
            CustomerRequest response = new CustomerRequest((Customer) customer);
            return Response.ok(response).build();
            
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Invalid UUID format: " + uuidString + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Error retrieving customer: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Path("create")
    @Produces("application/json")
    @Consumes("application/json")
    public Response createCustomer(Customer customer) {
        CustomerDAO customers = new CustomerDAO();
        return Response.ok(customers.create(customer)).build();
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateCustomerJson(CustomerRequest customerRequest) {
        try {
            if (customerRequest == null || customerRequest.getCustomer() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Invalid JSON format or missing customer object\"}")
                        .build();
            }
            
            return updateCustomerInternal(customerRequest.getCustomer());
            
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Error processing JSON request: " + e.getMessage() + "\"}")
                    .build();
        }
    }
    
    private Response updateCustomerInternal(Customer customerToUpdate) {
        try {
            CustomerDAO customerDAO = new CustomerDAO();
            
            // Validate required fields
            if (customerToUpdate.getId() == null || 
                customerToUpdate.getFirstName() == null || customerToUpdate.getFirstName().trim().isEmpty() ||
                customerToUpdate.getLastName() == null || customerToUpdate.getLastName().trim().isEmpty() ||
                customerToUpdate.getGender() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Missing required fields: id, firstName, lastName, gender\"}")
                        .build();
            }
            
            // Check if customer exists
            ICustomer existingCustomer = customerDAO.findById(customerToUpdate.getId());
            if (existingCustomer == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Customer with ID " + customerToUpdate.getId() + " not found\"}")
                        .build();
            }
            
            // Update customer
            boolean updated = customerDAO.update(customerToUpdate);
            if (updated) {
                // Return the updated customer wrapped in CustomerRequest format to match schema
                CustomerRequest response = new CustomerRequest(customerToUpdate);
                return Response.ok(response).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Failed to update customer\"}")
                        .build();
            }
            
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Error updating customer: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @DELETE
    @Path("{uuid}")
    @Produces("application/json")
    public Response deleteCustomer(@PathParam("uuid") String uuidString) {
        try {
            UUID uuid = UUID.fromString(uuidString);
            CustomerDAO customerDAO = new CustomerDAO();
            ReadingDAO readingDAO = new ReadingDAO();
            
            ICustomer customer = customerDAO.findById(uuid);
            
            if (customer == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Customer with ID " + uuid + " not found\"}")
                        .build();
            }
            
            // Get customer's readings before deletion
            List<IReading> customerReadings = readingDAO.findByCustomerId(uuid);
            
            // Delete customer
            boolean deleted = customerDAO.delete(uuid);
            if (deleted) {
                // Return the deleted customer with their readings wrapped in CustomerWithReadingsResponse format
                CustomerWithReadingsResponse response = new CustomerWithReadingsResponse(customer, customerReadings);
                return Response.ok(response).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Failed to delete customer\"}")
                        .build();
            }
            
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Invalid UUID format: " + uuidString + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Error deleting customer: " + e.getMessage() + "\"}")
                    .build();
        }
    }
}
