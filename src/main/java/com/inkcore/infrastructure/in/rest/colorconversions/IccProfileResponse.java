package com.inkcore.infrastructure.in.rest.colorconversions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "IccProfileResponse", description = "Perfil ICC de destino CMYK del catálogo CTP")
public record IccProfileResponse(
        @Schema(description = "Nombre de archivo canónico", example = "FOGRA39.icc")
        String fileName,

        @Schema(description = "Título legible", example = "Offset estucado Europa (ISO Coated v2 / FOGRA39)")
        String title,

        @Schema(description = "Cuándo usarlo", example = "Papel couché/estucado offset. Estándar CTP europeo y LATAM más usado.")
        String useCase,

        @Schema(
                description = "Clase de papel/prensa",
                example = "COATED_OFFSET",
                allowableValues = {
                        "COATED_OFFSET",
                        "UNCOATED_OFFSET",
                        "COATED_OFFSET_US",
                        "WEB_OFFSET_US",
                        "COATED_OFFSET_JP"
                }
        )
        String paperClass,

        @Schema(description = "Alias aceptados en iccProfile (mismo perfil)")
        List<String> aliases,

        @Schema(description = "true si el .icc está instalado en el servidor (classpath:/color-profiles/)")
        boolean available
) {
}
