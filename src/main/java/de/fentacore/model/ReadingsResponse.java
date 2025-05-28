package de.fentacore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.fentacore.interfaces.IReading;

import java.util.List;
import java.util.stream.Collectors;

public class ReadingsResponse {
    
    @JsonProperty("readings")
    private List<Reading> readings;
    
    public ReadingsResponse() {
    }
    
    public ReadingsResponse(List<IReading> readings) {
        this.readings = readings.stream()
                .map(reading -> (Reading) reading)
                .collect(Collectors.toList());
    }
    
    public List<Reading> getReadings() {
        return readings;
    }
    
    public void setReadings(List<Reading> readings) {
        this.readings = readings;
    }
} 