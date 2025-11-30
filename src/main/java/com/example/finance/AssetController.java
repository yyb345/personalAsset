package com.example.finance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api")
public class AssetController {

    private static final Logger logger = LoggerFactory.getLogger(AssetController.class);

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private StockService stockService;

    private static final BigDecimal SGD_TO_CNY_RATE = new BigDecimal("5.4");

    @GetMapping("/months")
    public List<String> getMonths() {
        return assetRepository.findDistinctMonths();
    }

    @GetMapping("/assets")
    public List<Asset> getAssetsByMonth(@RequestParam(required = false) String month) {
        logger.info("Fetching assets for month: {}", month);
        if (month == null || month.isEmpty()) {
            logger.warn("Month parameter is null or empty");
            return Collections.emptyList();
        }
        try {
            YearMonth yearMonth = YearMonth.parse(month); // Expects "YYYY-MM"
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();
            List<Asset> assets = assetRepository.findAllByEntryDateBetween(startDate, endDate);
            logger.info("Found {} assets for month {}", assets.size(), month);
            return assets;
        } catch (DateTimeParseException e) {
            logger.error("Invalid month format: {}", month, e);
            return Collections.emptyList();
        }
    }

    @PostMapping("/assets")
    public Asset addAsset(@RequestBody Asset asset) {
        logger.info("Adding new asset: {} of type {}", asset.getName(), asset.getType());
        if (asset.getEntryDate() == null) {
            asset.setEntryDate(LocalDate.now());
        }
        if (asset.getCurrency() == null || asset.getCurrency().isEmpty()) {
            asset.setCurrency("CNY");
        }
        Asset savedAsset = assetRepository.save(asset);
        logger.info("Successfully saved asset with ID: {}", savedAsset.getId());
        return savedAsset;
    }

    @PutMapping("/assets/{id}")
    public ResponseEntity<Asset> updateAsset(@PathVariable Long id, @RequestBody Asset assetDetails) {
        logger.info("Updating asset with ID: {}", id);
        Optional<Asset> optionalAsset = assetRepository.findById(id);
        if (!optionalAsset.isPresent()) {
            logger.warn("Asset not found with ID: {}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        Asset asset = optionalAsset.get();
        asset.setName(assetDetails.getName());
        asset.setType(assetDetails.getType());
        asset.setValue(assetDetails.getValue());
        asset.setCurrency(assetDetails.getCurrency());
        asset.setEntryDate(assetDetails.getEntryDate());
        
        Asset updatedAsset = assetRepository.save(asset);
        logger.info("Successfully updated asset with ID: {}", id);
        return new ResponseEntity<>(updatedAsset, HttpStatus.OK);
    }

    @DeleteMapping("/assets/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long id) {
        logger.info("Deleting asset with ID: {}", id);
        if (!assetRepository.existsById(id)) {
            logger.warn("Asset not found for deletion with ID: {}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        assetRepository.deleteById(id);
        logger.info("Successfully deleted asset with ID: {}", id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/report")
    public Map<String, Object> getReport(@RequestParam(required = false) String month) {
        List<Asset> assets;
        if (month == null || month.isEmpty()) {
            assets = Collections.emptyList();
        } else {
            try {
                YearMonth yearMonth = YearMonth.parse(month);
                LocalDate startDate = yearMonth.atDay(1);
                LocalDate endDate = yearMonth.atEndOfMonth();
                assets = assetRepository.findAllByEntryDateBetween(startDate, endDate);
            } catch (DateTimeParseException e) {
                assets = Collections.emptyList();
            }
        }

        BigDecimal totalValue = assets.stream()
                .map(this::convertToCny)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> valueByType = assets.stream()
                .collect(Collectors.groupingBy(
                        Asset::getType,
                        Collectors.mapping(this::convertToCny, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        return Map.of(
                "totalValue", totalValue,
                "valueByType", valueByType,
                "totalCount", assets.size()
        );
    }

    @GetMapping("/reports/monthly-summary")
    public List<Map<String, Object>> getMonthlySummary() {
        List<Asset> allAssets = assetRepository.findAll();

        Map<YearMonth, BigDecimal> monthlyTotals = allAssets.stream()
                .collect(Collectors.groupingBy(
                        asset -> YearMonth.from(asset.getEntryDate()),
                        Collectors.mapping(this::convertToCny, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        return monthlyTotals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("month", entry.getKey().format(DateTimeFormatter.ofPattern("yyyy-MM")));
                    map.put("total", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/reports/stacked-bar-data")
    public List<List<Object>> getStackedBarData() {
        List<Asset> allAssets = assetRepository.findAll();

        // 1. Group data by month and then by asset type
        Map<YearMonth, Map<String, BigDecimal>> data = allAssets.stream()
                .collect(Collectors.groupingBy(
                        asset -> YearMonth.from(asset.getEntryDate()),
                        Collectors.groupingBy(
                                Asset::getType,
                                Collectors.mapping(this::convertToCny, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                        )
                ));

        // 2. Get all unique, sorted months and types
        List<YearMonth> sortedMonths = data.keySet().stream().sorted().collect(Collectors.toList());
        List<String> allTypes = allAssets.stream().map(Asset::getType).distinct().sorted().collect(Collectors.toList());

        // 3. Create the header row for the dataset
        List<Object> header = Stream.concat(
                Stream.of("product"), // ECharts dimension name
                sortedMonths.stream().map(ym -> ym.format(DateTimeFormatter.ofPattern("yyyy-MM")))
        ).collect(Collectors.toList());

        List<List<Object>> dataset = new ArrayList<>();
        dataset.add(header);

        // 4. Create a data row for each asset type
        for (String type : allTypes) {
            List<Object> row = new ArrayList<>();
            row.add(type);
            for (YearMonth month : sortedMonths) {
                BigDecimal value = data.getOrDefault(month, Collections.emptyMap()).getOrDefault(type, BigDecimal.ZERO);
                row.add(value);
            }
            dataset.add(row);
        }

        return dataset;
    }

    private BigDecimal convertToCny(Asset asset) {
        if (asset == null || asset.getValue() == null) {
            return BigDecimal.ZERO;
        }
        if ("SGD".equalsIgnoreCase(asset.getCurrency())) {
            return asset.getValue().multiply(SGD_TO_CNY_RATE).setScale(2, RoundingMode.HALF_UP);
        }
        return asset.getValue();
    }

    @GetMapping("/stocks/data")
    public ResponseEntity<String> getStockData(@RequestParam String symbol, 
                                                 @RequestParam Long period1, 
                                                 @RequestParam Long period2) {
        logger.info("Fetching stock data for symbol: {}", symbol);
        try {
            String data = stockService.getStockData(symbol, period1, period2);
            logger.info("Successfully retrieved stock data for symbol: {}", symbol);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            logger.error("Error fetching stock data for symbol: {}", symbol, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
