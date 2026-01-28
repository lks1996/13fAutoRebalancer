package com.autoRebalancer._13f.Service;

import com.autoRebalancer._13f.Dto.Filing;
import com.autoRebalancer._13f.Dto.Holding;

import java.io.IOException;
import java.util.List;

public interface DataScrapService {

    /**
     * 최근 공시 정보 조회.
     * @return Filing 리스트
     */
    List<Filing> getFilings() throws IOException, InterruptedException;

    /**
     * 특정 공시의 모든 보유 종목 리스트 조회.
     * @param cik 기관의 CIK 번호
     * @param accessionNumber 공시의 고유 번호
     * @return 보유 종목(Holding) 리스트
     */
    List<Holding> getHoldings(String cik, String accessionNumber) throws IOException, InterruptedException ;
}
