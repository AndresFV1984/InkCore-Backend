package com.inkcore.infrastructure.out.persistence.papertype.repository;

import com.inkcore.infrastructure.out.persistence.papertype.entity.PaperTypeSupplierEntity;
import com.inkcore.infrastructure.out.persistence.papertype.entity.PaperTypeSupplierEntity.PaperTypeSupplierId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaPaperTypeSupplierRepository
        extends JpaRepository<PaperTypeSupplierEntity, PaperTypeSupplierId> {

    List<PaperTypeSupplierEntity> findAllByIdPaperTypeId(String paperTypeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PaperTypeSupplierEntity e WHERE e.id.paperTypeId = :paperTypeId")
    void deleteAllByPaperTypeId(@Param("paperTypeId") String paperTypeId);
}
