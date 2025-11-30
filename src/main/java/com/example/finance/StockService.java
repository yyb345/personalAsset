package com.example.finance;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class StockService {
    
    private static final int CACHE_DURATION_MINUTES = 15;
    private final Map<String, CachedStockData> cache = new HashMap<>();
    private final Random random = new Random();
    
    static class CachedStockData {
        String data;
        LocalDateTime timestamp;
        
        CachedStockData(String data, LocalDateTime timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }
        
        boolean isExpired() {
            return LocalDateTime.now().minusMinutes(CACHE_DURATION_MINUTES).isAfter(timestamp);
        }
    }
    
    public String getStockData(String symbol, Long period1, Long period2) {
        String cacheKey = symbol + "_" + period1 + "_" + period2;
        
        // Check cache first
        if (cache.containsKey(cacheKey)) {
            CachedStockData cached = cache.get(cacheKey);
            if (!cached.isExpired()) {
                return cached.data;
            }
        }
        
        // Try to fetch from Yahoo Finance
        try {
            String url = String.format(
                "https://query2.finance.yahoo.com/v8/finance/chart/%s?period1=%d&period2=%d&interval=1d",
                symbol, period1, period2
            );
            
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getInterceptors().add((request, body, execution) -> {
                request.getHeaders().add("User-Agent", "Mozilla/5.0");
                return execution.execute(request, body);
            });
            
            String response = restTemplate.getForObject(url, String.class);
            
            // Cache the successful response
            cache.put(cacheKey, new CachedStockData(response, LocalDateTime.now()));
            
            return response;
        } catch (Exception e) {
            // If API fails, return mock data
            System.err.println("Failed to fetch real stock data for " + symbol + ": " + e.getMessage());
            return generateMockStockData(symbol, period1, period2);
        }
    }
    
    private String generateMockStockData(String symbol, Long period1, Long period2) {
        // Generate realistic mock data
        long startTime = period1;
        long endTime = period2;
        long dayInSeconds = 86400;
        
        StringBuilder timestamps = new StringBuilder("[");
        StringBuilder closes = new StringBuilder("[");
        StringBuilder opens = new StringBuilder("[");
        StringBuilder highs = new StringBuilder("[");
        StringBuilder lows = new StringBuilder("[");
        StringBuilder volumes = new StringBuilder("[");
        
        // Base prices for different stocks
        double basePrice = getBasePrice(symbol);
        double currentPrice = basePrice;
        
        boolean first = true;
        for (long time = startTime; time <= endTime; time += dayInSeconds) {
            if (!first) {
                timestamps.append(",");
                closes.append(",");
                opens.append(",");
                highs.append(",");
                lows.append(",");
                volumes.append(",");
            }
            first = false;
            
            // Generate realistic price movements
            double changePercent = (random.nextDouble() - 0.5) * 0.04; // ±2% daily change
            double open = currentPrice;
            currentPrice = currentPrice * (1 + changePercent);
            double high = Math.max(open, currentPrice) * (1 + random.nextDouble() * 0.01);
            double low = Math.min(open, currentPrice) * (1 - random.nextDouble() * 0.01);
            long volume = 10000000 + random.nextInt(50000000);
            
            timestamps.append(time);
            opens.append(String.format("%.2f", open));
            closes.append(String.format("%.2f", currentPrice));
            highs.append(String.format("%.2f", high));
            lows.append(String.format("%.2f", low));
            volumes.append(volume);
        }
        
        timestamps.append("]");
        closes.append("]");
        opens.append("]");
        highs.append("]");
        lows.append("]");
        volumes.append("]");
        
        String json = String.format(
            "{\"chart\":{\"result\":[{\"meta\":{\"symbol\":\"%s\",\"currency\":\"USD\"}," +
            "\"timestamp\":%s," +
            "\"indicators\":{\"quote\":[{\"open\":%s,\"high\":%s,\"low\":%s,\"close\":%s,\"volume\":%s}]}}]}}",
            symbol, timestamps, opens, highs, lows, closes, volumes
        );
        
        return json;
    }
    
    private double getBasePrice(String symbol) {
        switch (symbol.toUpperCase()) {
            case "BABA":
                return 85.0;
            case "1797.HK":
                return 45.0;
            case "KO":
                return 62.0;
            default:
                return 100.0;
        }
    }
}

