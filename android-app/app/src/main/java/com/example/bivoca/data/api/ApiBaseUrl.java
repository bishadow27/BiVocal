package com.example.bivoca.data.api;

import android.os.Build;

import com.example.bivoca.BuildConfig;

/** Resolves the local backend address for the current Android runtime. */
public final class ApiBaseUrl {

    private static final String EMULATOR_URL = "http://10.0.2.2:3000/api/v1/";
    private static final String DEVICE_URL = "http://127.0.0.1:3000/api/v1/";

    private ApiBaseUrl() {
    }

    public static String current() {
        String override = BuildConfig.API_BASE_URL_OVERRIDE;
        if (override != null && !override.trim().isEmpty()) {
            return override;
        }
        return resolve(
                Build.FINGERPRINT,
                Build.MODEL,
                Build.MANUFACTURER,
                Build.BRAND,
                Build.DEVICE,
                Build.PRODUCT
        );
    }

    static String resolve(
            String fingerprint,
            String model,
            String manufacturer,
            String brand,
            String device,
            String product
    ) {
        String safeFingerprint = lower(fingerprint);
        String safeModel = lower(model);
        String safeManufacturer = lower(manufacturer);
        String safeBrand = lower(brand);
        String safeDevice = lower(device);
        String safeProduct = lower(product);

        boolean emulator = safeFingerprint.startsWith("generic")
                || safeFingerprint.startsWith("unknown")
                || safeModel.contains("google_sdk")
                || safeModel.contains("emulator")
                || safeModel.contains("android sdk built for")
                || safeManufacturer.contains("genymotion")
                || (safeBrand.startsWith("generic") && safeDevice.startsWith("generic"))
                || safeProduct.contains("sdk_gphone")
                || safeProduct.contains("google_sdk")
                || safeProduct.contains("sdk");

        return emulator ? EMULATOR_URL : DEVICE_URL;
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(java.util.Locale.ROOT);
    }
}
