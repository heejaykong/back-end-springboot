package com.mycompany.backend.controller;

import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mycompany.backend.dto.Board;
import com.mycompany.backend.dto.Pager;
import com.mycompany.backend.service.BoardService;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping("/board")
public class BoardController {
  @Resource
  private BoardService boardService;

  @GetMapping("/list")
  public Map<String, Object> list(@RequestParam(defaultValue = "1") int pageNo) { // 넘어오지 않을 경우를 대비해 기본값 설정.
    log.info("실행");

    int totalRows = boardService.getTotalBoardNum();
    Pager pager = new Pager(5, 5, totalRows, pageNo);

    List<Board> list = boardService.getBoards(pager);

    Map<String, Object> map = new HashMap<>();
    map.put("boards", list);
    map.put("pager", pager); // 페이징 처리를 해주기 위해 pager도 같이 넘겨줘야 한다.
    return map;
  }

  @PostMapping("/")
  public Board create(Board board) { // @RequestBody 어노테이션이 없다는 것은 Body에 담아보내는 방식이 아니라는 뜻.
    log.info("실행");
    // 첨부파일이 있는가?
    if (board.getBattach() != null && !board.getBattach().isEmpty()) {
      // 있으면 뚱땅뚱땅 파일명 만들어서 저장해~!
      MultipartFile mf = board.getBattach();
      board.setBattachoname(mf.getOriginalFilename());
      board.setBattachsname(new Date().getTime() + "-" + mf.getOriginalFilename());
      // 그리고 contentType도 저장해~!
      board.setBattachtype(mf.getContentType());
      // 파일 만들어서 따로 저장해~!
      try {
        File file = new File("C:/Temp/uploadedfiles/" + board.getBattachsname());
        mf.transferTo(file);
      } catch (Exception e) {
        log.error(e.getMessage()); // error 레벨 로깅을 해보자
        e.printStackTrace();
      }
    }
    boardService.writeBoard(board); // 이거 실행하고나서는 selectKey 덕분에 아래처럼 getBno()를 사용할 수 있어짐..
    // 성공적으로 저장되면 데이터베이스로부터 저장된걸 가져와보자~
    Board dbBoard = boardService.getBoard(board.getBno(), false);
    return dbBoard;
  }

  @PutMapping("/")
  public Board put(Board board) {
    // create() 메소드 고대로 복붙해옴
    log.info("실행");
    // 첨부파일이 있는가?
    if (board.getBattach() != null && !board.getBattach().isEmpty()) {
      // 있으면 뚱땅뚱땅 파일명 만들어서 서장해~!
      MultipartFile mf = board.getBattach();
      board.setBattachoname(mf.getOriginalFilename());
      board.setBattachsname(new Date().getTime() + "-" + mf.getOriginalFilename());
      // 그리고 contentType도 저장해~!
      board.setBattachtype(mf.getContentType());
      // 파일 만들어서 따로 저장해~!
      try {
        File file = new File("C:/Temp/uploadedfiles/" + board.getBattachsname());
        mf.transferTo(file);
      } catch (Exception e) {
        log.error(e.getMessage()); // error 레벨 로깅을 해보자
        e.printStackTrace();
      }
    }
    boardService.updateBoard(board); // 이거 실행하고나서는 selectKey 덕분에 아래처럼 getBno()를 사용할 수 있어짐..
    // 성공적으로 저장되면 데이터베이스로부터 저장된걸 가져와보자~
    Board dbBoard = boardService.getBoard(board.getBno(), false);
    return dbBoard;
  }

  @GetMapping("/{bno}")
  // http://localhost/board/3 이런식으로 가져오고 싶은 거임.
  public Board read(@PathVariable int bno, @RequestParam(defaultValue = "false") boolean hit) { // @PathVariable 어노테이션을
                                                                                                // 쓰면 된다.
    // 성공적으로 가져와지면 리턴하자~
    Board dbBoard = boardService.getBoard(bno, hit);
    return dbBoard;
  }

  @DeleteMapping("/{bno}")
  public Map<String, String> delete(@PathVariable int bno) {
    boardService.removeBoard(bno);
    Map<String, String> map = new HashMap<>();
    map.put("result", "success");
    return map;
  }

  @GetMapping("/battach/{bno}")
  public ResponseEntity<InputStreamResource> download(@PathVariable int bno) throws Exception {
    Board board = boardService.getBoard(bno, false);
    String battachoname = board.getBattachoname();
    if (battachoname == null)
      return null; // 만약 첨부됐던 파일이 없으면 null 리턴

    // 가져온 파일명이 한글인 경우:
    battachoname = new String(battachoname.getBytes("UTF-8"), "ISO-8859-1");
    // 굳이 크로스브라우징 처리할 게 아니라면 위 한 줄로 충분하다(만약 크로스브라우징도 하고 싶으면 기존 springframework 프로젝트의
    // Ch09Controller 코드 참고.)

    // 파일 입력스트림 생성하기
    FileInputStream fis = new FileInputStream("C:/Temp/uploadedfiles/" + board.getBattachsname());
    InputStreamResource resource = new InputStreamResource(fis); // 이게 필요한 이유는... 아래 ResponseEntity에 담기 위해.

    // 응답 생성하기
    return ResponseEntity.ok()
        // 파일을 다운로드하고자 할때 필요한, "CONTENT_DISPOSITION" 한 줄.
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + battachoname + "\";")
        .header(HttpHeaders.CONTENT_TYPE, board.getBattachtype())
        .body(resource);
  }
}
