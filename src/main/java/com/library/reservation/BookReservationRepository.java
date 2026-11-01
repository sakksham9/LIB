package com.library.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BookReservationRepository extends JpaRepository<BookReservation, Long> {

    List<BookReservation> findByStudentIdOrderByReservedAtDesc(Long studentId);

    List<BookReservation> findByStudentIdAndStatusOrderByReservedAtDesc(Long studentId, String status);

    List<BookReservation> findByBookIdAndStatus(Long bookId, String status);

    List<BookReservation> findAllByOrderByReservedAtDesc();

    @Query("SELECT count(r) > 0 FROM BookReservation r WHERE r.studentId = :studentId AND r.bookId = :bookId AND r.status IN ('PENDING', 'READY_FOR_PICKUP')")
    boolean hasActiveReservation(@Param("studentId") Long studentId, @Param("bookId") Long bookId);

    long countByStudentIdAndStatus(Long studentId, String status);
}
