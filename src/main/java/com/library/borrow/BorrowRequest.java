package com.library.borrow;

import jakarta.validation.constraints.NotNull;

public class BorrowRequest {

    @NotNull
    private Long bookId;

    private Long studentId;

    private String borrowerName;

    private String borrowerContact;

    private Integer days = 14;

    public BorrowRequest() {}

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }

    public String getBorrowerContact() { return borrowerContact; }
    public void setBorrowerContact(String borrowerContact) { this.borrowerContact = borrowerContact; }

    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
}
