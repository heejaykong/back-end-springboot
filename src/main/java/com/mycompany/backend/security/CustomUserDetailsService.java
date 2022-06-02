package com.mycompany.backend.security;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.mycompany.backend.dao.MemberDao;
import com.mycompany.backend.dto.Member;

@Service
public class CustomUserDetailsService implements UserDetailsService {
  // 이 서비스도 특별할 것 없는 똑같은 Service객체들 중 하나라고 생각하면 됨.
  
	private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);
	
	@Resource
	private MemberDao memberDao;	
	
	// UserDetailsService인터페이스의 loadUserByUsername를 오버라이드하면, 스프링 시큐리티가 알아서
	// 오버라이드된 loadUserByUsername 메소드를 갖다 쓴다.
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// username이 mid라고 보면 됨
		Member member = memberDao.selectByMid(username); 
		if(member == null) {
			throw new UsernameNotFoundException(username);
		}
		
		List<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority(member.getMrole())); // 권한을 알아내서 authorities에 추가해준다.
		
		CustomUserDetails userDetails = new CustomUserDetails(
				member.getMid(), 
				member.getMpassword(),
				member.isMenabled(),
				authorities,  //여기까지 4개 정보는 스프링시큐리티에서는 반드시 넣어야 하는 정보고.
				member.getMname(),
				member.getMemail()); //여기까지 2개는 추가적으로 내가 정의한 필드들.
		
		return userDetails;
	}
}

