import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {

    public static class Card { public String number, password; }
    public static class Payment { public String time; public long amount; public String result; }
    public static class NumVal { public String value; public boolean decimal; }
    public static class Points { public long paid, bonus; }
    public static class Pending {
        public String expression; public List<String> rpn = new ArrayList<>(); public int position; public List<NumVal> stack = new ArrayList<>();
    }
    public static class Member {
        public String id, name, role; public Card creditCard; public Points points = new Points();
        public List<String> loginHistory = new ArrayList<>(); public List<Payment> paymentHistory = new ArrayList<>();
        public Pending pendingCalculation;
    }
    public static class Root { public List<Member> members = new ArrayList<>(); }

    static class AppEx extends RuntimeException { AppEx(String msg) { super(msg); } }
    static class EofEx extends RuntimeException { }

    static final Path FILE = Path.of("data", "members.json"), TMP_FILE = Path.of("data", "members.json.tmp");
    static final ObjectMapper MAPPER = new ObjectMapper();
    static final Map<String, Member> members = new LinkedHashMap<>();
    static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    static BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

    static void load() throws IOException {
        if (!Files.exists(FILE)) return;
        String js = Files.readString(FILE);
        if (js.isBlank()) throw new IOException("데이터 파일이 비어 있습니다.");
        Root rt = MAPPER.readValue(js, Root.class);
        if (rt == null || rt.members == null) throw new IOException("데이터 파일에 members 목록이 없습니다.");
        for (Member mem : rt.members) members.put(mem.id, mem);
    }

    static void save() {
        try {
            Files.createDirectories(Path.of("data"));
            Root rt = new Root();
            rt.members = new ArrayList<>(members.values());
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(TMP_FILE.toFile(), rt);
            try { Files.move(TMP_FILE, FILE, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(TMP_FILE, FILE, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException e) { throw new AppEx("저장 실패: " + e.getMessage()); }
    }

    static String now() { return TIME_FMT.format(LocalDateTime.now().withNano(0)); }
    static String fmtTime(String time) { return time == null ? "-" : time.replace('T', ' '); }
    static long total(Member mem) { return mem.points.paid + mem.points.bonus; }

    static boolean isOp(String tok) { return tok.length() == 1 && "+-*/".indexOf(tok.charAt(0)) >= 0; }
    static boolean isDigit(char ch) { return ch >= '0' && ch <= '9'; }
    static List<String> toRpn(String expr) {
        if (expr == null || expr.isBlank()) throw new AppEx("수식 오류: 수식이 비어 있습니다.");
        List<String> toks = new ArrayList<>();
        boolean needNum = true;
        int dep = 0, i = 0, len = expr.length();
        while (i < len) {
            char ch = expr.charAt(i);
            if (Character.isWhitespace(ch)) { i++; }
            else if (isDigit(ch)) {
                if (!needNum) throw new AppEx("수식 오류: " + (i + 1) + "번째 위치에 연산자가 필요합니다.");
                int beg = i;
                while (i < len && isDigit(expr.charAt(i))) i++;
                if (i < len && expr.charAt(i) == '.') {
                    i++;
                    if (i >= len || !isDigit(expr.charAt(i))) throw new AppEx("수식 오류: 소수점 뒤에 숫자가 필요합니다.");
                    while (i < len && isDigit(expr.charAt(i))) i++;
                }
                toks.add(expr.substring(beg, i)); needNum = false;
            }
            else if (ch == '(') { if (!needNum) throw new AppEx("수식 오류: '(' 앞에 연산자가 필요합니다."); toks.add("("); dep++; i++; }
            else if (ch == ')') {
                if (needNum) throw new AppEx("수식 오류: ')' 앞에 숫자가 필요합니다.");
                if (dep == 0) throw new AppEx("수식 오류: 짝이 맞지 않는 ')' 입니다.");
                toks.add(")"); dep--; i++;
            }
            else if (ch == '+' || ch == '*' || ch == '/') {
                if (needNum) throw new AppEx("수식 오류: '" + ch + "' 앞에 숫자가 필요합니다.");
                toks.add("" + ch); needNum = true; i++;
            }
            else if (ch == '-') { toks.add(needNum ? "neg" : "-"); needNum = true; i++; }
            else throw new AppEx("수식 오류: 허용되지 않는 문자 '" + ch + "' (" + (i + 1) + "번째)");
        }
        if (needNum) throw new AppEx("수식 오류: 수식이 숫자 또는 ')'로 끝나야 합니다.");
        if (dep != 0) throw new AppEx("수식 오류: 닫히지 않은 '(' 가 있습니다.");

        List<String> rpn = new ArrayList<>();
        Deque<String> ops = new ArrayDeque<>();
        for (String tok : toks) {
            if (isDigit(tok.charAt(0))) rpn.add(tok);
            else if (tok.equals("neg") || tok.equals("(")) ops.push(tok);
            else if (tok.equals(")")) { while (!ops.peek().equals("(")) rpn.add(ops.pop()); ops.pop(); }
            else {
                while (!ops.isEmpty() && !ops.peek().equals("(") && priority(ops.peek()) >= priority(tok)) rpn.add(ops.pop());
                ops.push(tok);
            }
        }
        while (!ops.isEmpty()) rpn.add(ops.pop());
        return rpn;
    }

    static int priority(String op) { return op.equals("neg") ? 3 : (op.equals("*") || op.equals("/")) ? 2 : 1; }

    static NumVal newNum(BigDecimal value, boolean decimal) {
        NumVal num = new NumVal(); num.value = value.toPlainString(); num.decimal = decimal; return num;
    }

    static String fmtNum(NumVal num) {
        String txt = new BigDecimal(num.value).stripTrailingZeros().toPlainString();
        return (num.decimal && !txt.contains(".")) ? txt + ".0" : txt;
    }

    static void runCalc(Member mem, Pending cal) {
        while (cal.position < cal.rpn.size()) {
            String tok = cal.rpn.get(cal.position);
            int sz = cal.stack.size();
            if (isOp(tok)) {
                if (sz < 2) { mem.pendingCalculation = null; save(); throw new AppEx("연산 상태가 올바르지 않아 종료합니다."); }
                NumVal rhs = cal.stack.get(sz - 1), lhs = cal.stack.get(sz - 2);
                boolean isDec = lhs.decimal || rhs.decimal;
                int fee = (tok.equals("+") || tok.equals("-") ? 1 : tok.equals("*") ? 10 : 100) * (isDec ? 5 : 1);
                if (total(mem) < fee) {
                    int opCnt = 0;
                    for (int i = 0; i <= cal.position; i++) if (isOp(cal.rpn.get(i))) opCnt++;
                    mem.pendingCalculation = cal; save();
                    System.out.println("포인트가 부족하여 " + opCnt + "번째 연산에서 일시 중지되었습니다.");
                    System.out.println("필요한 포인트: " + fee + " / 보유한 포인트: " + total(mem));
                    System.out.println("충전 후 '중지된 연산 이어서 진행'을 선택해 주세요.");
                    return;
                }
                long useBonus = Math.min(mem.points.bonus, fee);
                mem.points.bonus -= useBonus; mem.points.paid -= fee - useBonus;
                save();
                System.out.println(fmtNum(lhs) + " " + tok + " " + fmtNum(rhs) + " → " + fee + "포인트 차감, 잔액 " + total(mem));
                cal.stack.remove(sz - 1); cal.stack.remove(sz - 2);
                BigDecimal x = new BigDecimal(lhs.value), y = new BigDecimal(rhs.value), result;
                if (tok.equals("+")) result = x.add(y);
                else if (tok.equals("-")) result = x.subtract(y);
                else if (tok.equals("*")) result = x.multiply(y);
                else {
                    if (y.signum() == 0) { mem.pendingCalculation = null; save(); throw new AppEx("0으로 나눌 수 없습니다. 연산을 종료합니다."); }
                    if (!isDec && x.remainder(y).signum() == 0) result = x.divide(y);
                    else {
                        result = x.divide(y, 10, RoundingMode.HALF_UP).stripTrailingZeros();
                        if (result.scale() < 0) result = result.setScale(0);
                        isDec = true;
                    }
                }
                cal.stack.add(newNum(result, isDec));
            } else if (tok.equals("neg")) {
                NumVal tp = cal.stack.remove(sz - 1);
                cal.stack.add(newNum(new BigDecimal(tp.value).negate(), tp.decimal));
            } else {
                cal.stack.add(newNum(new BigDecimal(tok), tok.contains(".")));
            }
            cal.position++;
        }
        mem.pendingCalculation = null; save();
        System.out.println("결과: " + fmtNum(cal.stack.get(0)));
    }

    static String ask(String pmt) {
        System.out.print(pmt); System.out.flush();
        try { String ln = input.readLine(); if (ln == null) throw new EofEx(); return ln; }
        catch (IOException e) { throw new EofEx(); }
    }

    static int askNo() {
        while (true) {
            try { return Integer.parseInt(ask("선택> ").trim()); }
            catch (NumberFormatException e) { System.out.println("숫자를 입력해 주세요."); }
        }
    }

    static long toAmount(String txt) {
        try { long value = Long.parseLong(txt.trim()); if (value < 1) throw new NumberFormatException(); return value; }
        catch (NumberFormatException e) { throw new AppEx("금액은 1 이상의 정수여야 합니다."); }
    }

    static Member getMember(String id) {
        Member mem = members.get(id.trim());
        if (mem == null) throw new AppEx("존재하지 않는 ID입니다: " + id.trim());
        return mem;
    }

    static void showBalance(Member mem) {
        System.out.println("결제 포인트: " + mem.points.paid + " / 보너스 포인트: " + mem.points.bonus + " / 합계: " + total(mem));
    }

    static void showLogins(Member mem) {
        List<String> hist = mem.loginHistory;
        System.out.println("총 로그인 횟수: " + hist.size() + "회 / 최근 로그인 시간: " + fmtTime(hist.isEmpty() ? null : hist.get(hist.size() - 1)));
        for (int i = hist.size() - 1; i >= 0; i--) System.out.println("  " + fmtTime(hist.get(i)));
    }

    static void showPayments(Member mem) {
        if (mem.paymentHistory.isEmpty()) System.out.println("결제 이력이 없습니다.");
        for (int i = mem.paymentHistory.size() - 1; i >= 0; i--) {
            Payment pm = mem.paymentHistory.get(i);
            System.out.println("  " + fmtTime(pm.time) + "  " + pm.amount + "원  " + pm.result);
        }
    }

    static void historyMenu(Member tgt) {
        System.out.println("1. 로그인 이력  2. 결제 이력");
        int sel = askNo();
        if (sel == 1) showLogins(tgt);
        else if (sel == 2) showPayments(tgt);
        else System.out.println("1 또는 2를 입력해 주세요.");
    }

    static boolean userMenu(Member me) {
        System.out.println();
        System.out.println("[메뉴] 1. 회원정보 수정  2. 내 이력 보기  3. 포인트 잔액 확인  4. 포인트 충전(결제)");
        System.out.println("       5. GPU 연산  6. 중지된 연산 이어서 진행  7. 로그아웃");
        boolean admin_ = "ADMIN".equals(me.role);
        if (admin_) System.out.println("[관리자] 8. 보너스 포인트 추가  9. 보너스 포인트 제거  10. 회원 이력 조회");
        int sel = askNo();
        if (sel == 1) editInfo(me);
        else if (sel == 2) historyMenu(me);
        else if (sel == 3) showBalance(me);
        else if (sel == 4) charge(me);
        else if (sel == 5) newCalc(me);
        else if (sel == 6) contCalc(me);
        else if (sel == 7) { System.out.println("로그아웃되었습니다."); return false; }
        else if (admin_ && sel == 8) addBonus();
        else if (admin_ && sel == 9) removeBonus();
        else if (admin_ && sel == 10) historyMenu(getMember(ask("조회할 회원 ID: ")));
        else System.out.println(admin_ ? "1~10 사이의 번호를 입력해 주세요." : "1~7 사이의 번호를 입력해 주세요.");
        return true;
    }

    static void editInfo(Member me) {
        System.out.println("1. 이름 수정  2. 신용카드 등록/변경");
        int sel = askNo();
        if (sel == 1) {
            String nm = ask("새 이름: ").trim();
            if (nm.isEmpty()) throw new AppEx("이름을 입력해 주세요.");
            me.name = nm; save();
            System.out.println("이름이 수정되었습니다.");
        } else if (sel == 2) {
            String number = ask("카드번호(숫자 6자리): ").trim();
            String password = ask("카드 비밀번호(숫자 2자리): ").trim();
            if (!number.matches("\\d{6}")) throw new AppEx("카드번호는 숫자 6자리여야 합니다.");
            if (!password.matches("\\d{2}")) throw new AppEx("카드 비밀번호는 숫자 2자리여야 합니다.");
            Card cd = new Card(); cd.number = number; cd.password = password;
            me.creditCard = cd; save();
            System.out.println("신용카드가 등록되었습니다.");
        } else System.out.println("1 또는 2를 입력해 주세요.");
    }

    static void charge(Member me) {
        long amount = toAmount(ask("충전 금액(원, 1원 = 1포인트): "));
        if (me.creditCard == null) throw new AppEx("등록된 신용카드가 없습니다. 먼저 '회원정보 수정'에서 등록해 주세요.");
        boolean paySuccess = true;
        Payment pm = new Payment(); pm.time = now(); pm.amount = amount; pm.result = paySuccess ? "SUCCESS" : "FAIL";
        me.paymentHistory.add(pm);
        if (paySuccess) me.points.paid += amount;
        save();
        if (!paySuccess) throw new AppEx("결제에 실패했습니다.");
        System.out.println(amount + "포인트가 충전되었습니다.");
        showBalance(me);
    }

    static void newCalc(Member me) {
        String expr = ask("수식 (예: 3 + 4 * (2 - 1.5)): ");
        List<String> rpn = toRpn(expr);
        if (me.pendingCalculation != null) {
            System.out.println("중지된 연산이 있습니다: " + me.pendingCalculation.expression);
            String ans = ask("기존 연산을 버리고 새 수식을 시작할까요? (y/n): ").trim();
            if (!ans.equalsIgnoreCase("y")) { System.out.println("새 수식을 시작하지 않았습니다."); return; }
            me.pendingCalculation = null; save();
        }
        Pending cal = new Pending(); cal.expression = expr.trim(); cal.rpn = rpn;
        runCalc(me, cal);
    }

    static void contCalc(Member me) {
        if (me.pendingCalculation == null) { System.out.println("중지된 연산이 없습니다."); return; }
        System.out.println("이어서 진행: " + me.pendingCalculation.expression);
        runCalc(me, me.pendingCalculation);
    }

    static void addBonus() {
        Member tgt = getMember(ask("대상 회원 ID: "));
        long amount = toAmount(ask("추가할 보너스 포인트: "));
        tgt.points.bonus += amount; save();
        System.out.println("추가되었습니다.");
        showBalance(tgt);
    }

    static void removeBonus() {
        Member tgt = getMember(ask("대상 회원 ID: "));
        long amount = toAmount(ask("제거할 보너스 포인트: "));
        if (amount > tgt.points.bonus) throw new AppEx("보유한 보너스 포인트(" + tgt.points.bonus + ")보다 많이 제거할 수 없습니다.");
        tgt.points.bonus -= amount; save();
        System.out.println("제거되었습니다.");
        showBalance(tgt);
    }

    static void signUp() {
        String id = ask("ID: ").trim(), name = ask("이름: ").trim();
        if (id.isEmpty() || name.isEmpty()) throw new AppEx("ID와 이름을 입력해 주세요.");
        if (members.containsKey(id)) throw new AppEx("이미 존재하는 ID입니다: " + id);
        Member mem = new Member(); mem.id = id; mem.name = name; mem.role = "USER";
        members.put(id, mem); save();
        System.out.println("가입되었습니다. (" + id + " / " + name + ")");
    }

    static void signIn() {
        Member me = getMember(ask("ID: "));
        me.loginHistory.add(now()); save();
        System.out.println(me.name + "님 환영합니다.");
        System.out.println("마지막 접속 시간: " + fmtTime(me.loginHistory.get(me.loginHistory.size() - 1))
                + " / 총 로그인 횟수: " + me.loginHistory.size() + "회");
        boolean stay = true;
        while (stay) {
            try { stay = userMenu(me); }
            catch (AppEx e) { System.out.println("[오류] " + e.getMessage()); }
        }
    }

    public static void main(String[] args) {
        try { load(); }
        catch (IOException e) {
            System.err.println("[오류] 데이터 파일을 읽을 수 없습니다: " + e.getMessage() + " (" + FILE.toAbsolutePath() + ")");
            System.exit(1);
        }
        if (!members.containsKey("admin")) {
            Member admin = new Member(); admin.id = "admin"; admin.name = "관리자"; admin.role = "ADMIN";
            members.put("admin", admin);
            try { save(); } catch (AppEx e) { System.err.println("[오류] " + e.getMessage()); }
        }

        System.out.println("=== GPU 연산 시스템 ===");
        try {
            boolean alive = true;
            while (alive) {
                try {
                    System.out.println();
                    System.out.println("[메뉴] 1. 회원 가입  2. 로그인  3. 종료");
                    int sel = askNo();
                    if (sel == 1) signUp();
                    else if (sel == 2) signIn();
                    else if (sel == 3) alive = false;
                    else System.out.println("1~3 사이의 번호를 입력해 주세요.");
                } catch (AppEx e) { System.out.println("[오류] " + e.getMessage()); }
            }
        } catch (EofEx e) { System.out.println(); }
        System.out.println("프로그램을 종료합니다.");
    }
}
