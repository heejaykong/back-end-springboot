package com.mycompany.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Configuration
public class RedisConfig {
  // application.properties에 정의한 값들 불러와 주입받는 것임.(참고로 springframework에서 제공하는 @Value어노테이션 import해야 함)
  @Value("${spring.redis.hostName}")
  private String hostName;
  @Value("${spring.redis.port}")
  private String port;
  @Value("${spring.redis.password}")
  private String password;
  
  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    log.info("실행");
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
    
    // 다음과 같이 코드상에서 설정해주는 config들은 언제든지 바뀔수 있는 속성들임.
    // 코드에 직접 하드코딩하면 유지보수가 어려워짐.
    // 따라서 이런 가변성이 있는 값들은 따로 properties파일에 몰아 넣어두는 것이 좋다.
    
    config.setHostName(hostName);
    config.setPort(Integer.parseInt(port)); // 기본 포트
    config.setPassword(password); // 우리가 .conf 파일에서 설정할때 수정한 비밀번호
    LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(config); // LettuceConnectionFactory<< 이것은... 연결을 위한 redis 클라이언트라고 생각하면 됨.
    return connectionFactory;
  }
  
  @Bean
  public RedisTemplate<String, String> redisTemplate(/*RedisConnectionFactory redisConnectionFactory*/) { // 1. 위에서 만든걸 매개변수로 받아서 주입할 수 있음.
    log.info("실행");
    RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(redisConnectionFactory()); // 2. 또는 메소드처럼 호출해서 주입할 수 있음.
    redisTemplate.setKeySerializer(new StringRedisSerializer()); // key와 value 둘다 문자열인데, 바이트 배열로 넣어라 이거임
    redisTemplate.setValueSerializer(new StringRedisSerializer());
    return redisTemplate;
  }
}
