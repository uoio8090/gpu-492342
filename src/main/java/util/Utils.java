package util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import exception.AppException;
import model.Member;

/** 여러 곳에서 쓰는 시간/문자/숫자 관련 보조 함수 모음. */
public class Utils {

    static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ===== 포인트 =====

    /** 결제 포인트와 보너스 포인트의 합계를 반환한다. */
    public static long totalPoints(Member member) {
        return member.points.paid + member.points.bonus;
    }

    // ===== 시간 =====

    /** 현재 시각을 초 단위 ISO 문자열로 반환한다. */
    public static String currentTime() {
        return TIME_FORMAT.format(LocalDateTime.now().withNano(0));
    }

    /** 저장된 시각 문자열을 화면 표시용("yyyy-MM-dd HH:mm:ss")으로 바꾼다. null 이면 "-". */
    public static String formatTime(String time) {
        return time == null ? "-" : time.replace('T', ' ');
    }

    // ===== 입력값 =====

    /** 문자열을 1 이상의 정수 금액으로 변환한다. 아니면 AppException. */
    public static long parseAmount(String text) {
        try {
            long amount = Long.parseLong(text.trim());

            if (amount < 1) {
                throw new NumberFormatException();
            }

            return amount;
        } catch (NumberFormatException e) {
            throw new AppException("금액은 1 이상의 정수여야 합니다.");
        }
    }
}
