package com.mycompany.backend.config;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.WebApplicationContext;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Configuration
@MapperScan(basePackages={"com.mycompany.backend.dao"})
public class MyBatisConfig {
	
	@Resource
	private DataSource dataSource; // DataSourceConfig.java에서 @Bean으로 등록한 관리객체를, 의존주입해 줌.
//	근데 위처럼 하지 않아도, 객체를 주입하고자 하는 메소드의 파라미터로 넘겨도, 자동적으로 의존주입이 된다고.
	
	@Resource
	WebApplicationContext wac;
	
	@Bean
	public SqlSessionFactory sqlSessionFactory() throws Exception {
		SqlSessionFactoryBean ssfb = new SqlSessionFactoryBean();
		ssfb.setDataSource(dataSource);
		// setConfigLocation를 사용하려면 Resource객체를 파라미터에 넘겨줘야 하므로 WebApplicationContext를 활용해야 한다고...
		ssfb.setConfigLocation(wac.getResource("classpath:mybatis/mapper-config.xml"));
		ssfb.setMapperLocations(wac.getResources("classpath:mybatis/mapper/*.xml"));
		return ssfb.getObject();
	}
	
	@Bean
	public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory ssf) { // 앞서 관리객체로 등록한 sqlSessionFactory를 파라미터로 넘기면 자동으로 의존주입이 됨.
		SqlSessionTemplate sst = new SqlSessionTemplate(ssf);
		return sst;
	}
}
