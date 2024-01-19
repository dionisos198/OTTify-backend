package tavebalak.OTTify.community.service;

import org.springframework.data.domain.Pageable;
import tavebalak.OTTify.community.dto.request.CommunitySubjectImageCreateDTO;
import tavebalak.OTTify.community.dto.request.CommunitySubjectImageEditDTO;
import tavebalak.OTTify.community.dto.response.CommunityAriclesDTO;
import tavebalak.OTTify.community.dto.response.CommunitySubjectDTO;
import tavebalak.OTTify.community.dto.response.CommunitySubjectsListDTO;
import tavebalak.OTTify.community.entity.Community;

public interface CommunityService {

    Community saveSubject(CommunitySubjectImageCreateDTO c);

    void modifySubject(CommunitySubjectImageEditDTO c);

    void deleteSubject(Long subjectId);

    CommunitySubjectsListDTO findAllSubjects(Pageable pageable);

    CommunityAriclesDTO getArticleOfASubject(Long subjectId);

    CommunitySubjectDTO getSubject(Long subjectId);

    CommunitySubjectsListDTO findSingleProgramSubjects(Pageable pageable, Long programId);

    boolean likeSubject(Long subjectId);

    boolean likeComment(Long subjectId, Long commentId);
}
