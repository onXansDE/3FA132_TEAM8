package de.fentacore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.fentacore.interfaces.ICustomer;
import de.fentacore.interfaces.IReading;

import java.util.List;
import java.util.stream.Collectors;

public class CustomerWithReadingsResponse {
    
    @JsonProperty("customer")
    private Customer customer;
    
    @JsonProperty("readings")
    private List<ReadingWithNullCustomer> readings;
    
    public CustomerWithReadingsResponse() {
    }
    
    public CustomerWithReadingsResponse(ICustomer customer, List<IReading> readings) {
        this.customer = (Customer) customer;
        this.readings = readings.stream()
                .map(reading -> new ReadingWithNullCustomer((Reading) reading))
                .collect(Collectors.toList());
    }
    
    public Customer getCustomer() {
        return customer;
    }
    
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    
    public List<ReadingWithNullCustomer> getReadings() {
        return readings;
    }
    
    public void setReadings(List<ReadingWithNullCustomer> readings) {
        this.readings = readings;
    }
    
    // Inner class for readings with null customer as per schema
    public static class ReadingWithNullCustomer {
        @JsonProperty("uuid")
        private String uuid;
        
        @JsonProperty("customer")
        private Object customer = null; // Always null as per schema
        
        @JsonProperty("dateOfReading")
        private String dateOfReading;
        
        @JsonProperty("comment")
        private String comment;
        
        @JsonProperty("meterId")
        private String meterId;
        
        @JsonProperty("substitute")
        private Boolean substitute;
        
        @JsonProperty("metercount")
        private Double metercount;
        
        @JsonProperty("kindOfMeter")
        private String kindOfMeter;
        
        public ReadingWithNullCustomer() {
        }
        
        public ReadingWithNullCustomer(Reading reading) {
            this.uuid = reading.getId() != null ? reading.getId().toString() : null;
            this.customer = null; // Always null as per schema requirement
            this.dateOfReading = reading.getDateOfReading() != null ? reading.getDateOfReading().toString() : null;
            this.comment = reading.getComment();
            this.meterId = reading.getMeterId();
            this.substitute = reading.getSubstitute();
            this.metercount = reading.getMeterCount();
            this.kindOfMeter = reading.getKindOfMeter() != null ? reading.getKindOfMeter().name() : null;
        }
        
        // Getters and setters
        public String getUuid() { return uuid; }
        public void setUuid(String uuid) { this.uuid = uuid; }
        
        public Object getCustomer() { return customer; }
        public void setCustomer(Object customer) { this.customer = customer; }
        
        public String getDateOfReading() { return dateOfReading; }
        public void setDateOfReading(String dateOfReading) { this.dateOfReading = dateOfReading; }
        
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        
        public String getMeterId() { return meterId; }
        public void setMeterId(String meterId) { this.meterId = meterId; }
        
        public Boolean getSubstitute() { return substitute; }
        public void setSubstitute(Boolean substitute) { this.substitute = substitute; }
        
        public Double getMetercount() { return metercount; }
        public void setMetercount(Double metercount) { this.metercount = metercount; }
        
        public String getKindOfMeter() { return kindOfMeter; }
        public void setKindOfMeter(String kindOfMeter) { this.kindOfMeter = kindOfMeter; }
    }
} 