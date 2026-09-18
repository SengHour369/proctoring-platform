package com.example.identityservice.user.dto;

/**
 * Input to {@code update*}. Every field is optional — {@code null} means "leave it alone", which
 * is why this is a mutable holder rather than a record: a client submits only the fields it wants
 * changed, and Jackson leaves the rest {@code null} on deserialization.
 */
public class UpdateUserCommand {

    private String fullName;
    private String phoneNumber;
    private String timeZone;
    private String locale;
    private String enrolmentPhotoPath;
    private String voiceprintPath;
    private String email;
    private String externalRef;
    private String reason;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getEnrolmentPhotoPath() {
        return enrolmentPhotoPath;
    }

    public void setEnrolmentPhotoPath(String enrolmentPhotoPath) {
        this.enrolmentPhotoPath = enrolmentPhotoPath;
    }

    public String getVoiceprintPath() {
        return voiceprintPath;
    }

    public void setVoiceprintPath(String voiceprintPath) {
        this.voiceprintPath = voiceprintPath;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public void setExternalRef(String externalRef) {
        this.externalRef = externalRef;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
