package com.library.book;
import com.library.dsa.BookSearchIndex;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
public class BookService {
 private final BookRepository repo; private final BookSearchIndex index;
 public BookService(BookRepository r,BookSearchIndex i){repo=r;index=i;}
 public List<Book> all(String q,String category){List<Book> list;
  if(q!=null&&!q.isBlank())list=repo.search(q.trim());
  else if(category!=null&&!category.isBlank())list=repo.findByCategoryIgnoreCaseOrderByTitleAsc(category.trim());
  else list=repo.findAllByOrderByTitleAsc();
  index.rebuild(repo.findAll());return list;
 }
 public Book one(Long id){return repo.findById(id).orElseThrow(()->new NoSuchElementException("Book not found: "+id));}
 @Transactional public Book create(Book b){b.setId(null);Book saved=repo.save(b);index.rebuild(repo.findAll());return saved;}
 @Transactional public Book update(Long id,Book b){Book x=one(id);x.setTitle(b.getTitle());x.setAuthor(b.getAuthor());x.setCategory(b.getCategory());x.setIsbn(b.getIsbn());x.setPublicationYear(b.getPublicationYear());x.setDescription(b.getDescription());x.setCoverUrl(b.getCoverUrl());x.setRating(b.getRating());x.setAvailableCopies(b.getAvailableCopies());return repo.save(x);}
 @Transactional public void delete(Long id){if(!repo.existsById(id))throw new NoSuchElementException("Book not found: "+id);repo.deleteById(id);index.rebuild(repo.findAll());}
 @Transactional public int seed(){if(repo.count()>0)return 0;
  repo.saveAll(List.of(book("Atomic Habits","James Clear","Self-Help","9780735211292",2018,"Practical systems for building good habits.",5,4),
   book("Clean Code","Robert C. Martin","Programming","9780132350884",2008,"A handbook of software craftsmanship.",5,3),
   book("The Pragmatic Programmer","Andrew Hunt and David Thomas","Programming","9780135957059",2019,"Timeless advice for software developers.",5,2),
   book("Deep Work","Cal Newport","Productivity","9781455586691",2016,"Rules for focused success.",4,2),
   book("The Alchemist","Paulo Coelho","Fiction","9780061122415",1988,"A journey about following your dreams.",4,5),
   book("Introduction to Algorithms","Thomas H. Cormen et al.","Computer Science","9780262046305",2022,"A comprehensive algorithms textbook.",5,2),
   book("Design Patterns","Erich Gamma et al.","Programming","9780201633610",1994,"Reusable object-oriented software design patterns.",5,1),
   book("Ikigai","Hector Garcia and Francesc Miralles","Self-Help","9780143130727",2016,"A guide to purpose and wellbeing.",4,3)));
  index.rebuild(repo.findAll());return 8;
 }
 private Book book(String t,String a,String c,String isbn,int y,String d,int r,int copies){Book b=new Book();b.setTitle(t);b.setAuthor(a);b.setCategory(c);b.setIsbn(isbn);b.setPublicationYear(y);b.setDescription(d);b.setRating(r);b.setAvailableCopies(copies);b.setCoverUrl("/images/covers/" + isbn + ".jpg");return b;}
}
