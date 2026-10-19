import java.io.*;
import java.util.*;

import exception.*;
import model.*;
import util.Utils;
import view.MemberInfoViewer;

public class Main {

    // ===== 전역 상태 =====

    static final Map<String, Member> membersById = new LinkedHashMap<>();
    static BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

    // ===== 저장 / 불러오기 =====

    /** 데이터 파일을 읽어 membersById 에 채운다. 파일이 없으면 아무것도 하지 않는다. */
    static void loadMembers() throws IOException {
        for (Member member : MemberStorage.load()) {
            membersById.put(member.id, member);
        }
    }

    /** 모든 회원 정보를 파일에 저장한다. */
    static void saveMembers() {
        MemberStorage.save(membersById.values());
    }

    // ===== 입력 =====

    /** 안내 문구를 출력하고 한 줄을 입력받는다. 입력이 끝났으면 EndOfInputException. */
    static String prompt(String message) {
        System.out.print(message);
        System.out.flush();

        try {
            String line = consoleReader.readLine();

            if (line == null) {
                throw new EndOfInputException();
            }

            return line;
        } catch (IOException e) {
            throw new EndOfInputException();
        }
    }

    /** 메뉴 번호를 입력받는다. 숫자가 아니면 다시 묻는다. */
    static int promptMenuNumber() {
        while (true) {
            try {
                return Integer.parseInt(prompt("선택> ").trim());
            } catch (NumberFormatException e) {
                System.out.println("숫자를 입력해 주세요.");
            }
        }
    }

    /** ID 로 회원을 찾는다. 없으면 AppException. */
    static Member findMember(String id) {
        Member member = membersById.get(id.trim());

        if (member == null) {
            throw new AppException("존재하지 않는 ID입니다: " + id.trim());
        }

        return member;
    }

    // ===== 조회 화면 =====

    /** 로그인 이력 / 결제 이력 중 보여줄 항목을 고르는 메뉴. */
    static void showHistoryMenu(Member target) {
        System.out.println("1. 로그인 이력  2. 결제 이력");
        int choice = promptMenuNumber();

        if (choice == 1) {
            MemberInfoViewer.showLoginHistory(target);
        } else if (choice == 2) {
            MemberInfoViewer.showPaymentHistory(target);
        } else {
            System.out.println("1 또는 2를 입력해 주세요.");
        }
    }

    // ===== 로그인 후 메뉴 =====

    /**
     * 로그인한 회원의 메뉴를 한 번 보여주고 선택을 처리한다.
     * @return 계속 메뉴를 유지하면 true, 로그아웃하면 false
     */
    static boolean runUserMenu(Member currentMember) {
        System.out.println();
        System.out.println("[메뉴] 1. 회원정보 수정  2. 내 이력 보기  3. 포인트 잔액 확인  4. 포인트 충전(결제)");
        System.out.println("       5. GPU 연산  6. 중지된 연산 이어서 진행  7. 로그아웃");

        boolean isAdmin = "ADMIN".equals(currentMember.role);

        if (isAdmin) {
            System.out.println("[관리자] 8. 보너스 포인트 추가  9. 보너스 포인트 제거  10. 회원 이력 조회");
        }

        int choice = promptMenuNumber();

        if (choice == 1) {
            MemberEditor.editMemberInfo(currentMember);
        } else if (choice == 2) {
            showHistoryMenu(currentMember);
        } else if (choice == 3) {
            MemberInfoViewer.showBalance(currentMember);
        } else if (choice == 4) {
            chargePoints(currentMember);
        } else if (choice == 5) {
            CalculationService.startNewCalculation(currentMember);
        } else if (choice == 6) {
            CalculationService.resumeCalculation(currentMember);
        } else if (choice == 7) {
            System.out.println("로그아웃되었습니다.");
            return false;
        } else if (isAdmin && choice == 8) {
            AdminService.addBonusPoints();
        } else if (isAdmin && choice == 9) {
            AdminService.removeBonusPoints();
        } else if (isAdmin && choice == 10) {
            showHistoryMenu(findMember(prompt("조회할 회원 ID: ")));
        } else {
            System.out.println(isAdmin ? "1~10 사이의 번호를 입력해 주세요." : "1~7 사이의 번호를 입력해 주세요.");
        }

        return true;
    }

    /** 포인트 충전(결제): 1원 = 1포인트. 결제 이력을 남기고 결제 포인트를 늘린다. */
    static void chargePoints(Member currentMember) {
        long amount = Utils.parseAmount(prompt("충전 금액(원, 1원 = 1포인트): "));

        if (currentMember.creditCard == null) {
            throw new AppException("등록된 신용카드가 없습니다. 먼저 '회원정보 수정'에서 등록해 주세요.");
        }

        // 실제 결제 연동이 없으므로 항상 성공으로 처리한다
        boolean paymentSucceeded = true;

        Payment payment = new Payment();
        payment.time = Utils.currentTime();
        payment.amount = amount;
        payment.result = paymentSucceeded ? "SUCCESS" : "FAIL";
        currentMember.paymentHistory.add(payment);

        if (paymentSucceeded) {
            currentMember.points.paid += amount;
        }

        saveMembers();

        if (!paymentSucceeded) {
            throw new AppException("결제에 실패했습니다.");
        }

        System.out.println(amount + "포인트가 충전되었습니다.");
        MemberInfoViewer.showBalance(currentMember);
    }

    // ===== 시작 =====

    /** 프로그램 시작: 데이터 로드 → 관리자 계정 보장 → 메인 메뉴 반복. */
    public static void main(String[] args) {
        try {
            loadMembers();
        } catch (IOException e) {
            System.err.println("[오류] 데이터 파일을 읽을 수 없습니다: " + e.getMessage() + " (" + MemberStorage.DATA_FILE.toAbsolutePath() + ")");
            System.exit(1);
        }

        // 관리자 계정이 없으면 기본 관리자(admin)를 만든다
        if (!membersById.containsKey("admin")) {
            Member admin = new Member();
            admin.id = "admin";
            admin.name = "관리자";
            admin.role = "ADMIN";
            membersById.put("admin", admin);

            try {
                saveMembers();
            } catch (AppException e) {
                System.err.println("[오류] " + e.getMessage());
            }
        }

        System.out.println("=== GPU 연산 시스템 ===");

        try {
            boolean running = true;

            while (running) {
                try {
                    System.out.println();
                    System.out.println("[메뉴] 1. 회원 가입  2. 로그인  3. 종료");
                    int choice = promptMenuNumber();

                    if (choice == 1) {
                        AuthService.signUp();
                    } else if (choice == 2) {
                        AuthService.signIn();
                    } else if (choice == 3) {
                        running = false;
                    } else {
                        System.out.println("1~3 사이의 번호를 입력해 주세요.");
                    }
                } catch (AppException e) {
                    System.out.println("[오류] " + e.getMessage());
                }
            }
        } catch (EndOfInputException e) {
            System.out.println();
        }

        System.out.println("프로그램을 종료합니다.");
    }
}
