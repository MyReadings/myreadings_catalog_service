package org.modular.playground.catalog.web.controllers;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.modular.playground.catalog.core.domain.Book;
import org.modular.playground.catalog.core.usecases.BookService;
import org.modular.playground.catalog.infrastructure.persistence.postgres.mapper.BookMapper;
import org.modular.playground.catalog.web.dto.BookResponseDTO;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

@Path("/api/internal/books")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InternalBookController {

    private static final Logger LOGGER = Logger.getLogger(InternalBookController.class);

    @Inject
    BookService bookService;

    @Inject
    BookMapper bookMapper;

    @POST
    @Path("/batch")
    public List<BookResponseDTO> getBooksByIds(List<UUID> bookIds) {
        LOGGER.debugf("Internal batch request for %d books", bookIds.size());
        List<Book> books = bookService.getBooksByIds(bookIds);
        return bookMapper.toResponseDTOs(books);
    }

    @GET
    @Path("/{bookId}")
    public Response getBookById(@PathParam("bookId") UUID bookId) {
        LOGGER.debugf("Internal request for book %s", bookId);
        return bookService.getBookById(bookId)
            .map(book -> Response.ok(bookMapper.toResponseDTO(book)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
}
