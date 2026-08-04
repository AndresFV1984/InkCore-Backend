Perfiles ICC para conversion RGB a CMYK (offline).
Fuente principal: ICC Profile Registry (color.org) + FOGRA39 de desarrollo.

Origen (RGB):
- sRGB.icc: JDK CS_sRGB (si la imagen no trae ICC embebido).

Destino CMYK instalados (parametro iccProfile / GET color-conversions/list):
- FOGRA39.icc          Offset estucado EU / ISO Coated v2 (alias: ISOcoated_v2_eci.icc)
- FOGRA51.icc          Offset estucado EU v3 / PSO Coated v3 (alias: PSOcoated_v3.icc)
- FOGRA52.icc          Offset no estucado EU v3 / PSO Uncoated v3
- GRACoL2013.icc       Offset estucado EE.UU. (GRACoL 2013 / CRPC6)
- SWOP2006_Coated3v2.icc  Web offset EE.UU. (alias: USWebCoatedSWOP.icc)
- JapanColor2011Coated.icc Offset estucado Japon

No incluido (licencia / no en registry publico usado):
- FOGRA47.icc (no estucado clasico). Usar FOGRA52 o copiar PSO_Uncoated_ISO12647_eci.icc aqui.

Los alias se resuelven en codigo al archivo canonico instalado.
Produccion: elegir el perfil que indique la imprenta/CTP (COLOR_DEST_ICC).

## LittleCMS (lcms2) — CMM nativo

La conversion RGB↔CMYK (CTP, soft-proof y estimacion de tintas) usa LittleCMS 2 via JNA.

### Sin instalacion manual
Los binarios van en el proyecto y en el JAR. JNA los extrae en runtime:

  src/main/resources/win32-x86-64/lcms2.dll
  src/main/resources/linux-x86-64/liblcms2.so

No hace falta instalar lcms2 en Windows, Linux ni Docker, ni configurar PATH.
Licencia MIT: LICENSE-lcms2.txt junto a cada binario.

### Override (solo diagnostico)
- `inkcore.color-conversion.lcms-library-path` / `LCMS_LIBRARY_PATH` = ruta a otra DLL/SO.
  Por defecto vacio: se usa el recurso empaquetado.

### Config
- `inkcore.color-conversion.black-point-compensation` / `COLOR_BLACK_POINT_COMPENSATION` (default true)
- `inkcore.color-conversion.lcms-library-path` / `LCMS_LIBRARY_PATH` (opcional)

Si lcms2 no carga: CTP y estimacion de tinta fallan con mensaje claro (sin fallback naive).

