package gpu;

import java.math.BigDecimal;
import java.math.RoundingMode;

import exception.AppException;
import model.Member;
import model.NumberValue;
import model.Pending;
import util.Utils;

/** GPU 연산 기능: 후위 표기식을 순서대로 계산하며 연산마다 포인트를 차감한다. */
public class GpuCalculator {

    /**
     * 후위 표기식을 position 부터 순서대로 계산한다.
     * 연산자마다 포인트를 차감하며, 포인트가 부족하면 진행 상태를 저장하고 일시 중지한다.
     * 요금: + - 는 1, * 는 10, / 는 100 포인트이며 소수가 섞이면 5배.
     */
    public static void runCalculation(Member member, Pending calculation, Runnable saveMembers) {
        while (calculation.position < calculation.rpn.size()) {
            String token = calculation.rpn.get(calculation.position);
            int stackSize = calculation.stack.size();

            if (isOperator(token)) {
                // 이항 연산: 스택에서 두 피연산자를 꺼내야 한다
                if (stackSize < 2) {
                    member.pendingCalculation = null;
                    saveMembers.run();
                    throw new AppException("연산 상태가 올바르지 않아 종료합니다.");
                }

                NumberValue right = calculation.stack.get(stackSize - 1);
                NumberValue left = calculation.stack.get(stackSize - 2);
                boolean isDecimal = left.decimal || right.decimal;
                int baseCost = token.equals("+") || token.equals("-") ? 1 : token.equals("*") ? 10 : 100;
                int cost = baseCost * (isDecimal ? 5 : 1);

                // 포인트 부족 → 진행 상태를 저장하고 중지
                if (Utils.totalPoints(member) < cost) {
                    int operatorCount = 0;

                    for (int i = 0; i <= calculation.position; i++) {
                        if (isOperator(calculation.rpn.get(i))) {
                            operatorCount++;
                        }
                    }

                    member.pendingCalculation = calculation;
                    saveMembers.run();
                    System.out.println("포인트가 부족하여 " + operatorCount + "번째 연산에서 일시 중지되었습니다.");
                    System.out.println("필요한 포인트: " + cost + " / 보유한 포인트: " + Utils.totalPoints(member));
                    System.out.println("충전 후 '중지된 연산 이어서 진행'을 선택해 주세요.");
                    return;
                }

                // 보너스 포인트를 먼저 사용하고, 모자란 만큼 결제 포인트에서 차감
                long bonusUsed = Math.min(member.points.bonus, cost);
                member.points.bonus -= bonusUsed;
                member.points.paid -= cost - bonusUsed;
                saveMembers.run();
                System.out.println(formatNumber(left) + " " + token + " " + formatNumber(right)
                        + " → " + cost + "포인트 차감, 잔액 " + Utils.totalPoints(member));

                calculation.stack.remove(stackSize - 1);
                calculation.stack.remove(stackSize - 2);

                BigDecimal leftValue = new BigDecimal(left.value);
                BigDecimal rightValue = new BigDecimal(right.value);
                BigDecimal result;

                if (token.equals("+")) {
                    result = leftValue.add(rightValue);
                } else if (token.equals("-")) {
                    result = leftValue.subtract(rightValue);
                } else if (token.equals("*")) {
                    result = leftValue.multiply(rightValue);
                } else {
                    if (rightValue.signum() == 0) {
                        member.pendingCalculation = null;
                        saveMembers.run();
                        throw new AppException("0으로 나눌 수 없습니다. 연산을 종료합니다.");
                    }

                    if (!isDecimal && leftValue.remainder(rightValue).signum() == 0) {
                        // 정수끼리 나누어떨어지면 정수 결과
                        result = leftValue.divide(rightValue);
                    } else {
                        // 나누어떨어지지 않으면 소수 10자리까지 반올림, 결과는 소수로 취급
                        result = leftValue.divide(rightValue, 10, RoundingMode.HALF_UP).stripTrailingZeros();

                        if (result.scale() < 0) {
                            result = result.setScale(0);
                        }

                        isDecimal = true;
                    }
                }

                calculation.stack.add(createNumber(result, isDecimal));
            } else if (token.equals("neg")) {
                // 단항 마이너스: 스택 맨 위 값의 부호를 뒤집는다
                NumberValue operand = calculation.stack.remove(stackSize - 1);
                calculation.stack.add(createNumber(new BigDecimal(operand.value).negate(), operand.decimal));
            } else {
                // 숫자 토큰: 스택에 쌓는다
                calculation.stack.add(createNumber(new BigDecimal(token), token.contains(".")));
            }

            calculation.position++;
        }

        member.pendingCalculation = null;
        saveMembers.run();
        System.out.println("결과: " + formatNumber(calculation.stack.get(0)));
    }

    /** 토큰이 사칙연산자(+ - * /) 중 하나인지 확인한다. */
    private static boolean isOperator(String token) {
        return token.length() == 1 && "+-*/".indexOf(token.charAt(0)) >= 0;
    }

    /** BigDecimal 값과 소수 여부로 NumberValue 를 만든다. */
    private static NumberValue createNumber(BigDecimal value, boolean decimal) {
        NumberValue number = new NumberValue();
        number.value = value.toPlainString();
        number.decimal = decimal;
        return number;
    }

    /** 출력용 문자열로 바꾼다. 불필요한 0은 지우고, 소수로 취급되는 정수는 "3.0" 형태로 표시한다. */
    private static String formatNumber(NumberValue number) {
        String text = new BigDecimal(number.value).stripTrailingZeros().toPlainString();

        if (number.decimal && !text.contains(".")) {
            return text + ".0";
        }

        return text;
    }
}
