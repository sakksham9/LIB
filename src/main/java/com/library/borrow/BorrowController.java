package com.library.borrow;

import com.library.security.AuthInterceptor;
import com.library.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api")
public class BorrowController {

    private final BorrowService service;

    public BorrowController(BorrowService service) {
        this.service = service;
    }

    @PostMapping("/borrows")
    @ResponseStatus(HttpStatus.CREATED)
    public BorrowRecord borrow(@Valid @RequestBody BorrowRequest req, HttpServletRequest request) {
        User currentUser = (User) request.getAttribute(AuthInterceptor.CURRENT_USER_ATTR);
        if (currentUser == null) {
            throw new IllegalStateException("Authentication required: Please sign in to borrow books.");
        }

        if (currentUser.isStudent()) {
            if (!currentUser.isActive()) {
                throw new IllegalStateException("Your account is deactivated. Please contact library staff.");
            }
            // Automatically link to authenticated student account
            req.setStudentId(currentUser.getId());
            req.setBorrowerName(currentUser.getFullName());
            req.setBorrowerContact(currentUser.getStudentIdNumber() != null ? currentUser.getStudentIdNumber() : currentUser.getEmail());
        } else if (currentUser.isAdmin()) {
            // Admin can optionally supply studentId or manual borrower info
            if (req.getBorrowerName() == null || req.getBorrowerName().isBlank()) {
                req.setBorrowerName("Admin Checkout");
            }
        }

        return service.borrowBook(req);
    }

    @PostMapping("/borrows/{id}/return")
    public BorrowRecord returnRecord(@PathVariable Long id, HttpServletRequest request) {
        User currentUser = (User) request.getAttribute(AuthInterceptor.CURRENT_USER_ATTR);
        if (currentUser != null && currentUser.isStudent()) {
            return service.returnStudentBook(id, currentUser.getId());
        }
        return service.returnRecord(id);
    }

    @PostMapping("/borrows/{id}/report-lost")
    public BorrowRecord reportLost(@PathVariable Long id) {
        return service.reportLost(id);
    }

    @PostMapping("/borrows/{id}/pay-fine")
    public BorrowRecord payFine(@PathVariable Long id) {
        return service.payFine(id);
    }

    @PostMapping("/notifications/remind/{id}")
    public Map<String, Object> sendReminder(@PathVariable Long id) {
        return service.sendReminder(id);
    }

    @GetMapping("/notifications/summary")
    public Map<String, Object> getNotificationSummary() {
        return service.getNotificationSummary();
    }

    @GetMapping("/borrows/check-fines")
    public Map<String, Object> checkFines(
            @RequestParam String name,
            @RequestParam(required = false, defaultValue = "") String contact
    ) {
        return service.checkBorrowerFineStatus(name, contact);
    }

    @GetMapping("/borrows")
    public List<BorrowRecord> all(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long bookId
    ) {
        return service.getAll(status, bookId);
    }

    @GetMapping("/books/{id}/borrows")
    public List<BorrowRecord> forBook(@PathVariable Long id) {
        return service.getForBook(id);
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
