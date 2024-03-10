package tavebalak.OTTify.program.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tavebalak.OTTify.error.ErrorCode;
import tavebalak.OTTify.error.exception.NotFoundException;
import tavebalak.OTTify.error.exception.UnauthorizedException;
import tavebalak.OTTify.genre.entity.Genre;
import tavebalak.OTTify.genre.entity.ProgramGenre;
import tavebalak.OTTify.genre.entity.UserGenre;
import tavebalak.OTTify.genre.repository.GenreRepository;
import tavebalak.OTTify.genre.repository.ProgramGenreRepository;
import tavebalak.OTTify.genre.repository.UserGenreRepository;
import tavebalak.OTTify.oauth.jwt.SecurityUtil;
import tavebalak.OTTify.program.dto.response.RecommendProgramsDTO;
import tavebalak.OTTify.program.dto.response.ServiceListsDTO;
import tavebalak.OTTify.program.entity.Program;
import tavebalak.OTTify.program.repository.ProgramRepository;
import tavebalak.OTTify.user.entity.LikedProgram;
import tavebalak.OTTify.user.entity.User;
import tavebalak.OTTify.user.repository.LikedProgramRepository;
import tavebalak.OTTify.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class ProgramServiceImpl implements ProgramService {

    private final UserGenreRepository userGenreRepository;
    private final LikedProgramRepository likedProgramRepository;
    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final GenreRepository genreRepository;
    private final ProgramGenreRepository programGenreRepository;


    public RecommendProgramsDTO getRecommendProgram(int count) {
        User savedUser = getUser();
        Set<Program> recommendPrograms = new HashSet<>();

        Optional<UserGenre> byGenreIdAndIsFirst = userGenreRepository.findByUserIdAndIsFirst(
            savedUser.getId(), true);

        if (byGenreIdAndIsFirst.isPresent()) {
            addFirstGenreProgram(byGenreIdAndIsFirst, recommendPrograms);
        }

        List<UserGenre> byUserIdAndIsFirst = userGenreRepository.findAllByUserIdAndIsFirst(
            savedUser.getId(), false);

        if (!byUserIdAndIsFirst.isEmpty()) {
            byUserIdAndIsFirst.forEach(addItemToRecommendProgramsBySecondGenre(recommendPrograms));
        }

        AtomicInteger likedProgramsSize = new AtomicInteger(1);
        likedProgramRepository.findByUserId(savedUser.getId()).forEach(
            likedProgram -> addLikeListProgram(likedProgram, likedProgramsSize, recommendPrograms)
        );
        
        List<Program> programList = programRepository.findTop10ByOrderByAverageRatingDesc();
        if (programList.isEmpty()) {
            return builderRecommendProgramsDTO();
        }
        int idx = new Random().nextInt(programList.size());
        Optional<Program> program = programRepository.findById(programList.get(idx).getId());
        program.ifPresent(recommendPrograms::add);

        int size = recommendPrograms.size();
        for (; size < 6; size++) {
            Long index = getRandomIndex();
            Optional<Program> findProgram = programRepository.findById(index);
            findProgram.ifPresent(recommendPrograms::add);
        }

        return RecommendProgramsDTO.builder()
            .recommentAmount(count)
            .serviceListsDTOList(builderListOfServiceListsDTO(recommendPrograms))
            .build();
    }

    private Consumer<UserGenre> addItemToRecommendProgramsBySecondGenre(
        Set<Program> recommendPrograms) {
        return userGenre -> addSecondGenreProgram(userGenre, recommendPrograms);
    }

    private List<ServiceListsDTO> builderListOfServiceListsDTO(Set<Program> recommendPrograms) {
        return recommendPrograms.stream().map(this::builderServiceListsDTO)
            .collect(Collectors.toList());
    }

    private ServiceListsDTO builderServiceListsDTO(Program pg) {
        return ServiceListsDTO.builder()
            .programId(pg.getId())
            .title(pg.getTitle())
            .posterPath(pg.getPosterPath())
            .createdYear(pg.getCreatedYear())
            .genreNameList(findGenreList(pg))
            .build();
    }

    private Long getRandomIndex() {
        return
            1L + ((long) (new Random().nextDouble() * (programRepository.findAll().size() - 1L)));
    }

    private static RecommendProgramsDTO builderRecommendProgramsDTO() {
        return RecommendProgramsDTO.builder()
            .recommentAmount(0)
            .serviceListsDTOList(List.of())
            .build();
    }

    private void addLikeListProgram(LikedProgram likedProgram, AtomicInteger likedProgramsSize,
        Set<Program> recommendPrograms) {
        if (likedProgramsSize.get() == 0) {
            return;
        }
        Optional<Program> program = programRepository.findById(
            likedProgram.getProgram().getId());
        program.ifPresent(recommendPrograms::add);
        likedProgramsSize.getAndDecrement();
    }

    private void addSecondGenreProgram(UserGenre userGenre, Set<Program> recommendPrograms) {
        Genre userSecondGenre = genreRepository.findById(userGenre.getGenre().getId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.GENRE_NOT_FOUND));

        List<ProgramGenre> programGenreList = programGenreRepository.findByGenreId(
            userSecondGenre.getId());

        int idx = new Random().nextInt(programGenreList.size());
        Optional<Program> program = programRepository.findById(
            programGenreList.get(idx).getProgram().getId());
        program.ifPresent(recommendPrograms::add);
    }

    private void addFirstGenreProgram(Optional<UserGenre> byGenreIdAndIsFirst,
        Set<Program> recommendPrograms) {
        Genre userFirstGenre = genreRepository.findById(getGenreId(byGenreIdAndIsFirst))
            .orElseThrow(() -> new NotFoundException(ErrorCode.GENRE_NOT_FOUND));

        List<ProgramGenre> programGenreList = programGenreRepository.findByGenreId(
            userFirstGenre.getId());

        for (int i = 1; i < 4; i++) {
            int idx = new Random().nextInt(programGenreList.size());
            Optional<Program> program = getRandomProgram(programGenreList, idx);
            program.ifPresent(recommendPrograms::add);
        }
    }

    private Optional<Program> getRandomProgram(List<ProgramGenre> programGenreList, int idx) {
        return programRepository.findById(
            programGenreList.get(idx).getId());
    }

    private static Long getGenreId(Optional<UserGenre> byGenreIdAndIsFirst) {
        return byGenreIdAndIsFirst.orElseThrow(
            () -> new NotFoundException(ErrorCode.GENRE_NOT_FOUND)).getId();
    }

    private List<String> findGenreList(Program pg) {
        return programGenreRepository.findByProgram(pg.getId())
            .stream()
            .map(this::getProgramName)
            .collect(Collectors.toList());
    }

    private String getProgramName(ProgramGenre programGenre) {
        return genreRepository.findById(programGenre.getGenre().getId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.GENRE_NOT_FOUND))
            .getName();
    }

    private User getUser() {
        return userRepository.findByEmail(SecurityUtil.getCurrentEmail().get())
            .orElseThrow(() -> new UnauthorizedException(ErrorCode.UNAUTHORIZED)
            );
    }
}
