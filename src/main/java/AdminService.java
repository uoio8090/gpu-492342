import exception.AppException;
import model.Member;
import util.Utils;
import view.MemberInfoViewer;

/** 관리자 전용 기능: 회원의 보너스 포인트 추가/제거. */
public class AdminService {

    /** (관리자) 대상 회원에게 보너스 포인트를 추가한다. */
    static void addBonusPoints() {
        Member target = Main.findMember(Main.prompt("대상 회원 ID: "));
        long amount = Utils.parseAmount(Main.prompt("추가할 보너스 포인트: "));

        target.points.bonus += amount;
        Main.saveMembers();
        System.out.println("추가되었습니다.");
        MemberInfoViewer.showBalance(target);
    }

    /** (관리자) 대상 회원의 보너스 포인트를 제거한다. 보유량보다 많이는 제거할 수 없다. */
    static void removeBonusPoints() {
        Member target = Main.findMember(Main.prompt("대상 회원 ID: "));
        long amount = Utils.parseAmount(Main.prompt("제거할 보너스 포인트: "));

        if (amount > target.points.bonus) {
            throw new AppException("보유한 보너스 포인트(" + target.points.bonus + ")보다 많이 제거할 수 없습니다.");
        }

        target.points.bonus -= amount;
        Main.saveMembers();
        System.out.println("제거되었습니다.");
        MemberInfoViewer.showBalance(target);
    }
}
