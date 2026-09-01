package com.inkcore.infrastructure.out.persistence.papertype.adapter;

import com.inkcore.domain.papertype.model.PaperType;
import com.inkcore.domain.papertype.model.PaperTypeCutAssignment;
import com.inkcore.domain.papertype.model.PaperTypeSupplierAssignment;
import com.inkcore.domain.papertype.ports.out.PaperTypeRepositoryPort;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.infrastructure.out.persistence.papertype.entity.PaperTypeCutLayoutEntity;
import com.inkcore.infrastructure.out.persistence.papertype.entity.PaperTypeEntity;
import com.inkcore.infrastructure.out.persistence.papertype.entity.PaperTypeSupplierEntity;
import com.inkcore.infrastructure.out.persistence.papertype.mapper.PaperTypePersistenceMapper;
import com.inkcore.infrastructure.out.persistence.papertype.repository.JpaPaperTypeCutLayoutRepository;
import com.inkcore.infrastructure.out.persistence.papertype.repository.JpaPaperTypeRepository;
import com.inkcore.infrastructure.out.persistence.papertype.repository.JpaPaperTypeSupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class PaperTypePersistenceAdapter implements PaperTypeRepositoryPort {

    private final JpaPaperTypeRepository jpaPaperTypeRepository;
    private final JpaPaperTypeCutLayoutRepository jpaPaperTypeCutLayoutRepository;
    private final JpaPaperTypeSupplierRepository jpaPaperTypeSupplierRepository;
    private final PaperTypePersistenceMapper mapper;
    private final Clock clock;

    public PaperTypePersistenceAdapter(
            JpaPaperTypeRepository jpaPaperTypeRepository,
            JpaPaperTypeCutLayoutRepository jpaPaperTypeCutLayoutRepository,
            JpaPaperTypeSupplierRepository jpaPaperTypeSupplierRepository,
            PaperTypePersistenceMapper mapper,
            Clock clock
    ) {
        this.jpaPaperTypeRepository = jpaPaperTypeRepository;
        this.jpaPaperTypeCutLayoutRepository = jpaPaperTypeCutLayoutRepository;
        this.jpaPaperTypeSupplierRepository = jpaPaperTypeSupplierRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PaperType save(PaperType paperType) {
        PaperTypeEntity existing = jpaPaperTypeRepository.findById(paperType.getPaperTypeId()).orElse(null);
        if (existing == null) {
            PaperTypeEntity entity = mapper.toNewEntity(paperType);
            PaperTypeEntity saved = jpaPaperTypeRepository.save(entity);
            replaceCutAssignments(saved.getPaperTypeId(), paperType.getCutAssignments());
            replaceSupplierAssignments(saved.getPaperTypeId(), paperType.getSupplierAssignments());
            return toDomain(saved);
        }
        mapper.copyScalars(paperType, existing);
        PaperTypeEntity saved = jpaPaperTypeRepository.save(existing);
        replaceCutAssignments(saved.getPaperTypeId(), paperType.getCutAssignments());
        replaceSupplierAssignments(saved.getPaperTypeId(), paperType.getSupplierAssignments());
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaperType> findById(String paperTypeId) {
        return jpaPaperTypeRepository.findById(paperTypeId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PaperType> findPage(PageQuery pageQuery) {
        return mapPage(jpaPaperTypeRepository.findAll(pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PaperType> findPageByState(boolean state, PageQuery pageQuery) {
        return mapPage(jpaPaperTypeRepository.findAllByState(state, pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PaperType> findPageByCompanyId(String companyId, PageQuery pageQuery) {
        return mapPage(jpaPaperTypeRepository.findAllByCompanyId(companyId, pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PaperType> findPageByCompanyIdAndState(String companyId, boolean state, PageQuery pageQuery) {
        return mapPage(
                jpaPaperTypeRepository.findAllByCompanyIdAndState(companyId, state, pageable(pageQuery)),
                pageQuery
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndNameIgnoreCase(String companyId, String name) {
        return companyId != null
                && name != null
                && jpaPaperTypeRepository.existsByCompanyIdAndNameIgnoreCase(companyId.trim(), name.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndNameIgnoreCaseExcludingPaperTypeId(
            String companyId,
            String name,
            String paperTypeId
    ) {
        return companyId != null
                && name != null
                && paperTypeId != null
                && jpaPaperTypeRepository.existsByCompanyIdAndNameIgnoreCaseAndPaperTypeIdNot(
                companyId.trim(), name.trim(), paperTypeId);
    }

    private PaperType toDomain(PaperTypeEntity entity) {
        return mapper.toDomain(
                entity,
                jpaPaperTypeCutLayoutRepository.findAllByIdPaperTypeId(entity.getPaperTypeId()),
                jpaPaperTypeSupplierRepository.findAllByIdPaperTypeId(entity.getPaperTypeId())
        );
    }

    private void replaceCutAssignments(String paperTypeId, List<PaperTypeCutAssignment> assignments) {
        jpaPaperTypeCutLayoutRepository.deleteAllByPaperTypeId(paperTypeId);
        if (assignments == null || assignments.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        List<PaperTypeCutLayoutEntity> rows = assignments.stream()
                .map(a -> mapper.toCutAssignmentEntity(paperTypeId, a, now))
                .toList();
        jpaPaperTypeCutLayoutRepository.saveAll(rows);
    }

    private void replaceSupplierAssignments(String paperTypeId, List<PaperTypeSupplierAssignment> assignments) {
        jpaPaperTypeSupplierRepository.deleteAllByPaperTypeId(paperTypeId);
        if (assignments == null || assignments.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        List<PaperTypeSupplierEntity> rows = assignments.stream()
                .map(a -> mapper.toSupplierAssignmentEntity(paperTypeId, a, now))
                .toList();
        jpaPaperTypeSupplierRepository.saveAll(rows);
    }

    private static PageRequest pageable(PageQuery pageQuery) {
        return PageRequest.of(
                pageQuery.page(),
                pageQuery.size(),
                Sort.by(Sort.Order.asc("name"))
        );
    }

    private PageResult<PaperType> mapPage(Page<PaperTypeEntity> page, PageQuery pageQuery) {
        List<PaperType> content = page.getContent().stream()
                .map(this::toDomain)
                .toList();
        return new PageResult<>(
                content,
                pageQuery.page(),
                pageQuery.size(),
                page.getTotalElements()
        );
    }
}
