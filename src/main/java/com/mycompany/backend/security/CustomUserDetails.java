package com.mycompany.backend.security;

import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

// User를 상속받았다는 건 곧 UserDetails를 상속받았단 소리와도 마찬가지. (User도 UserDetails꺼기 때문)
public class CustomUserDetails extends User {
  // 스프링시큐리티에서 인증이 성공하고 나서, 인증정보를 얻을 때 비로소 CustomUserDetails객체를 얻을 수 있다고 함.
  // 기본적으로 id, password, enabled활성화여부, 권한(role) 정도만 알 수 있다.
  // 스프링시큐리티는 이 4개 정보밖에 모르는 바보이기 때문.
  // 그 정보 이외의 정보(예: 사용자이름, 이메일 등등)를 얻고 싶으면, 데이터베이스를 굳이 또 꺼내오기보다는
  // 어차피 한번 인증한거, 거기다 담아서 가져오고 싶다는 거임. 그럴때 커스텀으로 더 저장하고 싶은 걸 필드로 지정하는 거임.
  // 인증 정보로써 추가로 저장하고 싶은 내용을 정의한 필드가 다음 두 줄임.
	private String mname;
	private String memail;
	
	public CustomUserDetails(
			String mid, 
			String mpassword, 
			boolean menabled, 
			List<GrantedAuthority> mauthorities,
			String mname,
			String memail) {
		super(mid, mpassword, menabled, true, true, true, mauthorities);
		// 그래서 우선 super로 있던 것들 먼저 가져오고, 아래 두개는 내가 설정한 필드를 지정해주는 것.
		this.mname = mname;
		this.memail = memail;
	}
	
	public String getMname() {
		return mname;
	}

	public String getMemail() {
		return memail;
	}
}

