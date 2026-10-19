package view;

import java.util.List;

import model.Member;
import model.Payment;
import util.Utils;

/** 회원 정보(포인트 잔액, 로그인 이력, 결제 이력) 조회 화면 출력. */
public class MemberInfoViewer {

    /** 포인트 잔액(결제/보너스/합계)을 출력한다. */
    public static void showBalance(Member member) {
        System.out.println("결제 포인트: " + member.points.paid
                + " / 보너스 포인트: " + member.points.bonus
                + " / 합계: " + Utils.totalPoints(member));
    }

    /** 로그인 횟수와 이력을 최신순으로 출력한다. */
    public static void showLoginHistory(Member member) {
        List<String> history = member.loginHistory;
        String lastLogin = history.isEmpty() ? null : history.get(history.size() - 1);
        System.out.println("총 로그인 횟수: " + history.size() + "회 / 최근 로그인 시간: " + Utils.formatTime(lastLogin));

        for (int i = history.size() - 1; i >= 0; i--) {
            System.out.println("  " + Utils.formatTime(history.get(i)));
        }
    }

    /** 결제 이력을 최신순으로 출력한다. */
    public static void showPaymentHistory(Member member) {
        if (member.paymentHistory.isEmpty()) {
            System.out.println("결제 이력이 없습니다.");
        }

        for (int i = member.paymentHistory.size() - 1; i >= 0; i--) {
            Payment payment = member.paymentHistory.get(i);
            System.out.println("  " + Utils.formatTime(payment.time) + "  " + payment.amount + "원  " + payment.result);
        }
    }
}
