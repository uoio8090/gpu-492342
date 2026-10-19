package exception;

/** 사용자에게 메시지를 보여주고 현재 작업만 중단하는 예외 */
public class AppException extends RuntimeException {

    public AppException(String message) {
        super(message);
    }
}
