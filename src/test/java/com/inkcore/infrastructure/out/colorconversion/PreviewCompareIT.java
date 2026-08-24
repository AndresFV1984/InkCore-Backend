package com.inkcore.infrastructure.out.colorconversion;

import com.inkcore.domain.colorconversion.model.*;
import com.inkcore.infrastructure.config.ColorConversionProperties;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PreviewCompareIT {
  @Test
  void dumpVividPreview() throws Exception {
    Path rgbPath = Path.of("target/compare-imgs2/rgb.png");
    assumeTrue(Files.exists(rgbPath));
    ColorConversionProperties p = new ColorConversionProperties();
    p.setBrightnessLift(0.16f); p.setVibranceBoost(0.35f);
    p.setSoftProofBrightnessMatch(true); p.setBlackPointCompensation(true);
    var lcms = ColorConversionTestSupport.littleCms(p);
    assumeTrue(lcms.isNativeAvailable());
    var adapter = ColorConversionTestSupport.imageAdapter(p);
    byte[] png = Files.readAllBytes(rgbPath);
    var req = new ConversionRequest(png, "foto.png", "image/png", RenderingIntent.PERCEPTUAL,
      "FOGRA39.icc", null, null, 0.16f, 0.35f, true, QualityPreset.VIVID, true);
    var conv = adapter.convertToCmykTiff(req, "sRGB.icc", "FOGRA39.icc");
    Path out = Path.of("target/compare-imgs2");
    Files.write(out.resolve("latest_preview.jpg"), conv.previewJpeg());
    Files.write(out.resolve("latest.tif"), conv.tiffBytes());
    System.out.println("ratio=" + conv.softProofLumaRatio() + " preview=" + conv.previewJpeg().length);
    BufferedImage preview = ImageIO.read(new ByteArrayInputStream(conv.previewJpeg()));
    BufferedImage rgb = ImageIO.read(new ByteArrayInputStream(png));
    System.out.println("rgbL=" + meanL(rgb) + " prevL=" + meanL(preview) + " rgbC=" + meanC(rgb) + " prevC=" + meanC(preview));
  }
  static double meanL(BufferedImage im){ long s=0;int n=0; for(int y=0;y<im.getHeight();y+=3)for(int x=0;x<im.getWidth();x+=3){int p=im.getRGB(x,y);int r=(p>>16)&255,g=(p>>8)&255,b=p&255;s+=Math.round(0.2126*r+0.7152*g+0.0722*b);n++;} return (double)s/n; }
  static double meanC(BufferedImage im){ long s=0;int n=0; for(int y=0;y<im.getHeight();y+=3)for(int x=0;x<im.getWidth();x+=3){int p=im.getRGB(x,y);int r=(p>>16)&255,g=(p>>8)&255,b=p&255;s+=Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b));n++;} return (double)s/n; }
}