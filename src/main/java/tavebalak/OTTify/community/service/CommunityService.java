package tavebalak.OTTify.community.service;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import tavebalak.OTTify.community.dto.request.CommunitySubjectCreateDTO;
import tavebalak.OTTify.community.dto.request.CommunitySubjectEditDTO;
import tavebalak.OTTify.community.dto.response.CommunityAriclesDTO;
import tavebalak.OTTify.community.dto.response.CommunitySubjectsListDTO;
import tavebalak.OTTify.community.entity.Community;

public interface CommunityService {

    Community saveSubject(CommunitySubjectCreateDTO c, MultipartFile image);

    void modifySubject(CommunitySubjectEditDTO c, MultipartFile image);

    void deleteSubject(Long subjectId);

    CommunitySubjectsListDTO findAllSubjects(Pageable pageable);

    CommunityAriclesDTO getArticleOfASubject(Long subjectId);

    CommunitySubjectsListDTO findSingleProgramSubjects(Pageable pageable, Long programId);

    void likeSubject(Long subjectId);

    void likeComment(Long subjectId, Long commentId);
}
