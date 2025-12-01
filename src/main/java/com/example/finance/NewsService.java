package com.example.finance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NewsService {
    
    @Value("${news.api.source:newsapi}")
    private String apiSource;
    
    @Value("${news.api.newsapi.key:}")
    private String newsApiKey;
    
    @Value("${news.api.newsapi.country:cn}")
    private String newsApiCountry;
    
    @Value("${news.api.newsapi.pageSize:20}")
    private int newsApiPageSize;
    
    @Value("${news.api.tianapi.key:}")
    private String tianApiKey;
    
    @Value("${news.cache.duration.minutes:30}")
    private int cacheDurationMinutes;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 缓存新闻数据，避免频繁调用API
    private List<News> cachedNews = new ArrayList<>();
    private LocalDateTime lastFetchTime = null;
    
    /**
     * 获取热门新闻
     */
    public List<News> getHotNews() {
        // 如果缓存有效，直接返回缓存
        if (isCacheValid()) {
            return cachedNews;
        }
        
        // 从API获取新闻
        List<News> news = fetchNewsFromAPI();
        
        // 更新缓存
        cachedNews = news;
        lastFetchTime = LocalDateTime.now();
        
        return news;
    }
    
    /**
     * 按分类获取新闻
     */
    public List<News> getNewsByCategory(String category) {
        List<News> allNews = getHotNews();
        if (category == null || category.equals("all")) {
            return allNews;
        }
        return allNews.stream()
                .filter(news -> category.equals(news.getCategory()))
                .collect(Collectors.toList());
    }
    
    /**
     * 检查缓存是否有效
     */
    private boolean isCacheValid() {
        if (lastFetchTime == null || cachedNews.isEmpty()) {
            return false;
        }
        long minutesSinceLastFetch = ChronoUnit.MINUTES.between(lastFetchTime, LocalDateTime.now());
        return minutesSinceLastFetch < cacheDurationMinutes;
    }
    
    /**
     * 从API获取新闻
     */
    private List<News> fetchNewsFromAPI() {
        try {
            if ("newsapi".equalsIgnoreCase(apiSource)) {
                return fetchFromNewsAPI();
            } else if ("tianapi".equalsIgnoreCase(apiSource)) {
                return fetchFromTianAPI();
            } else {
                System.out.println("Unknown API source: " + apiSource + ", using mock data");
                return getMockNews();
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch news from API: " + e.getMessage());
            e.printStackTrace();
            // 降级到模拟数据
            return getMockNews();
        }
    }
    
    /**
     * 从NewsAPI获取新闻
     * API文档: https://newsapi.org/docs/endpoints/top-headlines
     */
    private List<News> fetchFromNewsAPI() {
        if (newsApiKey == null || newsApiKey.isEmpty() || newsApiKey.equals("YOUR_NEWSAPI_KEY_HERE")) {
            System.out.println("NewsAPI key not configured, using mock data");
            return getMockNews();
        }
        
        try {
            String url = String.format(
                "https://newsapi.org/v2/top-headlines?country=%s&pageSize=%d&apiKey=%s",
                newsApiCountry, newsApiPageSize, newsApiKey
            );
            
            System.out.println("Fetching news from NewsAPI...");
            String response = restTemplate.getForObject(url, String.class);
            
            return parseNewsAPIResponse(response);
        } catch (Exception e) {
            System.err.println("NewsAPI fetch failed: " + e.getMessage());
            throw new RuntimeException("Failed to fetch from NewsAPI", e);
        }
    }
    
    /**
     * 解析NewsAPI响应
     */
    private List<News> parseNewsAPIResponse(String response) throws Exception {
        List<News> newsList = new ArrayList<>();
        JsonNode root = objectMapper.readTree(response);
        
        if (!"ok".equals(root.path("status").asText())) {
            throw new RuntimeException("NewsAPI returned error: " + root.path("message").asText());
        }
        
        JsonNode articles = root.path("articles");
        for (JsonNode article : articles) {
            try {
                String title = article.path("title").asText();
                String description = article.path("description").asText("暂无描述");
                String url = article.path("url").asText();
                String imageUrl = article.path("urlToImage").asText();
                String source = article.path("source").path("name").asText("未知来源");
                String publishedAtStr = article.path("publishedAt").asText();
                
                // 解析时间
                LocalDateTime publishedAt;
                try {
                    ZonedDateTime zdt = ZonedDateTime.parse(publishedAtStr, DateTimeFormatter.ISO_DATE_TIME);
                    publishedAt = zdt.toLocalDateTime();
                } catch (Exception e) {
                    publishedAt = LocalDateTime.now();
                }
                
                // 根据内容智能分类
                String category = categorizeNews(title, description);
                
                News news = new News(title, description, url, imageUrl, source, category, publishedAt);
                newsList.add(news);
            } catch (Exception e) {
                System.err.println("Failed to parse article: " + e.getMessage());
            }
        }
        
        System.out.println("Successfully fetched " + newsList.size() + " news from NewsAPI");
        return newsList;
    }
    
    /**
     * 从天行数据API获取新闻
     * API文档: https://www.tianapi.com/apiview/235
     */
    private List<News> fetchFromTianAPI() {
        if (tianApiKey == null || tianApiKey.isEmpty() || tianApiKey.equals("YOUR_TIANAPI_KEY_HERE")) {
            System.out.println("TianAPI key not configured, using mock data");
            return getMockNews();
        }
        
        try {
            String url = String.format(
                "http://api.tianapi.com/generalnews/index?key=%s&num=%d",
                tianApiKey, 20
            );
            
            System.out.println("Fetching news from TianAPI...");
            String response = restTemplate.getForObject(url, String.class);
            
            return parseTianAPIResponse(response);
        } catch (Exception e) {
            System.err.println("TianAPI fetch failed: " + e.getMessage());
            throw new RuntimeException("Failed to fetch from TianAPI", e);
        }
    }
    
    /**
     * 解析天行数据API响应
     */
    private List<News> parseTianAPIResponse(String response) throws Exception {
        List<News> newsList = new ArrayList<>();
        JsonNode root = objectMapper.readTree(response);
        
        if (root.path("code").asInt() != 200) {
            throw new RuntimeException("TianAPI returned error: " + root.path("msg").asText());
        }
        
        JsonNode newsArray = root.path("result").path("newslist");
        for (JsonNode item : newsArray) {
            try {
                String title = item.path("title").asText();
                String description = item.path("description").asText("");
                String url = item.path("url").asText();
                String imageUrl = item.path("picUrl").asText("");
                String source = item.path("source").asText("天行数据");
                String category = item.path("ctime").asText("综合");
                
                LocalDateTime publishedAt = LocalDateTime.now();
                
                News news = new News(title, description, url, imageUrl, source, category, publishedAt);
                newsList.add(news);
            } catch (Exception e) {
                System.err.println("Failed to parse TianAPI item: " + e.getMessage());
            }
        }
        
        System.out.println("Successfully fetched " + newsList.size() + " news from TianAPI");
        return newsList;
    }
    
    /**
     * 智能分类新闻
     */
    private String categorizeNews(String title, String description) {
        String content = (title + " " + description).toLowerCase();
        
        // 科技类关键词
        if (content.matches(".*(ai|人工智能|科技|互联网|5g|芯片|手机|电脑|软件|硬件|科学|技术|创新|研发).*")) {
            return "科技";
        }
        // 财经类关键词
        if (content.matches(".*(股票|经济|金融|投资|银行|市场|交易|货币|财经|上市|债券|基金).*")) {
            return "财经";
        }
        // 体育类关键词
        if (content.matches(".*(足球|篮球|nba|world cup|奥运|比赛|运动|体育|球员|冠军|联赛).*")) {
            return "体育";
        }
        // 娱乐类关键词
        if (content.matches(".*(电影|电视|明星|演员|音乐|娱乐|综艺|导演|票房|上映).*")) {
            return "娱乐";
        }
        // 健康类关键词
        if (content.matches(".*(健康|医疗|疾病|医院|药物|养生|营养|锻炼|运动|保健).*")) {
            return "健康";
        }
        // 教育类关键词
        if (content.matches(".*(教育|学校|大学|考试|学生|教师|培训|课程|学习).*")) {
            return "教育";
        }
        
        return "综合";
    }
    
    /**
     * 获取模拟新闻数据（降级方案）
     */
    private List<News> getMockNews() {
        System.out.println("Using mock news data");
        List<News> newsList = new ArrayList<>();
        
        // 模拟科技新闻
        newsList.add(createNews(
            "人工智能领域再获突破：新型AI模型性能提升30%",
            "研究团队开发出新一代人工智能模型，在多项基准测试中性能提升显著，特别是在自然语言处理和图像识别领域表现突出。",
            "https://example.com/news/ai-breakthrough",
            "https://images.unsplash.com/photo-1677442136019-21780ecad995?w=800&h=450&fit=crop",
            "科技日报",
            "科技",
            LocalDateTime.now().minusHours(2)
        ));
        
        newsList.add(createNews(
            "新能源汽车市场持续增长，销量同比增长45%",
            "根据最新数据显示，本月新能源汽车销量继续保持强劲增长势头，智能化和电动化成为汽车行业发展新趋势。",
            "https://example.com/news/ev-growth",
            "https://images.unsplash.com/photo-1593941707882-a5bba14938c7?w=800&h=450&fit=crop",
            "经济观察报",
            "财经",
            LocalDateTime.now().minusHours(4)
        ));
        
        newsList.add(createNews(
            "国际空间站完成新一轮科学实验，探索太空生命可能性",
            "国际空间站的科学家们成功完成了一系列微重力环境下的生物实验，为未来长期太空探索奠定基础。",
            "https://example.com/news/space-station",
            "https://images.unsplash.com/photo-1516849841032-87cbac4d88f7?w=800&h=450&fit=crop",
            "科学探索",
            "科技",
            LocalDateTime.now().minusHours(6)
        ));
        
        newsList.add(createNews(
            "全球股市集体上涨，科技股领涨",
            "受利好消息影响，全球主要股市今日集体上涨，其中科技板块表现最为强劲，多只科技龙头股创历史新高。",
            "https://example.com/news/stock-market",
            "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?w=800&h=450&fit=crop",
            "财经周刊",
            "财经",
            LocalDateTime.now().minusHours(1)
        ));
        
        newsList.add(createNews(
            "NBA总决赛激战正酣，东部冠军晋级在望",
            "NBA季后赛进入关键阶段，东部冠军争夺战异常激烈，明星球员表现出色成为比赛焦点。",
            "https://example.com/news/nba-finals",
            "https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800&h=450&fit=crop",
            "体育周报",
            "体育",
            LocalDateTime.now().minusHours(3)
        ));
        
        newsList.add(createNews(
            "年度热播剧集获得观众一致好评",
            "今年最受期待的电视剧系列终于播出，凭借精良的制作和出色的演员表演获得观众高度评价。",
            "https://example.com/news/tv-series",
            "https://images.unsplash.com/photo-1522869635100-9f4c5e86aa37?w=800&h=450&fit=crop",
            "娱乐周刊",
            "娱乐",
            LocalDateTime.now().minusHours(8)
        ));
        
        newsList.add(createNews(
            "研究发现：规律运动可显著改善睡眠质量",
            "最新医学研究表明，每周进行3-5次适量运动可以有效改善睡眠质量，提高生活幸福感。",
            "https://example.com/news/health-exercise",
            "https://images.unsplash.com/photo-1476480862126-209bfaa8edc8?w=800&h=450&fit=crop",
            "健康生活",
            "健康",
            LocalDateTime.now().minusHours(9)
        ));
        
        newsList.add(createNews(
            "在线教育平台助力教育公平，惠及偏远地区",
            "新型在线教育平台通过互联网技术，将优质教育资源输送到偏远地区，为更多学生提供学习机会。",
            "https://example.com/news/online-education",
            "https://images.unsplash.com/photo-1503676260728-1c00da094a0b?w=800&h=450&fit=crop",
            "教育观察",
            "教育",
            LocalDateTime.now().minusHours(12)
        ));
        
        return newsList;
    }
    
    /**
     * 创建新闻对象
     */
    private News createNews(String title, String description, String url, 
                           String imageUrl, String source, String category, 
                           LocalDateTime publishedAt) {
        return new News(title, description, url, imageUrl, source, category, publishedAt);
    }
    
    /**
     * 刷新新闻缓存
     */
    public void refreshNews() {
        cachedNews.clear();
        lastFetchTime = null;
    }
}
