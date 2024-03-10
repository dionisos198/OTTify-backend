package tavebalak.OTTify.community.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import tavebalak.OTTify.community.entity.Community;
import tavebalak.OTTify.community.repository.CommunityRepository;
import tavebalak.OTTify.error.ErrorCode;
import tavebalak.OTTify.error.exception.NotFoundException;
import tavebalak.OTTify.user.entity.User;
import tavebalak.OTTify.user.repository.LikedCommunityRepository;
import tavebalak.OTTify.user.repository.UserRepository;

@SpringBootTest
public class RedissonLockTest {

    @Autowired
    private LikedCommunityRepository likedCommunityRepository;
    @Autowired
    private CommunityRepository communityRepository;
    @Autowired
    private CommunityServiceImpl communityService;

    @Autowired
    private UserRepository userRepository;
    private static final int THREAD_COUNT = 2;

    @Test
    @DisplayName("likeCount에서 Redisson 적용후 동시성 문제 있는 경우")
    @Rollback(false)
    public void likedCommunity() throws Exception {

        User user1 = userRepository.findById(5L).get();
        User user2 = userRepository.findById(7L).get();

        Community community = communityRepository.findById(12L).orElseThrow(() ->
            new NotFoundException(ErrorCode.COMMUNITY_NOT_FOUND));

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        executorService.submit(() -> {
            try {
                communityService.likeSubjectForTest(user1, community, 12L);
            } finally {
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                communityService.likeSubjectForTest(user2, community, 12L);
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        assertThat(community.getLikeCount()).isNotEqualTo(2);
    }
}
