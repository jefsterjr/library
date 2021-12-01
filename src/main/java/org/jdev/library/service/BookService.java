package org.jdev.library.service;

import lombok.extern.slf4j.Slf4j;
import org.jdev.library.model.Book;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@Slf4j
public class BookService {

    List<Book> books;

    public BookService() {
        populateBooksList();
    }

    public Book getBook(Long id) {
        log.trace("Searching for book with id: {}", id);
        return books.stream().filter(book -> Objects.equals(book.id(), id)).findFirst().orElseThrow(NoSuchElementException::new);
    }

    public List<Book> getAllBooks() {
        log.trace("Searching for all books");
        return books;
    }

    public Long createBook(Book book) {
        log.trace("Creating a new book {}", book);
        log.info("Book created! {}", book);
        return book.id();
    }

    public void deleteBook(Long id) {
        log.trace("Deleting a book with id: {}", id);
        if (books.removeIf(book -> book.id().equals(id))) {
            log.info("Book deleted!");
        } else {
            throw new NoSuchElementException();
        }

    }

    private void populateBooksList() {
        books = new ArrayList<>();
        books.add(new Book(1L, "The Lord of the Rings - The Fellowship of the Ring", "J. R. R. Tolkien"));
        books.add(new Book(2L, "The Lord of the Rings - The Two Towers", "J. R. R. Tolkien"));
        books.add(new Book(3L, "The Lord of the Rings - The Return of the King", "J. R. R. Tolkien"));
    }
}
