import exception.AppException;
import model.Card;
import model.Member;

/** 회원정보 수정(이름 변경, 신용카드 등록/변경) 처리. */
public class MemberEditor {

    /** 회원정보 수정: 이름 변경 또는 신용카드 등록/변경. */
    static void editMemberInfo(Member currentMember) {
        System.out.println("1. 이름 수정  2. 신용카드 등록/변경");
        int choice = Main.promptMenuNumber();

        if (choice == 1) {
            String newName = Main.prompt("새 이름: ").trim();

            if (newName.isEmpty()) {
                throw new AppException("이름을 입력해 주세요.");
            }

            currentMember.name = newName;
            Main.saveMembers();
            System.out.println("이름이 수정되었습니다.");
        } else if (choice == 2) {
            String cardNumber = Main.prompt("카드번호(숫자 6자리): ").trim();
            String cardPassword = Main.prompt("카드 비밀번호(숫자 2자리): ").trim();

            if (!cardNumber.matches("\\d{6}")) {
                throw new AppException("카드번호는 숫자 6자리여야 합니다.");
            }

            if (!cardPassword.matches("\\d{2}")) {
                throw new AppException("카드 비밀번호는 숫자 2자리여야 합니다.");
            }

            Card card = new Card();
            card.number = cardNumber;
            card.password = cardPassword;
            currentMember.creditCard = card;
            Main.saveMembers();
            System.out.println("신용카드가 등록되었습니다.");
        } else {
            System.out.println("1 또는 2를 입력해 주세요.");
        }
    }
}
