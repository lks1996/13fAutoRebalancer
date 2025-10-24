package com.autoRebalancer._13f.Dto;

// 구굴 시트에 보유 종목 리스트 출력용.
public record PortfolioHolding(
        String ticker,
        String nameOfIssuer,
        long totalShares,
        long totalValue,
        double totalPercentage // 소수점 포함 가능하도록 double 사용
) {}