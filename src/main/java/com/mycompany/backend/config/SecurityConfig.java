package com.mycompany.backend.config;

import javax.annotation.Resource;

import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.mycompany.backend.security.JwtAuthenticationFilter;

import lombok.extern.log4j.Log4j2;

// * 스프링시큐리티는 어정쩡하게 설정해놓고 테스트할 수가 없는 구조. 다~~ 설정한 담에 테스트해야 한다

@Log4j2
@EnableWebSecurity // @EnableWebSecurity를 해줘야 자동적으로 confirue() 메소드들을 실행시켜준다고...
public class SecurityConfig extends WebSecurityConfigurerAdapter {
//  @Resource
//  JwtAuthenticationFilter jwtAuthenticationFilter;//*2022.05.31.(화)실습내용*/
  @Resource
  private RedisTemplate redisTemplate;
  
  // 각각의 메소드가 설정이 다르기 때문에 configure()메소드 3개를 다 선언한다고...
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    log.info("실행");
    // 서버세션 비활성화하기(기본적으로 spring security는 세션을 활용하기 때문에 그걸 막아줄거임(우린 jwt쓸거니까.))
    http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS); // "세션을 사용하지 않겠소!" -> 이제 JSESSIONID가 생성되지 않을 것임.
    // 폼 로그인 필터도 비활성화하기(왜냐? rest API는 로그인폼따위 없음.)
    http.formLogin().disable();
    // 사이트간 요청 위조 방지 비활성화. csrf 조사하는 필터도 비활성화하겠단 소리
    // 개발할 때 매우 귀찮은거다.라고 설명한 바 있었음. 다 개발 끝나고 나서 활성화 시키면 된다고햇었는데, restAPI에서는 폼조차 사용하지 않기 때문에 걍 비활성화시키면 됨.
    http.csrf().disable();
    // 요청경로 권한 설정하기
    http.authorizeRequests()
        .antMatchers("/board/**").authenticated() // (** <<이런식으로 표현하는게 ant표현식이라 antMatchers인거임.) /board/밑에 요청하는 것들은 반드시 인정된 사용자여야만 한다.
        .antMatchers("/**").permitAll(); // 그 이외의 것들은 로그인 없이도 사용할 수 있도록 다 허용하겠다.

    // CORS설정(rest API는 반드시 설정해야 하는 부분 -> 다른 도메인의 자바스크립트로 접근을 할 수 있도록 허용해주는 설정임)
    http.cors(); // 일단 활성화만 시키고, 어떤 요청방식으로 넘어왔을 때 허용하겠네 뭐네 추가적인 설정은 나중에 나온다. 아래 corsConfigurationSource() 메소드 참고하기.
    
//    (다음은원래 기본적으로 제공되는 Security FilterChain에는 없는, 필터를 끼워넣겠다는 설정임)
    // JWT 인증 필터 추가
//    인터넷에 흔히 나오는 방법은,
//    UsernamePasswordAuthenticationFilter.class를 추가하는 건데,
    // 사실 이건 UsernamePasswordAuthenticationFilter 대신에 내꺼 JwtAuthenticationFilter를 쓰겠다는 소리라고...
    // 폼 요청 전(before)에 내 JwtAuthenticationFilter를 동작시키기 위함이라고...
    http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class); // UsernamePasswordAuthenticationFilter는 formLogin필터와 관련있음. 근데 formLogin은 비활성화시켰잖슴? jwt필터를 아무데나 끼워넣을 순 없고 폼로그인필터 앞에 끼워넣고 싶으니까 걍 UsernamePasswordAuthenticationFilter 먹버하는거임.
                          // /*2022.05.31.(화)실습내용*/ -> new JwtAuthenticationFilter()에서 jwtAuthenticationFilter()로 바꿈
                          // 아래 Bean객체로 만든 jwtAuthenticationFilter()를 참고하자.
  }
  
