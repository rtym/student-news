package com.example.studentnews.service;

import com.example.studentnews.dto.NewsRequest;
import com.example.studentnews.dto.NewsResponse;
import com.example.studentnews.entity.NewsItem;
import com.example.studentnews.entity.NewsStatus;
import com.example.studentnews.exception.NotFoundException;
import com.example.studentnews.repository.NewsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

    @Mock
    private NewsRepository newsRepository;

    @InjectMocks
    private NewsService newsService;

    @Test
    void createShouldTrimInputAndSetDraftStatus() {
        NewsRequest request = new NewsRequest("  New title  ", "  New content  ");
        NewsItem saved = newsItem(1L, "New title", "New content", NewsStatus.DRAFT, Instant.now());

        when(newsRepository.save(any(NewsItem.class))).thenReturn(saved);

        NewsResponse response = newsService.create(request);

        ArgumentCaptor<NewsItem> captor = ArgumentCaptor.forClass(NewsItem.class);
        verify(newsRepository).save(captor.capture());
        NewsItem toSave = captor.getValue();

        assertThat(toSave.getTitle()).isEqualTo("New title");
        assertThat(toSave.getContent()).isEqualTo("New content");
        assertThat(toSave.getStatus()).isEqualTo(NewsStatus.DRAFT);
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void updateShouldThrowWhenNewsNotFound() {
        when(newsRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> newsService.update(99L, new NewsRequest("title", "content")))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void listShouldReturnSortedByUpdatedAtDesc() {
        NewsItem older = newsItem(1L, "Older", "C1", NewsStatus.DRAFT, Instant.parse("2026-01-01T10:00:00Z"));
        NewsItem newer = newsItem(2L, "Newer", "C2", NewsStatus.PUBLISHED, Instant.parse("2026-01-01T12:00:00Z"));
        when(newsRepository.findAll()).thenReturn(List.of(older, newer));

        List<NewsResponse> result = newsService.list(null);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().id()).isEqualTo(2L);
        assertThat(result.get(1).id()).isEqualTo(1L);
    }

    @Test
    void deleteShouldNotCallRepositoryDeleteWhenMissing() {
        when(newsRepository.existsById(12L)).thenReturn(false);

        assertThatThrownBy(() -> newsService.delete(12L))
                .isInstanceOf(NotFoundException.class);
        verify(newsRepository, never()).deleteById(12L);
    }

    private NewsItem newsItem(Long id, String title, String content, NewsStatus status, Instant updatedAt) {
        NewsItem item = new NewsItem();
        item.setId(id);
        item.setTitle(title);
        item.setContent(content);
        item.setStatus(status);
        item.onCreate();
        return forceUpdatedAt(item, updatedAt);
    }

    private NewsItem forceUpdatedAt(NewsItem item, Instant updatedAt) {
        // Use pre-update hook to set a realistic updated timestamp in tests.
        item.onUpdate();
        return new NewsItemProxy(item, updatedAt).unwrap();
    }

    private static class NewsItemProxy {
        private final NewsItem item;
        private final Instant updatedAt;

        private NewsItemProxy(NewsItem item, Instant updatedAt) {
            this.item = item;
            this.updatedAt = updatedAt;
        }

        private NewsItem unwrap() {
            try {
                var field = NewsItem.class.getDeclaredField("updatedAt");
                field.setAccessible(true);
                field.set(item, updatedAt);
                return item;
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unable to set updatedAt for test", e);
            }
        }
    }
}
