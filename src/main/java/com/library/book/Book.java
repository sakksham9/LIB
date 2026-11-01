package com.library.book;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
@Entity @Table(name="books", indexes={@Index(name="idx_books_title",columnList="title"),@Index(name="idx_books_isbn",columnList="isbn")})
public class Book {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @NotBlank @Column(nullable=false) private String title;
 @NotBlank @Column(nullable=false) private String author;
 private String category; private String isbn; private Integer publicationYear;
 @Column(length=2000) private String description; private String coverUrl; private Integer rating;
 private Integer availableCopies=1;
 public Book(){}
 public Long getId(){return id;} public void setId(Long v){id=v;}
 public String getTitle(){return title;} public void setTitle(String v){title=v;}
 public String getAuthor(){return author;} public void setAuthor(String v){author=v;}
 public String getCategory(){return category;} public void setCategory(String v){category=v;}
 public String getIsbn(){return isbn;} public void setIsbn(String v){isbn=v;}
 public Integer getPublicationYear(){return publicationYear;} public void setPublicationYear(Integer v){publicationYear=v;}
 public String getDescription(){return description;} public void setDescription(String v){description=v;}
 public String getCoverUrl(){return coverUrl;} public void setCoverUrl(String v){coverUrl=v;}
 public Integer getRating(){return rating;} public void setRating(Integer v){rating=v;}
 public Integer getAvailableCopies(){return availableCopies;} public void setAvailableCopies(Integer v){availableCopies=v;}
}
