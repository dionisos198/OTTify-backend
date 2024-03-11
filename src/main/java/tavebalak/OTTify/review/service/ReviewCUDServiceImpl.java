package tavebalak.OTTify.review.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tavebalak.OTTify.common.lock.DistributeLock;
import tavebalak.OTTify.error.ErrorCode;
import tavebalak.OTTify.error.exception.BadRequestException;
import tavebalak.OTTify.error.exception.NotFoundException;
import tavebalak.OTTify.genre.entity.Genre;
import tavebalak.OTTify.genre.repository.UserGenreRepository;
import tavebalak.OTTify.program.entity.Program;
import tavebalak.OTTify.program.repository.ProgramRepository;
import tavebalak.OTTify.review.dto.reviewrequest.ReviewSaveDto;
import tavebalak.OTTify.review.dto.reviewrequest.ReviewUpdateDto;
import tavebalak.OTTify.review.entity.Review;
import tavebalak.OTTify.review.entity.ReviewTag;
import tavebalak.OTTify.review.repository.ReviewRepository;
import tavebalak.OTTify.review.repository.ReviewReviewTagRepository;
import tavebalak.OTTify.review.repository.ReviewTagRepository;
import tavebalak.OTTify.user.entity.User;
import tavebalak.OTTify.user.repository.LikedReviewRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewCUDServiceImpl implements ReviewCUDService {

    private final ReviewRepository reviewRepository;
    private final ProgramRepository programRepository;
    private final UserGenreRepository userGenreRepository;
    private final ReviewTagRepository reviewTagRepository;
    private final LikedReviewRepository likedReviewRepository;
    private final ReviewReviewTagRepository reviewReviewTagRepository;

    //리뷰 저장


    @Override
    @Transactional
    public void saveReview(User user, ReviewSaveDto reviewSaveDto) {
        Program program = programRepository.findById(reviewSaveDto.getProgramId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.PROGRAM_NOT_FOUND));

        //같은 프로그램에는 리뷰를 못남기게 변경
        if (reviewRepository.existsByProgramAndUser(program, user)) {
            throw new BadRequestException(ErrorCode.CAN_NOT_SAVE_REVIEW_IN_SAME_PROGRAM);
        }

        // 첫번째 장르를 선택하지 않았다면 리뷰를 남길 수 없습니다.
        Genre usersFirstGenre = userGenreRepository.findByUserAndIsFirst(user, true)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_FIRST_GENRE_NOT_FOUND))
            .getGenre();

        Review review = Review.builder()
            .content(reviewSaveDto.getContents())
            .user(user)
            .program(program)
            .rating(reviewSaveDto.getRating())
            .genre(usersFirstGenre.getName())
            .build();

        //리뷰 태그 저장
        reviewSaveDto.getReviewTagIdDtoList().forEach(tagId -> {
            ReviewTag reviewTag = reviewTagRepository.findById(tagId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.REVIEW_TAG_NOT_FOUND));
            review.addReviewTag(reviewTag);
        });

        // 리뷰 저장
        program.addReview(review);

        //유저 평균 별점 업데이트
        user.addUsersReviewAndRecalculateRating(review.getRating());

    }

    //리뷰 수정
    @Override
    @Transactional
    public void updateReview(User user, Long reviewId, ReviewUpdateDto reviewUpdateDto) {

        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.REVIEW_NOT_FOUND));

        //자신의 리뷰가 아니면 수정할 수 없게 했습니다.
        if (review.getUser().getId() != user.getId()) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }

        Program program = programRepository.findById(review.getProgram().getId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.PROGRAM_NOT_FOUND));

        // program 의 평균 별점 및 user 의 평균 별점 업데이트
        program.changeProgramReviewRatingAndRecalculatingAverage(review.getRating(),
            reviewUpdateDto.getRating());
        user.changeUsersReviewAndRecalculateRating(review.getRating(), reviewUpdateDto.getRating());

        //이전에 있었던 ReviewTag 리스트
        List<Long> preReviewTagList = review.getReviewReviewTags().stream()
            .map(reviewReviewTag -> reviewReviewTag.getReviewTag().getId())
            .collect(Collectors.toList());
        List<Long> nowReviewTagList = reviewUpdateDto.getReviewTagIdDtoList();

        if (!preReviewTagList.isEmpty()) {// 이전 ReviewTag가 존재하는 경우
            // 삭제 tags - 이전 리스트에는 있는데 현재 리스트에 없는 경우
            List<Long> deleteTags = preReviewTagList.stream()
                .filter(tag -> !nowReviewTagList.contains(tag))
                .collect(Collectors.toList());
            reviewReviewTagRepository.deleteAllByIdInQuery(deleteTags, reviewId);

            //추가 tags - 이전 리스트에는 없는데 현재 리스트에 있는 경우
            List<Long> insertTags = nowReviewTagList.stream()
                .filter(tag -> !preReviewTagList.contains(tag))
                .collect(Collectors.toList());

            insertTags.stream()
                .forEach(tag -> {
                    review.addReviewTag(reviewTagRepository.findById(tag)
                        .orElseThrow(() -> new NotFoundException(ErrorCode.REVIEW_TAG_NOT_FOUND)));
                });

        } else {
            nowReviewTagList.stream().forEach(tag -> {
                review.addReviewTag(reviewTagRepository.findById(tag)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.REVIEW_TAG_NOT_FOUND)));
            });
        }

        // contents 와 Rating 수정
        review.changeContentAndRatingReview(reviewUpdateDto.getContents(),
            reviewUpdateDto.getRating());
    }


    //리뷰 삭제
    @Override
    @Transactional
    public void deleteReview(User user, Long reviewId) {

        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.REVIEW_NOT_FOUND));
        Program program = programRepository.findById(review.getProgram().getId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.PROGRAM_NOT_FOUND));

        if (review.getUser().getId() != user.getId()) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }

        //user 의 평균 별점 및 업데이트
        user.deleteUsersReviewAndRecalculateRating(review.getRating());

        //프로그램에서 Review 삭제
        program.deleteReview(review);

    }

    //리뷰 좋아요 기능을 구현합니다.
    @Override
    @Transactional
    @DistributeLock(key = "T(java.lang.String).format('LikeReview%d', #reviewId)")
    public void likeReview(User user, Long reviewId) {
        //리뷰가 없을 경우 예외
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.REVIEW_NOT_FOUND));

        //자신의 리뷰에 추천했을 경우 예외
        if (review.getUser().getId() == user.getId()) {
            throw new BadRequestException(ErrorCode.CAN_NOT_SELF_LIKE_REVIEW_REQUEST);
        }

        //한번 누르면 좋아요 갯수 증가 , 두번 누르면 좋아요 갯수 감소
        likedReviewRepository.findByUserIdAndReviewId(user.getId(), reviewId)
            .ifPresentOrElse(likedReview -> {
                user.getLikedReviews().remove(likedReview);
                review.cancelLikeNumber();
            }, () -> {
                user.likeReview(review);
                review.addLikeNumber();
            });
    }
}
