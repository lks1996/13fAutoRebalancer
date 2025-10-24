package com.autoRebalancer.Kis.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDto {

    // 종합계좌번호
    String cano;
    // 계좌상품코드
    String acntPrdtCd;
    // 시간외단일가, 거래소여부
    String afhrFlprYn = "N";
    // 오프라인여부
    String oflYn = "";
    // 조회구분
    String inorDvsn = "01";
    // 단가구분
    String unprDvsn = "01";
    // 펀드결제분포함여부
    String fundSttlIcldYn = "Y";
    // 융자금액자동상환여부
    String fncgAmtAutoRdptYn = "N";
    // 처리구분
    String prcsDvsn = "00";
    // 연속조회검색조건 100
    String ctxAreaFk100 = "";
    // 연속조회키 100
    String ctxAreaNk100 = "";


    // 조건 시장 분류 코드( J:KRX, NX:NXT, UN:통합 )
    String fidCondMrktDivCode = "UN";
    // 입력 종목코드 (ex 005930 삼성전자)
    String fidInputIscd = "";


    // 주문타입
    int orderType;
    // 종목코드(6자리)
    String pdno = "";
    // 종목이름
    String prdtName = "";
    // 주문구분 ( 00 : KRX NXT SOR 지정가 , 01 : KRX SOR 시장가)
    String ordDvsn = "00";
    // 주문수량
    String ordQty;
    // 주문단가
    String ordUnpr;

    // 조회시작일자( YYYYMMDD )
    String inqrStrtDt;
    // 조회종료일자( YYYYMMDD )
    String inqrEndDt;
    // 매도매수구분코드( 00 : 전체 / 01 : 매도 / 02 : 매수 )
    String sllBuyDvsnCd;
    // 주문채번지점번호
    String ordGnoBrno;
    // 필수값 아님 !! 주문시 한국투자증권 시스템에서 채번된 주문번호
    String odno = "";
    // 체결구분( '00 전체 / 01 체결 / 02 미체결' )
    String ccldDvsn;
    // 조회구분( '00 역순 / 01 정순' )
    String inqrDvsn;
    // 조회구분1( '없음: 전체 / 1: ELW? 2: 프리보드' )
    String inqrDvsn1="";
    // 조회구분3( '00 전체 / 01 현금 / 02 신용 / 03 담보 / 04 대주 / 05 대여 / 06 자기융자신규/상환 / 07 유통융자신규/상환' )
    String inqrDvsn3="";
    // 거래소ID구분코드( 한국거래소 : KRX / 대체거래소 (NXT) : NXT / SOR (Smart Order Routing) : SOR / ALL : 전체 )
    String excgIdDvsnCd="ALL";

}
