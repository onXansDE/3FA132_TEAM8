package de.fentacore.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReadingRequest {
    @JsonProperty("reading")
    private Reading reading;

    public ReadingRequest() {
    }

    public ReadingRequest(Reading reading) {
        this.reading = reading;
    }

    public Reading getReading() {
        return reading;
    }

    public void setReading(Reading reading) {
        this.reading = reading;
    }
} 