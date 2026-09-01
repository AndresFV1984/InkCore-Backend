package com.inkcore.application.company.usecase;

import com.inkcore.infrastructure.out.persistence.company.entity.CompanyEntity;
import com.inkcore.infrastructure.out.persistence.company.repository.JpaCompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListCompanyIdsNamesUseCase {

    private final JpaCompanyRepository companyRepository;

    public ListCompanyIdsNamesUseCase(JpaCompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public List<CompanyIdName> execute() {
        return companyRepository.findAllByOrderByNameAsc().stream()
                .map(this::toIdName)
                .toList();
    }

    private CompanyIdName toIdName(CompanyEntity entity) {
        return new CompanyIdName(entity.getCompanyId(), entity.getName());
    }

    public record CompanyIdName(String companyId, String name) {
    }
}
