package com.mycompany.backend.controller;

import java.net.URI;

import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
//스프링부트는 controller중 ErrorController를 만나면 무조건 /error 요청을 실행시킨다고.
public class ErrorHandlerController implements ErrorController {
  @RequestMapping("/error")
  public ResponseEntity<String> error(HttpServletResponse response) {
    int status = response.getStatus();
    if (status == 404) { // 404일 때만 다음과 같이 실행하겠다는 것임.
      return ResponseEntity
          .status(HttpStatus.MOVED_PERMANENTLY) // 응답코드 301을 표현하는 상수임.
          .location(URI.create("/")) // 경로를 명시하는것임.
          .body(""); // redirect니까 특별한 메시지 없음
    } else {
      return ResponseEntity.status(status).body(""); // 그 이외 status들은 그대로 프론트에게 전달해주면 됨.
    }
  }
}
