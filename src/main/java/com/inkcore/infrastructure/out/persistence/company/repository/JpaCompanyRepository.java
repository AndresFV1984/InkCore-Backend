package com.inkcore.infrastructure.out.persistence.company.repository;

import com.inkcore.infrastructure.out.persistence.company.entity.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaCompanyRepository extends JpaRepository<CompanyEntity, String> {

    List<CompanyEntity> findAllByOrderByNameAsc();
}
