package de.fentacore.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.fentacore.interfaces.ICustomer;

import java.time.LocalDate;
import java.util.UUID;

public class Customer implements ICustomer {
    @JsonProperty("id")
    private UUID id;
    
    @JsonProperty("firstName")
    private String firstName;
    
    @JsonProperty("lastName")
    private String lastName;
    
    @JsonProperty("birthDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
    
    @JsonProperty("gender")
    private Gender gender;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public LocalDate getBirthDate() {
        return birthDate;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public Gender getGender() {
        return gender;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    @Override
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Override
    public void setGender(Gender gender) {
        this.gender = gender;
    }

    @Override
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
