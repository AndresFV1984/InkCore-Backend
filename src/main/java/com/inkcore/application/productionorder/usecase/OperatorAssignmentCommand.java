package com.inkcore.application.productionorder.usecase;

/**
 * Ítem de responsable por etapa en comandos de actualización de OP.
 */
public record OperatorAssignmentCommand(
        String stage,
        String userId,
        String roleCode
) {
}
