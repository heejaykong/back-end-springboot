package com.mycompany.backend.controller;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mycompany.backend.dto.Board;
import com.mycompany.backend.dto.Member;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping("/rest")
public class RestControllerTest {
	@GetMapping("/getObject")
	public Board getObject() {
		log.info("실행");
		Board board = new Board();
		board.setBno(1);
		board.setBtitle("제목");
		board.setBcontent("내용");
		board.setMid("user");
		board.setBdate(new Date());
		return board;
	}

	@GetMapping("/getMap")
	public Map<String, Object> getMap() {
		log.info("실행");

		Map<String, Object> map = new HashMap<>();
		map.put("name", "홍길동");
		map.put("age", 25);

		Board board = new Board();
		board.setBno(1);
		board.setBtitle("제목");
		board.setBcontent("내용");
		board.setMid("user");
		board.setBdate(new Date());
		map.put("board", board);

		return map;
	}

	@GetMapping("/getArray")
	public String[] getArray() {
		// 배열을 JSON으로 리턴하는 간단한 메소드
		log.info("실행");
		String[] array = { "Java", "Spring", "Vue", "JavaScript" };
		return array;
	}

	@GetMapping("/getList1")
	public List<String> getList1() {
		// List를 JSON으로 리턴하는 간단한 메소드
		log.info("실행");
		List<String> list = new ArrayList<>();
		list.add("Java");
		list.add("Spring");
		list.add("Vue");
		return list;
	}

	@GetMapping("/getList2")
	public List<Board> getList2() {
		log.info("실행");
		List<Board> list = new ArrayList<>();
		for (int i = 1; i <= 3; i++) {
			Board board = new Board();
			board.setBno(i);
			board.setBtitle("제목" + i);
			board.setBcontent("내용" + i);
			board.setMid("user");
			board.setBdate(new Date());
			list.add(board);
		}
		return list;
	}
	
	// 응답에 단순한 object가 아닌 헤더에 뭘 담아서 보내고 싶다? 위에 했던 방식으론 안된다.
	// 그러면 어떻게 해야 하는가?
	// 일단 이전 springframework 프로젝트에서 실습했던 것처럼 HttpServletResponse를 사용해봐보자.
	@GetMapping("/useHttpServletResponse")
	public void getHeader(HttpServletResponse response) throws Exception {
		// 응답헤더 설정
		response.setContentType("application/json; charset=UTF-8");
		response.addHeader("TestHeader", "test");
		
		Cookie cookie = new Cookie("refreshToken", "xxxxxx");
		response.addCookie(cookie);
		
		// 응답본문 설정
		PrintWriter pw = response.getWriter();
		// JSONObject를 쓰려면 dependency설정을 해야 함.
		// springframework 프로젝트에서 했던 것처럼 pom.xml에 JSONObject를 쓸 수 있게 해주는 dependency 코드를 여기 pom.xml에 복붙하자.
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("result", "success");
		String json = jsonObject.toString();
		
		pw.println(json);
		pw.flush();
		pw.close();
	}
	
	// 위에서 사용한 건 raw API다. 이제 스프링(부트)에서 제공하는 Advanced API를 사용해보자.
	// 그것은 바로 ResponseEntity.
	@GetMapping("/useResponseEntity")
	public ResponseEntity<String> useResponseEntity() {
		
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("result", "success");
		String json = jsonObject.toString();
		
		// 아래처럼 BodyBuilder를 통해 만들 수도 있다고... 그러나 이게 괜히 더 복잡하다.
		/*
		BodyBuilder bodyBuilder = ResponseEntity.ok();
		ResponseEntity<String> result = bodyBuilder.body(json);
		return result; */
		// 그래서 그 대신 아래처럼 보통 많이 작성한다고...
		
//		Cookie cookie = new Cookie("refreshToken", "xxxxxx"); // Raw API 방식
		String cookieStr = ResponseCookie.from("refreshToken", "yyyyyy") // Advanced API 방식
										.build()
										.toString();
		
		return ResponseEntity.ok() // 이 ok()가 리턴하는게 BodyBuilder다.
				.header(HttpHeaders.CONTENT_TYPE, "application/json;"/*charset=UTF-8" 안 넣어도 요즘 브라우저는 다 UTF-8로 해석해주기 때문에 생략.*/) // 직접 Content-Type 키값을 하드코딩하기보단 제공되는 상수를 사용하는게 좋다.
				.header("TestHeader", "value")
				.header(HttpHeaders.COOKIE, cookieStr)
				.body(json);
	}
	
	// QueryString으로 전송
	@RequestMapping("/sendQueryString")
	public Member sendQueryString(Member member) {
		return member;
	}
	
	// 바디에 application/json으로 전송
	@PostMapping("/sendJson")
	public Member sendJson(@RequestBody Member member) { //JSON으로 정보를 넘길때는 이렇게 @RequestBody를 잊지 말자~ 그래야 JSON을 파싱해서 Member 객체에 세팅해줌.
		return member;
	}
	
	@PostMapping("/sendMultipartFormData")
	public Map<String, String> sendMultipartFormData(String title, MultipartFile attach) throws Exception {
		// 저장될 파일명 생성하기~ ("날짜" + "원본파일명")
		String savedFile = new Date().getTime() + "-" + attach.getOriginalFilename();
		// 파일로 변환하기
		attach.transferTo(new File("C:/Temp/uploadedfiles/" + savedFile));
		// 결과 및 저장된이름 담아서 리턴하기
		Map<String, String> map = new HashMap<>();
		map.put("result", "success");
		map.put("savedFilename", savedFile);
		
		return map;
	}
}
