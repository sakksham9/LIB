package com.library.book;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface BookRepository extends JpaRepository<Book,Long> {
 @Query("select b from Book b where lower(b.title) like lower(concat('%',:q,'%')) or lower(b.author) like lower(concat('%',:q,'%')) or lower(coalesce(b.category,'')) like lower(concat('%',:q,'%')) or lower(coalesce(b.isbn,'')) like lower(concat('%',:q,'%')) order by b.title")
 List<Book> search(@Param("q") String q);
 List<Book> findByCategoryIgnoreCaseOrderByTitleAsc(String category);
 List<Book> findAllByOrderByTitleAsc();
}
