package tavebalak.OTTify.review.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tavebalak.OTTify.error.ErrorCode;
import tavebalak.OTTify.error.exception.NotFoundException;
import tavebalak.OTTify.oauth.jwt.SecurityUtil;
import tavebalak.OTTify.review.dto.response.LatestReviewsDTO;
import tavebalak.OTTify.review.entity.Review;
import tavebalak.OTTify.review.repository.ReviewRepository;
import tavebalak.OTTify.user.entity.LikedReview;
import tavebalak.OTTify.user.entity.User;
import tavebalak.OTTify.user.repository.LikedReviewRepository;
import tavebalak.OTTify.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final LikedReviewRepository likedReviewRepository;
    private final UserRepository userRepository;
    private final int TOP8_SIZE = 8;

    public List<LatestReviewsDTO> getLatestReviews() {
        List<Review> reviewList = findReviewList();
        List<Review> top8ReviewList = new ArrayList<>();
        if (reviewList.size() < TOP8_SIZE) {
            top8ReviewList.addAll(reviewList);
        } else {
            top8ReviewList.addAll(reviewList.subList(0, TOP8_SIZE));
        }
        return top8ReviewList.stream().map(
            listOne -> builderLatestReviewsDTO(listOne, getLikeSum(listOne.getId()))
        ).collect(Collectors.toList());

    }

    private List<Review> findReviewList() {
        return reviewRepository.findAll(
            Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private LatestReviewsDTO builderLatestReviewsDTO(Review listOne, Integer listOne1) {
        return LatestReviewsDTO.builder()
            .reviewId(listOne.getId())
            .nickName(listOne.getUser().getNickName())
            .content(listOne.getContent())
            .programTitle(listOne.getProgram().getTitle())
            .reviewRating(listOne.getRating())
            .profilePhoto(listOne.getUser().getProfilePhoto())
            .likeCount(listOne1)
            .build();
    }

    private Integer getLikeSum(Long reviewId) {
        Optional<List<LikedReview>> likedReviews = likedReviewRepository.findByReviewId(reviewId);
        return likedReviews
            .map(List::size)
            .orElse(0);
    }

    public List<LatestReviewsDTO> getLatestReviewsTest() {
        List<Review> reviewList = findReviewList();
        List<Review> top8ReviewList = new ArrayList<>();
        if (reviewList.size() < TOP8_SIZE) {
            top8ReviewList.addAll(reviewList);
        } else {
            top8ReviewList.addAll(reviewList.subList(0, TOP8_SIZE));
        }
        return top8ReviewList.stream().map(
            listOne -> builderLatestReviewsDTO(listOne, 0)
        ).collect(Collectors.toList());

    }


    public void saveReview(Review review) {
        reviewRepository.save(review);
    }

    @Override
    public void likeReview(Long id) {
        String userEmail = SecurityUtil.getCurrentEmail().get();
        User savedUser = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        Review review = findReviewById(id);

        likedReviewRepository.findByUserIdAndReviewId(savedUser.getId(), review.getId())
            .ifPresentOrElse(
                entity -> cancelLikeReview(entity, review),
                () -> postLikeReview(id, savedUser, review)
            );
    }

    private void postLikeReview(Long id, User savedUser,
        Review review) {
        saveLikeReview(id, savedUser);
        increaseLikeCountForReview(review);
    }

    private static void increaseLikeCountForReview(Review review) {
        review.addLikeNumber();
    }

    private void saveLikeReview(Long id, User savedUser) {
        likedReviewRepository.save(
            builderLikedReview(id, savedUser)
        );
    }

    private void cancelLikeReview(LikedReview entity, Review review) {
        deleteLikeReview(entity);
        decreaseLikeCountForReview(review);
    }

    private static void decreaseLikeCountForReview(Review review) {
        review.cancelLikeNumber();
    }

    private void deleteLikeReview(LikedReview entity) {
        likedReviewRepository.delete(entity);
    }

    private LikedReview builderLikedReview(Long id, User savedUser) {
        return LikedReview.builder()
            .user(savedUser)
            .review(findReviewById(id))
            .build();
    }

    private Review findReviewById(Long id) {
        return reviewRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.REVIEW_NOT_FOUND));
    }
}
