package com.greencart.dto;

import com.greencart.entity.Supplier;

import java.math.BigDecimal;

public record SupplierPerformanceDto(
        Supplier supplier,
        long totalOrders,
        long receivedOrders,
        long cancelledOrders,
        long onTimeOrders,
        BigDecimal completionRate,
        BigDecimal onTimeRate
) {}
