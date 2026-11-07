package com.example.finance.seo;

import com.example.finance.followread.YoutubeVideo;
import com.example.finance.followread.YoutubeVideoRepository;
import org.springframework.stereotype.Service;
import java.util.List;

/** Shared minimum quality gate for existing public transcript pages; not an editorial publication workflow. */
@Service
public class PublicVideoCatalog {
    private final YoutubeVideoRepository repository;
    public PublicVideoCatalog(YoutubeVideoRepository repository) { this.repository = repository; }
    public List<YoutubeVideo> all() { return repository.findPublicVideos(); }
    public boolean contains(String videoId) { return repository.existsPublicVideo(videoId); }
}
