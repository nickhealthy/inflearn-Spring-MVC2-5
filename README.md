# 인프런 강의

해당 저장소의 `README.md`는 인프런 김영한님의 SpringBoot 강의 시리즈를 듣고 Spring 프레임워크의 방대한 기술들을 복기하고자 공부한 내용을 가볍게 정리한 것입니다.

문제가 될 시 삭제하겠습니다.



# 섹션 6 | 로그인 처리1 - 쿠키, 세션

## 로그인 처리하기 - 쿠키 사용

로그인 상태를 유지하려면 어떻게 해야할까? -> **쿠키를 사용하면 된다.**

> 중요 데이터는 세션으로 처리해야 되는 것으로 알고 있는데 해당 강의 이후에 세션이 언급될 것 같다.

### 쿠키

서버에서 로그인에 성공하면 HTTP 응답에 쿠키를 담아서 브라우저에 전달하고, 그 이후 브라우저는 **일종의 인증정보인 해당 쿠키를 지속해서 보내준다.**

#### 쿠키의 종류

* 영속 쿠키: 만료 날짜를 입력하면 해당 날짜까지 유지
* 세션 쿠기: 만료 날짜를 생략하면 브라우저 종료시 까지만 유지



#### 예제 및 프로세스

1. 로그인에 성공하면 쿠키를 생성하고, `HttpServletResponse`에 담는다.
2. 쿠키 이름은 `MemberId`이고, 값은 회원의 `id`에 담아둔다. 
3. 웹 브라우저는 종료 전까지 회원의 `id`를 서버에 계속 보내줄 것이다.

```java
@PostMapping("/login")
    public String login(@Valid @ModelAttribute LoginForm form, BindingResult bindingResult, HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            return "login/loginForm";
        }

        Member loginMember = loginService.login(form.getLoginId(), form.getPassword());
        log.info("login? {}", loginMember);

        if (loginMember == null) {
            bindingResult.reject("loginFail", "아이디 또는 비밀번호가 맞지 않습니다.");
            return "login/loginForm";
        }

        // 로그인 성공 처리

        // 쿠키에 시간 정보를 주지 않으면 세션 쿠키(브라우저 종료시 모두 종료)
        Cookie idCookie = new Cookie("memberId", String.valueOf(loginMember.getId()));
        response.addCookie(idCookie);

        return "redirect:/";
    }
```



## 쿠키와 보안 문제

### 쿠키를 사용하는데에는 다음과 같은 보안 문제가 있다.

* <u>쿠키 값은 임의로 변경할 수 있다.</u>
* <u>쿠키에 보관된 정보는 훔쳐갈 수 있다.</u>



### 위와 같은 문제를 방지하기 위해 다음과 같은 대안이 있다.

* 쿠키에 중요한 값을 노출하지 않고, 사용자 별로 예측 불가능한 임의의 토큰(랜덤 값)을 노출하고, 서버에서 토큰과 사용자 id를 매핑해서 인식한다. 그리고 서버에서 토큰을 관리한다.
* 토큰은 해커가 임의의 값을 넣어도 찾을 수 없도록 예측 불가능해야 한다.
* 토큰의 만료시간을 짧게 유지한다. 또는 해킹이 의심되는 경우 서버에서 해당 토큰을 강제로 제거한다.



## 로그인 처리하기 - 세션 동작 방식

앞서 쿠키를 사용한 로그인 처리 방식은 보안에 큰 문제가 있었다.
따라서 중요한 정보는 모두 서버에 저장하고, 클라이언트와 서버는 추정 불가능한 임의의 식별자 값으로 연결한다.
이렇게 **서버에 중요한 정보를 보관하고 연결을 유지하는 방법을 세션**이라고 한다.



### 세션 방식의 로그인 프로세스

크게 4가지의 프로세스로 이루어져 있다.

#### 1. 로그인

* 사용자가 ID/PASSWORD 입력 후 로그인을 시도한다.

#### 2. 세션 관리

* 서버는 해당 서비스의 사용자가 맞는지 확인 후, 사용자가 맞다면 서버에서 세션ID를 발급한다.
  * 이때 세션ID는 추정 불가능해야하며, UUID는 추정이 불가능하다.
* 생성된 세션ID와 세션에 보관할 값을 서버의 세션 저장소에 보관한다.

#### 3. 세션ID를 쿠키로 전달

* **결국 클라이언트와 서버는 쿠키로 연결되어야 하기 때문에** 위에서 서버가 만든 **세션ID만** 쿠키에 담아 클라이언트에 전달한다.
  * **[중요]** <u>중요한 정보와 회원과 관련된 정보 제외한 채 오직 추정 불가능한 세션ID만 쿠키를 통해 클라이언트에게 전달하는 것이 핵심</u>
* 클라이언트는 쿠키 저장소에 해당 쿠키를 보관한다.

#### 4. 로그인 이후 접근

* 클라이언트는 요청할 때 항상 해당 쿠키와 함께 서버에 전달한다.
* 서버에서는 클라이언트가 전달한 쿠키 정보로 세션 저장소를 조회 후 로그인 시 보관한 세션 정보를 사용한다.



