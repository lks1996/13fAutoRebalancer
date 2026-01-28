package com.autoRebalancer._13f.Service;

import com.autoRebalancer._13f.Dto.Filing;
import com.autoRebalancer._13f.Dto.Holding;
import com.autoRebalancer._13f.Dto.sec.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecDataScrapService implements DataScrapService {

    // [중요] SEC는 User-Agent가 없으면 요청이 차단됨.
    private static final String USER_AGENT = "MyPortfolioTracker contact@example.com";

    // SEC RSS URL: 최근 제출된 13F-HR 공시 최대 100건 조회 (Atom 포맷)
    private static final String SEC_RSS_URL = "https://www.sec.gov/cgi-bin/browse-edgar?action=getcurrent&type=13F-HR&count=100&output=atom";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper jsonMapper = new ObjectMapper(); // JSON 파싱용 (Primary Doc 찾기)
    private final XmlMapper xmlMapper = new XmlMapper();        // XML 파싱용 (RSS, Holding 조회)

    /**
     * 최근 공시 목록 조회 (RSS 활용)
     * RSS 데이터를 파싱하고, 제출일을 기준으로 보고서 기준일(PeriodOfReport) 계산.
     */
    @Override
    public List<Filing> getFilings() throws IOException, InterruptedException {
        log.info("Scanning recent 13F filings via SEC RSS...");

        String xmlData = sendGetRequest(SEC_RSS_URL);
        SecRssFeed feed = xmlMapper.readValue(xmlData, SecRssFeed.class);

        List<Filing> filingList = new ArrayList<>();
        if (feed.getEntries() != null) {
            for (SecRssEntry entry : feed.getEntries()) {
                // RSS Entry -> Filing 변환 (여기서 Period 계산 로직 수행)
                Filing filing = mapEntryToFiling(entry);
                filingList.add(filing);
            }
        }

        log.info("Found {} recent filings from SEC RSS.", filingList.size());
        return filingList;
    }

    /**
     * 상세 보유 종목 조회
     * 특정 CIK와 AccessionNumber를 이용해 XML 파일 다운로드 및 파싱.
     */
    @Override
    public List<Holding> getHoldings(String cik, String accessionNumber) throws IOException, InterruptedException {
        // 1. URL용 CIK 변환 (Leading Zero 제거)
        // 예: "0001535172" -> "1535172"
        String cleanCik = String.valueOf(Long.parseLong(cik));
        String cleanAccessionNum = accessionNumber.replace("-", "");

        // 2. 해당 공시의 파일 목록(index.json)을 조회하여 'Information Table' XML 파일명 찾기
        String infoTableFileName = findInfoTableFileName(cleanCik, cleanAccessionNum);

        if (infoTableFileName == null) {
            log.error("Cannot find Information Table XML for Accession# {}", accessionNumber);
            return new ArrayList<>();
        }

        // 3. XML 다운로드 URL 생성 (301 방지를 위해 cleanCik 사용)
        String xmlUrl = String.format("https://www.sec.gov/Archives/edgar/data/%s/%s/%s",
                cleanCik, cleanAccessionNum, infoTableFileName);

        log.info("Fetching holdings XML from: {}", xmlUrl);
        String xmlData = sendGetRequest(xmlUrl);

        // 4. 파싱 및 매핑
        SecHoldingTable infoTable = xmlMapper.readValue(xmlData, SecHoldingTable.class);

        List<Holding> holdings = new ArrayList<>();
        if (infoTable.getHoldings() != null) {
            for (SecHolding secHolding : infoTable.getHoldings()) {
                Holding h = mapSecHoldingToRecord(secHolding, cik, accessionNumber);
                holdings.add(h);
            }
        }

        log.info("Parsed {} holdings for CIK {}", holdings.size(), cik);
        return holdings;
    }

    // --- Helper Methods ---
    /**
     * [신규] 공시 디렉토리의 index.json을 뒤져서 실제 종목 데이터가 든 XML 파일을 찾습니다.
     * 보통 이름에 'infotable', 'information', 'xml_filing' 등이 포함되어 있습니다.
     */
    private String findInfoTableFileName(String cleanCik, String cleanAccessionNum) {
        String indexUrl = String.format("https://www.sec.gov/Archives/edgar/data/%s/%s/index.json",
                cleanCik, cleanAccessionNum);
        try {
            String json = sendGetRequest(indexUrl);
            JsonNode root = jsonMapper.readTree(json);
            JsonNode items = root.path("directory").path("item");

            if (items.isArray()) {
                for (JsonNode item : items) {
                    String name = item.path("name").asText();
                    // XML 파일이면서, 이름이 정보 테이블(infotable)과 관련 있어 보이는 것 찾기
                    if (name.endsWith(".xml")) {
                        // primary_doc.xml은 표지이므로 제외
                        if (name.equalsIgnoreCase("primary_doc.xml")) continue;

                        // 보통 'infotable.xml', 'form13fInfoTable.xml', 'xml_filing.xml' 등으로 명명됨
                        if (name.toLowerCase().contains("info") || name.toLowerCase().contains("table") || name.contains("xml_filing")) {
                            return name;
                        }
                    }
                }

                // 만약 위 규칙으로 못 찾았다면, primary_doc.xml이 아닌 첫 번째 XML을 반환 (Fallback)
                for (JsonNode item : items) {
                    String name = item.path("name").asText();
                    if (name.endsWith(".xml") && !name.equalsIgnoreCase("primary_doc.xml")) {
                        return name;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to find info table file for CIK {}", cleanCik, e);
        }
        return null;
    }

    /**
     * RSS Entry를 Filing Record로 매핑
     * 핵심 로직: calculatePeriodOfReport()를 통해 정렬에 필요한 날짜를 채움
     */
    private Filing mapEntryToFiling(SecRssEntry entry) {
        String title = entry.getTitle();
        String cik = extractCikFromTitle(title);
        String companyName = extractCompanyNameFromTitle(title);
        String url = entry.getLink().getHref();
        String accessionNumber = extractAccessionNumberFromUrl(url);

        // 날짜 포맷 (YYYY-MM-DD)
        String filingDateStr = entry.getUpdated().length() >= 10
                ? entry.getUpdated().substring(0, 10)
                : entry.getUpdated();

        // [핵심] 제출일을 기반으로 보고 기준일(Period of Report) 계산
        String estimatedPeriod = calculatePeriodOfReport(filingDateStr);

        return new Filing(
                url,                    // url
                accessionNumber,        // accession_number
                "13F-HR",              // submission_type
                0,                      // public_document_count
                estimatedPeriod,        // [중요] 계산된 PeriodOfReport 주입
                filingDateStr,          // filed_as_of_date
                null,                   // date_as_of_change
                null,                   // effectiveness_date
                cik,                    // cik
                companyName,            // company_name
                null,                   // irs_number
                null,                   // state_of_incorporation
                null,                   // fiscal_year_end
                "13F-HR",               // form_type
                null,                   // sec_act
                null,                   // sec_file_number
                null,                   // film_number
                null,                   // business_address
                null,                   // business_phone
                0L,                     // table_value_total
                0L,                     // table_entry_total
                false,                  // is_amendment
                null,                   // amendment_type
                false,                  // conf_denied_expired
                null,                   // conf_date_denied_expired
                null                    // amendment_date_reported
        );
    }

    /**
     * SEC XML Holding -> User Holding Record 매핑
     */
    private Holding mapSecHoldingToRecord(SecHolding sec, String cik, String accessionNumber) {
        long sshPrnamt = (sec.getShrsOrPrnAmt() != null) ? sec.getShrsOrPrnAmt().getSshPrnamt() : 0;
        String sshPrnamtType = (sec.getShrsOrPrnAmt() != null) ? sec.getShrsOrPrnAmt().getSshPrnamtType() : "UNKNOWN";
        long sole = (sec.getVotingAuthority() != null) ? sec.getVotingAuthority().getSole() : 0;
        long shared = (sec.getVotingAuthority() != null) ? sec.getVotingAuthority().getShared() : 0;
        long none = (sec.getVotingAuthority() != null) ? sec.getVotingAuthority().getNone() : 0;

        return new Holding(
                accessionNumber, cik, sec.getNameOfIssuer(), sec.getTitleOfClass(),
                sec.getCusip(), null, sec.getValue(), sshPrnamt, sshPrnamtType,
                sec.getInvestmentDiscretion(), sole, shared, none, sec.getPutCall()
        );
    }

    /**
     * 제출일(Filing Date)을 기준으로 보고 기준일(Period Of Report) 역산 로직
     * 13F는 분기 종료 후 45일 이내 제출됨.
     */
    private String calculatePeriodOfReport(String filingDateStr) {
        try {
            LocalDate filingDate = LocalDate.parse(filingDateStr);
            int month = filingDate.getMonthValue();
            int year = filingDate.getYear();

            // 1, 2, 3월 제출 -> 작년 12월 31일 (Q4)
            if (month <= 3) {
                return LocalDate.of(year - 1, 12, 31).toString();
            }
            // 4, 5, 6월 제출 -> 올해 3월 31일 (Q1)
            else if (month <= 6) {
                return LocalDate.of(year, 3, 31).toString();
            }
            // 7, 8, 9월 제출 -> 올해 6월 30일 (Q2)
            else if (month <= 9) {
                return LocalDate.of(year, 6, 30).toString();
            }
            // 10, 11, 12월 제출 -> 올해 9월 30일 (Q3)
            else {
                return LocalDate.of(year, 9, 30).toString();
            }
        } catch (Exception e) {
            log.warn("Failed to calculate period of report for date: {}", filingDateStr);
            return filingDateStr; // 계산 실패 시 제출일이라도 반환
        }
    }

    private String sendGetRequest(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("SEC API Error: " + response.statusCode() + " URL: " + url);
        }
        return response.body();
    }

    private String findPrimaryDocumentName(String cik, String targetAccessionNum) throws IOException, InterruptedException {
        String url = "https://data.sec.gov/submissions/CIK" + cik + ".json";
        try {
            String json = sendGetRequest(url);
            JsonNode root = jsonMapper.readTree(json);

            JsonNode recent = root.path("filings").path("recent");
            JsonNode accNums = recent.get("accessionNumber");
            JsonNode docs = recent.get("primaryDocument");

            if (accNums != null && accNums.isArray()) {
                for (int i = 0; i < accNums.size(); i++) {
                    if (accNums.get(i).asText().equals(targetAccessionNum)) {
                        return docs.get(i).asText();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to find primary document for CIK {}", cik, e);
        }
        return null;
    }

    // --- Parsing Helpers ---
    private String extractCikFromTitle(String title) {
        try {
            int end = title.lastIndexOf(')');
            int lastOpen = title.lastIndexOf('(', end - 1);
            String temp = title.substring(lastOpen + 1, end);
            if (!temp.matches("\\d+")) {
                int realEnd = title.lastIndexOf('(', lastOpen - 1);
                int realStart = title.lastIndexOf('(', realEnd);
                return title.substring(realStart + 1, title.indexOf(')', realStart));
            } else {
                return temp;
            }
        } catch (Exception e) {
            return title.replaceAll("[^0-9]", "");
        }
    }

    private String extractCompanyNameFromTitle(String title) {
        try {
            // 1. 기본 추출: 첫 번째 "-" 뒤부터 첫 번째 "(" 앞까지
            // 예: "13F-HR - R/A - Cresset Asset Management, LLC (000...)"
            int start = title.indexOf("-") + 2;
            int end = title.indexOf("(");

            if (end == -1) end = title.length();

            String rawName = title.substring(start, end).trim();

            // "R - ", "A - ", "R/A - " 같은 패턴을 지움
            // 정규식 설명: ^(시작부분) (R 또는 A 또는 R/A) (공백가능) - (공백가능)
            String cleanName = rawName.replaceAll("^(R|A|R/A)\\s?-\\s?", "").trim();

            return cleanName;

        } catch (Exception e) {
            log.warn("Failed to extract company name from title: {}", title);
            return "UNKNOWN";
        }
    }

    private String extractAccessionNumberFromUrl(String url) {
        try {
            String temp = url.substring(0, url.lastIndexOf("-index.htm"));
            return temp.substring(temp.lastIndexOf('/') + 1);
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}