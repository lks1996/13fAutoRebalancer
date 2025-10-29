package com.autoRebalancer.Kis.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverseasStockDto {

    /** 잔고 조회 */
    // 종합계좌번호
    String cano;
    // 계좌상품코드
    String acntPrdtCd;
    // 해외거래소코드
    //    [모의]
    //    NASD : 나스닥
    //    NYSE : 뉴욕
    //    AMEX : 아멕스
    //
    //[실전]
    //    NASD : 미국전체
    //    NAS : 나스닥
    //    NYSE : 뉴욕
    //    AMEX : 아멕스
    //
    //[모의/실전 공통]
    //    SEHK : 홍콩
    //    SHAA : 중국상해
    //    SZAA : 중국심천
    //    TKSE : 일본
    //    HASE : 베트남 하노이
    //    VNSE : 베트남 호치민
    String ovrsExcgCd;

    // 거래통화코드
    //    USD : 미국달러
    //    HKD : 홍콩달러
    //    CNY : 중국위안화
    //    JPY : 일본엔화
    //    VND : 베트남동
    String trCrctCd;
    // 연속조회검색조건 200
    String ctxAreaFk200 = "";
    // 연속조회키 200
    String ctxAreaNk200 = "";

    /** 시세 조회 */
    // 사용자권한정보 	"" (Null 값 설정)
    String auth = "";
    // 거래소코드
    //    HKS : 홍콩
    //    NYS : 뉴욕
    //    NAS : 나스닥
    //    AMS : 아멕스
    //    TSE : 도쿄
    //    SHS : 상해
    //    SZS : 심천
    //    SHI : 상해지수
    //    SZI : 심천지수
    //    HSX : 호치민
    //    HNX : 하노이
    //    BAY : 뉴욕(주간)
    //    BAQ : 나스닥(주간)
    //    BAA : 아멕스(주간)
    String excd;
    // 종목코드
    String symb;


    /** 해외주식 주문 */
    // 주문타입
    int orderType;
    // 주문 수량
    String ordQty;
    // 해외주문단가
    String ovrsOrdUnpr;
    // 주문서버 구분코드
    String ordSvrDvsnCd;
    // 주문구분
    //[Header tr_id TTTT1002U(미국 매수 주문)]
    //00 : 지정가
    //32 : LOO(장개시지정가)
    //34 : LOC(장마감지정가)
    //35 : TWAP (시간가중평균)
    //36 : VWAP (거래량가중평균)
    //* 모의투자 VTTT1002U(미국 매수 주문)로는 00:지정가만 가능
    //* TWAP, VWAP 주문은 분할시간 주문 입력 필수
    //
    //[Header tr_id TTTT1006U(미국 매도 주문)]
    //00 : 지정가
    //31 : MOO(장개시시장가)
    //32 : LOO(장개시지정가)
    //33 : MOC(장마감시장가)
    //34 : LOC(장마감지정가)
    //35 : TWAP (시간가중평균)
    //36 : VWAP (거래량가중평균)
    //* 모의투자 VTTT1006U(미국 매도 주문)로는 00:지정가만 가능
    //* TWAP, VWAP 주문은 분할시간 주문 입력 필수
    String ordDvsn = "00";
    // 종목 이름
    String symbName;


    /** 해외주식 주문체결내역 조회 */
    // 주문시작일자
    String ordStrtDt;
    // 주문종료일자
    String ordEndDt;
    // 매도매수구분
    String sllBuyDvsnCd;
    // 체결미체결구분
    String ccldNccsDvsn;
    // 정렬순서
    String sortSqn;
    // 주문일자
    String ordDt;
    // 주문채번지점번호
    String ordGnoBrno;
    // 주문번호
    String ordNo;
}
