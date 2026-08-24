package com.inkcore.application.inkestimateasset.usecase;

import com.inkcore.domain.objectstorage.model.SignedUrl;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetInkEstimateAssetSignedUrlUseCase {

    private final InkEstimateAssetSupport support;

    public GetInkEstimateAssetSignedUrlUseCase(InkEstimateAssetSupport support) {
        this.support = support;
    }

    @Transactional(readOnly = true)
    public SignedUrlResult execute(String objectKey, String productionOrderId, Authentication authentication) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        SignedUrl signedUrl = support.presignedGetUrl(objectKey, companyId, userId, productionOrderId);
        return new SignedUrlResult(signedUrl.url(), signedUrl.expiresIn().toSeconds());
    }

    public record SignedUrlResult(String url, long expiresInSeconds) {
    }
}
