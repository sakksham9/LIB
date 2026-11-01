package com.library.user;

import com.library.borrow.BorrowRecord;
import com.library.borrow.BorrowRecordRepository;
import com.library.borrow.BorrowService;
import com.library.reservation.BookReservationRepository;
import com.library.user.dto.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final AuthTokenRepository tokenRepo;
    private final BorrowRecordRepository borrowRepo;
    private final BookReservationRepository reservationRepo;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepo,
            AuthTokenRepository tokenRepo,
            BorrowRecordRepository borrowRepo,
            BookReservationRepository reservationRepo,
            PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
        this.borrowRepo = borrowRepo;
        this.reservationRepo = reservationRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedInitialUsers() {
        // Seed default Admin if not present
        if (userRepo.countByRole("ADMIN") == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("System Administrator");
            admin.setEmail("admin@pageturn.com");
            admin.setPhone("+91 99999 00000");
            admin.setStudentIdNumber("ADM-001");
            admin.setDepartment("Library Administration");
            admin.setRole("ADMIN");
            admin.setStatus("ACTIVE");
            admin.setCreatedAt(LocalDateTime.now());
            userRepo.save(admin);
            System.out.println(">>> Initialized default Admin account (admin / admin123)");
        }

        // Seed demo student accounts if no students present
        if (userRepo.countByRole("STUDENT") == 0) {
            User student1 = new User();
            student1.setUsername("student");
            student1.setPassword(passwordEncoder.encode("student123"));
            student1.setFullName("Saksham Munda");
            student1.setEmail("saksham.munda@university.edu");
            student1.setPhone("+91 98765 43210");
            student1.setStudentIdNumber("STU-2026-001");
            student1.setDepartment("Computer Science & Engineering");
            student1.setRole("STUDENT");
            student1.setStatus("ACTIVE");
            student1.setCreatedAt(LocalDateTime.now());
            userRepo.save(student1);

            User student2 = new User();
            student2.setUsername("aarav");
            student2.setPassword(passwordEncoder.encode("aarav123"));
            student2.setFullName("Aarav Sharma");
            student2.setEmail("aarav.sharma@university.edu");
            student2.setPhone("+91 98123 45678");
            student2.setStudentIdNumber("STU-2026-002");
            student2.setDepartment("Information Technology");
            student2.setRole("STUDENT");
            student2.setStatus("ACTIVE");
            student2.setCreatedAt(LocalDateTime.now());
            userRepo.save(student2);

            System.out.println(">>> Initialized demo Student accounts (student / student123, aarav / aarav123)");
        }
    }

    @Transactional
    public LoginResponse login(LoginRequest req) {
        String identifier = req.getUsername().trim();
        User user = userRepo.findByIdentifier(identifier)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username, student ID, or email."));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid password. Please check your credentials.");
        }

        if (!user.isActive()) {
            throw new IllegalStateException("Your account has been deactivated by the Administrator. Please contact library staff.");
        }

        // Validate role matches login portal if loginType specified
        if (req.getLoginType() != null && !req.getLoginType().isBlank()) {
            if (!req.getLoginType().equalsIgnoreCase(user.getRole())) {
                if ("ADMIN".equalsIgnoreCase(req.getLoginType())) {
                    throw new IllegalStateException("Access Denied: This account does not possess Administrator privileges.");
                } else if ("STUDENT".equalsIgnoreCase(req.getLoginType())) {
                    throw new IllegalStateException("Please log in through the Administrator portal with an Admin account.");
                }
            }
        }

        // Issue auth token valid for 14 days
        String tokenStr = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        AuthToken token = new AuthToken(tokenStr, user, LocalDateTime.now().plusDays(14));
        tokenRepo.save(token);

        LoginResponse res = new LoginResponse();
        res.setToken(tokenStr);
        res.setId(user.getId());
        res.setUsername(user.getUsername());
        res.setFullName(user.getFullName());
        res.setEmail(user.getEmail());
        res.setPhone(user.getPhone());
        res.setStudentIdNumber(user.getStudentIdNumber());
        res.setDepartment(user.getDepartment());
        res.setRole(user.getRole());
        res.setStatus(user.getStatus());
        return res;
    }

    public User validateToken(String tokenStr) {
        if (tokenStr == null || tokenStr.isBlank()) {
            return null;
        }
        Optional<AuthToken> tokenOpt = tokenRepo.findByToken(tokenStr.trim());
        if (tokenOpt.isEmpty()) {
            return null;
        }
        AuthToken token = tokenOpt.get();
        if (token.isExpired()) {
            tokenRepo.delete(token);
            return null;
        }
        User user = token.getUser();
        if (user == null || !user.isActive()) {
            return null;
        }
        return user;
    }

    @Transactional
    public void logout(String tokenStr) {
        if (tokenStr != null && !tokenStr.isBlank()) {
            tokenRepo.deleteByToken(tokenStr.trim());
        }
    }

    // Admin exclusive control: Register Student
    @Transactional
    public User registerStudent(CreateStudentRequest req) {
        String studentIdNum = req.getStudentIdNumber().trim();
        if (userRepo.existsByStudentIdNumber(studentIdNum)) {
            throw new IllegalArgumentException("A student with ID '" + studentIdNum + "' is already registered.");
        }

        String username = (req.getUsername() != null && !req.getUsername().isBlank())
                ? req.getUsername().trim()
                : studentIdNum.toLowerCase().replace(" ", "").replace("-", "");

        if (userRepo.existsByUsername(username)) {
            throw new IllegalArgumentException("Username '" + username + "' is already in use.");
        }

        String email = req.getEmail() != null ? req.getEmail().trim() : null;
        if (email != null && !email.isBlank() && userRepo.existsByEmail(email)) {
            throw new IllegalArgumentException("Email '" + email + "' is already associated with another account.");
        }

        String rawPassword = (req.getPassword() != null && !req.getPassword().isBlank())
                ? req.getPassword()
                : "student123";

        User student = new User();
        student.setStudentIdNumber(studentIdNum);
        student.setFullName(req.getFullName().trim());
        student.setUsername(username);
        student.setPassword(passwordEncoder.encode(rawPassword));
        student.setEmail(email);
        student.setPhone(req.getPhone() != null ? req.getPhone().trim() : null);
        student.setDepartment(req.getDepartment() != null ? req.getDepartment().trim() : "General Studies");
        student.setRole("STUDENT");
        student.setStatus("ACTIVE");
        student.setCreatedAt(LocalDateTime.now());

        return userRepo.save(student);
    }

    // Admin exclusive control: Update Student
    @Transactional
    public User updateStudent(Long id, UpdateStudentRequest req) {
        User student = userRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Student not found with ID: " + id));

        if (!student.isStudent()) {
            throw new IllegalStateException("Only student records can be updated via student management.");
        }

        student.setFullName(req.getFullName().trim());
        student.setEmail(req.getEmail() != null ? req.getEmail().trim() : null);
        student.setPhone(req.getPhone() != null ? req.getPhone().trim() : null);
        student.setDepartment(req.getDepartment() != null ? req.getDepartment().trim() : null);
        student.setUpdatedAt(LocalDateTime.now());

        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            student.setPassword(passwordEncoder.encode(req.getPassword()));
            // Revoke current sessions
            tokenRepo.deleteByUserId(student.getId());
        }

        return userRepo.save(student);
    }

    // Admin exclusive control: Deactivate / Activate Student
    @Transactional
    public User toggleStudentStatus(Long id) {
        User student = userRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Student not found with ID: " + id));

        if (!student.isStudent()) {
            throw new IllegalStateException("Admin accounts cannot be deactivated here.");
        }

        if (student.isActive()) {
            student.setStatus("DEACTIVATED");
            // Invalidate all tokens so student is immediately logged out
            tokenRepo.deleteByUserId(student.getId());
        } else {
            student.setStatus("ACTIVE");
        }
        student.setUpdatedAt(LocalDateTime.now());
        return userRepo.save(student);
    }

    // Admin exclusive control: Reset Password
    @Transactional
    public void resetStudentPassword(Long id, String newPassword) {
        User student = userRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Student not found with ID: " + id));

        student.setPassword(passwordEncoder.encode(newPassword));
        student.setUpdatedAt(LocalDateTime.now());
        userRepo.save(student);
        tokenRepo.deleteByUserId(student.getId());
    }

    public List<StudentDto> getAllStudents() {
        List<User> students = userRepo.findByRoleOrderByCreatedAtDesc("STUDENT");
        List<StudentDto> dtoList = new ArrayList<>();

        for (User s : students) {
            dtoList.add(buildStudentDto(s));
        }
        return dtoList;
    }

    public StudentDto getStudentDto(Long id) {
        User student = userRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Student not found with ID: " + id));
        return buildStudentDto(student);
    }

    public Optional<User> findById(Long id) {
        return userRepo.findById(id);
    }

    private StudentDto buildStudentDto(User s) {
        StudentDto dto = new StudentDto();
        dto.setId(s.getId());
        dto.setStudentIdNumber(s.getStudentIdNumber());
        dto.setUsername(s.getUsername());
        dto.setFullName(s.getFullName());
        dto.setEmail(s.getEmail());
        dto.setPhone(s.getPhone());
        dto.setDepartment(s.getDepartment());
        dto.setRole(s.getRole());
        dto.setStatus(s.getStatus());
        dto.setCreatedAt(s.getCreatedAt());

        // Calculate active loans, overdue loans, and fines
        List<BorrowRecord> records = borrowRepo.findByStudentId(s.getId());
        long active = 0;
        long overdue = 0;
        double fines = 0.0;
        java.time.LocalDate today = java.time.LocalDate.now();

        for (BorrowRecord r : records) {
            if ("BORROWED".equalsIgnoreCase(r.getStatus())) {
                active++;
                if (r.getDueDate() != null && today.isAfter(r.getDueDate())) {
                    overdue++;
                }
            }
            if (!Boolean.TRUE.equals(r.getFinePaid()) && r.getFineAmount() != null && r.getFineAmount() > 0) {
                fines += r.getFineAmount();
            }
        }

        long reservations = reservationRepo.countByStudentIdAndStatus(s.getId(), "PENDING")
                + reservationRepo.countByStudentIdAndStatus(s.getId(), "READY_FOR_PICKUP");

        dto.setActiveLoansCount(active);
        dto.setOverdueLoansCount(overdue);
        dto.setTotalUnpaidFines(fines);
        dto.setReservationsCount(reservations);
        return dto;
    }
}
