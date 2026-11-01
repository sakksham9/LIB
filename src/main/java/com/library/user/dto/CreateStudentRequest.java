package com.library.user.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateStudentRequest {

    @NotBlank(message = "Student ID / Roll Number is required")
    private String studentIdNumber;

    @NotBlank(message = "Student Full Name is required")
    private String fullName;

    private String email;

    private String phone;

    private String department;

    private String username;

    private String password;

    public CreateStudentRequest() {}

    public String getStudentIdNumber() { return studentIdNumber; }
    public void setStudentIdNumber(String studentIdNumber) { this.studentIdNumber = studentIdNumber; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
