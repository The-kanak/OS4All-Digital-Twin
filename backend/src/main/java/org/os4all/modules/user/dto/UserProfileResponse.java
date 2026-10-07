package org.os4all.modules.user.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class UserProfileResponse {

    private UUID userId;
    private String email;
    private String fullName;
    private String role;
    private LocalDate dateOfBirth;
    private String biologicalSex;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private String bloodType;
    private String lifestyleNotes;

    public UserProfileResponse() {
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getBiologicalSex() {
        return biologicalSex;
    }

    public void setBiologicalSex(String biologicalSex) {
        this.biologicalSex = biologicalSex;
    }

    public BigDecimal getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(BigDecimal heightCm) {
        this.heightCm = heightCm;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public String getLifestyleNotes() {
        return lifestyleNotes;
    }

    public void setLifestyleNotes(String lifestyleNotes) {
        this.lifestyleNotes = lifestyleNotes;
    }
}
