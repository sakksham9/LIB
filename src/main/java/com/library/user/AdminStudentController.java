package com.library.user;

import com.library.borrow.BorrowRecord;
import com.library.borrow.BorrowService;
import com.library.user.dto.CreateStudentRequest;
import com.library.user.dto.StudentDto;
import com.library.user.dto.UpdateStudentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/admin/students")
public class AdminStudentController {

    private final UserService userService;
    private final BorrowService borrowService;

    public AdminStudentController(UserService userService, BorrowService borrowService) {
        this.userService = userService;
        this.borrowService = borrowService;
    }

    @GetMapping
    public List<StudentDto> all() {
        return userService.getAllStudents();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User create(@Valid @RequestBody CreateStudentRequest req) {
        return userService.registerStudent(req);
    }

    @GetMapping("/{id}")
    public StudentDto one(@PathVariable Long id) {
        return userService.getStudentDto(id);
    }

    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @Valid @RequestBody UpdateStudentRequest req) {
        return userService.updateStudent(id, req);
    }

    @PatchMapping("/{id}/status")
    public User toggleStatus(@PathVariable Long id) {
        return userService.toggleStudentStatus(id);
    }

    @PostMapping("/{id}/reset-password")
    public Map<String, String> resetPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password cannot be empty");
        }
        userService.resetStudentPassword(id, newPassword);
        return Map.of("message", "Password reset successfully for student.");
    }

    @GetMapping("/{id}/borrows")
    public List<BorrowRecord> getStudentBorrows(@PathVariable Long id) {
        return borrowService.getStudentHistory(id);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(Exception e) {
        return Map.of("error", e.getMessage());
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
