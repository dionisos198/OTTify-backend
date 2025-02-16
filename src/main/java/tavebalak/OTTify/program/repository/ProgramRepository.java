package tavebalak.OTTify.program.repository;

import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import javax.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import tavebalak.OTTify.program.entity.Program;
import tavebalak.OTTify.program.entity.ProgramType;

public interface ProgramRepository extends JpaRepository<Program, Long> {

    List<Program> findTop10ByOrderByAverageRatingDesc();

    boolean existsByTmDbProgramId(Long tmDbId);

    Program findByTmDbProgramId(Long tmDbId);

    boolean existsByTmDbProgramIdAndAndType(Long tmDbId, ProgramType type);

    Optional<Program> findByTmDbProgramIdAndType(Long tmDbId, ProgramType type);

    @Query(value = "SELECT * FROM program WHERE MATCH(title) AGAINST(:keyword) AND type=:type",
            countQuery = "SELECT COUNT(*) FROM program WHERE MATCH(title) AGAINST(:keyword) AND type=:type",
            nativeQuery = true)
    Page<Program> searchByTitle(@Param("keyword") String keyword, @Param("type") String type,
            Pageable pageable);

    @Query(value = "select p from Program p where p.title like :keyword% AND p.type=:type")
    Page<Program> searchByOneTitle(@Param("keyword") String keyword,
            @Param("type") ProgramType type,
            Pageable pageable);

    @Lock(value = LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "2000")})
    Optional<Program> findWithPessimisticWriteById(Long id);

}
