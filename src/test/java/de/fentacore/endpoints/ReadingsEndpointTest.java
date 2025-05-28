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
} 