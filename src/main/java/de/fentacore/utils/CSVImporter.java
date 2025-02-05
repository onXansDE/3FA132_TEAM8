package de.fentacore.utils;

import de.fentacore.dao.CustomerDAO;
import de.fentacore.dao.ReadingDAO;
import de.fentacore.interfaces.ICustomer;
import de.fentacore.interfaces.IReading;
import de.fentacore.model.Customer;
import de.fentacore.model.Reading;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

public class CSVImporter {

    private CustomerDAO customerDAO = new CustomerDAO();
    private ReadingDAO readingDAO = new ReadingDAO();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * Import all CSV files from the specified resources path.
     * @param resourcePath the folder under resources where csv files are stored (e.g. "data")
     */
    public void importAll(String resourcePath) {
        // Example: import customers first
        importCustomers(resourcePath + "/kunden_utf8.csv");

        // Import heizung, strom, wasser readings
        importReadings(resourcePath + "/heizung.csv");
        importReadings(resourcePath + "/strom.csv");
        importReadings(resourcePath + "/wasser.csv");
        // Add more if needed.
    }

    public void importCustomers(String csvPath) {
        try (BufferedReader reader = getResourceReader(csvPath)) {
            String line = reader.readLine(); // read header
            // expect "UUID,Anrede,Vorname,Nachname,Geburtsdatum"

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Split by comma
                String[] parts = line.split(",");
                if (parts.length < 4) {
                    // Not enough columns (some lines may have no birthdate)
                    continue;
                }

                String uuidStr = parts[0].trim();
                String anrede = parts[1].trim();
                String vorname = parts[2].trim();
                String nachname = parts[3].trim();
                String geburtsdatum = (parts.length > 4) ? parts[4].trim() : "";

                Customer c = new Customer();
                c.setId(UUID.fromString(uuidStr));
                c.setFirstName(vorname);
                c.setLastName(nachname);
                c.setGender(mapAnredeToGender(anrede));
                c.setBirthDate(parseDateOrNull(geburtsdatum));

                customerDAO.create(c);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void importReadings(String csvPath) {



        try (BufferedReader reader = getResourceReader(csvPath)) {
            String fileName = csvPath.toLowerCase();
            IReading.KindOfMeter kindOfMeter = determineMeterKindFromFilename(fileName);

            UUID customerId = null;
            String meterId = null;

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.equals(";;")) continue;

                // "Kunde";"ec617965-88b4-4721-8158-ee36c38e4db3";
                // "Zählernummer";"Xr-2018-2312456ab";
                if (line.startsWith("\"Kunde\"") || line.startsWith("Kunde")) {
                    String[] parts = line.split(";");
                    if (parts.length > 1) {
                        customerId = extractUuid(parts[1]);
                    }
                } else if (line.startsWith("\"Zählernummer\"") || line.startsWith("Zählernummer")) {
                    String[] parts = line.split(";");
                    if (parts.length > 1) {
                        meterId = stripQuotes(parts[1]);
                    }
                } else if (line.contains("Datum") && line.contains("Zählerstand")) {
                    // This is the header line for readings
                    break;
                }
            }

            if (customerId == null || meterId == null) {
                System.out.println("Could not extract customerId or meterId from file: " + csvPath);
                return;
            }


            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(";");
                if (parts.length < 2) {
                    continue;
                }

                String dateStr = stripQuotes(parts[0].trim());
                String readingStr = parts.length > 1 ? parts[1].trim() : "";
                String comment = parts.length > 2 ? stripQuotes(parts[2].trim()) : "";

                if (dateStr.isEmpty() || readingStr.isEmpty()) {
                    continue;
                }

                LocalDate date = parseDateOrNull(dateStr);
                Double meterCount = parseDoubleWithComma(readingStr);

                Reading r = new Reading();
                r.setId(UUID.randomUUID());
                r.setCustomer(customerDAO.findById(customerId));
                r.setMeterId(meterId);
                r.setKindOfMeter(kindOfMeter);
                r.setDateOfReading(date);
                r.setMeterCount(meterCount);
                r.setComment(comment);
                r.setSubstitute(false); // Not indicated otherwise

                readingDAO.create(r);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BufferedReader getResourceReader(String resourcePath) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new RuntimeException("Resource not found: " + resourcePath);
        }
        return new BufferedReader(new InputStreamReader(is));
    }

    private ICustomer.Gender mapAnredeToGender(String anrede) {
        anrede = anrede.toLowerCase();
        if (anrede.equals("herr")) return ICustomer.Gender.M;
        if (anrede.equals("frau")) return ICustomer.Gender.W;
        if (anrede.equals("divers")) return ICustomer.Gender.D;
        return ICustomer.Gender.U;
    }

    private LocalDate parseDateOrNull(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private Double parseDoubleWithComma(String numberStr) {
        if (numberStr == null || numberStr.isEmpty()) return null;
        // Replace comma with dot
        numberStr = numberStr.replace(',', '.');
        try {
            return Double.parseDouble(numberStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private UUID extractUuid(String str) {
        str = stripQuotes(str.trim());
        return UUID.fromString(str);
    }

    private String stripQuotes(String str) {
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length()-1);
        }
        return str;
    }

    private IReading.KindOfMeter determineMeterKindFromFilename(String filename) {
        if (filename.contains("heizung")) return IReading.KindOfMeter.HEIZUNG;
        if (filename.contains("strom")) return IReading.KindOfMeter.STROM;
        if (filename.contains("wasser")) return IReading.KindOfMeter.WASSER;
        return IReading.KindOfMeter.UNBEKANNT;
    }

}
