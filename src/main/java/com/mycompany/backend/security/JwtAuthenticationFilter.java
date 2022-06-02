package com.mycompany.backend.security;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.extern.log4j.Log4j2;

@Log4j2
//@Component//(2022.05.31.(화)실습내용) 이 필터를 관리객체로 만들어줘야 주입이 가능하니까.
// 그러나... 이제 관리객체가 되어버리기 때문에 새로운 객체로써 만들 수 없음. 대신 JwtAuthenticationFilter를 주입시켜야 함.(SecurityConfig.java파일 보기)
// 그게 번거로우니까 걍 SecurityConfig.java파일에서 @Bean 객체로 등록해서 주입해줌. SecurityConfig.java파일 코드 봐보기.
// @Bean객체로 등록하는게 좀더 교수님이 권장하는 방법.
public class JwtAuthenticationFilter extends OncePerRequestFilter {
//  @Resource
  private RedisTemplate redisTemplate; //(2022.05.31.(화)실습내용) 주입을 해야 Redis에 존재하는지 여부를 따질 수 있다.
  public void setRedisTemplate(RedisTemplate redisTemplate) { //private이니까 세터 만들어주고, SecurityConfing.java다시 참고
    this.redisTemplate = redisTemplate;
  }
  
  // 메소드 재정의하기
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    log.info("실행");
    
    // 요청 헤더로부터 Authorization 헤더 값 얻기(2022. 05. 30.(월) 실습)
    String authorization = request.getHeader("Authorization");
    
    // AccessToken 추출
    String accessToken = Jwt.getAccessToken(authorization);
    log.info(accessToken); //포스트맨에서 http://localhost/board/list할 때 Authorization헤더에 토큰을 담아 send를 해보고 로깅 잘 되는지 확인해보자.
    
    // 검증작업
    if(accessToken != null && Jwt.validateToken(accessToken)) { // null값인지 아닌지 확인하면서 && 검증작업하기
      //Redis에 존재하는지 여부 확인(2022.05.31.(화)실습내용)
      //MemberController에 있는 두줄 복붙해옴
      ValueOperations<String, String> vo = redisTemplate.opsForValue();
      String redisRefreshToken = vo.get(accessToken);
      if(redisRefreshToken != null) {/*이줄의redisRefreshToken는2022.05.31.(화)실습내용*/ 
        //인증처리하기
        Map<String, String> userInfo = Jwt.getUserInfo(accessToken); // 일단 사용자 정보를 얻어냄(mid와 authority)
        String mid = userInfo.get("mid");
        String authority = userInfo.get("authority");
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(mid, null, /*사실 패스워드는 입력할 필요 없다. 이미 validateToken 작업을 이미 끝낸 상태기 때문에.*/ AuthorityUtils.createAuthorityList(authority)); // 기본적으로 제공되는 이런 인증객체가 있다. 얘는 기본생성자가 없음. 생성자 파라미터로 뭐 들어가야 하는지 Ctrl+space눌러서 확인해보자. principal는 아이디를 말하는거고, credentials는 패스워드를 말하는거라고 보면 됨. authorities Collection은 권한을 얘기하는 거고. authorities Collection을 쉽게 만들라고 AuthorityUtils라는 유틸을 스프링시큐리티가 제공해준다.
        SecurityContext securityContext = SecurityContextHolder.getContext(); // 이런게 있다더라.
        securityContext.setAuthentication(authentication); // 인증처리 완료~ 다시 포스트맨 send해보면 이제는 데이터까지 잘 넘어오는 걸 볼 수 있다.
      }
    }
    
    // 다음 필터 실행
    filterChain.doFilter(request, response); //JwtAuthenticationFilter 다음에 실행될 필터를 위해 작성하는 코드 한줄이라고..
  }
}
