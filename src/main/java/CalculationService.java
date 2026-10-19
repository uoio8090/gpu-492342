import java.util.List;

import exception.AppException;
import gpu.GpuCalculator;
import gpu.RpnParser;
import model.Member;
import model.Pending;

/** GPU 연산 메뉴: 새 수식 계산의 진입점과 중지된 연산 이어서 진행. */
public class CalculationService {

    /** 새 수식을 입력받아 계산을 시작한다. 중지된 연산이 있으면 버릴지 확인한다. */
    static void startNewCalculation(Member currentMember) {
        String expression = Main.prompt("수식 (예: 3 + 4 * (2 - 1.5)): ");
        List<String> rpn = RpnParser.convertToRpn(expression); // 잘못된 수식이면 여기서 중단

        if (currentMember.pendingCalculation != null) {
            System.out.println("중지된 연산이 있습니다: " + currentMember.pendingCalculation.expression);
            String answer = Main.prompt("기존 연산을 버리고 새 수식을 시작할까요? (y/n): ").trim();

            if (!answer.equalsIgnoreCase("y")) {
                System.out.println("새 수식을 시작하지 않았습니다.");
                return;
            }

            currentMember.pendingCalculation = null;
            Main.saveMembers();
        }

        Pending calculation = new Pending();
        calculation.expression = expression.trim();
        calculation.rpn = rpn;
        GpuCalculator.runCalculation(currentMember, calculation, Main::saveMembers);
    }

    /** 포인트 부족으로 중지된 연산을 이어서 진행한다. */
    static void resumeCalculation(Member currentMember) {
        if (currentMember.pendingCalculation == null) {
            System.out.println("중지된 연산이 없습니다.");
            return;
        }

        System.out.println("이어서 진행: " + currentMember.pendingCalculation.expression);
        GpuCalculator.runCalculation(currentMember, currentMember.pendingCalculation, Main::saveMembers);
    }
}
