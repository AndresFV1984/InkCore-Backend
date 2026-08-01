package com.inkcore.infrastructure.out.persistence.papertype.repository;

import com.inkcore.infrastructure.out.persistence.papertype.entity.PaperTypeCutLayoutEntity;
import com.inkcore.infrastructure.out.persistence.papertype.entity.PaperTypeCutLayoutEntity.PaperTypeCutLayoutId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaPaperTypeCutLayoutRepository
        extends JpaRepository<PaperTypeCutLayoutEntity, PaperTypeCutLayoutId> {

    List<PaperTypeCutLayoutEntity> findAllByIdPaperTypeId(String paperTypeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PaperTypeCutLayoutEntity e WHERE e.id.paperTypeId = :paperTypeId")
    void deleteAllByPaperTypeId(@Param("paperTypeId") String paperTypeId);
}
