package com.library.user.dto;

import java.time.LocalDateTime;

public class StudentDto {

    private Long id;
    private String studentIdNumber;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String department;
    private String role;
    private String status;
    private LocalDateTime createdAt;
    private long activeLoansCount = 0;
    private long overdueLoansCount = 0;
    private double totalUnpaidFines = 0.0;
    private long reservationsCount = 0;

    public StudentDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentIdNumber() { return studentIdNumber; }
    public void setStudentIdNumber(String studentIdNumber) { this.studentIdNumber = studentIdNumber; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public long getActiveLoansCount() { return activeLoansCount; }
    public void setActiveLoansCount(long activeLoansCount) { this.activeLoansCount = activeLoansCount; }

    public long getOverdueLoansCount() { return overdueLoansCount; }
    public void setOverdueLoansCount(long overdueLoansCount) { this.overdueLoansCount = overdueLoansCount; }

    public double getTotalUnpaidFines() { return totalUnpaidFines; }
    public void setTotalUnpaidFines(double totalUnpaidFines) { this.totalUnpaidFines = totalUnpaidFines; }

    public long getReservationsCount() { return reservationsCount; }
    public void setReservationsCount(long reservationsCount) { this.reservationsCount = reservationsCount; }
}
