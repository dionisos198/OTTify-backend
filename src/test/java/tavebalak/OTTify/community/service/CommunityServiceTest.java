package tavebalak.OTTify.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import tavebalak.OTTify.community.dto.request.CommunitySubjectCreateDTO;
import tavebalak.OTTify.community.dto.request.CommunitySubjectEditDTO;
import tavebalak.OTTify.community.dto.response.CommunitySubjectsDTO;
import tavebalak.OTTify.community.dto.response.CommunitySubjectsListDTO;
import tavebalak.OTTify.community.entity.Community;
import tavebalak.OTTify.community.repository.CommunityRepository;
import tavebalak.OTTify.error.exception.NotFoundException;
import tavebalak.OTTify.program.entity.Program;
import tavebalak.OTTify.program.repository.ProgramRepository;
import tavebalak.OTTify.user.entity.LikedCommunity;
import tavebalak.OTTify.user.entity.User;
import tavebalak.OTTify.user.repository.LikedCommunityRepository;

@ExtendWith(MockitoExtension.class)
public class CommunityServiceTest {

    @Mock
    private CommunityRepository communityRepository;
    @Mock
    private LikedCommunityRepository likedCommunityRepository;
    @Mock
    private ProgramRepository programRepository;
    @InjectMocks
    private CommunityServiceImpl communityService;
    User.TestUserBuilder testUserBuilder = User.testUserBuilder();


    @Test
    @DisplayName("주제 save 기능이 제대로 동작하는지 확인")
    public void saveSubject() throws Exception {
        //given
        User user = testUserBuilder.create(1L, "test-nickName", "test-url", 5.55);

        CommunitySubjectCreateDTO requestDto = registerSubjectRequest();
        Program program = Program.testBuilder().id(1L).build();
        when(programRepository.findById(anyLong())).thenReturn(Optional.of(program));
        when(communityRepository.save(any())).thenReturn(
            toEntity(requestDto, user, program));

        //when
        communityService.saveSubjectForTest(requestDto);

        //then
        //id 생성 전략을 identity를 사용, 실제 DB에 저장되어야만 ID가 저장된다. 따라서 테스트에서 ID를 검증 불가
        //ID 검증을 위해서는 Repository를 사용하므로 mock이 아닌 실제 빈으로 사용해야 가능할 것 같다.
    }

    private CommunitySubjectCreateDTO registerSubjectRequest() {
        return CommunitySubjectCreateDTO.builder()
            .programId(1L)
            .subjectName("사랑과 우정의 따뜻한 이야기 '응답하라 1988'")
            .content("'응답하라 1988'은 사랑과 우정의 따뜻한 이야기를 그려냈습니다. 이 드라마가 많은 사랑을 받은 이유에 대해 토론해보세요!")
            .build();
    }

    private Community toEntity(CommunitySubjectCreateDTO dto, User user,
        Program program) {
        return Community.builder()
            .user(user)
            .id(1L)
            .title(dto.getSubjectName())
            .program(program)
            .content(dto.getContent())
            .build();
    }

    @Test
    @DisplayName("주제 수정 기능이 제대로 동작하는지 확인")
    public void modifySubject() throws Exception, NotFoundException {
        //given
        User user = testUserBuilder.create(1L, "test-nickName", "test-url", 5.55);

        CommunitySubjectCreateDTO requestDto = registerSubjectRequest();
        Program program = Program.testBuilder().id(1L).title("test-program-title").build();
        when(programRepository.findById(anyLong())).thenReturn(Optional.of(program));
        when(communityRepository.save(any())).thenReturn(
            toEntity(requestDto, user, program));
        communityService.saveSubjectForTest(requestDto);
        when(communityRepository.findById(anyLong())).thenReturn(
            Optional.of(toEntity(requestDto, user, program)));

        //when
        Community findCommunity = communityService.modifySubjectForTest(
            makeCommunitySubjectEditDTO(), user);

        //then
        assertThat(findCommunity).isNotNull();
        assertThat(findCommunity.getTitle()).isEqualTo("test-subject");
        assertThat(findCommunity.getContent()).isEqualTo("test-content");
        assertThat(findCommunity.getProgram().getTitle()).isEqualTo("test-program-title");
    }

