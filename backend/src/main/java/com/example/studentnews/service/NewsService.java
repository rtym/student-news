package com.example.studentnews.service;

import com.example.studentnews.dto.NewsRequest;
import com.example.studentnews.dto.NewsResponse;
import com.example.studentnews.entity.NewsItem;
import com.example.studentnews.entity.NewsStatus;
import com.example.studentnews.exception.NotFoundException;
import com.example.studentnews.repository.NewsRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class NewsService {

    private final NewsRepository newsRepository;

    public NewsService(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    public List<NewsResponse> list(NewsStatus status) {
        List<NewsItem> items = status == null
                ? newsRepository.findAll()
                : newsRepository.findAllByStatusOrderByUpdatedAtDesc(status);

        return items.stream()
                .sorted(Comparator.comparing(NewsItem::getUpdatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    public NewsResponse create(NewsRequest request) {
        NewsItem item = new NewsItem();
        item.setTitle(request.title().trim());
        item.setContent(request.content().trim());
        item.setStatus(NewsStatus.DRAFT);
        return toResponse(newsRepository.save(item));
    }

    public NewsResponse update(Long id, NewsRequest request) {
        NewsItem item = findOrThrow(id);
        item.setTitle(request.title().trim());
        item.setContent(request.content().trim());
        return toResponse(newsRepository.save(item));
    }

    public NewsResponse publish(Long id) {
        NewsItem item = findOrThrow(id);
        item.setStatus(NewsStatus.PUBLISHED);
        return toResponse(newsRepository.save(item));
    }

    public NewsResponse archive(Long id) {
        NewsItem item = findOrThrow(id);
        item.setStatus(NewsStatus.ARCHIVED);
        return toResponse(newsRepository.save(item));
    }

    public void delete(Long id) {
        if (!newsRepository.existsById(id)) {
            throw new NotFoundException("News with id " + id + " not found");
        }
        newsRepository.deleteById(id);
    }

    private NewsItem findOrThrow(Long id) {
        return newsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("News with id " + id + " not found"));
    }

    private NewsResponse toResponse(NewsItem item) {
        return new NewsResponse(
                item.getId(),
                item.getTitle(),
                item.getContent(),
                item.getStatus(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
