package com.inkcore.infrastructure.out.colorconversion;

import com.inkcore.infrastructure.config.ColorConversionProperties;
import com.inkcore.infrastructure.out.cache.InMemoryIccProfileCacheAdapter;
import com.inkcore.infrastructure.out.colorconversion.lcms.LittleCmsColorConverter;

/**
 * Helpers para tests de conversión (LittleCMS).
 */
public final class ColorConversionTestSupport {

    private ColorConversionTestSupport() {
    }

    public static LittleCmsColorConverter littleCms(ColorConversionProperties properties) {
        return new LittleCmsColorConverter(properties);
    }

    public static IccProfileLoader loader(ColorConversionProperties properties) {
        return new IccProfileLoader(new InMemoryIccProfileCacheAdapter(properties));
    }

    public static ImageColorConverterAdapter imageAdapter(ColorConversionProperties properties) {
        IccProfileLoader loader = loader(properties);
        return new ImageColorConverterAdapter(loader, properties, littleCms(properties));
    }

    public static ImageColorConverterAdapter imageAdapter(
            IccProfileLoader loader,
            ColorConversionProperties properties
    ) {
        return new ImageColorConverterAdapter(loader, properties, littleCms(properties));
    }
}
