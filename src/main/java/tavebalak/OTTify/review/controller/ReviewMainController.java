package tavebalak.OTTify.review.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tavebalak.OTTify.common.BaseResponse;
import tavebalak.OTTify.review.dto.response.LatestReviewsDTO;
import tavebalak.OTTify.review.service.ReviewService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/main")
@Api(tags = {"메인페이지 리뷰 컨트롤러"})
public class ReviewMainController {

    private final ReviewService reviewService;

    @ApiOperation(value = "최신 리뷰 조회", notes = "회원이 작성한 프로그램에 대한 최신 리뷰들을 조회한다.")
    @GetMapping("/latestReviews")
    public BaseResponse<List<LatestReviewsDTO>> getLatestReviews() {
        List<LatestReviewsDTO> reviewsDTOList = reviewService.getLatestReviews();
        return BaseResponse.success(reviewsDTOList);
    }

    @ApiOperation(value = "최신리뷰 공감 적용/해제", notes = "회원이 작성한 리뷰에 대해 공감/해제한다.")
    @ApiResponse(code = 200, message = "id = ? 인 아이디 값을 가진 리뷰에 공감이 적용/해제되었습니다.")
    @ApiImplicitParam(name = "id", value = "리뷰 ID 값", required = true, paramType = "query")
    @PostMapping("/latestReviews/like")
    public BaseResponse<String> likeReview(@RequestParam("id") Long id) {
        reviewService.likeReview(id);
        return BaseResponse.success("id = " + id + "인 아이디 값을 가진 리뷰에 공감이 적용되었습니다.");
    }

}
