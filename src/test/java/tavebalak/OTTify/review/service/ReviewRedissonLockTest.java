package tavebalak.OTTify.review.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import tavebalak.OTTify.error.ErrorCode;
import tavebalak.OTTify.error.exception.NotFoundException;
import tavebalak.OTTify.review.entity.Review;
import tavebalak.OTTify.review.repository.ReviewRepository;
import tavebalak.OTTify.user.entity.User;
import tavebalak.OTTify.user.repository.UserRepository;


@SpringBootTest
public class ReviewRedissonLockTest {

    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private ReviewCUDService reviewCUDService;

    @Autowired
    private UserRepository userRepository;
    private static final int THREAD_COUNT = 2;

    @Test
    @DisplayName("Review의 likeCount에서 Redisson 적용후 동시성 문제를 테스트합니다")
    @Rollback(false)
    @Transactional
    public void likedReview() throws Exception {

        User user1 = userRepository.findById(57L).get();
        User user2 = userRepository.findById(58L).get();

        Review review = reviewRepository.findById(21L).orElseThrow(() -> new NotFoundException(
            ErrorCode.REVIEW_NOT_FOUND));

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        executorService.submit(() -> {
            try {
                reviewCUDService.likeReview(user1, review.getId());
            } finally {
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                reviewCUDService.likeReview(user2, review.getId());
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        assertThat(review.getLikeCounts()).isEqualTo(2);
    }
}
