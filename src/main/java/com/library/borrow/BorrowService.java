package com.library.borrow;

import com.library.book.Book;
import com.library.book.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class BorrowService {

    public static final double DAILY_FINE_RATE_INR = 10.0; // ₹10 per day overdue
    public static final double LOST_BOOK_FINE_INR = 500.0; // ₹500 replacement fine for lost book

    private final BorrowRecordRepository borrowRepo;
    private final BookRepository bookRepo;

    public BorrowService(BorrowRecordRepository borrowRepo, BookRepository bookRepo) {
        this.borrowRepo = borrowRepo;
        this.bookRepo = bookRepo;
    }

    public void calculateFine(BorrowRecord record) {
        if (record == null)
            return;

        // If a book is reported as LOST, its fine includes the replacement fee; don't
        // overwrite
        if ("LOST".equalsIgnoreCase(record.getStatus())) {
            return;
        }

        if (record.getDueDate() == null)
            return;

        LocalDate returnOrCurrentDate;
        if ("RETURNED".equalsIgnoreCase(record.getStatus())) {
            returnOrCurrentDate = (record.getReturnedAt() != null)
                    ? record.getReturnedAt().toLocalDate()
                    : LocalDate.now();
        } else {
            returnOrCurrentDate = LocalDate.now();
        }

        if (returnOrCurrentDate.isAfter(record.getDueDate())) {
            long days = ChronoUnit.DAYS.between(record.getDueDate(), returnOrCurrentDate);
            record.setDaysOverdue((int) days);
            record.setFineAmount(days * DAILY_FINE_RATE_INR);
        } else {
            record.setDaysOverdue(0);
            record.setFineAmount(0.0);
        }
    }

    public double getPendingFineForBorrower(String borrowerName, String borrowerContact) {
        return getPendingFineForStudent(null, borrowerName, borrowerContact);
    }

    public double getPendingFineForStudent(Long studentId, String borrowerName, String borrowerContact) {
        String contact = (borrowerContact != null) ? borrowerContact.trim() : "";
        String name = (borrowerName != null) ? borrowerName.trim() : "";
        List<BorrowRecord> history;
        if (studentId != null) {
            history = borrowRepo.findByStudentOrDetails(studentId, name, contact);
        } else {
            history = borrowRepo.findByBorrower(name, contact);
        }
        double totalFine = 0.0;

        for (BorrowRecord r : history) {
            calculateFine(r);
            boolean isPaid = Boolean.TRUE.equals(r.getFinePaid());
            double fine = (r.getFineAmount() != null) ? r.getFineAmount() : 0.0;
            if (!isPaid && fine > 0.0) {
                totalFine += fine;
            }
        }
        return totalFine;
    }

    public void validateNoUnpaidFines(String borrowerName, String borrowerContact) {
        validateNoUnpaidFines(null, borrowerName, borrowerContact);
    }

    public void validateNoUnpaidFines(Long studentId, String borrowerName, String borrowerContact) {
        double totalFine = getPendingFineForStudent(studentId, borrowerName, borrowerContact);
        if (totalFine > 0.0) {
            throw new IllegalStateException(String.format(
                    "Borrowing Blocked: Borrower '%s' has an outstanding unpaid fine of ₹%.0f (late return or lost book fee). No new book can be issued until all fines are cleared.",
                    borrowerName != null ? borrowerName.trim() : "Student", totalFine));
        }
    }

    @Transactional
    public BorrowRecord borrowBook(BorrowRequest req) {
        // Enforce rule: No new book will be issued till the fine is cleared
        validateNoUnpaidFines(req.getStudentId(), req.getBorrowerName(), req.getBorrowerContact());

        Book book = bookRepo.findById(req.getBookId())
                .orElseThrow(() -> new NoSuchElementException("Book not found with ID: " + req.getBookId()));

        int copies = book.getAvailableCopies() != null ? book.getAvailableCopies() : 0;
        if (copies <= 0) {
            throw new IllegalStateException("No available copies to borrow for: " + book.getTitle());
        }

        // Decrement book stock
        book.setAvailableCopies(copies - 1);
        bookRepo.save(book);

        // Create transaction record
        BorrowRecord record = new BorrowRecord();
        record.setBookId(book.getId());
        record.setBookTitle(book.getTitle());
        record.setStudentId(req.getStudentId());
        record.setBorrowerName(req.getBorrowerName() != null ? req.getBorrowerName().trim() : "Unknown Borrower");
        record.setBorrowerContact(req.getBorrowerContact() != null ? req.getBorrowerContact().trim() : "");
        record.setBorrowedAt(LocalDateTime.now());

        int durationDays = (req.getDays() != null && req.getDays() > 0) ? req.getDays() : 14;
        record.setDueDate(LocalDate.now().plusDays(durationDays));
        record.setStatus("BORROWED");
        record.setFineAmount(0.0);
        record.setDaysOverdue(0);
        record.setFinePaid(false);

        return borrowRepo.save(record);
    }

    @Transactional
    public BorrowRecord returnRecord(Long recordId) {
        BorrowRecord record = borrowRepo.findById(recordId)
                .orElseThrow(() -> new NoSuchElementException("Borrow record not found with ID: " + recordId));

        if ("RETURNED".equalsIgnoreCase(record.getStatus())) {
            throw new IllegalStateException("This book copy has already been returned.");
        }
        if ("LOST".equalsIgnoreCase(record.getStatus())) {
            throw new IllegalStateException("This book copy was reported as lost and cannot be returned normally.");
        }

        record.setStatus("RETURNED");
        record.setReturnedAt(LocalDateTime.now());
        calculateFine(record);

        // If no fine is due, mark finePaid as true automatically
        if (record.getFineAmount() == null || record.getFineAmount() <= 0.0) {
            record.setFinePaid(true);
        }

        // Increment book stock
        bookRepo.findById(record.getBookId()).ifPresent(b -> {
            int current = b.getAvailableCopies() != null ? b.getAvailableCopies() : 0;
            b.setAvailableCopies(current + 1);
            bookRepo.save(b);
        });

        return borrowRepo.save(record);
    }

    @Transactional
    public BorrowRecord reportLost(Long recordId) {
        BorrowRecord record = borrowRepo.findById(recordId)
                .orElseThrow(() -> new NoSuchElementException("Borrow record not found with ID: " + recordId));

        if ("RETURNED".equalsIgnoreCase(record.getStatus())) {
            throw new IllegalStateException("This book was already returned and cannot be reported lost.");
        }
        if ("LOST".equalsIgnoreCase(record.getStatus())) {
            throw new IllegalStateException("This book has already been reported as lost.");
        }

        // Calculate any late fine accrued up to today
        calculateFine(record);
        double accruedLateFine = (record.getFineAmount() != null) ? record.getFineAmount() : 0.0;
        double totalFine = accruedLateFine + LOST_BOOK_FINE_INR;

        record.setStatus("LOST");
        record.setReturnedAt(LocalDateTime.now());
        record.setFineAmount(totalFine);
        record.setFinePaid(false); // Must be cleared before borrower can take new books!

        // Note: Do NOT increment book.availableCopies, because the book is permanently
        // lost

        return borrowRepo.save(record);
    }

    @Transactional
    public BorrowRecord payFine(Long recordId) {
        BorrowRecord record = borrowRepo.findById(recordId)
                .orElseThrow(() -> new NoSuchElementException("Borrow record not found with ID: " + recordId));

        calculateFine(record);
        record.setFinePaid(true);
        return borrowRepo.save(record);
    }

    @Transactional
    public Map<String, Object> sendReminder(Long recordId) {
        BorrowRecord record = borrowRepo.findById(recordId)
                .orElseThrow(() -> new NoSuchElementException("Borrow record not found with ID: " + recordId));

        calculateFine(record);
        record.setLastRemindedAt(LocalDateTime.now());
        record.setReminderCount(record.getReminderCount() + 1);
        borrowRepo.save(record);

        String message;
        double fine = record.getFineAmount() != null ? record.getFineAmount() : 0.0;
        int overdue = record.getDaysOverdue() != null ? record.getDaysOverdue() : 0;

        if ("LOST".equalsIgnoreCase(record.getStatus())) {
            message = String.format(
                    "URGENT: Your borrowed book '%s' is marked as LOST. Outstanding penalty fine: ₹%.0f. Please clear this fine to unblock your borrowing account.",
                    record.getBookTitle(), fine);
        } else if (overdue > 0) {
            message = String.format(
                    "OVERDUE NOTICE: '%s' was due on %s and is now %d day(s) overdue. Current late fine: ₹%.0f (₹10/day). Please return the book and clear your fine.",
                    record.getBookTitle(), record.getDueDate(), overdue, fine);
        } else {
            message = String.format(
                    "FRIENDLY REMINDER: Your book '%s' is due on %s. Please return it on time to avoid late fees (₹10/day).",
                    record.getBookTitle(), record.getDueDate());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recordId", record.getId());
        result.put("borrowerName", record.getBorrowerName());
        result.put("borrowerContact", record.getBorrowerContact() != null ? record.getBorrowerContact() : "");
        result.put("bookTitle", record.getBookTitle());
        result.put("dueDate", record.getDueDate() != null ? record.getDueDate().toString() : "");
        result.put("daysOverdue", overdue);
        result.put("fineAmount", fine);
        result.put("status", record.getStatus());
        result.put("reminderCount", record.getReminderCount());
        result.put("lastRemindedAt", record.getLastRemindedAt() != null ? record.getLastRemindedAt().toString() : "");
        result.put("message", message);
        return result;
    }

    private Map<String, Object> buildAlertMap(BorrowRecord r, String type, double fine, int overdue) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("recordId", r.getId());
        map.put("borrowerName", r.getBorrowerName());
        map.put("borrowerContact", r.getBorrowerContact() != null ? r.getBorrowerContact() : "");
        map.put("bookTitle", r.getBookTitle());
        map.put("type", type);
        map.put("fineAmount", fine);
        map.put("daysOverdue", overdue);
        map.put("dueDate", r.getDueDate() != null ? r.getDueDate().toString() : "");
        map.put("status", r.getStatus());
        map.put("reminderCount", r.getReminderCount());
        map.put("lastRemindedAt", r.getLastRemindedAt() != null ? r.getLastRemindedAt().toString() : "");
        return map;
    }

    public Map<String, Object> getNotificationSummary() {
        List<BorrowRecord> all = borrowRepo.findAllByOrderByBorrowedAtDesc();
        all.forEach(this::calculateFine);

        LocalDate today = LocalDate.now();
        LocalDate soon = today.plusDays(3);

        int overdueCount = 0;
        int dueSoonCount = 0;
        int lostCount = 0;
        double totalUnpaidFines = 0.0;

        List<Map<String, Object>> alerts = new ArrayList<>();

        for (BorrowRecord r : all) {
            double fine = r.getFineAmount() != null ? r.getFineAmount() : 0.0;
            boolean isPaid = Boolean.TRUE.equals(r.getFinePaid());
            if (!isPaid && fine > 0.0) {
                totalUnpaidFines += fine;
            }

            if ("LOST".equalsIgnoreCase(r.getStatus()) && !isPaid) {
                lostCount++;
                alerts.add(buildAlertMap(r, "LOST_PENALTY", fine, r.getDaysOverdue() != null ? r.getDaysOverdue() : 0));
            } else if ("BORROWED".equalsIgnoreCase(r.getStatus())) {
                if (r.getDueDate() != null && today.isAfter(r.getDueDate())) {
                    overdueCount++;
                    alerts.add(buildAlertMap(r, "OVERDUE", fine, r.getDaysOverdue() != null ? r.getDaysOverdue() : 0));
                } else if (r.getDueDate() != null && !r.getDueDate().isAfter(soon)) {
                    dueSoonCount++;
                    alerts.add(buildAlertMap(r, "DUE_SOON", 0.0, 0));
                }
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalUnpaidFines", totalUnpaidFines);
        summary.put("overdueCount", overdueCount);
        summary.put("dueSoonCount", dueSoonCount);
        summary.put("lostCount", lostCount);
        summary.put("totalAlertsCount", alerts.size());
        summary.put("alerts", alerts);
        return summary;
    }

    public Map<String, Object> checkBorrowerFineStatus(String name, String contact) {
        double fine = getPendingFineForBorrower(name, contact);
        return Map.of(
                "borrowerName", name,
                "unpaidFine", fine,
                "canBorrow", fine <= 0.0);
    }

    public List<BorrowRecord> getAll(String status, Long bookId) {
        List<BorrowRecord> list;
        if (bookId != null && status != null && !status.isBlank()) {
            list = borrowRepo.findByBookIdAndStatus(bookId, status.toUpperCase());
        } else if (bookId != null) {
            list = borrowRepo.findByBookIdOrderByBorrowedAtDesc(bookId);
        } else if (status != null && !status.isBlank()) {
            list = borrowRepo.findByStatusOrderByBorrowedAtDesc(status.toUpperCase());
        } else {
            list = borrowRepo.findAllByOrderByBorrowedAtDesc();
        }

        list.forEach(this::calculateFine);
        return list;
    }

    public List<BorrowRecord> getForBook(Long bookId) {
        List<BorrowRecord> list = borrowRepo.findByBookIdOrderByBorrowedAtDesc(bookId);
        list.forEach(this::calculateFine);
        return list;
    }

    @Transactional
    public BorrowRecord borrowBookForStudent(Long bookId, Long studentId, String studentName, String studentContact, Integer days) {
        double totalFine = getPendingFineForStudent(studentId, studentName, studentContact);
        if (totalFine > 0.0) {
            throw new IllegalStateException(String.format(
                    "Borrowing Blocked: You have an outstanding unpaid fine of ₹%.0f. Please clear all pending fines before borrowing new books.",
                    totalFine));
        }

        Book book = bookRepo.findById(bookId)
                .orElseThrow(() -> new NoSuchElementException("Book not found with ID: " + bookId));

        int copies = book.getAvailableCopies() != null ? book.getAvailableCopies() : 0;
        if (copies <= 0) {
            throw new IllegalStateException("No available copies to borrow for: " + book.getTitle() + ". You can reserve it instead!");
        }

        book.setAvailableCopies(copies - 1);
        bookRepo.save(book);

        BorrowRecord record = new BorrowRecord();
        record.setBookId(book.getId());
        record.setBookTitle(book.getTitle());
        record.setStudentId(studentId);
        record.setBorrowerName(studentName);
        record.setBorrowerContact(studentContact != null ? studentContact : "");
        record.setBorrowedAt(LocalDateTime.now());

        int durationDays = (days != null && days > 0) ? days : 14;
        record.setDueDate(LocalDate.now().plusDays(durationDays));
        record.setStatus("BORROWED");
        record.setFineAmount(0.0);
        record.setDaysOverdue(0);
        record.setFinePaid(false);

        return borrowRepo.save(record);
    }

    public List<BorrowRecord> getStudentActiveLoans(Long studentId) {
        List<BorrowRecord> list = borrowRepo.findByStudentIdAndStatusOrderByBorrowedAtDesc(studentId, "BORROWED");
        list.forEach(this::calculateFine);
        return list;
    }

    public List<BorrowRecord> getStudentHistory(Long studentId) {
        List<BorrowRecord> list = borrowRepo.findByStudentIdOrderByBorrowedAtDesc(studentId);
        list.forEach(this::calculateFine);
        return list;
    }

    @Transactional
    public BorrowRecord returnStudentBook(Long recordId, Long studentId) {
        BorrowRecord record = borrowRepo.findById(recordId)
                .orElseThrow(() -> new NoSuchElementException("Borrow record not found with ID: " + recordId));

        if (record.getStudentId() != null && !record.getStudentId().equals(studentId)) {
            throw new IllegalStateException("Access Denied: You cannot return another student's loan.");
        }

        return returnRecord(recordId);
    }

    public Map<String, Object> getStudentDashboardSummary(Long studentId) {
        List<BorrowRecord> loans = getStudentActiveLoans(studentId);
        List<BorrowRecord> all = getStudentHistory(studentId);

        LocalDate today = LocalDate.now();
        LocalDate soon = today.plusDays(3);

        int overdueCount = 0;
        int dueSoonCount = 0;
        double totalFine = 0.0;

        for (BorrowRecord r : loans) {
            double fine = r.getFineAmount() != null ? r.getFineAmount() : 0.0;
            if (fine > 0) {
                totalFine += fine;
                overdueCount++;
            } else if (r.getDueDate() != null && !r.getDueDate().isAfter(soon)) {
                dueSoonCount++;
            }
        }

        for (BorrowRecord r : all) {
            if (!"BORROWED".equalsIgnoreCase(r.getStatus())) {
                if (!Boolean.TRUE.equals(r.getFinePaid()) && r.getFineAmount() != null && r.getFineAmount() > 0) {
                    totalFine += r.getFineAmount();
                }
            }
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("studentId", studentId);
        map.put("activeLoansCount", loans.size());
        map.put("overdueLoansCount", overdueCount);
        map.put("dueSoonLoansCount", dueSoonCount);
        map.put("totalUnpaidFines", totalFine);
        map.put("totalBorrowHistoryCount", all.size());
        map.put("activeLoans", loans);
        return map;
    }
}