## 로그인 처리하기 - 세션 직접 만들기



### 세셔 관리에 필요한 기능 3가지

* 세션 생성
  * sessionId 생성(임의의 추정 불가능한 랜덤 값)
  * 세션 저장소에 sessionId와 보관할 값 저장
  * sessionId로 응답 쿠키를 생성해서 클라이언트에 전달
* 세션 조회
  * 클라이언트가 요청한 sessionId 쿠키의 값으로, 세션 저장소에 보관한 값 조회
* 세션 만료
  * 클라이언트가 요청한 sessionId 쿠키의 값으로, 세션 저장소에 보관한 sessionId와 값 제거



### 예제

[`hello.login.web.session.SessionManager`]

```java
package hello.login.web.session;

import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 세션 관리
 */
@Component
public class SessionManager {

    public static final String SESSION_COOKIE_NAME = "mySessionId";
    private Map<String, Object> sessionStore = new ConcurrentHashMap<>();

    /**
     * 세션 생성
     */
    public void createSession(Object value, HttpServletResponse response) {

        // 세션id를 생성하고, 값을 세션에 저장
        String sessionId = UUID.randomUUID().toString();
        sessionStore.put(sessionId, value); // 세션 저장소에 sessionId와 보관할 값 저장

        // sessionId로 응답 쿠키를 생성해서 클라이언트에 전달
        Cookie mySessionCookie = new Cookie(SESSION_COOKIE_NAME, sessionId);
        response.addCookie(mySessionCookie);
    }

    /**
     * 세션 조회
     */
    public Object getSession(HttpServletRequest request) {
        Cookie sessionCookie = findCookie(request, SESSION_COOKIE_NAME);
        if (sessionCookie == null) {
            return null;
        }

        return sessionStore.get(sessionCookie.getValue());
    }

    /**
     * 세션 만료
     */
    public void expire(HttpServletRequest request) {
        Cookie sessionCookie = findCookie(request, SESSION_COOKIE_NAME);
        if (sessionCookie != null) {
            sessionStore.remove(sessionCookie.getValue());
        }
    }

    private Cookie findCookie(HttpServletRequest request, String sessionCookieName) {
        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals(sessionCookieName))
                .findAny()
                .orElse(null);
    }
}

```



[테스트 - `hello.login.web.session.SessionManagerTest`]

```java
package hello.login.web.session;

import hello.login.domain.member.Member;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class SessionManagerTest {


    SessionManager sessionManager = new SessionManager();

    @Test
    void sessionTest() {
        // 세션 생성
        // MockHttpServletRequest, MockHttpServletResponse 테스트에서 사용할 가짜 객체
        MockHttpServletResponse response = new MockHttpServletResponse();
        Member member = new Member();
        sessionManager.createSession(member, response);

        // 요청에 응답 쿠키 저장
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(response.getCookies());

        // 세션 조회
        Object result = sessionManager.getSession(request);
        assertThat(result).isEqualTo(member);

        // 세션 만료
        sessionManager.expire(request);
        Object expired = sessionManager.getSession(request);
        assertThat(expired).isNull();
    }

}
```



## 로그인 처리하기 - 직접 만든 세션 적용

> `SessionManager` 부분은 위의 소스코드 참고

### 예제

[`hello.login.web.login.LoginController`]

* 로그인 성공 시 세션을 등록한다.
* 세션에 `loginMember`를 저장해두고(값), 쿠키도 함께 발행한다.(쿠키의 값은 세션의 UUID)

```java
@PostMapping("/login")
public String loginV2(@Valid @ModelAttribute LoginForm form, BindingResult bindingResult, HttpServletResponse response) {

    if (bindingResult.hasErrors()) {
        return "login/loginForm";
    }

    Member loginMember = loginService.login(form.getLoginId(), form.getPassword());
    log.info("login? {}", loginMember);

    if (loginMember == null) {
        bindingResult.reject("loginFail", "아이디 또는 비밀번호가 맞지 않습니다.");
        return "login/loginForm";
    }

    // 로그인 성공 처리
    // 세션 관리자를 통해 세션을 생성하고, 회원 데이터를 보관
    sessionManager.createSession(loginMember, response);

    return "redirect:/";
}
```

* 로그아웃 시 해당 세션의 정보를 제거한다.

```java
@PostMapping("/logout")
public String logoutV2(HttpServletRequest request) {
    sessionManager.expire(request);
    return "redirect:/";
}
```



[`hello.login.web.HomeController`]

* 사용자가 로그인 했을 때 홈 화면을 처리해주는 컨트롤러도 수정

```java
@GetMapping("/")
public String homeLoginV2(HttpServletRequest request, Model model) {

    // 세션 관리자에 저장된 회원 정보 조회
    Member member = (Member) sessionManager.getSession(request);
    if (member == null) {
        return "home";
    }

    // 로그인
    model.addAttribute("member", member);
    return "loginHome";
}
```



이를 통해 세션이라는 것은 뭔가 특별한 것이 아니라 <u>**단지 쿠키를 사용하는데, 서버에서 데이터를 유지하는 방법일 뿐이다.**</u>



