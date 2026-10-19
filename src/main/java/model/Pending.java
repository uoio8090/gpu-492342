package model;

import java.util.ArrayList;
import java.util.List;

/** 포인트 부족으로 중지된 계산의 진행 상태 */
public class Pending {
    public String expression;                           // 사용자가 입력한 원본 수식
    public List<String> rpn = new ArrayList<>();        // 후위 표기식 토큰
    public int position;                                // 다음에 처리할 rpn 위치
    public List<NumberValue> stack = new ArrayList<>(); // 계산 중간 결과 스택
}
