package com.library.borrow;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "borrow_records", indexes = {
    @Index(name = "idx_borrow_book_id", columnList = "bookId"),
    @Index(name = "idx_borrow_status", columnList = "status"),
    @Index(name = "idx_borrow_borrower", columnList = "borrowerName"),
    @Index(name = "idx_borrow_student_id", columnList = "student_id")
})
public class BorrowRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Long bookId;

    @NotBlank
    @Column(nullable = false)
    private String bookTitle;

    @Column(name = "student_id")
    private Long studentId;

    @NotBlank
    @Column(nullable = false)
    private String borrowerName;

    private String borrowerContact;

    @Column(nullable = false)
    private LocalDateTime borrowedAt = LocalDateTime.now();

    private LocalDate dueDate;

    private LocalDateTime returnedAt;

    @Column(nullable = false)
    private String status = "BORROWED"; // "BORROWED", "RETURNED", or "LOST"

    private Double fineAmount = 0.0; // In INR (₹)
    private Integer daysOverdue = 0;
    private Boolean finePaid = false;

    private LocalDateTime lastRemindedAt;
    private Integer reminderCount = 0;

    public BorrowRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }

    public String getBorrowerContact() { return borrowerContact; }
    public void setBorrowerContact(String borrowerContact) { this.borrowerContact = borrowerContact; }

    public LocalDateTime getBorrowedAt() { return borrowedAt; }
    public void setBorrowedAt(LocalDateTime borrowedAt) { this.borrowedAt = borrowedAt; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getFineAmount() { return fineAmount; }
    public void setFineAmount(Double fineAmount) { this.fineAmount = fineAmount; }

    public Integer getDaysOverdue() { return daysOverdue; }
    public void setDaysOverdue(Integer daysOverdue) { this.daysOverdue = daysOverdue; }

    public Boolean getFinePaid() { return finePaid; }
    public void setFinePaid(Boolean finePaid) { this.finePaid = finePaid; }

    public LocalDateTime getLastRemindedAt() { return lastRemindedAt; }
    public void setLastRemindedAt(LocalDateTime lastRemindedAt) { this.lastRemindedAt = lastRemindedAt; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Integer getReminderCount() { return reminderCount != null ? reminderCount : 0; }
    public void setReminderCount(Integer reminderCount) { this.reminderCount = reminderCount; }
}
