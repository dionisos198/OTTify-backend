package tavebalak.OTTify.community.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tavebalak.OTTify.community.dto.request.CommunitySubjectCreateDTO;
import tavebalak.OTTify.community.dto.request.CommunitySubjectEditDTO;
import tavebalak.OTTify.community.dto.request.ReplyCommentCreateDTO;
import tavebalak.OTTify.community.dto.request.ReplyCommentEditDTO;
import tavebalak.OTTify.community.dto.request.ReplyRecommentCreateDTO;
import tavebalak.OTTify.community.entity.Community;
import tavebalak.OTTify.community.entity.Reply;
import tavebalak.OTTify.community.service.CommunityServiceImpl;
import tavebalak.OTTify.community.service.ReplyService;
import tavebalak.OTTify.error.exception.NotFoundException;
import tavebalak.OTTify.program.entity.Program;

@ExtendWith(MockitoExtension.class)
class CommunityControllerTest {

    @InjectMocks
    private CommunityController communityController;
    @Mock
    private CommunityServiceImpl communityService;
    @Mock
    private ReplyService replyService;
    private MockMvc mockMvc;

    @InjectMocks
    private ObjectMapper objectMapper;

    Long programId = 1L;
    Long subjectId = 1L;
    Long commentId = 1L;

    private static final String testComment = "test content";


    @BeforeEach
    public void init() { // mockMvc 초기화, 각메서드가 실행되기전에 초기화 되게 함
        mockMvc = MockMvcBuilders.standaloneSetup(communityController).build();
        // standaloneMockMvcBuilder() 호출하고 스프링 테스트 설정 커스텀으로 mockMvc 객체 생성

    }

