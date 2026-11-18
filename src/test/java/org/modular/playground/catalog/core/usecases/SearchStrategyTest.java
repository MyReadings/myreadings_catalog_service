package org.modular.playground.catalog.core.usecases;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modular.playground.catalog.core.domain.Book;
import org.modular.playground.catalog.core.domain.BookImpl;
import org.modular.playground.catalog.core.domain.DomainPage;
import org.modular.playground.catalog.core.usecases.repositories.BookRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchStrategyTest {
    @Mock BookRepository repository;
    @InjectMocks BookServiceImpl service;

    @Test
    void healthySearchReturnsThePageWithoutRefetching() {
        service.searchEnrichmentStrategy = "normal";
        var page = page();
        when(repository.searchBooks("demo", 0, 12, "title", "asc")).thenReturn(page);
        assertSame(page, service.searchBooks("demo", 0, 12, "title", "asc"));
        verify(repository, never()).clearCache();
        verify(repository, never()).findById(any());
    }

    @Test
    void brokenSearchAddsOneReadPerResultAndKeepsPageMetadata() {
        service.searchEnrichmentStrategy = "broken";
        service.enrichmentDelayMs = 0;
        var page = page();
        when(repository.searchBooks("demo", 0, 12, "title", "asc")).thenReturn(page);
        page.content().forEach(book -> when(repository.findById(book.getBookId())).thenReturn(Optional.of(book)));
        assertEquals(page, service.searchBooks("demo", 0, 12, "title", "asc"));
        verify(repository).clearCache();
        verify(repository, times(page.content().size())).findById(any());
    }

    private DomainPage<Book> page() {
        return new DomainPage<>(List.of(BookImpl.builder().bookId(UUID.randomUUID()).build(),
            BookImpl.builder().bookId(UUID.randomUUID()).build()), 25, 3, 0, 12, false, true);
    }
}
