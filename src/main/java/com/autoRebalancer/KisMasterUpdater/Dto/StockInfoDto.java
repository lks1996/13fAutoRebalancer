package com.autoRebalancer.KisMasterUpdater.Dto;

/**
 * @param symbol         종목 심볼 (티커)
 * @param exchangeId     거래소 ID (예: NAS, NYS)
 * @param realtimeSymbol 실시간 심볼
 * @param koreanName     한글 종목명
 * @param englishName    영문 종목명
 * @param currency       통화
 * @param securityType   증권 종류 (1:Index, 2:Stock, 3:ETF, 4:Warrant)
 */
public record StockInfoDto (
        String symbol,
        String exchangeId,
        String realtimeSymbol,
        String koreanName,
        String englishName,
        String currency,
        String securityType
) {}
