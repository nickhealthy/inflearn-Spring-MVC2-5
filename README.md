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



## 로그인 처리하기 - 서블릿 HTTP 세션1

서블릿은 세션을 위해 `HttpSession` 이라는 기능을 제공한다.



### HttpSession이란?

서블릿이 제공하는 세션도 위에서 직접 만든 `SessionManager` 와 같은 방식으로 동작한다.
**서블릿을 통해 `HttpSession`을 생성하면 `JSESSIONID` 이름의 쿠키를 생성하고, 추정 불가능한 랜덤 값을 생성한다.**



### 예제

[`hello.login.web.login.LoginController`]

* `request.getSession();`  - 세션이 있으면 있는 세션 반환, 아니면 세션 새로 생성
* `session.setAttribute(SessionConst.LOGIN_MEMBER, loginMember);` - 세션에 로그인 회원 정보 보관

```java
@PostMapping("/login")
public String loginV3(@Valid @ModelAttribute LoginForm form, BindingResult bindingResult, HttpServletRequest request) {

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
    // 세션이 있으면 있는 세션 반환, 없으면 신규 세션 생성
    HttpSession session = request.getSession();
    // 세션에 로그인 회원 정보 보관
    session.setAttribute(SessionConst.LOGIN_MEMBER, loginMember);

    return "redirect:/";
}
```



* 로그아웃 시 해당 세션의 정보를 제거한다.

```java
@PostMapping("/logout")
public String logoutV3(HttpServletRequest request) {
    HttpSession session = request.getSession();
    if (session != null) {
        session.invalidate();
    }

    return "redirect:/";
}
```



[`hello.login.web.HomeController`]

* 사용자가 로그인 했을 때 홈 화면을 처리해주는 컨트롤러도 수정
* `request.getSession(false);` - <u>세션을 찾아서 사용하는 시점에는</u> 해당 옵션을 사용해서 세션을 생성하지 않아야 한다.
* `session.getAttribute(SessionConst.LOGIN_MEMBER);` - 로그인 시점에 세션에 보관한 회원 객체를 찾는다.

```java
@GetMapping("/")
public String homeLoginV3(HttpServletRequest request, Model model) {

    // 세션이 없으면 home
    HttpSession session = request.getSession(false);
    if (session == null) {
        return "home";
    }

    Member loginMember = (Member) session.getAttribute(SessionConst.LOGIN_MEMBER);
    // 세션에 회원 데이터가 없으면 home
    if (loginMember == null) {
        return "home";
    }

    // 세션이 유지되면 로그인으로 이동
    model.addAttribute("member", loginMember);
    return "loginHome";
    
}
```



### 세션 생성과 조회

세션의 `create` 옵션

* `request.getSession(true)`
  * 세션이 있으면 기존 세션을 반환한다.
  * 세션이 없으면 새로운 세션을 생성해서 반환한다.
* `request.getSession(false)`
  * 세션이 있으면 기존 세션을 반환한다.
  * 세션이 없으면 새로운 세션을 생성하지 않는다. `null` 을 반환한다.



## 로그인 처리하기 - 서블릿 HTTP 세션2

### @SessionAttribute

스프링은 세션을 더 편리하게 사용할 수 있도록 `@SessionAttribute` 을 지원한다.
이미 로그인 된 사용자를 찾을 때는 다음과 같이 사용하면 된다.

```java
@SessionAttribute(name = "loginMember", required = false) Member loginMember
```



### 예제

[`hello.login.web.HomeController`]

* <u>세션을 찾고, 세션에 들어있는 데이터를 찾는 번거로운 과정을 스프링이 한번에 처리해주는 것을 확인할 수 있다.</u>

```java
@GetMapping("/")
public String homeLoginV3Spring(
        @SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) Member loginMember, Model model) {

	  /** 이 과정이 생략 되었다.
	  
  	HttpSession session = request.getSession(false);
    if (session == null) {
        return "home";
    }

    Member loginMember = (Member) session.getAttribute(SessionConst.LOGIN_MEMBER);
    **/
  
    // 세션에 회원 데이터가 없으면 home
    if (loginMember == null) {
        return "home";
    }

    // 세션이 유지되면 로그인으로 이동
    model.addAttribute("member", loginMember);
    return "loginHome";

}
```



### TrackingModes

