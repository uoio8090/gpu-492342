package gpu;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import exception.AppException;

/** 수식 문자열을 검사하고 후위 표기식(RPN)으로 변환한다. */
public class RpnParser {

    /**
     * 수식 문자열을 검사하고 후위 표기식(RPN) 토큰 목록으로 변환한다.
     * 1단계: 문자를 토큰으로 나누며 문법을 검사한다. (단항 마이너스는 "neg" 로 표시)
     * 2단계: 연산자 우선순위에 따라 후위 표기로 재배열한다.
     */
    public static List<String> convertToRpn(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new AppException("수식 오류: 수식이 비어 있습니다.");
        }

        // --- 1단계: 토큰화 및 문법 검사 ---
        List<String> tokens = new ArrayList<>();
        boolean expectOperand = true; // 지금 숫자/여는 괄호/단항 마이너스가 와야 하는 자리인지
        int parenDepth = 0;           // 열린 괄호 개수
        int index = 0;
        int length = expression.length();

        while (index < length) {
            char currentChar = expression.charAt(index);

            if (Character.isWhitespace(currentChar)) {
                index++;
            } else if (isDigit(currentChar)) {
                if (!expectOperand) {
                    throw new AppException("수식 오류: " + (index + 1) + "번째 위치에 연산자가 필요합니다.");
                }

                int start = index;

                // 정수부
                while (index < length && isDigit(expression.charAt(index))) {
                    index++;
                }

                // 소수부 (있다면 '.' 뒤에 숫자가 반드시 와야 함)
                if (index < length && expression.charAt(index) == '.') {
                    index++;

                    if (index >= length || !isDigit(expression.charAt(index))) {
                        throw new AppException("수식 오류: 소수점 뒤에 숫자가 필요합니다.");
                    }

                    while (index < length && isDigit(expression.charAt(index))) {
                        index++;
                    }
                }

                tokens.add(expression.substring(start, index));
                expectOperand = false;
            } else if (currentChar == '(') {
                if (!expectOperand) {
                    throw new AppException("수식 오류: '(' 앞에 연산자가 필요합니다.");
                }

                tokens.add("(");
                parenDepth++;
                index++;
            } else if (currentChar == ')') {
                if (expectOperand) {
                    throw new AppException("수식 오류: ')' 앞에 숫자가 필요합니다.");
                }

                if (parenDepth == 0) {
                    throw new AppException("수식 오류: 짝이 맞지 않는 ')' 입니다.");
                }

                tokens.add(")");
                parenDepth--;
                index++;
            } else if (currentChar == '+' || currentChar == '*' || currentChar == '/') {
                if (expectOperand) {
                    throw new AppException("수식 오류: '" + currentChar + "' 앞에 숫자가 필요합니다.");
                }

                tokens.add("" + currentChar);
                expectOperand = true;
                index++;
            } else if (currentChar == '-') {
                // 피연산자 자리에 나온 '-' 는 부호(neg), 아니면 뺄셈
                tokens.add(expectOperand ? "neg" : "-");
                expectOperand = true;
                index++;
            } else {
                throw new AppException("수식 오류: 허용되지 않는 문자 '" + currentChar + "' (" + (index + 1) + "번째)");
            }
        }

        if (expectOperand) {
            throw new AppException("수식 오류: 수식이 숫자 또는 ')'로 끝나야 합니다.");
        }

        if (parenDepth != 0) {
            throw new AppException("수식 오류: 닫히지 않은 '(' 가 있습니다.");
        }

        // --- 2단계: 후위 표기 변환 (shunting-yard) ---
        List<String> rpn = new ArrayList<>();
        Deque<String> operatorStack = new ArrayDeque<>();

        for (String token : tokens) {
            if (isDigit(token.charAt(0))) {
                rpn.add(token);
            } else if (token.equals("neg") || token.equals("(")) {
                operatorStack.push(token);
            } else if (token.equals(")")) {
                // 여는 괄호가 나올 때까지 연산자를 출력으로 옮긴다
                while (!operatorStack.peek().equals("(")) {
                    rpn.add(operatorStack.pop());
                }

                operatorStack.pop();
            } else {
                // 우선순위가 같거나 높은 연산자를 먼저 출력
                while (!operatorStack.isEmpty()
                        && !operatorStack.peek().equals("(")
                        && operatorPriority(operatorStack.peek()) >= operatorPriority(token)) {
                    rpn.add(operatorStack.pop());
                }

                operatorStack.push(token);
            }
        }

        while (!operatorStack.isEmpty()) {
            rpn.add(operatorStack.pop());
        }

        return rpn;
    }

    /** 문자가 0~9 숫자인지 확인한다. */
    private static boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    /** 연산자 우선순위를 반환한다. (neg 3 > * / 2 > + - 1) */
    private static int operatorPriority(String operator) {
        if (operator.equals("neg")) {
            return 3;
        }

        if (operator.equals("*") || operator.equals("/")) {
            return 2;
        }

        return 1;
    }
}
