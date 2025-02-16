package tavebalak.OTTify.program.entity;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tavebalak.OTTify.genre.entity.Genre;
import tavebalak.OTTify.genre.entity.ProgramGenre;
import tavebalak.OTTify.review.entity.Review;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(indexes = {@Index(name = "IDX_tm_db_program_id_type", columnList = "tmDbProgramId,type")})
public class Program {

    @Id
    @Column(name = "program_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String posterPath;
    private double averageRating;
    private int reviewCount;
    private Long tmDbProgramId;

    @Enumerated(EnumType.STRING)
    private ProgramType type;

    private String createdYear;

    private String createdDate;
    private String originalCountry;
    private String originalTitle;
    @Column(length = 3000)
    private String overView;
    private String tagLine;
    private String backDropPath;
    private boolean willDeleted;


    @Builder
    public Program(String title, String posterPath, Long tmDbProgramId,
            ProgramType type, String createdYear, String createdDate, String originalCountry,
            String originalTitle,
            String overView, String tagLine, String backDropPath) {

        this.title = title;
        this.posterPath = posterPath;
        this.averageRating = 0;
        this.reviewCount = 0;
        this.tmDbProgramId = tmDbProgramId;
        this.type = type;
        this.createdYear = createdYear;
        this.createdDate = createdDate;
        this.originalCountry = originalCountry;
        this.originalTitle = originalTitle;
        this.overView = overView;
        this.tagLine = tagLine;
        this.backDropPath = backDropPath;
        this.willDeleted = false;
    }


    @Builder(builderMethodName = "testBuilder")
    public Program(Long id, String title, String posterPath) {
        this.id = id;
        this.title = title;
        this.posterPath = posterPath;
    }

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProgramGenre> programGenreList = new ArrayList<>();


    public void addGenre(Genre genre) {
        ProgramGenre programGenre = ProgramGenre.builder().genre(genre).program(this).build();
        programGenreList.add(programGenre);
    }

    // 프로그램과 리뷰 양방향 관계 세팅
    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviewList = new ArrayList<>();

    public void addReview(Review review) {
        this.reviewList.add(review);

        double beforeRatingSum = this.averageRating * this.reviewCount;
        double afterRatingSum = beforeRatingSum + review.getRating();

        this.reviewCount++;
        this.averageRating = afterRatingSum / this.reviewCount;

    }

    //리뷰 삭제시 평점 변화

    public void deleteReview(Review review) {
        double beforeRatingSum = this.averageRating * this.reviewCount;
        double afterRatingSum = beforeRatingSum - review.getRating();

        this.reviewCount--;
        if (this.reviewCount == 0) {
            this.averageRating = 0;
        } else {
            this.averageRating = afterRatingSum / this.reviewCount;
        }

        reviewList.remove(review);
    }

    //리뷰 점수 업데이트시 평점 변화
    public void changeProgramReviewRatingAndRecalculatingAverage(double beforeRating,
            double afterRating) {
        double beforeRatingSum = averageRating * reviewCount;
        double afterRatingSum = beforeRatingSum - beforeRating + afterRating;
        averageRating = afterRatingSum / reviewCount;
    }

}