로그인을 처음 시도했을 때 `jsessionid`가 URL에 포함되어 있는 것을 확인할 수 있는데, 이것은 <u>웹 브라우저가 쿠키를 지원하지 않을 때 쿠키 대신 URL을 통해서 세션을 유지하는 방법이다.</u>  서버 입장에서는 웹 브라우저가 쿠키를 지원하는지 하지 않는지 최초에는 판단하지 못하므로, 쿠키 값도 전달하고, URL에 `jsessionid`도 함께 전달하는 것이다.



#### URL 전달 방식이 아닌 항상 쿠키를 통해서만 세션을 유지하고 싶으면 다음 옵션을 넣으면 된다.

[`application.properties`]

```java
 server.servlet.session.tracking-modes=cookie
```



## 세션 정보와 타임아웃 설정

### 세션 정보 확인

* `sessionId`: 세션ID, `JSESSIONID`의 값이다.
* `maxInactiveInterval`: 세션의 유효 시간
* `creationTime`: 세션 생성일시
* `lastAccessedTime`: 세션과 연결된 사용자가 최근에 서버에 접근한 시간
* `isNew`: 새로 생성된 세션인지, 아니면 과거에 이미 만들어졌고 클라이언트에서 서버로 `sessionId(JSESSIONID)`를 요청해서 조회된 세션인지의 여부

```java
package hello.login.web.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Date;

@Slf4j
@RestController
public class SessionInfoController {

    @GetMapping("/session-info")
    public String sessionInfo(HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        if (session == null) {
            return "세션이 없습니다.";
        }

        // 세션 데이터 출력
        session.getAttributeNames().asIterator()
                .forEachRemaining(name -> log.info("session name = {}, value = {}", name, session.getAttribute(name)));

        log.info("sessionId={}", session.getId());
        log.info("maxInactiveInterval={}", session.getMaxInactiveInterval());
        log.info("creationTime={}", new Date(session.getCreationTime()));
        log.info("lastAccessedTime={}", new Date(session.getLastAccessedTime()));
        log.info("isNew={}", session.isNew());

        return "세션 출력";
    }
}

```



### 세션 타임아웃 설정

세션은 사용자가 로그아웃을 호출해서 `session.invalidate()`를 호출 되는 경우 삭제되는데, <u>대부분의 사용자는 웹 브라우저를 강제로 종료하고, HTTP가 비 연결성이므로 서버 입장에서는 웹 브라우저가 종료되었는지 알아낼 수가 없다.</u> 따라서 세션 데이터도 언제 삭제해야 되는지 판단하기 어렵게 된다.



#### 문제점 발생

위와 같은 상황으로 다음과 같은 문제들이 일어날 수 있다.

* 세션과 관련된 쿠키(`JSESSIONID`)를 탈취 당했을 경우 오랜 시간이 지나도 해당 쿠키로 악의적인 요청을 할 수 있다.
* <u>세션은 기본적으로 메모리에 생성되는데</u>, 메모리 자원은 무한하지 않기 때문에 불필요한 세션을 지우지 않으면 서버에 장애가 생길 수 있다.



#### 세션의 종료 시점

서블릿에서 제공하는 `HttpSession`은 세션과 관련된 쿠키(`JSESSIONID`)를 가지고 서버에 새로운 요청을 할 때 세션의 생존 시간을 기본으로 30분 연장시켜준다.

<u>즉, `session.getLastAccessedTime()` : 최근 세션 접근 시간을 가지고 새로운 요청이 오면 이 값을 갱신 시켜서 30분씩 세션 시간을 연장해주는 것이다.</u>



#### 세션 타임아웃 설정

[`application.properties`]

* 글로벌 설정은 분 단위로 설정해야 한다.

```java
server.servlet.session.timeout=1800 // 1800초(기본 값)
```



* 특정 세션 단위로 시간 설정

```java
session.setMaxInactiveInterval(1800); //1800초
```



> 참고사항
>
> <u>실무에서 주의할 점은 세션에는 최소한의 데이터만 보관해야 한다는 점이다.</u>
> 메모리 사용량이 급격하게 늘어나면서 장애로 이어질 수 있고, 추가로 세션 시간을 길게 가져가면 마찬가지로 메모리 사용량이 누적될 수 있으므로 적당한 시간을 고려해서 선택하는 것이 필요하다.