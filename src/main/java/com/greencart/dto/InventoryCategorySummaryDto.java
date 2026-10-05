package com.greencart.dto;

import java.math.BigDecimal;

public record InventoryCategorySummaryDto(
        String category,
        long productCount,
        long totalUnits,
        BigDecimal stockValue
) {}
