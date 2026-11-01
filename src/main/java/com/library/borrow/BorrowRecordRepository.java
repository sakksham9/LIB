package com.library.borrow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {
    List<BorrowRecord> findAllByOrderByBorrowedAtDesc();
    List<BorrowRecord> findByBookIdOrderByBorrowedAtDesc(Long bookId);
    List<BorrowRecord> findByStatusOrderByBorrowedAtDesc(String status);
    List<BorrowRecord> findByBookIdAndStatus(Long bookId, String status);

    List<BorrowRecord> findByStudentIdOrderByBorrowedAtDesc(Long studentId);
    List<BorrowRecord> findByStudentIdAndStatusOrderByBorrowedAtDesc(Long studentId, String status);
    List<BorrowRecord> findByStudentId(Long studentId);
    long countByStudentIdAndStatus(Long studentId, String status);

    @Query("select r from BorrowRecord r where lower(r.borrowerName) = lower(:name) or (length(:contact) > 0 and lower(coalesce(r.borrowerContact, '')) = lower(:contact))")
    List<BorrowRecord> findByBorrower(@Param("name") String name, @Param("contact") String contact);

    @Query("select r from BorrowRecord r where r.studentId = :studentId or lower(r.borrowerName) = lower(:name) or (length(:contact) > 0 and lower(coalesce(r.borrowerContact, '')) = lower(:contact))")
    List<BorrowRecord> findByStudentOrDetails(@Param("studentId") Long studentId, @Param("name") String name, @Param("contact") String contact);
}
