package com.library.user.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateStudentRequest {

    @NotBlank(message = "Student Full Name is required")
    private String fullName;

    private String email;

    private String phone;

    private String department;

    private String password; // Optional new password

    public UpdateStudentRequest() {}

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
