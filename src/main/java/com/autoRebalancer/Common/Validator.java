package com.autoRebalancer.Common;

import com.autoRebalancer.Kis.Dto.DomesticStockDto;
import com.autoRebalancer.Kis.Dto.OverseasStockDto;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Validator {

    public static boolean isValidDomesticOrder(DomesticStockDto order) {
        if (isBlank(order.getPdno())) {
            log.error("주문 실패: 종목코드 누락");
            return false;
        }
        if (!isPositiveNumeric(order.getOrdQty())) {
            log.error("주문 실패: 잘못된 수량 값 -> {}", order.getOrdQty());
            return false;
        }
        if (!isPositiveNumeric(order.getOrdUnpr())) {
            log.error("주문 실패: 잘못된 주문단가 값 -> {}", order.getOrdUnpr());
            return false;
        }
        if (order.getOrderType() <= 0) {
            log.error("주문 실패: 잘못된 주문유형 값 -> {}", order.getOrderType());
            return false;
        }
        return true;
    }

    public static boolean isValidOverseasOrder(OverseasStockDto order) {
        if (isBlank(order.getOvrsExcgCd())) {
            log.error("주문 실패: 거래소 코드 누락");
            return false;
        }
        if (isBlank(order.getSymb())) {
            log.error("주문 실패: 티커/심볼 코드 누락 -> {}", order.getSymb());
            return false;
        }
        if (!isPositiveNumeric(order.getOrdQty())) {
            log.error("주문 실패: 잘못된 수량 값 -> {}", order.getOrdQty());
            return false;
        }
        if (!isPositiveNumeric(order.getOvrsOrdUnpr())) {
            log.error("주문 실패: 잘못된 주문단가 값 -> {}", order.getOvrsOrdUnpr());
            return false;
        }
        if (order.getOrderType() <= 0) {
            log.error("주문 실패: 잘못된 주문유형 값 -> {}", order.getOrderType());
            return false;
        }
        return true;
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isPositiveNumeric(String value) {
        if (isBlank(value)) return false;
        try {
            return Long.parseLong(value) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