    private CommunitySubjectEditDTO makeCommunitySubjectEditDTO() {
        return CommunitySubjectEditDTO.builder()
            .subjectId(1L)
            .subjectName("test-subject")
            .content("test-content")
            .build();
    }

    @Test
    @DisplayName("주제 삭제 기능이 제대로 동작하는지 확인")
    public void deleteSubject() throws Exception, NotFoundException {
        //given
        User user = testUserBuilder.create(1L, "test-nickName", "test-url", 5.55);

        CommunitySubjectCreateDTO requestDto = registerSubjectRequest();
        Program program = Program.testBuilder().id(1L).title("test-program-title").build();
        when(communityRepository.save(any())).thenReturn(
            toEntity(requestDto, user, program));
        when(programRepository.findById(anyLong())).thenReturn(Optional.of(program));
        communityService.saveSubjectForTest(requestDto);
        when(communityRepository.findById(anyLong())).thenReturn(
            Optional.of(toEntity(requestDto, user, program)));
        doNothing().when(communityRepository).delete(any());

        //when
        communityService.deleteSubjectForTest(1L, user);

        //then
        assertEquals(0, communityRepository.count());
    }

    @Test
    @DisplayName("전체 주제 조회 기능이 제대로 동작하는지 확인")
    public void findAllSubjects() throws Exception {
        //given
        User user = testUserBuilder.create(1L, "test-nickName", "test-url", 5.55);
        CommunitySubjectCreateDTO requestDto = registerSubjectRequest();
        Program program = Program.testBuilder().id(1L).title("test-program-title").build();
        when(communityRepository.save(any())).thenReturn(
            toEntity(requestDto, user, program));
        when(programRepository.findById(anyLong())).thenReturn(Optional.of(program));
        communityService.saveSubjectForTest(requestDto);

        CommunitySubjectCreateDTO other = CommunitySubjectCreateDTO.builder()
            .programId(10L)
            .subjectName("사랑과 우정의 따뜻한 이야기 '응답하라 1988'")
            .content("'응답하라 1988'은 사랑과 우정의 따뜻한 이야기를 그려냈습니다. 이 드라마가 많은 사랑을 받은 이유에 대해 토론해보세요!")
            .build();
        communityService.saveSubjectForTest(other);

        CommunitySubjectsDTO build = CommunitySubjectsDTO.builder()
            .subjectId(1L)
            .programId(5L)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .nickName(user.getNickName())
            .title(requestDto.getSubjectName())
            .build();

        CommunitySubjectsDTO build2 = CommunitySubjectsDTO.builder()
            .subjectId(1L)
            .programId(10L)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .nickName(user.getNickName())
            .title(requestDto.getSubjectName())
            .build();

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.Direction.DESC, "createdAt");
        Page<Community> page = new PageImpl<>(
            List.of(toEntity(requestDto, user, program), toEntity(requestDto, user, program),
                toEntity(other, user, program),
                toEntity(other, user, program)),
            pageRequest, 4);
        when(communityRepository.findCommunitiesBy(pageRequest)).thenReturn(page);

        //when
        CommunitySubjectsListDTO allSubjects = communityService.findAllSubjects(pageRequest);