//  @Bean/*2022.05.31.(화)실습내용*/ 사실 여기저기 쓰이는 놈이 아니라 여기에서만 쓰일 거니까 꼭 @Bean안붙여도 됨.
  public JwtAuthenticationFilter jwtAuthenticationFilter() {
    JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter();
    jwtAuthenticationFilter.setRedisTemplate(redisTemplate);
    return jwtAuthenticationFilter;
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    // id와 password를 DB에 있는 것과 비교해야 하는데, DB에 잇는 놈을 불러와서 비교작업을 해주는 건 AuthenticationManager다.
    log.info("실행");
    
    // MPA 폼 인증 방식에서 다음과 같이 사용(JWT 인증방식에서는 사용하지 않음.)
    /* DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(new CustomUserDetailsService()); // 데이터베이스에서 뭘 가져올 건가?를 정의하는 것. (자... 이걸 위해 예전에 만들어뒀던게 사실 있었다고. 아랫출 참고)
    // CustomUserDetails.java와 CustomUserDetailsService.java 파일을 참고하자. (안에 주석도 있음)
    provider.setPasswordEncoder(passwordEncoder()); // 비밀번호 암호화를 위해 뭘 사용할건가?를 정의. 아래 passwordEncoder 메소드 참고
    auth.authenticationProvider(provider); //비로소 우리가 정의한 provider를 제공하면 끝난다. */
    
    // 위의 코드 블록... 실컷 했는데 왜 주석처리하느냐
    // MPA 폼인증방식에서나 저렇게 쓰기 때문. 즉 MPA 개발환경에서나 저렇게 쓰기 때문이다.(formLogin 비활성화했잖슴)
  }

  @Override
  public void configure(WebSecurity web) throws Exception { // 인증이 필요 없는 경로가 뭐뭐 있는지
    // 인증이 없어도 요청할 수 있는 자원들. ex: src/main/webapp/resources/ 밑의 css나 images들 등...
    // 이것도 Rest API 방식에서는 사용 안합니다! 하여간 서버에 뭘 두고 하는 게 없음.
    log.info("실행");
    DefaultWebSecurityExpressionHandler defaultWebSecurityExpressionHandler = new DefaultWebSecurityExpressionHandler();
    defaultWebSecurityExpressionHandler.setRoleHierarchy(roleHierarchyImpl()); // 아래 roleHierarchyImpl()메소드 참고. ()이 붙었다고 메소드 실행이 아니고, @Bean이 붙은 메소드일 경우라면, ()는 실행하라는 뜻이 아니고 "이 이름을 갖는 객체를 주입받아서 매개값으로 받겠다"는 소리라고... (실행되는 시점은 처음 어플리케이션이 로딩될때 딱한번 실행된다고.)
    web.expressionHandler(defaultWebSecurityExpressionHandler);
    
    /* MPA에서 시큐리티를 적용하지 않는 경로 설정하기(spring framework의 resources폴더 아래 있던 정적 자원들 있잖슴.)
    web.ignoring()
    .antMatchers("/images/**")
    .antMatchers("/css/**")
    .antMatchers("/js/**")
    .antMatchers("/bootstrap/**")
    .antMatchers("/jquery/**")
    .antMatchers("/favicon.ico"); // 이 모든건 다 MPA방식에 따른 방법임. rest API는 css고 뭐고 js, bootstrap 등 제공 안함.!!
     */
  }
  
  @Bean // 메소드 이름으로, 이 메소드가 리턴하는 객체를 관리객체로 만들어주는 역할을 @Bean이 해주는데, @Configuration 어노테이션이 있어야 사용할 수 있음
  // 근데 여기 클래스에서는 @Configuration이 없는데 어케 쓸 수 있는거지? @EnableWebSecurity도 @Configuration이 포함되어 있기 때문에.
  public PasswordEncoder passwordEncoder() {
//    return new BCryptPasswordEncoder(); // 비번암호화 종류 걍 다양하게 하지 않고 무조건 BCrypt로 하겠다고 하면 이거 리턴하면 됨.
    return PasswordEncoderFactories.createDelegatingPasswordEncoder(); // (권장)
  }
  
  @Bean
  public RoleHierarchyImpl roleHierarchyImpl() { // ch17_security.xml의 roleHierarchyImpl과 비교해보자.
     log.info("실행");
     RoleHierarchyImpl roleHierarchyImpl = new RoleHierarchyImpl();
     roleHierarchyImpl.setHierarchy("ROLE_ADMIN > ROLE_MANAGER > ROLE_USER");
     return roleHierarchyImpl;
  }
  
  // Rest API에서만 다음과 같이 사용
  // 위에 http.cors();라고 설정한 것을 더 상세히 설정해주는 부분임.
  @Bean
  public CorsConfigurationSource corsConfigurationSource() { // 메소드명은 꼬옥 똑같이 해라
    log.info("실행");
      CorsConfiguration configuration = new CorsConfiguration();
      //모든 요청 사이트 허용
      configuration.addAllowedOrigin("*"); // (모든 도메인의 자바스크립트가 접근을 할 수 있도록 허용하는 것)
      //모든 요청 방식 허용
      configuration.addAllowedMethod("*");
      //모든 요청 헤더 허용
      configuration.addAllowedHeader("*");
      //모든 URL 요청에 대해서 위 내용을 적용
      UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
      source.registerCorsConfiguration("/**", configuration);
      return source;
  }
  
}
