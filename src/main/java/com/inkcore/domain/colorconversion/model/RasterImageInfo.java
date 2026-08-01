package com.inkcore.domain.colorconversion.model;

/**
 * Metadatos de integridad de imagen raster (TIFF/JPG/PNG) o página PDF rasterizada.
 */
public final class RasterImageInfo {

    private final int widthPx;
    private final int heightPx;
    private final Double xResolutionDpi;
    private final Double yResolutionDpi;
    private final int bitsPerSample;
    private final String compression;
    private final int pageCount;

    public RasterImageInfo(
            int widthPx,
            int heightPx,
            Double xResolutionDpi,
            Double yResolutionDpi,
            int bitsPerSample,
            String compression,
            int pageCount
    ) {
        this.widthPx = widthPx;
        this.heightPx = heightPx;
        this.xResolutionDpi = xResolutionDpi;
        this.yResolutionDpi = yResolutionDpi;
        this.bitsPerSample = bitsPerSample;
        this.compression = compression;
        this.pageCount = pageCount;
    }

    public int getWidthPx() {
        return widthPx;
    }

    public int getHeightPx() {
        return heightPx;
    }

    public Double getXResolutionDpi() {
        return xResolutionDpi;
    }

    public Double getYResolutionDpi() {
        return yResolutionDpi;
    }

    public int getBitsPerSample() {
        return bitsPerSample;
    }

    public String getCompression() {
        return compression;
    }

    public int getPageCount() {
        return pageCount;
    }
}
