package tavebalak.OTTify.community.service;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tavebalak.OTTify.common.lock.DistributeLock;
import tavebalak.OTTify.common.s3.AWSS3Service;
import tavebalak.OTTify.community.dto.request.CommunitySubjectCreateDTO;
import tavebalak.OTTify.community.dto.request.CommunitySubjectEditDTO;
import tavebalak.OTTify.community.dto.response.CommentListsDTO;
import tavebalak.OTTify.community.dto.response.CommunityAriclesDTO;
import tavebalak.OTTify.community.dto.response.CommunitySubjectEditorDTO;
import tavebalak.OTTify.community.dto.response.CommunitySubjectImageEditorDTO;
import tavebalak.OTTify.community.dto.response.CommunitySubjectsDTO;
import tavebalak.OTTify.community.dto.response.CommunitySubjectsListDTO;
import tavebalak.OTTify.community.dto.response.ReplyListsDTO;
import tavebalak.OTTify.community.entity.Community;
import tavebalak.OTTify.community.entity.Reply;
import tavebalak.OTTify.community.repository.CommunityRepository;
import tavebalak.OTTify.community.repository.ReplyRepository;
import tavebalak.OTTify.error.ErrorCode;
import tavebalak.OTTify.error.exception.BadRequestException;
import tavebalak.OTTify.error.exception.ForbiddenException;
import tavebalak.OTTify.error.exception.NotFoundException;
import tavebalak.OTTify.error.exception.UnauthorizedException;
import tavebalak.OTTify.oauth.jwt.SecurityUtil;
import tavebalak.OTTify.program.entity.Program;
import tavebalak.OTTify.program.repository.ProgramRepository;
import tavebalak.OTTify.user.entity.LikedCommunity;
import tavebalak.OTTify.user.entity.LikedReply;
import tavebalak.OTTify.user.entity.User;
import tavebalak.OTTify.user.repository.LikedCommunityRepository;
import tavebalak.OTTify.user.repository.LikedReplyRepository;
import tavebalak.OTTify.user.repository.UserRepository;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CommunityServiceImpl implements CommunityService {

    private final CommunityRepository communityRepository;
    private final ProgramRepository programRepository;
    private final ReplyRepository replyRepository;
    private final UserRepository userRepository;
    private final LikedCommunityRepository likedCommunityRepository;
    private final LikedReplyRepository likedReplyRepository;
    private final AWSS3Service awss3Service;
    private static final String AWS_S3_DISCUSSION_DIR_NAME = "discussion-images";

    @Override
    public Community saveSubject(CommunitySubjectCreateDTO c, MultipartFile image) {

        String imageUrl = null;
        if (image != null) {
            imageUrl = awss3Service.upload(image, AWS_S3_DISCUSSION_DIR_NAME);
        }

        Program program = isPresent(c);
        Community community = Community.builder()
            .title(c.getSubjectName())
            .content(c.getContent())
            .user(getUser())
            .program(program)
            .imageUrl(imageUrl)
            .build();

        return communityRepository.save(community);

    }

    public Community saveSubjectForTest(CommunitySubjectCreateDTO c) {
        Program program = isPresent(c);

        Community community = Community.builder()
            .title(c.getSubjectName())
            .content(c.getContent())
            .program(program)
            .build();

        return communityRepository.save(community);
    }

    private Program isPresent(CommunitySubjectCreateDTO c) {
        Optional<Program> optionalProgram = programRepository.findById(c.getProgramId());
        return optionalProgram.orElseThrow(
            () -> new NotFoundException(ErrorCode.SAVED_PROGRAM_NOT_FOUND));
    }


    @Override
    public void modifySubject(CommunitySubjectEditDTO c, MultipartFile image) {
        Community community = communityRepository.findById(c.getSubjectId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.COMMUNITY_NOT_FOUND));

        if (!Objects.equals(community.getUser().getId(), getUser().getId())) {
            throw new BadRequestException(ErrorCode.CAN_NOT_UPDATE_OTHER_SUBJECT_REQUEST);
        }

        CommunitySubjectImageEditorDTO communitySubjectEditorDTOBuilder = community.toImageEdior();

        String communityImgUrl = null;
        String imgPath = c.getImageUrl();

        if (!"".equals(imgPath) && imgPath != null) {
            communityImgUrl = changeNewImageUrl(imgPath, communityImgUrl);
        } else {
            communityImgUrl = changeImageUrl(image, community, communityImgUrl);
        }

        CommunitySubjectImageEditorDTO communitySubjectImageEditorDTO = communitySubjectEditorDTOBuilder.changeTitleContentImage(
            c.getSubjectName(), c.getContent(), communityImgUrl);
        community.editImage(communitySubjectImageEditorDTO);
    }

    private String changeNewImageUrl(String imgPath, String communityImgUrl) {
        if (awss3Service.isExist(imgPath)) {
            communityImgUrl = imgPath;
        }
        return communityImgUrl;
    }

    private String changeImageUrl(MultipartFile image, Community community,
        String communityImgUrl) {
        if (image != null && !image.isEmpty()) {
            deleteCommunityS3Image(community);
            return awss3Service.upload(image, AWS_S3_DISCUSSION_DIR_NAME);
        }
        deleteCommunityS3Image(community);
        return communityImgUrl;
    }

    public void deleteCommunityS3Image(Community community) {
        if (community.getImageUrl() != null) {
            awss3Service.delete(community.getImageUrl());
        }
    }

    public Community modifySubjectForTest(CommunitySubjectEditDTO c, User user) {
        Community community = communityRepository.findById(c.getSubjectId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.COMMUNITY_NOT_FOUND));

        if (!Objects.equals(community.getUser().getId(), user.getId())) {
            throw new BadRequestException(ErrorCode.CAN_NOT_UPDATE_OTHER_SUBJECT_REQUEST);
        }

        CommunitySubjectEditorDTO communitySubjectEditorDTOBuilder = community.toEditor();
        CommunitySubjectEditorDTO communitySubjectEditorDTO = communitySubjectEditorDTOBuilder.changeTitleContentProgram(
            c.getSubjectName(), c.getContent());

        community.edit(communitySubjectEditorDTO);

        return community;
    }

    @Override
    public void deleteSubject(Long subjectId) {
        Community community = communityRepository.findById(subjectId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.COMMUNITY_NOT_FOUND));
        if (!Objects.equals(community.getUser().getId(), getUser().getId())) {
            throw new ForbiddenException(ErrorCode.CAN_NOT_DELETE_OTHER_SUBJECT_REQUEST);
        }
        List<Reply> replyList = community.getReplyList();
        for (Reply item : replyList) {
            likedReplyRepository.deleteAllByReply(item);
        }

        communityRepository.delete(community);
        if (community.getImageUrl() != null) {
            awss3Service.delete(community.getImageUrl());
        }
    }

    public void deleteSubjectForTest(Long subjectId, User user) {
        Community community = communityRepository.findById(subjectId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.COMMUNITY_NOT_FOUND));
        if (!Objects.equals(community.getUser().getId(), user.getId())) {
            throw new ForbiddenException(ErrorCode.CAN_NOT_DELETE_OTHER_SUBJECT_REQUEST);
        }
        communityRepository.delete(community);
    }

    @Override
    public CommunitySubjectsListDTO findAllSubjects(Pageable pageable) {
        Slice<Community> communities = communityRepository.findCommunitiesBy(pageable);
        List<CommunitySubjectsDTO> listDTO = communities.stream().map(
            CommunityServiceImpl::buildCommunitySubjectsDTO
        ).collect(Collectors.toList());
        return CommunitySubjectsListDTO.builder().subjectAmount(communities.getNumberOfElements())
            .list(listDTO).hasNext(communities.hasNext()).build();
    }

    private static CommunitySubjectsDTO buildCommunitySubjectsDTO(Community community) {
        return CommunitySubjectsDTO
            .builder()
            .createdAt(community.getCreatedAt())
            .updatedAt(community.getUpdatedAt())
            .title(community.getTitle())
            .nickName(community.getUser().getNickName())
            .programId(community.getProgram().getId())
            .subjectId(community.getId())
            .likeCount(community.getLikeCount())
            .imageUrl(community.getImageUrl())
            .programName(community.getProgram().getTitle())
            .content(community.getContent())
            .commentCount(community.getCommentCount())
            .build();
    }

    @Override
    public CommunitySubjectsListDTO findSingleProgramSubjects(Pageable pageable, Long programId) {
        Slice<Community> communities = communityRepository.findCommunitiesByProgramId(pageable,
            programId);
        List<CommunitySubjectsDTO> list = communities.stream().map(
            community -> buildCommunitySubjectsDTOForProgram(programId, community)
        ).collect(Collectors.toList());

        return CommunitySubjectsListDTO.builder().subjectAmount(communities.getNumberOfElements())
            .list(list).hasNext(communities.hasNext()).build();
    }

    private static CommunitySubjectsDTO buildCommunitySubjectsDTOForProgram(Long programId,
        Community community) {
        return CommunitySubjectsDTO
            .builder()
            .createdAt(community.getCreatedAt())
            .updatedAt(community.getUpdatedAt())
            .title(community.getTitle())
            .nickName(community.getUser().getNickName())
            .subjectId(community.getId())
            .likeCount(community.getLikeCount())
            .programId(programId)
            .imageUrl(community.getImageUrl())
            .programName(community.getProgram().getTitle())
            .content(community.getContent())
            .commentCount(community.getCommentCount())
            .build();
    }

    @Override
    @DistributeLock(key = "T(java.lang.String).format('LikeSubject%d', #subjectId)")
    public void likeSubject(Long subjectId) {
        User savedUser = getUser();
        Community findCommunity = communityRepository.findById(subjectId).orElseThrow(() ->
            new NotFoundException(ErrorCode.COMMUNITY_NOT_FOUND));

        likedCommunityRepository.findByCommunityIdAndUserId(findCommunity.getId(),
            savedUser.getId()).ifPresentOrElse(
            entity -> cancelLikeCommunity(entity, findCommunity),
            () -> postLikeCommunity(savedUser, findCommunity)
        );
    }

    private void postLikeCommunity(User savedUser,
        Community findCommunity) {
        saveLikeCommunity(savedUser, findCommunity);
        increaseLikeCountForCommunity(findCommunity);
    }

    private void saveLikeCommunity(User savedUser, Community findCommunity) {
        likedCommunityRepository.save(
            builderLikeCommunity(savedUser, findCommunity)
        );
    }

    private void increaseLikeCountForCommunity(Community findCommunity) {
        findCommunity.increaseLikeCount();
    }

    private static LikedCommunity builderLikeCommunity(User savedUser, Community findCommunity) {
        return LikedCommunity.builder()
            .user(savedUser)
            .community(findCommunity)
            .build();
    }

    private void cancelLikeCommunity(
        LikedCommunity entity, Community findCommunity) {
        deleteLikeCommunity(entity);
        decreaseLikeCountForCommunity(findCommunity);
    }

    private static void decreaseLikeCountForCommunity(Community findCommunity) {
        findCommunity.decreaseLikeCount();
    }

    private void deleteLikeCommunity(LikedCommunity entity) {
        likedCommunityRepository.delete(entity);
    }

    @DistributeLock(key = "T(java.lang.String).format('LikeSub%d', #id)")
    public void likeSubjectForTest(User user, Community community, Long id) {
        Community findCommunity = communityRepository.findById(id).orElseThrow(() ->
            new NotFoundException(ErrorCode.COMMUNITY_NOT_FOUND));
        likedCommunityRepository.findByCommunityIdAndUserId(community.getId(),
            user.getId()).ifPresentOrElse(
            entity -> cancelLikeCommunity(entity, findCommunity),
            () -> postLikeCommunity(user, community)
        );
    }


    @Override
    public void likeComment(Long subjectId, Long commentId) {
        User savedUser = getUser();
        Community community = communityRepository.findById(subjectId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.COMMUNITY_NOT_FOUND));
        Reply findReply = replyRepository.findById(commentId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.REPLY_NOT_FOUND));

        likedReplyRepository.findByUserIdAndReplyIdAndCommunityId(savedUser.getId(),
                findReply.getId(), community.getId())
            .ifPresentOrElse(entity -> cancelLikeReply(entity, findReply),
                () -> postLikeReply(savedUser, community, findReply)
            );

    }

    private void postLikeReply(User savedUser, Community community,
        Reply findReply) {
        saveLikeReply(savedUser, community, findReply);
        increaseLikeCountForReply(findReply);
    }

    private static void increaseLikeCountForReply(Reply findReply) {
        findReply.increaseLikeCount();
    }

    private void saveLikeReply(User savedUser, Community community, Reply findReply) {
        likedReplyRepository.save(builderLikedReply(savedUser, community, findReply));
    }

    private void cancelLikeReply(LikedReply entity, Reply findReply) {
        deleteLikeReply(entity);
        decreaseLikeCountForReply(findReply);
    }

    private static void decreaseLikeCountForReply(Reply findReply) {
        findReply.decreaseLikeCount();
    }

    private void deleteLikeReply(LikedReply entity) {
        likedReplyRepository.delete(entity);
    }

    private static LikedReply builderLikedReply(User savedUser, Community community,
        Reply findReply) {
        return LikedReply.builder()
            .user(savedUser)
            .community(community)
            .reply(findReply)
            .build();
    }

    @Override
    public CommunityAriclesDTO getArticleOfASubject(Long subjectId) {
        Community community = communityRepository.findCommunityBySubjectId(subjectId).orElseThrow(
            () -> new NotFoundException(ErrorCode.COMMUNITY_NOT_FOUND)
        );

        List<Reply> parentList = getParentCommentList(community);
        List<CommentListsDTO> commentListsDTOList = new ArrayList<>();
        for (Reply parent : parentList) {
            commentListsDTOListAddItem(parent, community, commentListsDTOList);
        }

        return CommunityAriclesDTO.builder()
            .title(community.getTitle())
            .writer(community.getUser().getNickName())
            .content(community.getContent())
            .createdAt(community.getCreatedAt())
            .updatedAt(community.getUpdatedAt())
            .commentAmount(community.getCommentCount())
            .commentListsDTOList(commentListsDTOList)
            .userId(getUser().getId())
            .likeCount(community.getLikeCount())
            .subjectId(community.getId())
            .programTitle(community.getProgram().getTitle())
            .imageUrl(community.getImageUrl())
            .build();
    }

    private List<Reply> getParentCommentList(Community community) {
        return community.getReplyList().stream()
            .filter(reply -> reply.getParent() == null)
            .sorted(Comparator.comparing(Reply::getCreatedAt))
            .collect(Collectors.toList());
    }

    private static void commentListsDTOListAddItem(Reply parent, Community community,
        List<CommentListsDTO> commentListsDTOList) {
        List<Reply> childList = community.getReplyList().stream()
            .filter(reply -> reply.getParent() == parent)
            .sorted(Comparator.comparing(Reply::getCreatedAt))
            .collect(Collectors.toList());

        List<ReplyListsDTO> collect = childList.stream()
            .map(CommunityServiceImpl::builderReplyListsDTO).collect(Collectors.toList());

        CommentListsDTO build = CommentListsDTO.builder()
            .content(parent.getContent())
            .nickName(parent.getUser().getNickName())
            .profileUrl(parent.getUser().getProfilePhoto())
            .createdAt(parent.getCreatedAt())
            .userId(parent.getUser().getId())
            .replyListsDTOList(collect)
            .likeCount(parent.getLikeCount())
            .commentId(parent.getId())
            .build();
        commentListsDTOList.add(build);
    }

    private static ReplyListsDTO builderReplyListsDTO(Reply listone) {
        return ReplyListsDTO.builder()
            .recommentId(listone.getId())
            .content(listone.getContent())
            .nickName(listone.getUser().getNickName())
            .userId(listone.getUser().getId())
            .createdAt(listone.getCreatedAt())
            .likeCount(listone.getLikeCount())
            .build();
    }

    public User getUser() {
        return userRepository.findByEmail(SecurityUtil.getCurrentEmail().get())
            .orElseThrow(() -> new UnauthorizedException(ErrorCode.UNAUTHORIZED));
    }

}
