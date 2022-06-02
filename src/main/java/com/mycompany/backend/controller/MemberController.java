package com.mycompany.backend.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.json.JSONObject;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mycompany.backend.dto.Member;
import com.mycompany.backend.security.Jwt;
import com.mycompany.backend.service.MemberService;
import com.mycompany.backend.service.MemberService.JoinResult;
import com.mycompany.backend.service.MemberService.LoginResult;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping("/member")
public class MemberController {
  @Resource
  private MemberService memberService;
  
  @Resource
  private PasswordEncoder passwordEncoder;
  
  @Resource
  private RedisTemplate<String, String> redisTemplate;
  
  @PostMapping("/join")
  public Map<String, Object> join(@RequestBody Member member) { // @RequestBody를 붙이면, 요청할 때 요청Body에 json이 있다는 의미임.
    //계정 활성화
    member.setMenabled(true);
    //패스워드 암호화
    member.setMpassword(passwordEncoder.encode(member.getMpassword())); // 기존에 SecurityConfig.java에서 구현해둔 인코더 주입받아서 사용하는 것임.
    //회원가입 처리
    JoinResult joinResult = memberService.join(member);
    //응답내용 설정
    Map<String, Object> map = new HashMap<>();
    if(joinResult == JoinResult.SUCCESS) {
      map.put("result", "success");
    } else if (joinResult == JoinResult.DUPLICATED) {
      map.put("result", "duplicated");
    } else {
      map.put("result", "fail");
    }
    return map;
    
    
//    Map<String, Object> map = new HashMap<>();  // 지금은 MemberService 없어서 우선 이렇게 함(27.금요일 실습 기준)
//    map.put("result", "success");
//    map.put("member", member);
//    return map;
  }
  
  @PostMapping("/login")
  public ResponseEntity<String> login(@RequestBody Member member) {
    log.info("실행");
    
    /*30(월) 실습코드*/
    // 사실 유효성검사기를 사용하는게 제일 좋은데 여기선 그냥 바로 작성해보겠음.
    // mid나 mpassword가 없을 경우
    if(member.getMid() == null || member.getMpassword() == null) {
      return ResponseEntity
              .status(401) // 에러코드 리턴해주는 거임.(401: 인증(=로그인할때 아이디/패스워드가 잘못된) 문제)
              .body("mid or mpassword cannot be null"); // 포스트맨에서 login 테스트를 틀리게 해보자. 401과 바디가 잘 넘어온다.
    }
    //로그인 결과 얻기
    LoginResult loginResult = memberService.login(member);
    if(loginResult != LoginResult.SUCCESS) {
      return ResponseEntity
          .status(401) // 에러코드 리턴해주는 거임.(401: 인증(=로그인할때 아이디/패스워드가 잘못된) 문제)
          .body("mid or mpassword is wrong"); // 포스트맨에서 login 테스트를 틀리게 해보자. 401과 바디가 잘 넘어온다.
    }
    
    Member dbMember = memberService.getMember(member.getMid());
    // 만들어둔 static 메소드 사용해서 토큰 얻기
    String accessToken = Jwt.createAccessToken(member.getMid(), dbMember.getMrole()); // mrole값 이제 제대로 넣음.
    String refreshToken = Jwt.createRefreshToken(member.getMid(), dbMember.getMrole());
    
    // Redis에 저장
    ValueOperations<String, String> vo = redisTemplate.opsForValue();
    vo.set(accessToken, refreshToken, Jwt.REFRESH_TOKEN_DURATION, TimeUnit.MILLISECONDS); // 대부분 이 형태로 많이 작성한다고.(키: accessToken, 값: refreshToken, redis에서의 저장만료기간(refreshToken의 만료기간과 동일하게 줘야함), 시간 단위(ms))
    
    // 쿠키 생성하기
    String refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                              .httpOnly(true) // ******제일 중요!!******
                              .secure(false) //http, https프로토콜에서도 쓸 수 있는 경우. (true로 설정하면 https에서만 사용 가능)
                              .path("/")
                              .maxAge(Jwt.REFRESH_TOKEN_DURATION / 1000) // 초 단위만 받아서 나누기 1000 해 줌.
                              .domain("localhost") // rest API의 도메인 명시
                              .build()
                              .toString();
    // 본문 생성하기
    String json = new JSONObject()
                              .put("accessToken", accessToken)
                              .put("mid", member.getMid())
                              .toString();
    
    // 응답 설정하기
    return ResponseEntity
                         // ok()란: 응답상태 코드를 200번으로 설정하고 응답을 하겠다는 소리
                         .ok()
                         // 응답 헤더 추가하기
                         .header(HttpHeaders.SET_COOKIE, refreshTokenCookie)
                         .header(HttpHeaders.CONTENT_TYPE, "application/json;")
                         // 응답 바디 추가하기
                         .body(json);
    
    
    /*아래는 27(금) 실습코드내용*/
    /*
    // 만들어둔 static 메소드 사용해서 토큰 얻기
    String accessToken = Jwt.createAccessToken(member.getMid(), "ROLE_USER"); // DB 연결전이라 우선 스트링으로 "ROLE_USER"라고 넣자.
    String refreshToken = Jwt.createAccessToken(member.getMid(), "ROLE_USER");
    
    // 쿠키 생성하기
    String refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                              .httpOnly(true) // ******제일 중요!!******
                              .secure(false) //http, https프로토콜에서도 쓸 수 있는 경우. (true로 설정하면 https에서만 사용 가능)
                              .path("/")
                              .maxAge(Jwt.REFRESH_TOKEN_DURATION / 1000) // 초 단위만 받아서 나누기 1000 해 줌.
                              .domain("localhost") // rest API의 도메인 명시
                              .build()
                              .toString();
    // 본문 생성하기
    String json = new JSONObject()
                              .put("accessToken", accessToken)
                              .put("mid", member.getMid())
                              .toString();
    
    // 응답 설정하기
    return ResponseEntity
                         // ok()란: 응답상태 코드를 200번으로 설정하고 응답을 하겠다는 소리
                         .ok()
                         // 응답 헤더 추가하기
                         .header(HttpHeaders.SET_COOKIE, refreshTokenCookie)
                         .header(HttpHeaders.CONTENT_TYPE, "application/json;")
                         // 응답 바디 추가하기
                         .body(json);
                         */
  }
  
