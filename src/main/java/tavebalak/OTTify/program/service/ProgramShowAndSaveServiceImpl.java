package tavebalak.OTTify.program.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import tavebalak.OTTify.error.ErrorCode;
import tavebalak.OTTify.error.exception.InternalServerErrorException;
import tavebalak.OTTify.error.exception.NotFoundException;
import tavebalak.OTTify.genre.entity.Genre;
import tavebalak.OTTify.genre.repository.GenreRepository;
import tavebalak.OTTify.genre.repository.ProgramGenreRepository;
import tavebalak.OTTify.program.dto.searchTrending.Response.ProgramSearchInfo;
import tavebalak.OTTify.program.dto.searchTrending.Response.ProgramTrendingDayInfo;
import tavebalak.OTTify.program.dto.searchTrending.Response.ProgramTrendingWeekInfo;
import tavebalak.OTTify.program.dto.searchTrending.Response.SearchMovieResponseDto;
import tavebalak.OTTify.program.dto.searchTrending.Response.SearchResponseDto;
import tavebalak.OTTify.program.dto.searchTrending.Response.SearchTvResponseDto;
import tavebalak.OTTify.program.dto.searchTrending.Response.TrendingResponseDto;
import tavebalak.OTTify.program.dto.searchTrending.openApiRequest.OpenApiSearchTrendingDto;
import tavebalak.OTTify.program.dto.searchTrending.openApiRequest.SearchTrendingOpenApiProgramInfo;
import tavebalak.OTTify.program.entity.Program;
import tavebalak.OTTify.program.entity.ProgramType;
import tavebalak.OTTify.program.repository.ProgramRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProgramShowAndSaveServiceImpl implements ProgramShowAndSaveService {

    private final ProgramRepository programRepository;
    private final WebClient webClient;
    private final GenreRepository genreRepository;
    private final ProgramGenreRepository programGenreRepository;


    // 트렌딩 관련 저장 및 조회
    @Override
    @Cacheable(cacheNames = "programTrending")
    @Transactional
    public TrendingResponseDto showTrending() {

        //actor 타입을 거르기 위함
        List<String> trendingMediaTypeEnterList = new ArrayList<>();
        trendingMediaTypeEnterList.add("movie");
        trendingMediaTypeEnterList.add("tv");

        //open api 를 통해 day trending 데이터 가져오기
        OpenApiSearchTrendingDto openApiSearchTrendingDto = getTrendingProgram("day");

        //trendingDayInfo 만들기
        List<ProgramTrendingDayInfo> programTrendingDayInfos = new ArrayList<>();

        openApiSearchTrendingDto.getResults().stream().forEach(trendingDayProgramInfo -> {
                    if (trendingMediaTypeEnterList.contains(trendingDayProgramInfo.getMedia_type())) {

                        Program program = (trendingDayProgramInfo.getMedia_type().equals("movie"))
                                ? getProgramByTrendingTDDBProgram(trendingDayProgramInfo,
                                ProgramType.Movie)
                                : getProgramByTrendingTDDBProgram(trendingDayProgramInfo,
                                        ProgramType.TV);

                        String firstGenreName = getProgramFirstGenre(trendingDayProgramInfo.getGenre_ids());
                        programTrendingDayInfos.add(
                                new ProgramTrendingDayInfo(program.getId(), program.getTitle(),
                                        program.getCreatedYear(), firstGenreName,
                                        trendingDayProgramInfo.getBackdrop_path(),
                                        program.getAverageRating()));
                    }
                }
        );

        //open api 를 통해 week trending 가져오기
        openApiSearchTrendingDto = getTrendingProgram("week");

        //trendingWeek 만들기
        List<ProgramTrendingWeekInfo> programTrendingWeekInfos = new ArrayList<>();
        openApiSearchTrendingDto.getResults().stream().forEach(trendingWeekProgramInfo -> {

            if (trendingMediaTypeEnterList.contains(trendingWeekProgramInfo.getMedia_type())) {
                Program program = (trendingWeekProgramInfo.getMedia_type().equals("movie"))
                        ? getProgramByTrendingTDDBProgram(trendingWeekProgramInfo,
                        ProgramType.Movie)
                        : getProgramByTrendingTDDBProgram(trendingWeekProgramInfo, ProgramType.TV);

                String firstGenreName = getProgramFirstGenre(
                        trendingWeekProgramInfo.getGenre_ids());

                programTrendingWeekInfos.add(
                        new ProgramTrendingWeekInfo(program.getId(), program.getTitle(),
                                program.getCreatedYear(), firstGenreName,
                                trendingWeekProgramInfo.getPoster_path(),
                                program.getAverageRating()));

            }
        });

        return new TrendingResponseDto(programTrendingDayInfos, programTrendingWeekInfos);
    }

    // 가져온 트렌딩 프로그램 정보 + 부가 정보를 OpenApiSearchTrendingDto 로 매핑합니다

    private OpenApiSearchTrendingDto getTrendingProgram(String choiceArrange) {
        OpenApiSearchTrendingDto openApiSearchTrendingDto = webClient.get()
                .uri("/trending/all/" + choiceArrange + "?language=ko")
                .retrieve()
                .bodyToMono(OpenApiSearchTrendingDto.class)
                .block();
        return openApiSearchTrendingDto;
    }

    // 프로그램이 저장되어 있는 경우 저장된 프로그램을 반환하고, 아닌 경우 저장하고 반환합니다.

    private Program saveProgramAndGetProgram(
            SearchTrendingOpenApiProgramInfo searchTrendingOpenApiProgramInfo,
            ProgramType programType) {
        Program program;

        try {
            program = programRepository.findByTmDbProgramIdAndType(
                    searchTrendingOpenApiProgramInfo.getId(), programType).orElseGet(() -> {
                Program newProgram = apiProgramToProgram(searchTrendingOpenApiProgramInfo,
                        programType);
                programRepository.save(newProgram);
                return newProgram;
            });
        } catch (DataIntegrityViolationException e) {
            throw new InternalServerErrorException(ErrorCode.DATA_SAVING_ERROR);
        }

        return program;

    }

    private Program getProgramByTrendingTDDBProgram(
            SearchTrendingOpenApiProgramInfo searchTrendingOpenApiProgramInfo,
            ProgramType programType) {
        Program program = programRepository.findByTmDbProgramIdAndType(
                searchTrendingOpenApiProgramInfo.getId(), programType).orElseGet(null);

        return program;
    }

    //받아온 api 정보를 이용해 우리가 만든 Program 에 필요한 값을 넣어줍니다.
    private Program apiProgramToProgram(
            SearchTrendingOpenApiProgramInfo searchTrendingOpenApiProgramInfo,
            ProgramType programType) {
        // 공통 관련 빌드
        Program.ProgramBuilder programBuilder = Program.builder()
                .tmDbProgramId(searchTrendingOpenApiProgramInfo.getId())
                .type(programType)
                .posterPath(searchTrendingOpenApiProgramInfo.getPoster_path());

        //영화일 경우
        if (programType == ProgramType.Movie) {
            programBuilder.title(searchTrendingOpenApiProgramInfo.getTitle());
            String createdDate = searchTrendingOpenApiProgramInfo.getRelease_date();
            programBuilder.createdYear(
                    createdDate.length() >= 4 ? createdDate.substring(0, 4) : null);
        }
        //tv 일 경우
        else {
            programBuilder.title(searchTrendingOpenApiProgramInfo.getName());
            String createdDate = searchTrendingOpenApiProgramInfo.getFirst_air_date();
            programBuilder.createdYear(
                    createdDate.length() >= 4 ? createdDate.substring(0, 4) : null);
        }

        Program program = programBuilder.build();

        //연관관계 편의 메서드를 통한 장르 저장
        searchTrendingOpenApiProgramInfo.getGenre_ids().forEach(gi -> {
            Genre genre = genreRepository.findByTmDbGenreId(gi)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.PROGRAM_GENRE_NOT_FOUND));
            program.addGenre(genre);
        });
        return program;
    }


    //프로그램의 대표 장르 , program 의 첫번쨰 넘어오는 장르 이름을 가지고 옵니다. 프로그램에 장르가 명시되지 않은 경우 null 을 넣습니다.
    private String getProgramFirstGenre(List<Long> genreIdList) {
        return genreIdList.stream().findFirst().map(gi -> {
            return genreRepository.findByTmDbGenreId(gi)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.PROGRAM_GENRE_NOT_FOUND))
                    .getName();
        }).orElse(null);
    }


    //첫번째 검색시 기본적으로 검색할 때 TV 갯수, MOVIE 갯수, TV를 검색한 1페이지를 반환합니다.
    @Override
    @Transactional(readOnly = true)
    public SearchResponseDto searchByName(String name) {
        //tvResponse 를 검색한 DTO 를 반환합니다.
        SearchTvResponseDto searchTvResponseDto = searchByTvName(name, 1);

        SearchMovieResponseDto searchMovieResponseDto = searchByMovieName(name, 1);

        return new SearchResponseDto(searchMovieResponseDto.getTotalResults(),
                searchTvResponseDto.getTotalResults(),
                searchTvResponseDto);
    }

    //영화를 page 별로 검색할 수 있습니다.
    @Override
    @Transactional(readOnly = true)
    public SearchMovieResponseDto searchByMovieName(String name, int page) {

        Page<Program> movieProgramList = getPrograms(name, page, ProgramType.Movie);

        List<ProgramSearchInfo> movieSearchInfos = movieProgramList.stream().map(program -> {

            List<String> genreName = programGenreRepository.findByProgram(program.getId())
                    .stream().map(programGenre -> {
                        Genre genre = genreRepository.findById(programGenre.getGenre().getId())
                                .orElseThrow(
                                        () -> new NotFoundException(ErrorCode.ENTITY_NOT_FOUND));

                        return genre.getName();
                    }).collect(Collectors.toList());

            return new ProgramSearchInfo(program.getId(), program.getTitle(),
                    program.getCreatedDate(),
                    genreName.size() == 0 ? null : genreName.get(0)
                    , program.getPosterPath(), program.getOverView());
        }).collect(Collectors.toList());

        return new SearchMovieResponseDto(movieSearchInfos, page,
                movieProgramList.getTotalPages(), (int) movieProgramList.getTotalElements());

    }


    // 1글자는 like 'x%' 키워드로 index 최적화 , 그렇지 않은 경우 match 타입 사용
    private Page<Program> getPrograms(String name, int page, ProgramType programType) {
        if (name != null && name.length() == 1) {
            return programRepository.searchByOneTitle(name,
                    programType, PageRequest.of(
                            page - 1, 10));
        }

        return programRepository.searchByTitle(name,
                programType.name(),
                PageRequest.of(page - 1, 10));

    }

    //TV를 page 별로 검색할 수 있습니다.
    @Override
    public SearchTvResponseDto searchByTvName(String name, int page) {

        Page<Program> tvProgramList = getPrograms(name, page, ProgramType.TV);

        List<ProgramSearchInfo> tvSearchInfos = tvProgramList.stream().map(program -> {

            List<String> genreName = programGenreRepository.findByProgram(program.getId())
                    .stream().map(programGenre -> {
                        Genre genre = genreRepository.findById(programGenre.getGenre().getId())
                                .orElseThrow(
                                        () -> new NotFoundException(ErrorCode.ENTITY_NOT_FOUND));

                        return genre.getName();
                    }).collect(Collectors.toList());

            return new ProgramSearchInfo(program.getId(), program.getTitle(),
                    program.getCreatedDate(), genreName.size() == 0 ? null : genreName.get(0)
                    , program.getPosterPath(), program.getOverView());
        }).collect(Collectors.toList());

        return new SearchTvResponseDto(tvSearchInfos, page,
                tvProgramList.getTotalPages(), (int) tvProgramList.getTotalElements());
    }


}
