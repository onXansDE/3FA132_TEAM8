package de.fentacore.model;

import de.fentacore.interfaces.IReading;
import de.fentacore.interfaces.ICustomer;

import java.time.LocalDate;
import java.util.UUID;

public class Reading implements IReading {
    private UUID id;
    private String comment;
    private ICustomer customer;
    private LocalDate dateOfReading;
    private KindOfMeter kindOfMeter;
    private Double meterCount;
    private String meterId;
    private Boolean substitute;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public ICustomer getCustomer() {
        return customer;
    }

    @Override
    public LocalDate getDateOfReading() {
        return dateOfReading;
    }

    @Override
    public KindOfMeter getKindOfMeter() {
        return kindOfMeter;
    }

    @Override
    public Double getMeterCount() {
        return meterCount;
    }

    @Override
    public String getMeterId() {
        return meterId;
    }

    @Override
    public Boolean getSubstitute() {
        return substitute;
    }

    @Override
    public String printDateOfReading() {
        return dateOfReading != null ? dateOfReading.toString() : "";
    }

    @Override
    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public void setCustomer(ICustomer customer) {
        this.customer = customer;
    }

    @Override
    public void setDateOfReading(LocalDate dateOfReading) {
        this.dateOfReading = dateOfReading;
    }

    @Override
    public void setKindOfMeter(KindOfMeter kindOfMeter) {
        this.kindOfMeter = kindOfMeter;
    }

    @Override
    public void setMeterCount(Double meterCount) {
        this.meterCount = meterCount;
    }

    @Override
    public void setMeterId(String meterId) {
        this.meterId = meterId;
    }

    @Override
    public void setSubstitute(Boolean substitute) {
        this.substitute = substitute;
    }
}
