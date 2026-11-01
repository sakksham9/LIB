package com.library.reservation;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "book_reservations", indexes = {
    @Index(name = "idx_res_student", columnList = "studentId"),
    @Index(name = "idx_res_book", columnList = "bookId"),
    @Index(name = "idx_res_status", columnList = "status")
})
public class BookReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Long bookId;

    @Column(nullable = false)
    private String bookTitle;

    @NotNull
    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private String studentName;

    private String studentContact;

    @Column(nullable = false)
    private LocalDateTime reservedAt = LocalDateTime.now();

    private LocalDate expiryDate;

    @Column(nullable = false)
    private String status = "PENDING"; // "PENDING", "READY_FOR_PICKUP", "FULFILLED", "CANCELLED"

    private String notes;

    public BookReservation() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentContact() { return studentContact; }
    public void setStudentContact(String studentContact) { this.studentContact = studentContact; }

    public LocalDateTime getReservedAt() { return reservedAt; }
    public void setReservedAt(LocalDateTime reservedAt) { this.reservedAt = reservedAt; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