    @Test
    @DisplayName("POST 주제등록 컨트롤러 로직 성공")
    public void registerSubject() throws Exception {
        //given
        CommunitySubjectCreateDTO request = registerSubjectRequest();

        //when
        MockMultipartFile file = new MockMultipartFile("file", "song.png", "multipart/form-data",
            "uploadFile".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile json = new MockMultipartFile("dto", null, "application/json",
            objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(
                MockMvcRequestBuilders
                    .multipart("/api/v1/discussion/subject")
                    .file(json)
                    .file(file)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .characterEncoding("UTF-8")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", "성공적으로 토론주제를 생성하였습니다.").exists());

    }

    private CommunitySubjectCreateDTO registerSubjectRequest() {
        return CommunitySubjectCreateDTO.builder()
            .programId(1L)
            .subjectName("사랑과 우정의 따뜻한 이야기 '응답하라 1988'")
            .content("'응답하라 1988'은 사랑과 우정의 따뜻한 이야기를 그려냈습니다. 이 드라마가 많은 사랑을 받은 이유에 대해 토론해보세요!")
            .build();
    }

    @Test
    @DisplayName("POST 댓글 등록 컨트롤러 로직 성공")
    void registerComment() throws Exception, NotFoundException {
        //given
        Community response = Community.builder().title("test-title")
            .id(1L)
            .content("test-content")
            .program(
                Program.testBuilder().title("test-title").id(1L).posterPath("test-path").build())
            .title("test-title")
            .build();
        ReplyCommentCreateDTO replyRequest = ReplyCommentCreateDTO.builder()
            .subjectId(response.getId())
            .comment(testComment)
            .build();
        Reply replyResponse = replyService.saveComment(replyRequest);

        when(communityService.saveSubjectForTest(any())).thenReturn(response);
        communityService.saveSubjectForTest(registerSubjectRequest());

        when(replyService.saveComment(any(ReplyCommentCreateDTO.class)))
            .thenReturn(replyResponse);

        //when, then
        ResultActions resultActions = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/discussion/comment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new Gson().toJson(replyRequest))
        );

        //then
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$.data", "성공적으로 토론댓글을 생성하였습니다.").exists());

    }

    @Test
    @DisplayName("POST 대댓글 등록 컨트롤러 로직 성공")
    void registerRecomment() throws Exception, NotFoundException {
        //given
        Community response = Community.builder().title("test-title")
            .content("test-content")
            .program(
                Program.testBuilder().title("test-title").id(1L).posterPath("test-path").build())
            .title("test-title")
            .build();
        Reply replyResponse = Reply.builder()
            .id(1L)
            .community(response)
            .content("test-content")
            .build();

        when(communityService.saveSubjectForTest(any())).thenReturn(response);
        when(replyService.saveComment(any())).thenReturn(replyResponse);
        doNothing().when(replyService).saveRecomment(any());

        communityService.saveSubjectForTest(registerSubjectRequest());
        replyService.saveComment(registerCommentRequest());

        //when, then
        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/v1/discussion/recomment")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new Gson().toJson(registerRecommentRequest(1L))))
            .andExpect(status().isOk());

        //then
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$.data", "성공적으로 토론대댓글을 생성하였습니다.").exists());
    }

    private ReplyCommentCreateDTO registerCommentRequest() {
        return ReplyCommentCreateDTO.builder()
            .comment("test-comment")
            .subjectId(1L)
            .build();
    }

    private ReplyRecommentCreateDTO registerRecommentRequest(Long id) {
        return ReplyRecommentCreateDTO.builder()
            .subjectId(id)
            .commentId(1L)
            .comment(testComment)
            .build();
    }

    @Test
    @DisplayName("POST 대댓글 등록 컨트롤러 로직 실패 - 빈 내용")
    public void getFailBadIdTestRecommentContent() throws Exception, NotFoundException {
        //given
        Community response = Community.builder().title("test-title")
            .content("test-content")
            .program(
                Program.testBuilder().title("test-title").id(1L).posterPath("test-path").build())
            .title("test-title")
            .build();
        Reply replyResponse = Reply.builder()
            .id(1L)
            .community(response)
            .content("test-content")
            .build();

        when(communityService.saveSubjectForTest(any())).thenReturn(response);
        when(replyService.saveComment(any())).thenReturn(replyResponse);

        communityService.saveSubjectForTest(registerSubjectRequest());
        replyService.saveComment(registerCommentRequest());

        ReplyRecommentCreateDTO testContent = ReplyRecommentCreateDTO.builder()
            .subjectId(subjectId)
            .commentId(commentId)
            .comment(" ")
            .build();

        //when, then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/discussion/recomment")
                .content(new Gson().toJson(testContent))
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("utf-8"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("PUT 주제 수정 컨트롤러 로직 성공")
    public void 토론_주제_내용_수정_성공() throws Exception, NotFoundException {
        //given
        Community response = Community.builder().title("test-title")
            .id(1L)
            .content("test-content")
            .program(
                Program.testBuilder().title("test-title").id(1L).posterPath("test-path").build())
            .title("test-title")
            .build();
        when(communityService.saveSubjectForTest(any())).thenReturn(response);
        Community savedCommunity = communityService.saveSubjectForTest(registerSubjectRequest());

        doNothing().when(communityService).modifySubject(any(), any());

        CommunitySubjectEditDTO editDTO = CommunitySubjectEditDTO.builder()
            .subjectId(savedCommunity.getId())
            .content(savedCommunity.getContent())
            .subjectName(savedCommunity.getTitle())
            .build();

        //when
        MockMultipartFile file = new MockMultipartFile("file", "song.png", "multipart/form-data",
            "uploadFile".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile json = new MockMultipartFile("dto", null, "application/json",
            objectMapper.writeValueAsString(editDTO).getBytes(StandardCharsets.UTF_8));

        ResultActions perform = mockMvc.perform(
                MockMvcRequestBuilders
                    .multipart(HttpMethod.PUT, "/api/v1/discussion/subject")
                    .file(file)
                    .file(json)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                    .characterEncoding("utf-8"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data").value("성공적으로 토론주제를 수정하였습니다."))
            .andExpect(status().isOk())
            .andDo(print());

    }

    @Test
    @DisplayName("PUT 댓글 수정 컨트롤러 로직 성공")
    public void 댓글_수정_성공() throws Exception, NotFoundException {
        //given
        Community response = Community.builder().title("test-title")
            .id(1L)
            .content("test-content")
            .program(
                Program.testBuilder().title("test-title").id(1L).posterPath("test-path").build())
            .title("test-title")
            .build();
        Reply replyResponse = Reply.builder()
            .id(1L)
            .community(response)
            .content("test-content")
            .build();

        when(communityService.saveSubjectForTest(any())).thenReturn(response);
        when(replyService.saveComment(any())).thenReturn(replyResponse);
        doNothing().when(replyService).saveRecomment(any());

        Community savedCommunity = communityService.saveSubjectForTest(registerSubjectRequest());
        replyService.saveComment(registerCommentRequest());
        replyService.saveRecomment(registerRecommentRequest(1L));

        ReplyCommentEditDTO replyCommentEditDTOs = new ReplyCommentEditDTO(1L,
            1L, "test-content");

        doNothing().when(replyService).modifyComment(any());

        //when, then
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/discussion/comment")
                .content(new Gson().toJson(replyCommentEditDTOs))
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("utf-8"))
            .andExpect(jsonPath("$.data").value("성공적으로 토론댓글을 수정하였습니다."))
            .andDo(print())
            .andExpect(status().isOk())
            .andDo(print());


    }

    @Test
    @DisplayName("DELETE 주제 삭제 컨트롤러 성공")
    public void 주제_삭제_성공() throws Exception, NotFoundException {
        //given
        doNothing().when(communityService).deleteSubject(any());

        //when
        ResultActions resultActions = mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/discussion/subject/1")
                .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("성공적으로 토론주제를 삭제하였습니다."));
    }

    @Test
    @DisplayName("DELETE 댓글 삭제 컨트롤러 성공")
    public void 댓글_삭제_성공() throws Exception, NotFoundException {
        //given
        doNothing().when(replyService).deleteComment(anyLong(), anyLong());

        //when
        ResultActions resultActions = mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/discussion/comment/1/50")
                .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("성공적으로 토론댓글을 삭제하였습니다."));
    }

    @Test
    @DisplayName("DELETE 토론 대댓글 삭제 컨트롤러 성공")
    public void 대댓글_삭제_성공() throws NotFoundException, Exception {
        //given
        doNothing().when(replyService).deleteRecomment(anyLong());

        //when
        ResultActions resultActions = mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/discussion/recomment/27")
                .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("성공적으로 토론대댓글을 삭제하였습니다."));
    }

}