import exception.AppException;
import model.Member;
import util.Utils;

/** 회원 가입과 로그인(로그아웃 포함) 처리. */
public class AuthService {

    /** 회원 가입: ID 중복을 검사하고 일반 회원(USER)으로 등록한다. */
    static void signUp() {
        String id = Main.prompt("ID: ").trim();
        String name = Main.prompt("이름: ").trim();

        if (id.isEmpty() || name.isEmpty()) {
            throw new AppException("ID와 이름을 입력해 주세요.");
        }

        if (Main.membersById.containsKey(id)) {
            throw new AppException("이미 존재하는 ID입니다: " + id);
        }

        Member member = new Member();
        member.id = id;
        member.name = name;
        member.role = "USER";
        Main.membersById.put(id, member);
        Main.saveMembers();
        System.out.println("가입되었습니다. (" + id + " / " + name + ")");
    }

    /** 로그인: 이력을 기록하고, 로그아웃할 때까지 회원 메뉴를 반복한다. */
    static void signIn() {
        Member currentMember = Main.findMember(Main.prompt("ID: "));
        currentMember.loginHistory.add(Utils.currentTime());
        Main.saveMembers();

        System.out.println(currentMember.name + "님 환영합니다.");
        System.out.println("마지막 접속 시간: " + Utils.formatTime(currentMember.loginHistory.get(currentMember.loginHistory.size() - 1))
                + " / 총 로그인 횟수: " + currentMember.loginHistory.size() + "회");

        boolean loggedIn = true;

        while (loggedIn) {
            try {
                loggedIn = Main.runUserMenu(currentMember);
            } catch (AppException e) {
                System.out.println("[오류] " + e.getMessage());
            }
        }
    }
}
