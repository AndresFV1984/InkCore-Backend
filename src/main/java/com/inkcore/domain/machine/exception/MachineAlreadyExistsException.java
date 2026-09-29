package com.inkcore.domain.machine.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class MachineAlreadyExistsException extends DomainException {

    public MachineAlreadyExistsException(String name) {
        super("CONFLICT", "Ya existe una máquina con el nombre: " + name);
    }
}
