package tavebalak.OTTify.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tavebalak.OTTify.community.dto.response.MyDiscussionDto;
import tavebalak.OTTify.community.entity.Community;
import tavebalak.OTTify.community.repository.CommunityRepository;
import tavebalak.OTTify.community.repository.ReplyRepository;
import tavebalak.OTTify.error.ErrorCode;
import tavebalak.OTTify.error.exception.DuplicateException;
import tavebalak.OTTify.error.exception.NotFoundException;
import tavebalak.OTTify.genre.dto.GenreDTO;
import tavebalak.OTTify.genre.dto.request.GenreUpdateDTO;
import tavebalak.OTTify.genre.entity.UserGenre;
import tavebalak.OTTify.genre.entity.Genre;
import tavebalak.OTTify.genre.repository.UserGenreRepository;
import tavebalak.OTTify.genre.repository.GenreRepository;
import tavebalak.OTTify.oauth.jwt.SecurityUtil;
import tavebalak.OTTify.program.repository.OttRepository;
import tavebalak.OTTify.review.dto.UserReviewRatingListDTO;
import tavebalak.OTTify.review.dto.response.MyReviewDto;
import tavebalak.OTTify.review.entity.Review;
import tavebalak.OTTify.review.entity.ReviewTag;
import tavebalak.OTTify.review.repository.ReviewRepository;
import tavebalak.OTTify.review.repository.ReviewReviewTagRepository;
import tavebalak.OTTify.user.dto.Request.UserOttUpdateDTO;
import tavebalak.OTTify.user.dto.Request.UserProfileUpdateDTO;
import tavebalak.OTTify.user.dto.Response.*;
import tavebalak.OTTify.user.entity.User;
import tavebalak.OTTify.user.entity.UserSubscribingOTT;
import tavebalak.OTTify.user.repository.*;