        //then
        assertThat(allSubjects).isNotNull();
        assertThat(allSubjects.getSubjectAmount()).isEqualTo(4);
        allSubjects.getList().stream().anyMatch(dto -> dto.equals(build) || dto.equals(build2));
    }

    @Test
    @DisplayName("특정 프로그램의 주제 조회가 제대로 동작하는지 확인")
    public void findSingleProgramSubjects() throws Exception {
        //given
        User user = testUserBuilder.create(1L, "test-nickName", "test-url", 5.55);
        CommunitySubjectCreateDTO requestDto = registerSubjectRequest();
        Program program = Program.testBuilder().id(1L).title("test-program-title").build();
        when(communityRepository.save(any())).thenReturn(
            toEntity(requestDto, user, program));
        when(programRepository.findById(anyLong())).thenReturn(Optional.of(program));
        communityService.saveSubjectForTest(requestDto);

        CommunitySubjectCreateDTO other = CommunitySubjectCreateDTO.builder()
            .programId(10L)
            .subjectName("사랑과 우정의 따뜻한 이야기 '응답하라 1988'")
            .content("'응답하라 1988'은 사랑과 우정의 따뜻한 이야기를 그려냈습니다. 이 드라마가 많은 사랑을 받은 이유에 대해 토론해보세요!")
            .build();
        communityService.saveSubjectForTest(other);

        CommunitySubjectsDTO build = CommunitySubjectsDTO.builder()
            .subjectId(1L)
            .programId(5L)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .nickName(user.getNickName())
            .title(requestDto.getSubjectName())
            .build();

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.Direction.DESC, "createdAt");
        Page<Community> page = new PageImpl<>(
            List.of(toEntity(requestDto, user, program), toEntity(requestDto, user, program)),
            pageRequest, 2);
        //when
        when(communityRepository.findCommunitiesByProgramId(pageRequest, 5L)).thenReturn(page);
        CommunitySubjectsListDTO singleProgramSubjects = communityService.findSingleProgramSubjects(
            pageRequest, 5L);

        //then
        assertThat(singleProgramSubjects).isNotNull();
        assertThat(singleProgramSubjects.getSubjectAmount()).isEqualTo(2);
        singleProgramSubjects.getList().stream().anyMatch(dto -> dto.equals(build));
    }

    @Test
    @DisplayName("게시글 공감이 제대로 동작하는지 확인")
    public void likeSubject() throws Exception {
        //given
        User user = testUserBuilder.create(1L, "test-nickName", "test-url", 5.55);
        CommunitySubjectCreateDTO requestDto = registerSubjectRequest();
        Program program = Program.testBuilder().id(1L).title("test-program-title").build();
        when(communityRepository.save(any())).thenReturn(toEntity(requestDto, user, program));
        when(programRepository.findById(anyLong())).thenReturn(Optional.of(program));
        communityService.saveSubjectForTest(requestDto);

        Community entity = toEntity(requestDto, user, program);

        when(likedCommunityRepository.findByCommunityIdAndUserId(anyLong(), anyLong())).thenReturn(
            Optional.of(toLikedCommunity(entity, user)));

        //when
        communityService.likeSubjectForTest(user, entity, 1L);

        //then
        LikedCommunity likedCommunity = likedCommunityRepository.findByCommunityIdAndUserId(1L, 1L)
            .get();
        assertThat(likedCommunity).isNotNull();
        assertThat(likedCommunity.getCommunity().getContent()).isEqualTo(
            "'응답하라 1988'은 사랑과 우정의 따뜻한 이야기를 그려냈습니다. 이 드라마가 많은 사랑을 받은 이유에 대해 토론해보세요!");

    }

    private LikedCommunity toLikedCommunity(Community community, User user) {
        return LikedCommunity.builder()
            .community(community)
            .user(user)
            .build();
    }

    @Test
    @DisplayName("게시글 공감해제가 제대로 동작하는지 확인")
    public void unlikeSubject() throws Exception {
        //given
        User user = testUserBuilder.create(1L, "test-nickName", "test-url", 5.55);
        CommunitySubjectCreateDTO requestDto = registerSubjectRequest();
        Program program = Program.testBuilder().id(1L).title("test-program-title").build();
        when(communityRepository.save(any())).thenReturn(toEntity(requestDto, user, program));
        when(programRepository.findById(anyLong())).thenReturn(Optional.of(program));
        Community entity = communityService.saveSubjectForTest(requestDto);

        when(likedCommunityRepository.findByCommunityIdAndUserId(eq(1L),
            eq(user.getId()))).thenReturn(
            Optional.of(toLikedCommunity(entity, user)));

        //when
        communityService.likeSubjectForTest(user, entity, 1L);
        communityService.likeSubjectForTest(user, entity, 1L);

        //then
        Assertions.assertThat(entity.getLikeCount()).isEqualTo(0);
    }
}
