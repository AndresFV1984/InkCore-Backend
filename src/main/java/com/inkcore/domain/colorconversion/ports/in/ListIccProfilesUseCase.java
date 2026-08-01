package com.inkcore.domain.colorconversion.ports.in;

import com.inkcore.domain.colorconversion.model.IccProfileInfo;

import java.util.List;

/**
 * Lista perfiles ICC de destino del catálogo y su disponibilidad en el servidor.
 */
public interface ListIccProfilesUseCase {

    List<IccProfileInfo> listDestinationProfiles();
}
