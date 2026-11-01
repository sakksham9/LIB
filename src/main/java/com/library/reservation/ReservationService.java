package com.library.reservation;

import com.library.book.Book;
import com.library.book.BookRepository;
import com.library.borrow.BorrowService;
import com.library.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ReservationService {

    private final BookReservationRepository reservationRepo;
    private final BookRepository bookRepo;
    private final BorrowService borrowService;

    public ReservationService(
            BookReservationRepository reservationRepo,
            BookRepository bookRepo,
            BorrowService borrowService) {
        this.reservationRepo = reservationRepo;
        this.bookRepo = bookRepo;
        this.borrowService = borrowService;
    }

    @Transactional
    public BookReservation reserveBook(Long bookId, User student, String notes) {
        if (!student.isActive()) {
            throw new IllegalStateException("Your account is deactivated. You cannot reserve books.");
        }

        // Check if student has unpaid fines
        borrowService.validateNoUnpaidFines(student.getFullName(), student.getStudentIdNumber());

        Book book = bookRepo.findById(bookId)
                .orElseThrow(() -> new NoSuchElementException("Book not found with ID: " + bookId));

        // Check if already reserved
        if (reservationRepo.hasActiveReservation(student.getId(), bookId)) {
            throw new IllegalStateException("You already have an active reservation for: " + book.getTitle());
        }

        BookReservation res = new BookReservation();
        res.setBookId(book.getId());
        res.setBookTitle(book.getTitle());
        res.setStudentId(student.getId());
        res.setStudentName(student.getFullName());
        res.setStudentContact(student.getStudentIdNumber() != null ? student.getStudentIdNumber() : student.getEmail());
        res.setReservedAt(LocalDateTime.now());
        res.setExpiryDate(LocalDate.now().plusDays(7));
        res.setStatus("PENDING");
        res.setNotes(notes);

        return reservationRepo.save(res);
    }

    public List<BookReservation> getStudentReservations(Long studentId) {
        return reservationRepo.findByStudentIdOrderByReservedAtDesc(studentId);
    }

    @Transactional
    public BookReservation cancelStudentReservation(Long reservationId, Long studentId) {
        BookReservation res = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found with ID: " + reservationId));

        if (!res.getStudentId().equals(studentId)) {
            throw new IllegalStateException("Access Denied: You cannot cancel another student's reservation.");
        }

        if ("CANCELLED".equalsIgnoreCase(res.getStatus()) || "FULFILLED".equalsIgnoreCase(res.getStatus())) {
            throw new IllegalStateException("Reservation has already been " + res.getStatus().toLowerCase() + ".");
        }

        res.setStatus("CANCELLED");
        return reservationRepo.save(res);
    }

    public List<BookReservation> getAllReservations() {
        return reservationRepo.findAllByOrderByReservedAtDesc();
    }

    @Transactional
    public BookReservation updateStatus(Long id, String status) {
        BookReservation res = reservationRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found with ID: " + id));

        res.setStatus(status.toUpperCase());
        return reservationRepo.save(res);
    }
}
