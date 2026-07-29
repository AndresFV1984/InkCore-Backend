package com.inkcore.infrastructure.out.persistence.seller.adapter;

import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.domain.seller.model.Seller;
import com.inkcore.domain.seller.ports.out.SellerRepositoryPort;
import com.inkcore.infrastructure.out.persistence.seller.entity.SellerEntity;
import com.inkcore.infrastructure.out.persistence.seller.mapper.SellerPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.seller.repository.JpaSellerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class SellerPersistenceAdapter implements SellerRepositoryPort {

    private final JpaSellerRepository jpaSellerRepository;
    private final SellerPersistenceMapper mapper;

    public SellerPersistenceAdapter(
            JpaSellerRepository jpaSellerRepository,
            SellerPersistenceMapper mapper
    ) {
        this.jpaSellerRepository = jpaSellerRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Seller save(Seller seller) {
        SellerEntity existing = jpaSellerRepository.findById(seller.getSellerId()).orElse(null);
        if (existing == null) {
            SellerEntity entity = mapper.toNewEntity(seller);
            SellerEntity saved = jpaSellerRepository.save(entity);
            return mapper.toDomain(saved);
        }
        mapper.copyScalars(seller, existing);
        SellerEntity saved = jpaSellerRepository.save(existing);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Seller> findById(String sellerId) {
        return jpaSellerRepository.findById(sellerId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Seller> findPage(PageQuery pageQuery) {
        return mapPage(jpaSellerRepository.findAll(pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Seller> findPageByState(boolean state, PageQuery pageQuery) {
        return mapPage(jpaSellerRepository.findAllByState(state, pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Seller> findPageByCompanyId(String companyId, PageQuery pageQuery) {
        return mapPage(jpaSellerRepository.findAllByCompanyId(companyId, pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Seller> findPageByCompanyIdAndState(String companyId, boolean state, PageQuery pageQuery) {
        return mapPage(
                jpaSellerRepository.findAllByCompanyIdAndState(companyId, state, pageable(pageQuery)),
                pageQuery
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndIdentificationIgnoreCase(String companyId, String identification) {
        return companyId != null
                && identification != null
                && jpaSellerRepository.existsByCompanyIdAndIdentificationIgnoreCase(
                companyId.trim(), identification.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndIdentificationIgnoreCaseExcludingSellerId(
            String companyId,
            String identification,
            String sellerId
    ) {
        return companyId != null
                && identification != null
                && sellerId != null
                && jpaSellerRepository.existsByCompanyIdAndIdentificationIgnoreCaseAndSellerIdNot(
                companyId.trim(), identification.trim(), sellerId);
    }

    private static PageRequest pageable(PageQuery pageQuery) {
        return PageRequest.of(pageQuery.page(), pageQuery.size(), Sort.by(Sort.Direction.ASC, "fullName"));
    }

    private PageResult<Seller> mapPage(Page<SellerEntity> page, PageQuery pageQuery) {
        return new PageResult<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                pageQuery.page(),
                pageQuery.size(),
                page.getTotalElements()
        );
    }
}
