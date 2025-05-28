package de.fentacore.endpoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import de.fentacore.config.DatabaseConfig;
import de.fentacore.dao.CustomerDAO;
import de.fentacore.dao.ReadingDAO;
import de.fentacore.interfaces.ICustomer;
import de.fentacore.interfaces.IReading;
import de.fentacore.model.Customer;
import de.fentacore.model.Reading;
import de.fentacore.model.ReadingRequest;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ReadingsEndpointTest extends JerseyTest {

    private ObjectMapper objectMapper;
    private JsonSchema readingSchema;
    private JsonSchema readingsSchema;
    private CustomerDAO customerDAO;
    private ReadingDAO readingDAO;
    private Customer testCustomer;
    private Reading testReading;

    @Override
    protected Application configure() {
        return new ResourceConfig(Readings.class);
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        
        // Initialize database
        DatabaseConfig.deleteTables();
        DatabaseConfig.createTables();
        
        // Initialize ObjectMapper with JSR310 module for LocalDate support
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        // Load JSON schemas
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        
        InputStream readingSchemaStream = getClass().getClassLoader()
                .getResourceAsStream("JSON_Schema_Reading.json");
        readingSchema = factory.getSchema(readingSchemaStream);
        
        InputStream readingsSchemaStream = getClass().getClassLoader()
                .getResourceAsStream("JSON_Schema_Readings.json");
        readingsSchema = factory.getSchema(readingsSchemaStream);
        
        // Initialize DAOs
        customerDAO = new CustomerDAO();
        readingDAO = new ReadingDAO();
        
        // Create test customer
        testCustomer = new Customer();
        testCustomer.setFirstName("John");
        testCustomer.setLastName("Doe");
        testCustomer.setBirthDate(LocalDate.of(1990, 1, 1));
        testCustomer.setGender(ICustomer.Gender.M);
        testCustomer = (Customer) customerDAO.create(testCustomer);
        
        // Create test reading
        testReading = new Reading();
        testReading.setCustomer(testCustomer);
        testReading.setDateOfReading(LocalDate.of(2024, 1, 15));
        testReading.setMeterId("METER-001");
        testReading.setSubstitute(false);
        testReading.setMeterCount(1234.5);
        testReading.setKindOfMeter(IReading.KindOfMeter.STROM);
        testReading.setComment("Test reading");
        testReading = (Reading) readingDAO.create(testReading);
    }

    @AfterEach
    public void tearDown() throws Exception {
        DatabaseConfig.deleteTables();
        DatabaseConfig.createTables();
        super.tearDown();
    }

    // GET /readings/{uuid} Tests

    @Test
    public void testGetReadingById_Success() throws Exception {
        Response response = target("/readings/" + testReading.getId())
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        // Validate against JSON Schema Reading
        Set<ValidationMessage> errors = readingSchema.validate(jsonNode);
        assertTrue(errors.isEmpty(), "Response should match JSON Schema Reading: " + errors);
        
        // Verify content
        JsonNode readingNode = jsonNode.get("reading");
        assertNotNull(readingNode);
        assertEquals(testReading.getId().toString(), readingNode.get("id").asText());
        assertEquals("METER-001", readingNode.get("meterId").asText());
        assertEquals("STROM", readingNode.get("kindOfMeter").asText());
        assertEquals(1234.5, readingNode.get("meterCount").asDouble());
        assertEquals(false, readingNode.get("substitute").asBoolean());
        assertEquals("2024-01-15", readingNode.get("dateOfReading").asText());
        assertEquals("Test reading", readingNode.get("comment").asText());
        
        // Verify customer data
        JsonNode customerNode = readingNode.get("customer");
        assertNotNull(customerNode);
        assertEquals("John", customerNode.get("firstName").asText());
        assertEquals("Doe", customerNode.get("lastName").asText());
        assertEquals("M", customerNode.get("gender").asText());
    }

    @Test
    public void testGetReadingById_NotFound() {
        UUID nonExistentId = UUID.randomUUID();
        Response response = target("/readings/" + nonExistentId)
                .request()
                .get();

        assertEquals(404, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("not found"));
    }

    @Test
    public void testGetReadingById_InvalidUUID() {
        Response response = target("/readings/invalid-uuid")
                .request()
                .get();

        assertEquals(400, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("Invalid UUID format"));
    }

    // GET /readings Tests

    @Test
    public void testGetAllReadings_Success() throws Exception {
        Response response = target("/readings")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        // Validate against JSON Schema Readings
        Set<ValidationMessage> errors = readingsSchema.validate(jsonNode);
        assertTrue(errors.isEmpty(), "Response should match JSON Schema Readings: " + errors);
        
        // Verify content
        JsonNode readingsArray = jsonNode.get("readings");
        assertNotNull(readingsArray);
        assertTrue(readingsArray.isArray());
        assertEquals(1, readingsArray.size());
        
        JsonNode firstReading = readingsArray.get(0);
        assertEquals(testReading.getId().toString(), firstReading.get("id").asText());
        assertEquals("METER-001", firstReading.get("meterId").asText());
    }

    @Test
    public void testGetReadingsByCustomer_Success() throws Exception {
        Response response = target("/readings")
                .queryParam("customer", testCustomer.getId().toString())
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        // Validate against JSON Schema Readings
        Set<ValidationMessage> errors = readingsSchema.validate(jsonNode);
        assertTrue(errors.isEmpty(), "Response should match JSON Schema Readings: " + errors);
        
        JsonNode readingsArray = jsonNode.get("readings");
        assertEquals(1, readingsArray.size());
    }

    @Test
    public void testGetReadingsByCustomer_InvalidUUID() {
        Response response = target("/readings")
                .queryParam("customer", "invalid-uuid")
                .request()
                .get();

        assertEquals(400, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("Invalid customer UUID format"));
    }

    @Test
    public void testGetReadingsByCustomer_NotFound() throws Exception {
        UUID nonExistentCustomerId = UUID.randomUUID();
        Response response = target("/readings")
                .queryParam("customer", nonExistentCustomerId.toString())
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        // Should return empty array
        JsonNode readingsArray = jsonNode.get("readings");
        assertEquals(0, readingsArray.size());
    }

    @Test
    public void testGetReadingsByDateRange_Success() throws Exception {
        Response response = target("/readings")
                .queryParam("start", "2024-01-01")
                .queryParam("end", "2024-01-31")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        // Validate against JSON Schema Readings
        Set<ValidationMessage> errors = readingsSchema.validate(jsonNode);
        assertTrue(errors.isEmpty(), "Response should match JSON Schema Readings: " + errors);
        
        JsonNode readingsArray = jsonNode.get("readings");
        assertEquals(1, readingsArray.size());
    }

    @Test
    public void testGetReadingsByDateRange_OutOfRange() throws Exception {
        Response response = target("/readings")
                .queryParam("start", "2025-01-01")
                .queryParam("end", "2025-01-31")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        JsonNode readingsArray = jsonNode.get("readings");
        assertEquals(0, readingsArray.size());
    }

    @Test
    public void testGetReadingsByDateRange_InvalidStartDate() {
        Response response = target("/readings")
                .queryParam("start", "invalid-date")
                .request()
                .get();

        assertEquals(400, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("Invalid start date format"));
    }

    @Test
    public void testGetReadingsByDateRange_InvalidEndDate() {
        Response response = target("/readings")
                .queryParam("end", "invalid-date")
                .request()
                .get();

        assertEquals(400, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("Invalid end date format"));
    }

    @Test
    public void testGetReadingsByKindOfMeter_Success() throws Exception {
        Response response = target("/readings")
                .queryParam("kindOfMeter", "STROM")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        // Validate against JSON Schema Readings
        Set<ValidationMessage> errors = readingsSchema.validate(jsonNode);
        assertTrue(errors.isEmpty(), "Response should match JSON Schema Readings: " + errors);
        
        JsonNode readingsArray = jsonNode.get("readings");
        assertEquals(1, readingsArray.size());
        
        JsonNode firstReading = readingsArray.get(0);
        assertEquals("STROM", firstReading.get("kindOfMeter").asText());
    }

    @Test
    public void testGetReadingsByKindOfMeter_NoMatch() throws Exception {
        Response response = target("/readings")
                .queryParam("kindOfMeter", "WASSER")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        JsonNode readingsArray = jsonNode.get("readings");
        assertEquals(0, readingsArray.size());
    }

    @Test
    public void testGetReadingsByKindOfMeter_InvalidValue() {
        Response response = target("/readings")
                .queryParam("kindOfMeter", "INVALID")
                .request()
                .get();

        assertEquals(400, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("Invalid kindOfMeter"));
        assertTrue(jsonResponse.contains("HEIZUNG, STROM, WASSER, UNBEKANNT"));
    }

    @Test
    public void testGetReadingsWithMultipleFilters_Success() throws Exception {
        Response response = target("/readings")
                .queryParam("customer", testCustomer.getId().toString())
                .queryParam("start", "2024-01-01")
                .queryParam("end", "2024-01-31")
                .queryParam("kindOfMeter", "STROM")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        // Validate against JSON Schema Readings
        Set<ValidationMessage> errors = readingsSchema.validate(jsonNode);
        assertTrue(errors.isEmpty(), "Response should match JSON Schema Readings: " + errors);
        
        JsonNode readingsArray = jsonNode.get("readings");
        assertEquals(1, readingsArray.size());
    }

    @Test
    public void testGetReadingsWithMultipleFilters_NoMatch() throws Exception {
        Response response = target("/readings")
                .queryParam("customer", testCustomer.getId().toString())
                .queryParam("start", "2024-01-01")
                .queryParam("end", "2024-01-31")
                .queryParam("kindOfMeter", "WASSER")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        JsonNode readingsArray = jsonNode.get("readings");
        assertEquals(0, readingsArray.size());
    }

    @Test
    public void testGetReadingsEmptyDatabase() throws Exception {
        // Clear all readings
        readingDAO.delete(testReading.getId());
        
        Response response = target("/readings")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        // Validate against JSON Schema Readings
        Set<ValidationMessage> errors = readingsSchema.validate(jsonNode);
        assertTrue(errors.isEmpty(), "Response should match JSON Schema Readings: " + errors);
        
        JsonNode readingsArray = jsonNode.get("readings");
        assertEquals(0, readingsArray.size());
    }

    // Test case-insensitive kindOfMeter parameter
    @Test
    public void testGetReadingsByKindOfMeter_CaseInsensitive() throws Exception {
        Response response = target("/readings")
                .queryParam("kindOfMeter", "strom")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        JsonNode readingsArray = jsonNode.get("readings");
        assertEquals(1, readingsArray.size());
    }

    // Test with multiple readings
    @Test
    public void testGetReadingsWithMultipleReadings() throws Exception {
        // Create additional reading
        Reading secondReading = new Reading();
        secondReading.setCustomer(testCustomer);
        secondReading.setDateOfReading(LocalDate.of(2024, 2, 15));
        secondReading.setMeterId("METER-002");
        secondReading.setSubstitute(true);
        secondReading.setMeterCount(2345.6);
        secondReading.setKindOfMeter(IReading.KindOfMeter.HEIZUNG);
        secondReading.setComment("Second test reading");
        readingDAO.create(secondReading);
        
        Response response = target("/readings")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        // Validate against JSON Schema Readings
        Set<ValidationMessage> errors = readingsSchema.validate(jsonNode);
        assertTrue(errors.isEmpty(), "Response should match JSON Schema Readings: " + errors);
        
        JsonNode readingsArray = jsonNode.get("readings");
        assertEquals(2, readingsArray.size());
    }

    // DELETE /readings/{uuid} Tests

    @Test
    public void testDeleteReading_Success() throws Exception {
        Response response = target("/readings/" + testReading.getId())
                .request()
                .delete();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        // Validate against JSON Schema Reading
        Set<ValidationMessage> errors = readingSchema.validate(jsonNode);
        assertTrue(errors.isEmpty(), "Response should match JSON Schema Reading: " + errors);
        
        // Verify content of deleted reading
        JsonNode readingNode = jsonNode.get("reading");
        assertNotNull(readingNode);
        assertEquals(testReading.getId().toString(), readingNode.get("id").asText());
        assertEquals("METER-001", readingNode.get("meterId").asText());
        assertEquals("STROM", readingNode.get("kindOfMeter").asText());
        assertEquals(1234.5, readingNode.get("meterCount").asDouble());
        assertEquals(false, readingNode.get("substitute").asBoolean());
        assertEquals("2024-01-15", readingNode.get("dateOfReading").asText());
        assertEquals("Test reading", readingNode.get("comment").asText());
        
        // Verify reading is actually deleted
        Response getResponse = target("/readings/" + testReading.getId())
                .request()
                .get();
        assertEquals(404, getResponse.getStatus());
    }

    @Test
    public void testDeleteReading_NotFound() {
        UUID nonExistentId = UUID.randomUUID();
        Response response = target("/readings/" + nonExistentId)
                .request()
                .delete();

        assertEquals(404, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("not found"));
    }

    @Test
    public void testDeleteReading_InvalidUUID() {
        Response response = target("/readings/invalid-uuid")
                .request()
                .delete();

        assertEquals(400, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("Invalid UUID format"));
    }

    // POST /readings Tests

    @Test
    public void testCreateReading_Success() throws Exception {
        Reading newReading = new Reading();
        newReading.setCustomer(testCustomer);
        newReading.setDateOfReading(LocalDate.of(2024, 3, 15));
        newReading.setMeterId("METER-NEW-001");
        newReading.setSubstitute(true);
        newReading.setMeterCount(5678.9);
        newReading.setKindOfMeter(IReading.KindOfMeter.WASSER);
        newReading.setComment("New test reading");

        ReadingRequest request = new ReadingRequest(newReading);
        String jsonRequest = objectMapper.writeValueAsString(request);

        Response response = target("/readings")
                .request()
                .post(jakarta.ws.rs.client.Entity.entity(jsonRequest, "application/json"));

        assertEquals(201, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        // Validate against JSON Schema Reading
        Set<ValidationMessage> errors = readingSchema.validate(jsonNode);
        assertTrue(errors.isEmpty(), "Response should match JSON Schema Reading: " + errors);
        
        // Verify content
        JsonNode readingNode = jsonNode.get("reading");
        assertNotNull(readingNode);
        assertNotNull(readingNode.get("id").asText()); // UUID should be generated
        assertEquals("METER-NEW-001", readingNode.get("meterId").asText());
        assertEquals("WASSER", readingNode.get("kindOfMeter").asText());
        assertEquals(5678.9, readingNode.get("meterCount").asDouble());
        assertEquals(true, readingNode.get("substitute").asBoolean());
        assertEquals("2024-03-15", readingNode.get("dateOfReading").asText());
        assertEquals("New test reading", readingNode.get("comment").asText());
    }

    @Test
    public void testCreateReading_WithProvidedId() throws Exception {
        UUID providedId = UUID.randomUUID();
        Reading newReading = new Reading();
        newReading.setId(providedId);
        newReading.setCustomer(testCustomer);
        newReading.setDateOfReading(LocalDate.of(2024, 3, 15));
        newReading.setMeterId("METER-NEW-002");
        newReading.setSubstitute(false);
        newReading.setMeterCount(9876.5);
        newReading.setKindOfMeter(IReading.KindOfMeter.HEIZUNG);
        newReading.setComment("Reading with provided ID");

        ReadingRequest request = new ReadingRequest(newReading);
        String jsonRequest = objectMapper.writeValueAsString(request);

        Response response = target("/readings")
                .request()
                .post(jakarta.ws.rs.client.Entity.entity(jsonRequest, "application/json"));

        assertEquals(201, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        JsonNode readingNode = jsonNode.get("reading");
        assertEquals(providedId.toString(), readingNode.get("id").asText());
    }

    @Test
    public void testCreateReading_MissingCustomer() throws Exception {
        Reading newReading = new Reading();
        newReading.setDateOfReading(LocalDate.of(2024, 3, 15));
        newReading.setMeterId("METER-NEW-003");
        newReading.setSubstitute(false);
        newReading.setMeterCount(1111.1);
        newReading.setKindOfMeter(IReading.KindOfMeter.STROM);

        ReadingRequest request = new ReadingRequest(newReading);
        String jsonRequest = objectMapper.writeValueAsString(request);

        Response response = target("/readings")
                .request()
                .post(jakarta.ws.rs.client.Entity.entity(jsonRequest, "application/json"));

        assertEquals(400, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("Missing required fields"));
        assertTrue(jsonResponse.contains("customer"));
    }

    @Test
    public void testCreateReading_MissingDateOfReading() throws Exception {
        Reading newReading = new Reading();
        newReading.setCustomer(testCustomer);
        newReading.setMeterId("METER-NEW-004");
        newReading.setSubstitute(false);
        newReading.setMeterCount(2222.2);
        newReading.setKindOfMeter(IReading.KindOfMeter.STROM);

        ReadingRequest request = new ReadingRequest(newReading);
        String jsonRequest = objectMapper.writeValueAsString(request);

        Response response = target("/readings")
                .request()
                .post(jakarta.ws.rs.client.Entity.entity(jsonRequest, "application/json"));

        assertEquals(400, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("Missing required fields"));
        assertTrue(jsonResponse.contains("dateOfReading"));
    }

    @Test
    public void testCreateReading_MissingMeterId() throws Exception {
        Reading newReading = new Reading();
        newReading.setCustomer(testCustomer);
        newReading.setDateOfReading(LocalDate.of(2024, 3, 15));
        newReading.setSubstitute(false);
        newReading.setMeterCount(3333.3);
        newReading.setKindOfMeter(IReading.KindOfMeter.STROM);

        ReadingRequest request = new ReadingRequest(newReading);
        String jsonRequest = objectMapper.writeValueAsString(request);

        Response response = target("/readings")
                .request()
                .post(jakarta.ws.rs.client.Entity.entity(jsonRequest, "application/json"));

        assertEquals(400, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("Missing required fields"));
        assertTrue(jsonResponse.contains("meterId"));
    }

    @Test
    public void testCreateReading_EmptyMeterId() throws Exception {
        Reading newReading = new Reading();
        newReading.setCustomer(testCustomer);
        newReading.setDateOfReading(LocalDate.of(2024, 3, 15));
        newReading.setMeterId("");
        newReading.setSubstitute(false);
        newReading.setMeterCount(4444.4);
        newReading.setKindOfMeter(IReading.KindOfMeter.STROM);

        ReadingRequest request = new ReadingRequest(newReading);
        String jsonRequest = objectMapper.writeValueAsString(request);

        Response response = target("/readings")
                .request()
                .post(jakarta.ws.rs.client.Entity.entity(jsonRequest, "application/json"));

        assertEquals(400, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("Missing required fields"));
        assertTrue(jsonResponse.contains("meterId"));
    }

    @Test
    public void testCreateReading_MissingSubstitute() throws Exception {
        Reading newReading = new Reading();
        newReading.setCustomer(testCustomer);
        newReading.setDateOfReading(LocalDate.of(2024, 3, 15));
        newReading.setMeterId("METER-NEW-005");
        newReading.setMeterCount(5555.5);
        newReading.setKindOfMeter(IReading.KindOfMeter.STROM);

        ReadingRequest request = new ReadingRequest(newReading);
        String jsonRequest = objectMapper.writeValueAsString(request);

        Response response = target("/readings")
                .request()
                .post(jakarta.ws.rs.client.Entity.entity(jsonRequest, "application/json"));

        assertEquals(400, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("Missing required fields"));
        assertTrue(jsonResponse.contains("substitute"));
    }

    @Test
    public void testCreateReading_MissingMeterCount() throws Exception {
        Reading newReading = new Reading();
        newReading.setCustomer(testCustomer);
        newReading.setDateOfReading(LocalDate.of(2024, 3, 15));
        newReading.setMeterId("METER-NEW-006");
        newReading.setSubstitute(false);
        newReading.setKindOfMeter(IReading.KindOfMeter.STROM);

        ReadingRequest request = new ReadingRequest(newReading);
        String jsonRequest = objectMapper.writeValueAsString(request);

        Response response = target("/readings")
                .request()
                .post(jakarta.ws.rs.client.Entity.entity(jsonRequest, "application/json"));

        assertEquals(400, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("Missing required fields"));
        assertTrue(jsonResponse.contains("meterCount"));
    }

    @Test
    public void testCreateReading_MissingKindOfMeter() throws Exception {
        Reading newReading = new Reading();
        newReading.setCustomer(testCustomer);
        newReading.setDateOfReading(LocalDate.of(2024, 3, 15));
        newReading.setMeterId("METER-NEW-007");
        newReading.setSubstitute(false);
        newReading.setMeterCount(6666.6);

        ReadingRequest request = new ReadingRequest(newReading);
        String jsonRequest = objectMapper.writeValueAsString(request);

        Response response = target("/readings")
                .request()
                .post(jakarta.ws.rs.client.Entity.entity(jsonRequest, "application/json"));

        assertEquals(400, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("Missing required fields"));
        assertTrue(jsonResponse.contains("kindOfMeter"));
    }

    @Test
    public void testCreateReading_NullRequest() {
        Response response = target("/readings")
                .request()
                .post(jakarta.ws.rs.client.Entity.entity("", "application/json"));

        assertEquals(400, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("Invalid JSON format") || jsonResponse.contains("missing reading object"));
    }

    @Test
    public void testCreateReading_EmptyRequest() {
        Response response = target("/readings")
                .request()
                .post(jakarta.ws.rs.client.Entity.entity("{}", "application/json"));

        assertEquals(400, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("Invalid JSON format") || jsonResponse.contains("missing reading object"));
    }

    @Test
    public void testCreateReading_InvalidJson() {
        Response response = target("/readings")
                .request()
                .post(jakarta.ws.rs.client.Entity.entity("invalid json", "application/json"));

        assertEquals(400, response.getStatus());
    }

    @Test
    public void testCreateReading_WithoutComment() throws Exception {
        Reading newReading = new Reading();
        newReading.setCustomer(testCustomer);
        newReading.setDateOfReading(LocalDate.of(2024, 3, 15));
        newReading.setMeterId("METER-NEW-008");
        newReading.setSubstitute(false);
        newReading.setMeterCount(7777.7);
        newReading.setKindOfMeter(IReading.KindOfMeter.UNBEKANNT);

        ReadingRequest request = new ReadingRequest(newReading);
        String jsonRequest = objectMapper.writeValueAsString(request);

        Response response = target("/readings")
                .request()
                .post(jakarta.ws.rs.client.Entity.entity(jsonRequest, "application/json"));

        assertEquals(201, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        JsonNode readingNode = jsonNode.get("reading");
        assertEquals("UNBEKANNT", readingNode.get("kindOfMeter").asText());
        // Comment should be null or empty
        assertTrue(readingNode.get("comment").isNull() || readingNode.get("comment").asText().isEmpty());
    }

    // PUT /readings Tests

    @Test
    public void testUpdateReading_Success() throws Exception {
        // Update the existing test reading
        testReading.setMeterId("METER-UPDATED-001");
        testReading.setMeterCount(9999.9);
        testReading.setKindOfMeter(IReading.KindOfMeter.HEIZUNG);
        testReading.setComment("Updated test reading");
        testReading.setSubstitute(true);
        testReading.setDateOfReading(LocalDate.of(2024, 4, 20));

        ReadingRequest request = new ReadingRequest(testReading);
        String jsonRequest = objectMapper.writeValueAsString(request);

        Response response = target("/readings")
                .request()
                .put(jakarta.ws.rs.client.Entity.entity(jsonRequest, "application/json"));

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        // Validate against JSON Schema Reading
        Set<ValidationMessage> errors = readingSchema.validate(jsonNode);
        assertTrue(errors.isEmpty(), "Response should match JSON Schema Reading: " + errors);
        
        // Verify updated content
        JsonNode readingNode = jsonNode.get("reading");
        assertNotNull(readingNode);
        assertEquals(testReading.getId().toString(), readingNode.get("id").asText());
        assertEquals("METER-UPDATED-001", readingNode.get("meterId").asText());
        assertEquals("HEIZUNG", readingNode.get("kindOfMeter").asText());
        assertEquals(9999.9, readingNode.get("meterCount").asDouble());
        assertEquals(true, readingNode.get("substitute").asBoolean());
        assertEquals("2024-04-20", readingNode.get("dateOfReading").asText());
        assertEquals("Updated test reading", readingNode.get("comment").asText());
    }

    @Test
    public void testUpdateReading_NotFound() throws Exception {
        Reading nonExistentReading = new Reading();
        nonExistentReading.setId(UUID.randomUUID());
        nonExistentReading.setCustomer(testCustomer);
        nonExistentReading.setDateOfReading(LocalDate.of(2024, 3, 15));
        nonExistentReading.setMeterId("METER-NONEXISTENT");
        nonExistentReading.setSubstitute(false);
        nonExistentReading.setMeterCount(1111.1);
        nonExistentReading.setKindOfMeter(IReading.KindOfMeter.STROM);

        ReadingRequest request = new ReadingRequest(nonExistentReading);
        String jsonRequest = objectMapper.writeValueAsString(request);

        Response response = target("/readings")
                .request()
                .put(jakarta.ws.rs.client.Entity.entity(jsonRequest, "application/json"));

        assertEquals(404, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("not found"));
    }

    @Test
    public void testUpdateReading_MissingId() throws Exception {
        Reading readingWithoutId = new Reading();
        readingWithoutId.setCustomer(testCustomer);
        readingWithoutId.setDateOfReading(LocalDate.of(2024, 3, 15));
        readingWithoutId.setMeterId("METER-NO-ID");
        readingWithoutId.setSubstitute(false);
        readingWithoutId.setMeterCount(2222.2);
        readingWithoutId.setKindOfMeter(IReading.KindOfMeter.STROM);

        ReadingRequest request = new ReadingRequest(readingWithoutId);
        String jsonRequest = objectMapper.writeValueAsString(request);

        Response response = target("/readings")
                .request()
                .put(jakarta.ws.rs.client.Entity.entity(jsonRequest, "application/json"));

        assertEquals(400, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("Missing required fields"));
        assertTrue(jsonResponse.contains("id"));
    }

    @Test
    public void testUpdateReading_MissingCustomer() throws Exception {
        Reading readingWithoutCustomer = new Reading();
        readingWithoutCustomer.setId(testReading.getId());
        readingWithoutCustomer.setDateOfReading(LocalDate.of(2024, 3, 15));
        readingWithoutCustomer.setMeterId("METER-NO-CUSTOMER");
        readingWithoutCustomer.setSubstitute(false);
        readingWithoutCustomer.setMeterCount(3333.3);
        readingWithoutCustomer.setKindOfMeter(IReading.KindOfMeter.STROM);

        ReadingRequest request = new ReadingRequest(readingWithoutCustomer);
        String jsonRequest = objectMapper.writeValueAsString(request);

        Response response = target("/readings")
                .request()
                .put(jakarta.ws.rs.client.Entity.entity(jsonRequest, "application/json"));

        assertEquals(400, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("Missing required fields"));
        assertTrue(jsonResponse.contains("customer"));
    }

    @Test
    public void testUpdateReading_NullRequest() {
        Response response = target("/readings")
                .request()
                .put(jakarta.ws.rs.client.Entity.entity("", "application/json"));

        assertEquals(400, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("Invalid JSON format") || jsonResponse.contains("missing reading object"));
    }

    @Test
    public void testUpdateReading_EmptyRequest() {
        Response response = target("/readings")
                .request()
                .put(jakarta.ws.rs.client.Entity.entity("{}", "application/json"));

        assertEquals(400, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("Invalid JSON format") || jsonResponse.contains("missing reading object"));
    }

    @Test
    public void testUpdateReading_InvalidJson() {
        Response response = target("/readings")
                .request()
                .put(jakarta.ws.rs.client.Entity.entity("invalid json", "application/json"));

        assertEquals(400, response.getStatus());
    }

    // Additional Edge Case Tests

    @Test
    public void testGetReadingsByDateRange_StartAfterEnd() throws Exception {
        Response response = target("/readings")
                .queryParam("start", "2024-12-31")
                .queryParam("end", "2024-01-01")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        JsonNode readingsArray = jsonNode.get("readings");
        assertEquals(0, readingsArray.size());
    }

    @Test
    public void testGetReadingsByDateRange_OnlyStartDate() throws Exception {
        Response response = target("/readings")
                .queryParam("start", "2024-01-01")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        JsonNode readingsArray = jsonNode.get("readings");
        assertEquals(1, readingsArray.size());
    }

    @Test
    public void testGetReadingsByDateRange_OnlyEndDate() throws Exception {
        Response response = target("/readings")
                .queryParam("end", "2024-12-31")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        JsonNode readingsArray = jsonNode.get("readings");
        assertEquals(1, readingsArray.size());
    }

    @Test
    public void testGetReadingsByCustomer_EmptyUUID() {
        Response response = target("/readings")
                .queryParam("customer", "")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        // Should return all readings when customer parameter is empty
        assertTrue(jsonResponse.contains("readings"));
    }

    @Test
    public void testGetReadingsByKindOfMeter_EmptyValue() {
        Response response = target("/readings")
                .queryParam("kindOfMeter", "")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        // Should return all readings when kindOfMeter parameter is empty
        assertTrue(jsonResponse.contains("readings"));
    }

    @Test
    public void testGetReadingsByDateRange_EmptyDates() {
        Response response = target("/readings")
                .queryParam("start", "")
                .queryParam("end", "")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        // Should return all readings when date parameters are empty
        assertTrue(jsonResponse.contains("readings"));
    }

    @Test
    public void testGetReadingsWithAllKindOfMeterTypes() throws Exception {
        // Create readings for all meter types
        Reading heizungReading = new Reading();
        heizungReading.setCustomer(testCustomer);
        heizungReading.setDateOfReading(LocalDate.of(2024, 2, 1));
        heizungReading.setMeterId("METER-HEIZUNG");
        heizungReading.setSubstitute(false);
        heizungReading.setMeterCount(1000.0);
        heizungReading.setKindOfMeter(IReading.KindOfMeter.HEIZUNG);
        readingDAO.create(heizungReading);

        Reading wasserReading = new Reading();
        wasserReading.setCustomer(testCustomer);
        wasserReading.setDateOfReading(LocalDate.of(2024, 2, 2));
        wasserReading.setMeterId("METER-WASSER");
        wasserReading.setSubstitute(false);
        wasserReading.setMeterCount(2000.0);
        wasserReading.setKindOfMeter(IReading.KindOfMeter.WASSER);
        readingDAO.create(wasserReading);

        Reading unbekanntReading = new Reading();
        unbekanntReading.setCustomer(testCustomer);
        unbekanntReading.setDateOfReading(LocalDate.of(2024, 2, 3));
        unbekanntReading.setMeterId("METER-UNBEKANNT");
        unbekanntReading.setSubstitute(false);
        unbekanntReading.setMeterCount(3000.0);
        unbekanntReading.setKindOfMeter(IReading.KindOfMeter.UNBEKANNT);
        readingDAO.create(unbekanntReading);

        // Test each meter type
        for (IReading.KindOfMeter meterType : IReading.KindOfMeter.values()) {
            Response response = target("/readings")
                    .queryParam("kindOfMeter", meterType.name())
                    .request()
                    .get();

            assertEquals(200, response.getStatus());
            
            String jsonResponse = response.readEntity(String.class);
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            
            JsonNode readingsArray = jsonNode.get("readings");
            assertTrue(readingsArray.size() >= 1, "Should find at least one reading for " + meterType);
            
            // Verify all returned readings have the correct meter type
            for (JsonNode reading : readingsArray) {
                assertEquals(meterType.name(), reading.get("kindOfMeter").asText());
            }
        }
    }

    @Test
    public void testCreateReadingWithAllKindOfMeterTypes() throws Exception {
        // Test creating readings with all meter types
        for (IReading.KindOfMeter meterType : IReading.KindOfMeter.values()) {
            Reading newReading = new Reading();
            newReading.setCustomer(testCustomer);
            newReading.setDateOfReading(LocalDate.of(2024, 5, 1));
            newReading.setMeterId("METER-" + meterType.name());
            newReading.setSubstitute(false);
            newReading.setMeterCount(1000.0 + meterType.ordinal());
            newReading.setKindOfMeter(meterType);
            newReading.setComment("Test reading for " + meterType);

            ReadingRequest request = new ReadingRequest(newReading);
            String jsonRequest = objectMapper.writeValueAsString(request);

            Response response = target("/readings")
                    .request()
                    .post(jakarta.ws.rs.client.Entity.entity(jsonRequest, "application/json"));

            assertEquals(201, response.getStatus(), "Failed to create reading for meter type: " + meterType);
            
            String jsonResponse = response.readEntity(String.class);
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            
            JsonNode readingNode = jsonNode.get("reading");
            assertEquals(meterType.name(), readingNode.get("kindOfMeter").asText());
        }
    }

    // Additional Comprehensive Tests

    @Test
    public void testCreateReading_BoundaryValues() throws Exception {
        // Test with minimum and maximum values
        Reading newReading = new Reading();
        newReading.setCustomer(testCustomer);
        newReading.setDateOfReading(LocalDate.of(1900, 1, 1)); // Very old date
        newReading.setMeterId("A"); // Single character meter ID
        newReading.setSubstitute(true);
        newReading.setMeterCount(0.0); // Minimum meter count
        newReading.setKindOfMeter(IReading.KindOfMeter.UNBEKANNT);
        newReading.setComment(""); // Empty comment

        ReadingRequest request = new ReadingRequest(newReading);
        String jsonRequest = objectMapper.writeValueAsString(request);

        Response response = target("/readings")
                .request()
                .post(jakarta.ws.rs.client.Entity.entity(jsonRequest, "application/json"));

        assertEquals(201, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        JsonNode readingNode = jsonNode.get("reading");
        assertEquals("1900-01-01", readingNode.get("dateOfReading").asText());
        assertEquals("A", readingNode.get("meterId").asText());
        assertEquals(0.0, readingNode.get("meterCount").asDouble());
    }

    @Test
    public void testCreateReading_LargeValues() throws Exception {
        // Test with very large values
        Reading newReading = new Reading();
        newReading.setCustomer(testCustomer);
        newReading.setDateOfReading(LocalDate.of(2099, 12, 31)); // Future date
        newReading.setMeterId("VERY-LONG-METER-ID-WITH-MANY-CHARACTERS-123456789"); // Long meter ID
        newReading.setSubstitute(false);
        newReading.setMeterCount(999999999.99); // Large meter count
        newReading.setKindOfMeter(IReading.KindOfMeter.STROM);
        newReading.setComment("This is a very long comment that contains many characters to test the system's ability to handle large text inputs without any issues or truncation problems that might occur during processing or storage operations.");

        ReadingRequest request = new ReadingRequest(newReading);
        String jsonRequest = objectMapper.writeValueAsString(request);

        Response response = target("/readings")
                .request()
                .post(jakarta.ws.rs.client.Entity.entity(jsonRequest, "application/json"));

        assertEquals(201, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        JsonNode readingNode = jsonNode.get("reading");
        assertEquals("2099-12-31", readingNode.get("dateOfReading").asText());
        assertEquals(999999999.99, readingNode.get("meterCount").asDouble());
    }

    @Test
    public void testCreateReading_SpecialCharacters() throws Exception {
        // Test with special characters in text fields
        Reading newReading = new Reading();
        newReading.setCustomer(testCustomer);
        newReading.setDateOfReading(LocalDate.of(2024, 6, 15));
        newReading.setMeterId("METER-äöü-ß-€-@-#-$-%");
        newReading.setSubstitute(true);
        newReading.setMeterCount(1234.56);
        newReading.setKindOfMeter(IReading.KindOfMeter.HEIZUNG);
        newReading.setComment("Comment with special chars: äöüß€@#$%&*()[]{}|\\:;\"'<>,.?/~`");

        ReadingRequest request = new ReadingRequest(newReading);
        String jsonRequest = objectMapper.writeValueAsString(request);

        Response response = target("/readings")
                .request()
                .post(jakarta.ws.rs.client.Entity.entity(jsonRequest, "application/json"));

        assertEquals(201, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        JsonNode readingNode = jsonNode.get("reading");
        assertEquals("METER-äöü-ß-€-@-#-$-%", readingNode.get("meterId").asText());
        assertTrue(readingNode.get("comment").asText().contains("äöüß€"));
    }

    @Test
    public void testUpdateReading_PartialUpdate() throws Exception {
        // Test updating only some fields
        testReading.setMeterCount(8888.88);
        testReading.setComment("Partially updated reading");
        // Keep other fields the same

        ReadingRequest request = new ReadingRequest(testReading);
        String jsonRequest = objectMapper.writeValueAsString(request);

        Response response = target("/readings")
                .request()
                .put(jakarta.ws.rs.client.Entity.entity(jsonRequest, "application/json"));

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        JsonNode readingNode = jsonNode.get("reading");
        assertEquals(8888.88, readingNode.get("meterCount").asDouble());
        assertEquals("Partially updated reading", readingNode.get("comment").asText());
        // Verify other fields remain unchanged
        assertEquals("METER-001", readingNode.get("meterId").asText());
        assertEquals("STROM", readingNode.get("kindOfMeter").asText());
    }

    @Test
    public void testGetReadings_PaginationLikeScenario() throws Exception {
        // Create multiple readings to test large result sets
        for (int i = 1; i <= 10; i++) {
            Reading reading = new Reading();
            reading.setCustomer(testCustomer);
            reading.setDateOfReading(LocalDate.of(2024, 1, i));
            reading.setMeterId("METER-BULK-" + String.format("%03d", i));
            reading.setSubstitute(i % 2 == 0);
            reading.setMeterCount(1000.0 + i);
            reading.setKindOfMeter(IReading.KindOfMeter.values()[i % IReading.KindOfMeter.values().length]);
            reading.setComment("Bulk reading " + i);
            readingDAO.create(reading);
        }

        Response response = target("/readings")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        JsonNode readingsArray = jsonNode.get("readings");
        assertTrue(readingsArray.size() >= 11); // Original test reading + 10 new ones
        
        // Verify all readings are returned
        boolean foundBulkReading = false;
        for (JsonNode reading : readingsArray) {
            if (reading.get("meterId").asText().startsWith("METER-BULK-")) {
                foundBulkReading = true;
                break;
            }
        }
        assertTrue(foundBulkReading, "Should find at least one bulk reading");
    }

    @Test
    public void testGetReadings_ComplexFiltering() throws Exception {
        // Create readings with different characteristics for complex filtering
        Customer secondCustomer = new Customer();
        secondCustomer.setFirstName("Jane");
        secondCustomer.setLastName("Smith");
        secondCustomer.setBirthDate(LocalDate.of(1985, 5, 15));
        secondCustomer.setGender(ICustomer.Gender.W);
        secondCustomer = (Customer) customerDAO.create(secondCustomer);

        // Reading for second customer
        Reading reading1 = new Reading();
        reading1.setCustomer(secondCustomer);
        reading1.setDateOfReading(LocalDate.of(2024, 2, 10));
        reading1.setMeterId("METER-COMPLEX-001");
        reading1.setSubstitute(false);
        reading1.setMeterCount(2500.0);
        reading1.setKindOfMeter(IReading.KindOfMeter.WASSER);
        readingDAO.create(reading1);

        // Reading outside date range
        Reading reading2 = new Reading();
        reading2.setCustomer(testCustomer);
        reading2.setDateOfReading(LocalDate.of(2023, 12, 31));
        reading2.setMeterId("METER-COMPLEX-002");
        reading2.setSubstitute(true);
        reading2.setMeterCount(3500.0);
        reading2.setKindOfMeter(IReading.KindOfMeter.STROM);
        readingDAO.create(reading2);

        // Test filtering by customer and date range
        Response response = target("/readings")
                .queryParam("customer", testCustomer.getId().toString())
                .queryParam("start", "2024-01-01")
                .queryParam("end", "2024-12-31")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        JsonNode readingsArray = jsonNode.get("readings");
        
        // Should only return readings for testCustomer within 2024
        for (JsonNode reading : readingsArray) {
            JsonNode customer = reading.get("customer");
            assertEquals(testCustomer.getId().toString(), customer.get("id").asText());
            
            String dateStr = reading.get("dateOfReading").asText();
            assertTrue(dateStr.startsWith("2024"), "Date should be in 2024: " + dateStr);
        }
    }

    @Test
    public void testGetReadings_DateBoundaryConditions() throws Exception {
        // Create readings on exact boundary dates
        Reading boundaryReading1 = new Reading();
        boundaryReading1.setCustomer(testCustomer);
        boundaryReading1.setDateOfReading(LocalDate.of(2024, 1, 1)); // Exact start date
        boundaryReading1.setMeterId("METER-BOUNDARY-START");
        boundaryReading1.setSubstitute(false);
        boundaryReading1.setMeterCount(1000.0);
        boundaryReading1.setKindOfMeter(IReading.KindOfMeter.STROM);
        readingDAO.create(boundaryReading1);

        Reading boundaryReading2 = new Reading();
        boundaryReading2.setCustomer(testCustomer);
        boundaryReading2.setDateOfReading(LocalDate.of(2024, 12, 31)); // Exact end date
        boundaryReading2.setMeterId("METER-BOUNDARY-END");
        boundaryReading2.setSubstitute(true);
        boundaryReading2.setMeterCount(2000.0);
        boundaryReading2.setKindOfMeter(IReading.KindOfMeter.WASSER);
        readingDAO.create(boundaryReading2);

        // Test inclusive boundary behavior
        Response response = target("/readings")
                .queryParam("start", "2024-01-01")
                .queryParam("end", "2024-12-31")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        JsonNode readingsArray = jsonNode.get("readings");
        
        boolean foundStartBoundary = false;
        boolean foundEndBoundary = false;
        
        for (JsonNode reading : readingsArray) {
            String meterId = reading.get("meterId").asText();
            if ("METER-BOUNDARY-START".equals(meterId)) {
                foundStartBoundary = true;
            } else if ("METER-BOUNDARY-END".equals(meterId)) {
                foundEndBoundary = true;
            }
        }
        
        assertTrue(foundStartBoundary, "Should include reading on start boundary date");
        assertTrue(foundEndBoundary, "Should include reading on end boundary date");
    }

    @Test
    public void testCRUDOperationsSequence() throws Exception {
        // Test a complete CRUD sequence
        
        // 1. CREATE
        Reading newReading = new Reading();
        newReading.setCustomer(testCustomer);
        newReading.setDateOfReading(LocalDate.of(2024, 7, 1));
        newReading.setMeterId("METER-CRUD-TEST");
        newReading.setSubstitute(false);
        newReading.setMeterCount(5000.0);
        newReading.setKindOfMeter(IReading.KindOfMeter.HEIZUNG);
        newReading.setComment("CRUD test reading");

        ReadingRequest createRequest = new ReadingRequest(newReading);
        String createJson = objectMapper.writeValueAsString(createRequest);

        Response createResponse = target("/readings")
                .request()
                .post(jakarta.ws.rs.client.Entity.entity(createJson, "application/json"));

        assertEquals(201, createResponse.getStatus());
        
        String createResponseJson = createResponse.readEntity(String.class);
        JsonNode createNode = objectMapper.readTree(createResponseJson);
        String createdId = createNode.get("reading").get("id").asText();
        
        // 2. READ
        Response readResponse = target("/readings/" + createdId)
                .request()
                .get();

        assertEquals(200, readResponse.getStatus());
        
        String readResponseJson = readResponse.readEntity(String.class);
        JsonNode readNode = objectMapper.readTree(readResponseJson);
        assertEquals("METER-CRUD-TEST", readNode.get("reading").get("meterId").asText());
        
        // 3. UPDATE
        newReading.setId(UUID.fromString(createdId));
        newReading.setMeterCount(6000.0);
        newReading.setComment("Updated CRUD test reading");
        
        ReadingRequest updateRequest = new ReadingRequest(newReading);
        String updateJson = objectMapper.writeValueAsString(updateRequest);

        Response updateResponse = target("/readings")
                .request()
                .put(jakarta.ws.rs.client.Entity.entity(updateJson, "application/json"));

        assertEquals(200, updateResponse.getStatus());
        
        String updateResponseJson = updateResponse.readEntity(String.class);
        JsonNode updateNode = objectMapper.readTree(updateResponseJson);
        assertEquals(6000.0, updateNode.get("reading").get("meterCount").asDouble());
        assertEquals("Updated CRUD test reading", updateNode.get("reading").get("comment").asText());
        
        // 4. DELETE
        Response deleteResponse = target("/readings/" + createdId)
                .request()
                .delete();

        assertEquals(200, deleteResponse.getStatus());
        
        // 5. Verify deletion
        Response verifyResponse = target("/readings/" + createdId)
                .request()
                .get();

        assertEquals(404, verifyResponse.getStatus());
    }

    @Test
    public void testConcurrentOperations() throws Exception {
        // Test handling of multiple operations (simulated concurrency)
        
        // Create multiple readings quickly
        for (int i = 0; i < 5; i++) {
            Reading reading = new Reading();
            reading.setCustomer(testCustomer);
            reading.setDateOfReading(LocalDate.of(2024, 8, i + 1));
            reading.setMeterId("METER-CONCURRENT-" + i);
            reading.setSubstitute(i % 2 == 0);
            reading.setMeterCount(1000.0 + i * 100);
            reading.setKindOfMeter(IReading.KindOfMeter.STROM);
            reading.setComment("Concurrent test " + i);

            ReadingRequest request = new ReadingRequest(reading);
            String jsonRequest = objectMapper.writeValueAsString(request);

            Response response = target("/readings")
                    .request()
                    .post(jakarta.ws.rs.client.Entity.entity(jsonRequest, "application/json"));

            assertEquals(201, response.getStatus(), "Failed to create concurrent reading " + i);
        }

        // Verify all readings were created
        Response getAllResponse = target("/readings")
                .queryParam("start", "2024-08-01")
                .queryParam("end", "2024-08-31")
                .request()
                .get();

        assertEquals(200, getAllResponse.getStatus());
        
        String jsonResponse = getAllResponse.readEntity(String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        
        JsonNode readingsArray = jsonNode.get("readings");
        int concurrentReadingsCount = 0;
        
        for (JsonNode reading : readingsArray) {
            if (reading.get("meterId").asText().startsWith("METER-CONCURRENT-")) {
                concurrentReadingsCount++;
            }
        }
        
        assertEquals(5, concurrentReadingsCount, "Should have created all 5 concurrent readings");
    }

    @Test
    public void testErrorHandling_MalformedJson() {
        // Test various malformed JSON scenarios
        String[] malformedJsons = {
            "{\"reading\": {\"customer\": null}}",
            "{\"reading\": {\"dateOfReading\": \"invalid-date\"}}",
            "{\"reading\": {\"meterCount\": \"not-a-number\"}}",
            "{\"reading\": {\"substitute\": \"not-boolean\"}}",
            "{\"reading\": {\"kindOfMeter\": \"INVALID_TYPE\"}}",
            "{\"reading\": {\"id\": \"not-a-uuid\"}}"
        };

        for (String malformedJson : malformedJsons) {
            Response response = target("/readings")
                    .request()
                    .post(jakarta.ws.rs.client.Entity.entity(malformedJson, "application/json"));

            assertEquals(400, response.getStatus(), "Should return 400 for malformed JSON: " + malformedJson);
        }
    }

    @Test
    public void testGetReadings_EmptyFilters() {
        // Test behavior with empty filter values
        Response response = target("/readings")
                .queryParam("customer", "")
                .queryParam("start", "")
                .queryParam("end", "")
                .queryParam("kindOfMeter", "")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("readings"), "Should return readings array even with empty filters");
    }

    @Test
    public void testGetReadings_WhitespaceFilters() {
        // Test behavior with whitespace-only filter values
        Response response = target("/readings")
                .queryParam("customer", "   ")
                .queryParam("start", "\t")
                .queryParam("end", "\n")
                .queryParam("kindOfMeter", " ")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        
        String jsonResponse = response.readEntity(String.class);
        assertTrue(jsonResponse.contains("readings"), "Should handle whitespace-only filters gracefully");
    }
} 