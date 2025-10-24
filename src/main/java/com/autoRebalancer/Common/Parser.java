package com.autoRebalancer.Common;

public class Parser {

    /**
     * 구글 시트 데이터의 Object 값 double로 변환.
     * @param value
     * @return
     */
    public static Double safeParseDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        try {
            String str = value.toString().replace("%", "").trim();
            if (str.isEmpty()) {
                return 0.0;
            }
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return 0.0; // 혹은 null 반환도 가능
        }
    }

    /**
     * 구글 시트 데이터의 Object 값 long으로 변환.
     * @param value
     * @return
     */
    public static long safeParseLong(Object value) {
        long result = 0;
        try {
            // 1. Object -> String 변환
            String sshPrnamtStr = value.toString().trim();
            // 2. String -> long 파싱 (소수점 제거 필요 시 추가 처리)
            // 만약 Double(10778.0) 형태일 수 있다면, 먼저 Double로 변환 후 long으로 캐스팅
            if (sshPrnamtStr.contains(".")) {
                result = (long) Double.parseDouble(sshPrnamtStr);
            } else {
                result = Long.parseLong(sshPrnamtStr);
            }
        } catch (NumberFormatException e) {
            System.err.println("long 변환 실패.");
        }
        return result;
    }
}
