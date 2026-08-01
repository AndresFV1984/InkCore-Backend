package com.inkcore.application.colorconversion;

import com.inkcore.domain.colorconversion.model.DestinationIccProfile;
import com.inkcore.domain.colorconversion.model.IccProfileInfo;
import com.inkcore.domain.colorconversion.ports.in.ListIccProfilesUseCase;
import com.inkcore.infrastructure.out.colorconversion.IccProfileLoader;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class ListIccProfilesService implements ListIccProfilesUseCase {

    private final IccProfileLoader iccProfileLoader;

    public ListIccProfilesService(IccProfileLoader iccProfileLoader) {
        this.iccProfileLoader = iccProfileLoader;
    }

    @Override
    public List<IccProfileInfo> listDestinationProfiles() {
        return Arrays.stream(DestinationIccProfile.values())
                .map(profile -> new IccProfileInfo(
                        profile.getFileName(),
                        profile.getTitle(),
                        profile.getUseCase(),
                        profile.getPaperClass(),
                        List.of(profile.getAliases()),
                        iccProfileLoader.isProfileAvailable(profile)
                ))
                .toList();
    }
}
