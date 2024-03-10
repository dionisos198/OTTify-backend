package tavebalak.OTTify.review.service;

import java.util.List;
import tavebalak.OTTify.review.dto.response.LatestReviewsDTO;
import tavebalak.OTTify.review.entity.Review;

public interface ReviewService {

    List<LatestReviewsDTO> getLatestReviews();

    void saveReview(Review review);

    void likeReview(Long id);
}
