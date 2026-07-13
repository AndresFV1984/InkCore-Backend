package com.indicore.domain.client.exception;

import com.indicore.domain.shared.exception.DomainException;

public class ClientAlreadyExistsException extends DomainException {

    public ClientAlreadyExistsException(String nit) {
        super("CLIENT_ALREADY_EXISTS", "Ya existe un cliente con NIT/C.C.: " + nit);
    }
}
