package com.ktae23;

/**
 * 공휴일 API 호출/파싱 과정에서 발생하는 오류를 표현하는 비검사 예외.
 *
 * <p>네트워크 오류, 비정상 HTTP 응답, JSON 파싱 실패, 잘못된 API 키 설정 등을 이 예외로 일관되게 던진다.
 * <b>예외 메시지에는 API 키(및 키가 포함된 요청 URL)가 절대 포함되지 않는다.</b>
 */
public class HolidayClientException extends RuntimeException {

    /**
     * 메시지만으로 예외를 생성한다.
     *
     * @param message 오류 메시지(키를 포함하지 않아야 한다)
     */
    public HolidayClientException(String message) {
        super(message);
    }

    /**
     * 메시지와 원인으로 예외를 생성한다.
     *
     * @param message 오류 메시지(키를 포함하지 않아야 한다)
     * @param cause   원인 예외
     */
    public HolidayClientException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 원인만으로 예외를 생성한다.
     *
     * @param cause 원인 예외
     */
    public HolidayClientException(Throwable cause) {
        super(cause);
    }
}