  @GetMapping("/refreshToken")
  public ResponseEntity<String> refreshToken(
      @RequestHeader("Authorization") String authorization, // Authorization Header정보 얻고
      @CookieValue("refreshToken") String refreshToken // 쿠키 정보도 얻기
  ) {
    // AccessToken 얻기
    String accessToken = Jwt.getAccessToken(authorization);
    if(accessToken == null) {
      return ResponseEntity.status(401).body("no access token");
    }
    
    // RefreshToken 여부 figure out하기
    if(refreshToken == null) {
      return ResponseEntity.status(401).body("no refresh token");
    }
    
    // 각 토큰들이 실제로 맞는 토큰인지 비교작업을 해야 하는데... 이 토큰들을 어디에 저장할 것인가?는 좀이따 다시 생각해보자.
    // 일단 둘다 맞다고 가정하고 우선 짜보자.
    // ~잠시 후~
    // 이제 redis실습 잔뜩하고 진짜로 비교작업을 할 차례임.
    
    //동일한 토큰인지 확인(Redis 사용)
    ValueOperations<String, String> vo = redisTemplate.opsForValue();
    String redisRefreshToken = vo.get(accessToken); // 만약 엉뚱한 accessToken이 전송됐다면, 그 key에 대한 값이 null일 것임.
    if (redisRefreshToken == null) { // accessToken이 엉뚱한거라면, 401리턴
      return ResponseEntity.status(401).body("wrong access token");
    }
    if (!refreshToken.equals(redisRefreshToken)) { // refresh token이 유효하지 않다면, 401리턴
      return ResponseEntity.status(401).body("invalid refresh token");
    }
    // refreshToken의 유효성을 검증할 필요는 없다. 왜냐하면 refreshToken의 duration과 동일하게 redis도 설정했기 때문에, 이미 지금 검증중이라는건 valid하다는 뜻이니까.
    // 그래도 검증하고 싶다면 다음과 같이 하면 됨.
//    if(Jwt.validateToken(refreshToken)) {
//      return ResponseEntity.status(401).body("invalid refresh token");
//    }
    
    // 새로운 AccessToken 생성하기
    Map<String, String> userInfo = Jwt.getUserInfo(refreshToken);
    String mid       = userInfo.get("mid");
    String authority = userInfo.get("authority");
    String newAccessToken = Jwt.createAccessToken(mid, authority); // mid, authority는 어떻게 얻어내느냐? access token에서는 추출하지 못한다. 왜? 만료된 상태니까~ 그래서 refreshToken으로부터 얻어내야 한다. (위에서 Jwt.getUserInfo(refreshToken)으로 한 이유.)
    
    // +) AccessToken 생성한 뒤, redis에 저장하는 key도 함께 갱신시켜줘야 함
    // 1. redis에 저장된 기존 정보를 우선 삭제하기
    redisTemplate.delete(accessToken);
    // 2. redis에 새로운 정보를 저장하기
    vo.set(accessToken, refreshToken, Jwt.REFRESH_TOKEN_DURATION, TimeUnit.MILLISECONDS);
    Date expiration = Jwt.getExpiration(refreshToken);
    vo.set(newAccessToken, refreshToken, expiration.getTime() - new Date().getTime() /* Jwt.REFRESH_TOKEN_DURATION하면 안되고 남은 시간을 갖다 넣어줘야 함.*/, TimeUnit.MILLISECONDS);
    
    
    // 응답 설정하기
    String json = new JSONObject()
                      .put("newAccessToken", newAccessToken)
                      .put("mid", mid)
                      .toString();
    
    return ResponseEntity
            .ok()
            .header(HttpHeaders.CONTENT_TYPE, "application/json") //json을 넘겨준다는 의미에서 content-type을 헤더에 명시해주자.
            .body(json); // ResponseEntity<String>으로 리턴하는 메소드니까, string형태의 json을 바디에 담아 리턴
    // 포스트맨으로 테스트해보자.로그인을 해서 토큰을 받아 board/list에서 토큰을 입력해 access_token_duration시간 지나고나서도 되는지 확인하면 된다.
    // (http://localhost/member/refreshToken으로 테스트하면 된다) 마지막으로 실패한 인증토큰으로 복붙해서 테스트해야 함을 잊지말자. && 로그인할때 받은 Cookie의 refreshToken도 담아보내 테스트해야 함을 잊지 말자(이건 자동으로 됨).
  }
  
