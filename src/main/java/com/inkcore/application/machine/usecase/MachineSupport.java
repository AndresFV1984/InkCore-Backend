package com.inkcore.application.machine.usecase;

import com.inkcore.application.shared.AuthenticatedCompanyResolver;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class MachineSupport {

    private final AuthenticatedCompanyResolver companyResolver;

    public MachineSupport(AuthenticatedCompanyResolver companyResolver) {
        this.companyResolver = companyResolver;
    }

    public String companyId(Authentication authentication) {
        return companyResolver.resolveCompanyId(authentication);
    }

    public String userId(Authentication authentication) {
        return companyResolver.resolveUserId(authentication);
    }

    public void requireSameCompany(String resourceCompanyId, String authenticatedCompanyId) {
        companyResolver.requireSameCompany(resourceCompanyId, authenticatedCompanyId);
    }
}
