package com.library.user;

import com.library.borrow.BorrowRecord;
import com.library.borrow.BorrowService;
import com.library.reservation.BookReservation;
import com.library.reservation.ReservationService;
import com.library.security.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final BorrowService borrowService;
    private final ReservationService reservationService;

    public StudentController(BorrowService borrowService, ReservationService reservationService) {
        this.borrowService = borrowService;
        this.reservationService = reservationService;
    }

    private User getStudent(HttpServletRequest request) {
        User user = (User) request.getAttribute(AuthInterceptor.CURRENT_USER_ATTR);
        if (user == null) {
            throw new IllegalStateException("Authentication required");
        }
        return user;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(HttpServletRequest request) {
        User student = getStudent(request);
        Map<String, Object> summary = borrowService.getStudentDashboardSummary(student.getId());
        List<BookReservation> reservations = reservationService.getStudentReservations(student.getId());
        summary.put("studentName", student.getFullName());
        summary.put("studentIdNumber", student.getStudentIdNumber());
        summary.put("department", student.getDepartment());
        summary.put("activeReservationsCount", reservations.stream().filter(r -> "PENDING".equalsIgnoreCase(r.getStatus()) || "READY_FOR_PICKUP".equalsIgnoreCase(r.getStatus())).count());
        summary.put("reservations", reservations);
        return summary;
    }

    @GetMapping("/loans")
    public List<BorrowRecord> activeLoans(HttpServletRequest request) {
        User student = getStudent(request);
        return borrowService.getStudentActiveLoans(student.getId());
    }

    @GetMapping("/history")
    public List<BorrowRecord> history(HttpServletRequest request) {
        User student = getStudent(request);
        return borrowService.getStudentHistory(student.getId());
    }

    @PostMapping("/borrow")
    @ResponseStatus(HttpStatus.CREATED)
    public BorrowRecord borrowBook(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User student = getStudent(request);
        Long bookId = Long.valueOf(String.valueOf(body.get("bookId")));
        Integer days = body.get("days") != null ? Integer.valueOf(String.valueOf(body.get("days"))) : 14;

        String contact = student.getStudentIdNumber() != null ? student.getStudentIdNumber() : student.getEmail();
        return borrowService.borrowBookForStudent(bookId, student.getId(), student.getFullName(), contact, days);
    }

    @PostMapping("/return/{id}")
    public BorrowRecord returnBook(@PathVariable Long id, HttpServletRequest request) {
        User student = getStudent(request);
        return borrowService.returnStudentBook(id, student.getId());
    }

    @GetMapping("/reservations")
    public List<BookReservation> getReservations(HttpServletRequest request) {
        User student = getStudent(request);
        return reservationService.getStudentReservations(student.getId());
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public BookReservation reserveBook(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User student = getStudent(request);
        Long bookId = Long.valueOf(String.valueOf(body.get("bookId")));
        String notes = body.get("notes") != null ? String.valueOf(body.get("notes")) : "";
        return reservationService.reserveBook(bookId, student, notes);
    }

    @PostMapping("/reservations/{id}/cancel")
    public BookReservation cancelReservation(@PathVariable Long id, HttpServletRequest request) {
        User student = getStudent(request);
        return reservationService.cancelStudentReservation(id, student.getId());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegal(Exception e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(Exception e) {
        return Map.of("error", e.getMessage());
    }
}