  @GetMapping("/logout")
  public ResponseEntity<String> logout(@RequestHeader("Authorization") String authorization) {
    // AccessToken 얻기
    String accessToken = Jwt.getAccessToken(authorization);
    if(accessToken == null /*|| !Jwt.validateToken(accessToken)*/) { // 만약 null이거나, 또는 유효한 accessToken이 아니라면 -> 2022.05.31(화): 근데... 유효하지 않다고 로그아웃을 안시킬일은 없지 않음? 그래서 걍 !Jwt.validateToken(accessToken) 부분 삭제.
      return ResponseEntity.status(401).body("invalid access token");
    }
    
    // Redis에 저장된 인증 정보를 삭제하기
    redisTemplate.delete(accessToken);
    
    // 클라이언트로 보낸 RefreshToken 쿠키를 삭제하기
    // 참고: 쿠키의 max age를 0으로 설정하면 쿠키가 삭제된다.
    // 저~~ 위에 설정했던 쿠키코드 고대로 복붙하되 maxAge만 0으로 설정함
    String refreshTokenCookie = ResponseCookie.from("refreshToken", "" /*어차피 삭제할건데 값을 줄 필요 없으니 빈값주기*/)
                                              .httpOnly(true)
                                              .secure(false)
                                              .path("/")
                                              .maxAge(0)
                                              .domain("localhost")
                                              .build()
                                              .toString();
    
    // 응답 설정
    return ResponseEntity
            .ok()
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie)
            .body("success");
  }
}