import java.util.*;
import java.util.stream.Collectors;
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserGenreRepository userGenreRepository;
    private final UserSubscribingOttRepository userSubscribingOttRepository;
    private final OttRepository ottRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewReviewTagRepository reviewReviewTagRepository;
    private final LikedProgramRepository likedProgramRepository;
    private final LikedReviewRepository likedReviewRepository;
    private final LikedCommunityRepository likedCommunityRepository;
    private final LikedReplyRepository likedReplyRepository;
    private final UninterestedProgramRepository uninterestedProgramRepository;
    private final GenreRepository genreRepository;
    private final CommunityRepository communityRepository;
    private final ReplyRepository replyRepository;

    @Override
    public UserProfileDTO getUserProfile() {
        User user = getUser();
        Long userId = user.getId();

        // 1순위 & 2순위 장르 가져오기
        UserGenre firstUserGenre = userGenreRepository.find1stGenreByUserIdFetchJoin(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ENTITY_NOT_FOUND));
        GenreDTO firstGenre = new GenreDTO(firstUserGenre);

        List<GenreDTO> secondGenre = userGenreRepository.find2ndGenreByUserIdFetchJoin(userId).stream()
                .map(ug -> new GenreDTO(ug))
                .collect(Collectors.toList());

        // 별점 리스트 가져오기
        HashMap<Double, Integer> ratingList = new HashMap<Double, Integer>();
        ArrayList<Double> ratingSet = new ArrayList<>(Arrays.asList(0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0));

        List<Double> reviewRatingList = new ArrayList<>();
        reviewRepository.findByUserId(userId).stream()
                .forEach(r -> {
                    reviewRatingList.add(r.getRating());
                });
        for (Double r : ratingSet) {
            if (Collections.frequency(reviewRatingList, r) == 0) {
                ratingList.put(r, 0);
            }
            else {
                ratingList.put(r, Collections.frequency(reviewRatingList, r));
            }
        }

        UserReviewRatingListDTO userReviewRatingListDTO = UserReviewRatingListDTO.builder()
                .totalCnt(reviewRatingList.size())
                .pointFiveCnt(ratingList.get(0.5))
                .oneCnt(ratingList.get(1.0))
                .oneDotFiveCnt(ratingList.get(1.5))
                .twoCnt(ratingList.get(2.0))
                .twoDotFiveCnt(ratingList.get(2.5))
                .threeCnt(ratingList.get(3.0))
                .threeDotFiveCnt(ratingList.get(3.5))
                .fourCnt(ratingList.get(4.0))
                .fourDotFiveCnt(ratingList.get(4.5))
                .fiveCnt(ratingList.get(5.0))
                .build();

        // OTT 리스트 가져오기
        List<UserOttDTO> userOttDTOList = userSubscribingOttRepository.findByUserIdFetchJoin(userId).stream()
                .map((UserSubscribingOTT uso) -> new UserOttDTO(uso))
                .collect(Collectors.toList());
        UserOttListDTO userOttListDTO = UserOttListDTO.builder()
                .totalCnt(userOttDTOList.size())
                .ottList(userOttDTOList)
                .build();

        // 보고싶은 프로그램 가져오기
        List<LikedProgramDTO> likedProgramDTOList = likedProgramRepository.findByUserIdFetchJoin(userId).stream()
                .map(p -> new LikedProgramDTO(p.getId(), p.getProgram().getPosterPath()))
                .collect(Collectors.toList());
        LikedProgramListDTO likedProgramListDTO = LikedProgramListDTO.builder()
                .totalCnt(likedProgramDTOList.size())
                .likedProgramList(likedProgramDTOList)
                .build();

        // 관심없는 프로그램 가져오기
        List<UninterestedProgramDTO> uninterestedProgramDTOList = uninterestedProgramRepository.findByUserIdFetchJoin(userId).stream()
                .map(p -> new UninterestedProgramDTO(p.getId(), p.getProgram().getPosterPath()))
                .collect(Collectors.toList());
        UninterestedProgramListDTO uninterestedProgramListDTO = UninterestedProgramListDTO.builder()
                .totalCnt(uninterestedProgramDTOList.size())
                .uninterestedProgramList(uninterestedProgramDTOList)
                .build();

        return UserProfileDTO.builder()
                .profilePhoto(user.getProfilePhoto())
                .nickName(user.getNickName())
                .grade(user.getGrade())
                .email(user.getEmail())
                .averageRating(user.getAverageRating())
                .firstGenre(firstGenre)
                .secondGenre(secondGenre)
                .ott(userOttListDTO)
                .ratingList(userReviewRatingListDTO)
                .likedProgram(likedProgramListDTO)
                .uninterestedProgram(uninterestedProgramListDTO)
                .build();
    }

    @Override
    public UserOttListDTO getUserOTT() {
        User user = getUser();
        Long userId = user.getId();

        List<UserOttDTO> userOttDTOList = userSubscribingOttRepository.findByUserIdFetchJoin(userId).stream()
                .map((UserSubscribingOTT uso) -> new UserOttDTO(uso))
                .collect(Collectors.toList());
        return UserOttListDTO.builder()
                .totalCnt(userOttDTOList.size())
                .ottList(userOttDTOList)
                .build();
    }

    @Override
    @Transactional
    public void update1stGenre(GenreUpdateDTO updateRequestDTO) {
        User user = getUser();
        Long userId = user.getId();

        UserGenre userGenre = userGenreRepository.findByUserIdAndIsFirst(userId, true)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ENTITY_NOT_FOUND));
        Genre genre = genreRepository.findById(updateRequestDTO.getGenreId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.GENRE_NOT_FOUND));

        userGenre.changeGenre(genre);
    }

    @Override
    @Transactional
    public void update2ndGenre(GenreUpdateDTO updateRequestDTO) {
        User user = getUser();
        Long userId = user.getId();

        // req로 들어온 id 값이 유효한 장르 id인지 확인
        Genre genre = genreRepository.findById(updateRequestDTO.getGenreId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.GENRE_NOT_FOUND));

        // 조회된 UserGenre가 있을 경우 삭제 & 없을 경우 저장
        userGenreRepository.findByGenreIdAndUserIdAndIsFirst(genre.getId(), userId, false).ifPresentOrElse(
                ug -> userGenreRepository.delete(ug),
                () -> userGenreRepository.save(
                        UserGenre.builder()
                                .genre(genre)
                                .user(user)
                                .build())
        );
    }

    @Override
    @Transactional
    public Long updateUserProfile(UserProfileUpdateDTO updateRequestDTO) {
        User user = getUser();

        // 닉네임 중복 여부 검증
        checkNicknameDuplication(user, updateRequestDTO);

        user.changeNickName(updateRequestDTO.getNickName());
        user.changeProfilePhoto(updateRequestDTO.getProfilePhoto());

        return userRepository.save(user).getId();
    }

    public void checkNicknameDuplication(User user, UserProfileUpdateDTO updateRequestDTO) {
        if (userRepository.existsByNickName(updateRequestDTO.getNickName()) && !Objects.equals(user.getNickName(), updateRequestDTO.getNickName())) {
            throw new DuplicateException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }

    @Override
    @Transactional
    public Long updateUserOTT(List<UserOttUpdateDTO> updateRequestDTO) {
        User user = getUser();
        Long userId = user.getId();

        // 이전에 구독 중이던 ott 리스트
        List<Long> preSubscribingOttList = userSubscribingOttRepository.findByUserIdFetchJoin(userId).stream()
                .map((UserSubscribingOTT t) -> t.getOtt().getId())
                .collect(Collectors.toList());

        // 현재 구독 중인 ott 리스트
        List<Long> nowSubscribingOttList = updateRequestDTO.stream()
                .map(UserOttUpdateDTO::getOttId)
                .collect(Collectors.toList());

        if (!preSubscribingOttList.isEmpty()) { // 이전 구독 중인 OTT가 있는 경우
            // 삭제 Otts - 이전 리스트에는 있는데 현재 리스트에는 없는 경우
            List<Long> deleteOtts = preSubscribingOttList.stream()
                    .filter(ott -> !nowSubscribingOttList.contains(ott))
                    .collect(Collectors.toList());
            userSubscribingOttRepository.deleteAllByIdInQuery(deleteOtts, userId);

            // 추가 otts - 이전 리스트에는 없는데 현재 리스트에는 있는 경우
            List<Long> insertOtts = nowSubscribingOttList.stream()
                    .filter(ott -> !preSubscribingOttList.contains(ott))
                    .collect(Collectors.toList());

            insertOtts.stream()
                    .forEach(ott -> {
                        UserSubscribingOTT subscribingOTT = UserSubscribingOTT.create(
                                user,
                                ottRepository.findById(ott)
                                        .orElseThrow(() -> new NotFoundException(ErrorCode.OTT_NOT_FOUND))
                        );
                        userSubscribingOttRepository.save(subscribingOTT);
                    });
        } else { // 이전 구독 중인 OTT가 없는 경우
            nowSubscribingOttList
                    .stream()
                    .forEach(ott -> {
                        UserSubscribingOTT subscribingOTT = UserSubscribingOTT.create(
                                user,
                                ottRepository.findById(ott)
                                        .orElseThrow(() -> new NotFoundException(ErrorCode.OTT_NOT_FOUND))
                        );
                        userSubscribingOttRepository.save(subscribingOTT);
                    });
        }

        return userId;
    }

    @Override
    public List<MyReviewDto> getMyReview(Pageable pageable) {
        User user = getUser();
        Long userId = user.getId();

        Slice<Review> reviewList = reviewRepository.findByUserIdOrderByCreatedAt(userId, pageable);

        List<MyReviewDto> reviewDtoList = new ArrayList<>();
        reviewList.stream()
                .forEach(r -> {
                    // 리뷰에 달린 reviewTags 가져오기
                    List<ReviewTag> reviewTags = reviewReviewTagRepository.findReviewTagNameByReviewId(r.getId());

                    reviewDtoList.add(
                            MyReviewDto.builder()
                                    .reviewId(r.getId())
                                    .createdDate(r.getCreatedAt())
                                    .userProfilePhoto(r.getUser().getProfilePhoto())
                                    .userNickName(r.getUser().getNickName())
                                    .programTitle(r.getProgram().getTitle())
                                    .reviewRating(r.getRating())
                                    .reviewTags(reviewTags)
                                    .content(r.getContent())
                                    .likeCnt(r.getLikeCounts())
                                    .build()
                    );
                });

        return reviewDtoList;
    }

    @Override
    public List<MyReviewDto> getLikedReview(Pageable pageable) {
        User user = getUser();
        Long userId = user.getId();

        Slice<Review> reviewList = likedReviewRepository.findReviewByUserId(userId, pageable);

        List<MyReviewDto> reviewDtoList = new ArrayList<>();
        reviewList.stream()
                .forEach(r -> {
                    // 리뷰에 달린 reviewTags 가져오기
                    List<ReviewTag> reviewTags = reviewReviewTagRepository.findReviewTagNameByReviewId(r.getId());

                    reviewDtoList.add(
                            MyReviewDto.builder()
                                    .reviewId(r.getId())
                                    .createdDate(r.getCreatedAt())
                                    .userProfilePhoto(r.getUser().getProfilePhoto())
                                    .userNickName(r.getUser().getNickName())
                                    .programTitle(r.getProgram().getTitle())
                                    .reviewRating(r.getRating())
                                    .reviewTags(reviewTags)
                                    .content(r.getContent())
                                    .likeCnt(r.getLikeCounts())
                                    .build()
                    );
                });

        return reviewDtoList;
    }

    @Override
    public List<MyDiscussionDto> getHostedDiscussion(Pageable pageable) {
        User user = getUser();
        Long userId = user.getId();

        Slice<Community> discussionList = communityRepository.findByUserId(userId, pageable);

        List<MyDiscussionDto> discussionDtoList = new ArrayList<>();
        discussionList.stream()
                .forEach(d -> {
                    int likeCnt = likedCommunityRepository.countByCommunityId(d.getId());
                    int replyCnt = likedReplyRepository.countByCommunityId(d.getId());

                    discussionDtoList.add(
                            MyDiscussionDto.builder()
                                    .discussionId(d.getId())
                                    .createdDate(d.getCreatedAt())
                                    .programTitle(d.getProgram().getTitle())
                                    .discussionTitle(d.getTitle())
                                    .content(d.getContent())
                                    .imgUrl(d.getImageUrl())
                                    .likeCnt(likeCnt)
                                    .replyCnt(replyCnt)
                                    .build()
                    );
                });

        return discussionDtoList;
    }

    @Override
    public List<MyDiscussionDto> getParticipatedDiscussion(Pageable pageable) {
        User user = getUser();
        Long userId = user.getId();

        Slice<Community> discussionList = replyRepository.findAllCommunityByUserId(userId, pageable);

        List<MyDiscussionDto> discussionDtoList = new ArrayList<>();
        discussionList.stream()
                .forEach(d -> {
                    int likeCnt = likedCommunityRepository.countByCommunityId(d.getId());
                    int replyCnt = likedReplyRepository.countByCommunityId(d.getId());

                    discussionDtoList.add(
                            MyDiscussionDto.builder()
                                    .discussionId(d.getId())
                                    .createdDate(d.getCreatedAt())
                                    .programTitle(d.getProgram().getTitle())
                                    .discussionTitle(d.getTitle())
                                    .content(d.getContent())
                                    .imgUrl(d.getImageUrl())
                                    .likeCnt(likeCnt)
                                    .replyCnt(replyCnt)
                                    .build()
                    );
                });

        return discussionDtoList;
    }

    private User getUser() {
        return userRepository.findByEmail(SecurityUtil.getCurrentEmail().get())
                .orElseThrow(() -> new NotFoundException(ErrorCode.ENTITY_NOT_FOUND));
    }
}
