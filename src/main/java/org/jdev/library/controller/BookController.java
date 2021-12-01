package org.jdev.library.controller;

import lombok.extern.slf4j.Slf4j;
import org.jdev.library.model.Book;
import org.jdev.library.service.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/book")
public class BookController extends CommonsController {

    private final BookService service;

    public BookController(BookService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBook(@PathVariable Long id) {
        log.info("getBook with id: {}", id);
        return ResponseEntity.ok(service.getBook(id));
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAll() {
        log.info("getAll");
        return ResponseEntity.ok(service.getAllBooks());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Book book, UriComponentsBuilder uriComponentsBuilder) {
        log.info("create, {}",book);
        Long id = service.createBook(book);
        URI uri = uriComponentsBuilder.path("/book/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(uri).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        log.info("delete, {}",id);
        service.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
