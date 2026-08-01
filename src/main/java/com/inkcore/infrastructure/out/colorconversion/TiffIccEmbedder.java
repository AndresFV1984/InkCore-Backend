package com.inkcore.infrastructure.out.colorconversion;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Embebe el tag TIFF 34675 (ICC Profile) en binario, sin pasar por el árbol
 * de metadatos ImageIO (que serializa 200KB como texto y puede colgarse).
 */
final class TiffIccEmbedder {

    private static final int TAG_ICC = 34675;
    private static final int TYPE_UNDEFINED = 7;

    private TiffIccEmbedder() {
    }

    static byte[] embed(byte[] tiffBytes, byte[] iccProfile) {
        if (tiffBytes == null || tiffBytes.length < 8 || iccProfile == null || iccProfile.length == 0) {
            return tiffBytes;
        }
        boolean little = tiffBytes[0] == 'I' && tiffBytes[1] == 'I';
        boolean big = tiffBytes[0] == 'M' && tiffBytes[1] == 'M';
        if (!little && !big) {
            return tiffBytes;
        }
        ByteOrder order = little ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        ByteBuffer header = ByteBuffer.wrap(tiffBytes, 0, 8).order(order);
        int magic = header.getShort(2) & 0xffff;
        if (magic != 42) {
            return tiffBytes; // BigTIFF u otro
        }
        int ifdOffset = header.getInt(4);
        if (ifdOffset < 8 || ifdOffset + 2 > tiffBytes.length) {
            return tiffBytes;
        }

        ByteBuffer ifdBuf = ByteBuffer.wrap(tiffBytes).order(order);
        int entryCount = ifdBuf.getShort(ifdOffset) & 0xffff;
        int entriesStart = ifdOffset + 2;
        int entriesBytes = entryCount * 12;
        if (entriesStart + entriesBytes + 4 > tiffBytes.length) {
            return tiffBytes;
        }

        List<byte[]> entries = new ArrayList<>(entryCount + 1);
        for (int i = 0; i < entryCount; i++) {
            int off = entriesStart + i * 12;
            int tag = ifdBuf.getShort(off) & 0xffff;
            if (tag == TAG_ICC) {
                continue; // reemplazar
            }
            byte[] entry = new byte[12];
            System.arraycopy(tiffBytes, off, entry, 0, 12);
            entries.add(entry);
        }

        int nextIfdOffset = ifdBuf.getInt(entriesStart + entriesBytes);
        // Nuevo archivo: header + datos previos a IFD (sin IFD) es complejo si IFD no está al final.
        // Estrategia segura: copiar TIFF completo, anexar ICC al final, reescribir IFD0 al final del archivo.
        int iccOffset = tiffBytes.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream(tiffBytes.length + iccProfile.length + 64 + entries.size() * 12);
        out.writeBytes(tiffBytes);
        out.writeBytes(iccProfile);

        byte[] iccEntry = new byte[12];
        ByteBuffer eb = ByteBuffer.wrap(iccEntry).order(order);
        eb.putShort((short) TAG_ICC);
        eb.putShort((short) TYPE_UNDEFINED);
        eb.putInt(iccProfile.length);
        eb.putInt(iccOffset);
        entries.add(iccEntry);
        entries.sort((a, b) -> {
            int ta = ByteBuffer.wrap(a).order(order).getShort(0) & 0xffff;
            int tb = ByteBuffer.wrap(b).order(order).getShort(0) & 0xffff;
            return Integer.compare(ta, tb);
        });

        int newIfdOffset = out.size();
        ByteArrayOutputStream ifdOut = new ByteArrayOutputStream(2 + entries.size() * 12 + 4);
        ByteBuffer countBuf = ByteBuffer.allocate(2).order(order);
        countBuf.putShort((short) entries.size());
        ifdOut.writeBytes(countBuf.array());
        for (byte[] e : entries) {
            ifdOut.writeBytes(e);
        }
        ByteBuffer nextBuf = ByteBuffer.allocate(4).order(order);
        nextBuf.putInt(nextIfdOffset);
        ifdOut.writeBytes(nextBuf.array());
        out.writeBytes(ifdOut.toByteArray());

        byte[] result = out.toByteArray();
        ByteBuffer resultHeader = ByteBuffer.wrap(result, 0, 8).order(order);
        resultHeader.putInt(4, newIfdOffset);
        return result;
    }
}
