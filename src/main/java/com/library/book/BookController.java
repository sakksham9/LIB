package com.library.book;
import com.library.dsa.BookSearchIndex;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api")
public class BookController {
 private final BookService service;private final BookSearchIndex index;
 public BookController(BookService s,BookSearchIndex i){service=s;index=i;}
 @GetMapping("/books") public List<Book> all(@RequestParam(required=false)String q,@RequestParam(required=false)String category){return service.all(q,category);}
 @GetMapping("/books/{id}") public Book one(@PathVariable Long id){return service.one(id);}
 @PostMapping("/books") @ResponseStatus(HttpStatus.CREATED) public Book create(@Valid @RequestBody Book b){return service.create(b);}
 @PutMapping("/books/{id}") public Book update(@PathVariable Long id,@Valid @RequestBody Book b){return service.update(id,b);}
 @DeleteMapping("/books/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id){service.delete(id);}
 @PostMapping("/books/seed") public Map<String,Object> seed(){int n=service.seed();return Map.of("added",n,"message",n==0?"Database already contains books.":"Demo books added.");}
 @GetMapping("/dsa/suggest") public Map<String,List<String>> suggest(@RequestParam(defaultValue="")String prefix,@RequestParam(defaultValue="8")int limit){return Map.of("suggestions",index.suggestTitles(prefix,Math.max(1,Math.min(20,limit))));}
 @GetMapping("/dsa/sorted-titles") public Map<String,List<String>> sorted(){return Map.of("titles",index.titlesSorted());}
 @ExceptionHandler(NoSuchElementException.class) @ResponseStatus(HttpStatus.NOT_FOUND) public Map<String,String> missing(Exception e){return Map.of("error",e.getMessage());}
}
