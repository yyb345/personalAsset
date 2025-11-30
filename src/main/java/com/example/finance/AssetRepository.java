package com.example.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    /**
     * Finds all assets recorded between two dates.
     * @param startDate The start date of the period.
     * @param endDate The end date of the period.
     * @return A list of assets.
     */
    List<Asset> findAllByEntryDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Finds all distinct months (formatted as 'YYYY-MM') that have asset entries.
     * This uses a native SQLite query.
     * @return A list of strings representing the months, sorted in descending order.
     */
    @Query(value = "SELECT DISTINCT strftime('%Y-%m', entry_date) FROM asset ORDER BY 1 DESC", nativeQuery = true)
    List<String> findDistinctMonths();
}
