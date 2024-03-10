package tavebalak.OTTify.community.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tavebalak.OTTify.common.BaseResponse;
import tavebalak.OTTify.community.dto.request.CommunitySubjectCreateDTO;
import tavebalak.OTTify.community.dto.request.CommunitySubjectEditDTO;
import tavebalak.OTTify.community.dto.request.ReplyCommentCreateDTO;
import tavebalak.OTTify.community.dto.request.ReplyCommentEditDTO;
import tavebalak.OTTify.community.dto.request.ReplyRecommentCreateDTO;
import tavebalak.OTTify.community.dto.request.ReplyRecommentEditDTO;
import tavebalak.OTTify.community.dto.response.CommunityAriclesDTO;
import tavebalak.OTTify.community.dto.response.CommunitySubjectsListDTO;
import tavebalak.OTTify.community.service.CommunityService;
import tavebalak.OTTify.community.service.ReplyService;
import tavebalak.OTTify.error.exception.NotFoundException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/discussion")
@Api(tags = {"토론 컨트롤러"})

public class CommunityController {

    private final CommunityService communityService;
    private final ReplyService replyService;

    @ApiOperation(value = "토론주제 생성", notes = "회원이 작성한 토론내용을 기반으로 생성됩니다.")
    @ApiResponse(code = 200, message = "성공적으로 토론주제를 생성하였습니다.")
    @PostMapping(value = "/subject", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<String> registerSubject(
        @Valid @RequestPart(value = "dto") CommunitySubjectCreateDTO c,
        @RequestPart(value = "file", required = false) MultipartFile image) {
        communityService.saveSubject(c, image);
        return BaseResponse.success("성공적으로 토론주제를 생성하였습니다.");
    }

    @ApiOperation(value = "토론주제 수정", notes = "회원이 작성한 토론내용을 기반으로 수정됩니다.")
    @ApiResponse(code = 200, message = "성공적으로 토론주제를 수정하였습니다.")
    @PutMapping(value = "/subject", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<String> modifySubject(
        @Valid @RequestPart(value = "dto") CommunitySubjectEditDTO c,
        @RequestPart(value = "file", required = false) MultipartFile image)
        throws NotFoundException {
        communityService.modifySubject(c, image);
        return BaseResponse.success("성공적으로 토론주제를 수정하였습니다.");
    }

    @ApiOperation(value = "토론주제 삭제", notes = "회원이 작성한 토론주제를 삭제합니다.")
    @ApiResponse(code = 200, message = "성공적으로 토론주제를 삭제하였습니다.")
    @ApiImplicitParam(name = "subjectId", value = "토론글의 id", required = true, paramType = "path")
    @DeleteMapping("/subject/{subjectId}")
    public BaseResponse<String> deleteSubject(@PathVariable Long subjectId)
        throws NotFoundException {
        communityService.deleteSubject(subjectId);
        return BaseResponse.success("성공적으로 토론주제를 삭제하였습니다.");
    }

    @ApiOperation(value = "토론글 공감 및 해제", notes = "회원이 작성한 토론 주제에 대해 공감하고 해제합니다.")
    @ApiResponse(code = 200, message = "성공적으로 토론 게시글 공감 해제가 적용되었습니다.")
    @ApiImplicitParam(name = "subjectId", value = "토론글의 id", required = true, paramType = "query")
    @PostMapping("/like")
    public BaseResponse<String> likeSubject(@RequestParam("subjectId") Long subjectId) {
        communityService.likeSubject(subjectId);
        return BaseResponse.success("성공적으로 토론 게시글 공감이 적용되었습니다.");
    }

    @ApiOperation(value = "토론댓글 공감 및 해제", notes = "회원이 작성한 토론 주제의 댓글에 대해 공감하고 해제합니다.")
    @ApiResponse(code = 200, message = "성공적으로 토론 댓글 공감이 적용/해제되었습니다.")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "subjectId", value = "토론글의 id", required = true, paramType = "query"),
        @ApiImplicitParam(name = "commentId", value = "토론 댓글의 id", required = true, paramType = "query")
    })
    @PostMapping("/like/comment")
    public BaseResponse<String> likeComment(
        @RequestParam("subjectId") Long subjectId,
        @RequestParam("commentId") Long commentId) {
        communityService.likeComment(subjectId, commentId);
        return BaseResponse.success("성공적으로 토론 댓글 공감 적용이 되었습니다.");
    }

    @ApiOperation(value = "토론 주제 조회", notes = "전체 프로그램에 대해 작성된 토론글을 조회합니다.")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "page", value = "페이지 번호(0부터 시작)", paramType = "query"),
        @ApiImplicitParam(name = "direction", value = "내림차순과 오름차순", paramType = "query"),
        @ApiImplicitParam(name = "sort", value = "정렬기준(createdAt, updatedAt)", paramType = "query"),
        @ApiImplicitParam(name = "size", value = "페이지당 아이템 갯수", paramType = "query")
    })
    @GetMapping("/total")
    public BaseResponse<CommunitySubjectsListDTO> getTotalProgramsSubjects(
        @PageableDefault(size = 10,
            sort = "createdAt",
            direction = Sort.Direction.DESC,
            page = 0
        ) Pageable pageable) {
        CommunitySubjectsListDTO page = communityService.findAllSubjects(pageable);
        return BaseResponse.success(page);
    }

    @ApiOperation(value = "프로그램별 토론주제 조회", notes = "특정 프로그램에 대해 작성된 토론글을 조회합니다.")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "page", value = "페이지 번호(0부터 시작)", paramType = "query"),
        @ApiImplicitParam(name = "direction", value = "내림차순과 오름차순", paramType = "query"),
        @ApiImplicitParam(name = "sort", value = "정렬기준(createdAt, updatedAt)", paramType = "query"),
        @ApiImplicitParam(name = "size", value = "페이지당 아이템 갯수", paramType = "query"),
        @ApiImplicitParam(name = "programId", value = "프로그램 id", required = true, paramType = "query")
    })
    @GetMapping("/program")
    public BaseResponse<CommunitySubjectsListDTO> getTotalProgramSubjects(
        @PageableDefault(size = 10,
            sort = "createdAt",
            direction = Sort.Direction.DESC,
            page = 0) Pageable pageable,
        @RequestParam("programId") Long programId) {

        CommunitySubjectsListDTO page = communityService.findSingleProgramSubjects(pageable,
            programId);
        return BaseResponse.success(page);
    }

    @ApiOperation(value = "토론 댓글 생성", notes = "회원이 작성한 토론 주제에 댓글을 생성한다.")
    @ApiResponse(code = 200, message = "성공적으로 토론 댓글을 생성하였습니다.")
    @PostMapping("/comment")
    public BaseResponse<String> registerComment(
        @Valid @RequestBody ReplyCommentCreateDTO replyCommentCreateDTO)
        throws NotFoundException {
        replyService.saveComment(replyCommentCreateDTO);
        return BaseResponse.success("성공적으로 토론댓글을 생성하였습니다.");
    }

    @ApiOperation(value = "토론 대댓글 생성", notes = "회원이 작성한 토론 주제의 댓글에 대댓글을 생성한다.")
    @ApiResponse(code = 200, message = "성공적으로 토론 대댓글을 생성하였습니다.")
    @PostMapping("/recomment")
    public BaseResponse<String> registerRecomment(
        @Valid @RequestBody ReplyRecommentCreateDTO replyRecommentCreateDTO) {
        replyService.saveRecomment(replyRecommentCreateDTO);
        return BaseResponse.success("성공적으로 토론 대댓글을 생성하였습니다.");
    }

    @ApiOperation(value = "토론 댓글 수정", notes = "회원이 작성한 토론 주제의 댓글을 수정한다.")
    @ApiResponse(code = 200, message = "성공적으로 토론 댓글을 수정하였습니다.")
    @PutMapping("/comment")
    public BaseResponse<String> modifyComment(
        @Valid @RequestBody ReplyCommentEditDTO replyCommentEditDTO)
        throws NotFoundException {
        replyService.modifyComment(replyCommentEditDTO);
        return BaseResponse.success("성공적으로 토론댓글을 수정하였습니다.");
    }

    @ApiOperation(value = "토론 대댓글 수정", notes = "회원이 작성한 토론 주제의 대댓글을 수정한다.")
    @ApiResponse(code = 200, message = "성공적으로 토론 대댓글을 수정하였습니다.")
    @PutMapping("/recomment")
    public BaseResponse<String> modifyRecomment(
        @Valid @RequestBody ReplyRecommentEditDTO replyRecommentEditDTO)
        throws NotFoundException {
        replyService.modifyRecomment(replyRecommentEditDTO);
        return BaseResponse.success("성공적으로 토론대댓글을 수정하였습니다.");
    }

    @ApiOperation(value = "토론댓글 삭제", notes = "회원이 작성한 토론 주제의 댓글을 삭제한다.")
    @ApiResponse(code = 200, message = "성공적으로 토론 댓글을 삭제하였습니다.")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "subjectId", value = "토론글의 id", required = true, paramType = "path"),
        @ApiImplicitParam(name = "commentId", value = "토론댓글의 id", required = true, paramType = "path")
    })
    @DeleteMapping("/comment/{subjectId}/{commentId}")
    public BaseResponse<String> deleteComment(@PathVariable Long subjectId,
        @PathVariable Long commentId) throws NotFoundException {
        replyService.deleteComment(subjectId, commentId);
        return BaseResponse.success("성공적으로 토론댓글을 삭제하였습니다.");
    }

    @ApiOperation(value = "토론 대댓글 삭제", notes = "회원이 작성한 토론 주제의 대댓글을 삭제한다.")
    @ApiResponse(code = 200, message = "성공적으로 토론 대댓글을 삭제하였습니다.")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "recommentId", value = "토론 대댓글의 id", required = true, paramType = "path")
    })
    @DeleteMapping("/recomment/{recommentId}")
    public BaseResponse<String> deleteRecomment(
        @PathVariable Long recommentId) throws NotFoundException {
        replyService.deleteRecomment(recommentId);
        return BaseResponse.success("성공적으로 토론대댓글을 삭제하였습니다.");
    }

    @ApiOperation(value = "토론 게시글 조회", notes = "작성된 토론 게시글 하나를 조회한다.")
    @ApiImplicitParam(name = "subjectId", value = "토론주제 ID 값", required = true, paramType = "path")
    @GetMapping("/{subjectId}")
    public BaseResponse<CommunityAriclesDTO> getArticle(@PathVariable Long subjectId)
        throws NotFoundException {
        return BaseResponse.success(communityService.getArticleOfASubject(subjectId));
    }
}
