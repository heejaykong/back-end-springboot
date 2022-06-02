package com.mycompany.backend.security;



import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Jwt {
  // 상수
  private static final String JWT_SECRET_KEY = "kosa12345"; // 개인키라고 함. 사실 암호화해야 함. 우선 스트링으로 넣자.
  private static final long ACCESS_TOKEN_DURATION = 1000 * 5; // 30분(밀리세컨단위)
  public static final long REFRESH_TOKEN_DURATION = 1000 * 60 * 60 * 24; // 24시간
  
  // AccessToken 생성하기
  public static String createAccessToken(String mid, String authority) { // payload에 담길 정보를 파라미터로 받아옴
    log.info("실행");
    String accessToken = null;
    
    try {
      accessToken = Jwts.builder()
                        // 1. 헤더 설정
                        .setHeaderParam("alg", "HS256")
                        .setHeaderParam("typ", "JWT")
                        // 2. 페이로드 설정
                        .setExpiration(new Date(new Date().getTime() + ACCESS_TOKEN_DURATION))
                        .claim("mid", mid)
                        .claim("authority", authority)
                        // 3. 서명 설정
                        .signWith(SignatureAlgorithm.HS256, JWT_SECRET_KEY.getBytes("UTF-8"))
                        .compact();
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    
    return accessToken;
  }
  
  // RefreshToken 생성하기
  public static String createRefreshToken(String mid, String authority) { // payload에 담길 정보를 파라미터로 받아옴
    log.info("실행");
    String refreshToken = null;
    
    try {
      refreshToken = Jwts.builder()
                        // 1. 헤더 설정
                        .setHeaderParam("alg", "HS256")
                        .setHeaderParam("typ", "JWT")
                        // 2. 페이로드(claim) 설정
                        .setExpiration(new Date(new Date().getTime() + REFRESH_TOKEN_DURATION))
                        .claim("mid", mid)
                        .claim("authority", authority)
                        // 3. 서명 설정
                        .signWith(SignatureAlgorithm.HS256, JWT_SECRET_KEY.getBytes("UTF-8"))
                        .compact();
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    
    return refreshToken;
  }
  
  // 토큰의 유효성 판단하기
  public static boolean validateToken(String token) {
    log.info("실행");
//  null처리는 안함. 이게 사용되는 곳에서 해줘야 함...
    boolean result = false;
    try {
      result = Jwts.parser()
                   .setSigningKey(JWT_SECRET_KEY.getBytes("UTF-8")) // 애초에 설정했던 시크릿키와 일치하는지 확인하는거라고...
                   .parseClaimsJws(token) // payload를 담고 있는 claim객체를 리턴
                   .getBody() // claim객체 안에서 찐 claim 비로소 얻기
                   .getExpiration() // 유효기간 얻기(Date타입)
                   .after(new Date()); // "지금보다 더 나중인가요?(=토큰이 아직 유효한가요?)"
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    
    return result;
  }
  
  // 토큰 만료 시간 얻기
  public static Date getExpiration(String token) {
    log.info("실행");
    Date result = null;
    
    try {
      result = Jwts.parser()
                   .setSigningKey(JWT_SECRET_KEY.getBytes("UTF-8")) // 애초에 설정했던 시크릿키와 일치하는지 확인하는거라고...
                   .parseClaimsJws(token) // payload를 담고 있는 claim객체를 리턴
                   .getBody() // claim객체 안에서 찐 payload(=claim) 비로소 얻기
                   .getExpiration(); // 유효기간 얻기(Date타입)
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    
    return result;
  }
  
  // 인증 사용자 정보를 얻기
  public static Map<String, String> getUserInfo(String token) {
    log.info("실행");
    Map<String, String> result = new HashMap<>();
    
    try {
      Claims claims = Jwts.parser()
                   .setSigningKey(JWT_SECRET_KEY.getBytes("UTF-8")) // 애초에 설정했던 시크릿키와 일치하는지 확인하는거라고...
                   .parseClaimsJws(token) // payload를 담고 있는 claim객체를 리턴
                   .getBody(); // claim객체 안에서 찐 payload(=claim) 비로소 얻기
      result.put("mid", claims.get("mid", String.class));
      result.put("authority", claims.get("authority", String.class));      
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    
    return result;
  }
  
  // 요청 Authorization 헤더값에서 AccessToken 얻기.
  public static String getAccessToken(String authorization) {
    String accessToken = null;
    // Bearer xxxxx.xxxxx.xxxxxx...x (access token 문자열) 이런 형식이어야만 하기 때문에 아래같은 절차가 있다고...
    if(authorization != null && authorization.startsWith("Bearer ")) {
      accessToken = authorization.substring(7); // Bearer 이후부터 끝까지 읽으면 그게 accessToken임.
    }
    return accessToken;
  }
  
  public static void main(String[] args) throws Exception {
    String accessToken = createAccessToken("user", "ROLE_USER");
    log.info(accessToken);
//    Thread.sleep(2000); 유효기간을 1초로 하고 2초 쉬게 한 뒤 false로 잘 뜨는지 테스팅해본 흔적
//    System.out.println(validateToken(accessToken));
    
    Date expireation = getExpiration(accessToken);
    System.out.println(expireation);
    
    Map<String, String> userInfo = getUserInfo(accessToken);
    System.out.println(userInfo);
  }
}
