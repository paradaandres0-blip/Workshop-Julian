package com.pos.lambda;

import java.util.regex.Pattern;

/**
 * Detects barcode strings.
 * A barcode is identified by 6 or more consecutive digits.
 */
public class BarcodeDetector {

    private static final Pattern BARCODE_PATTERN = Pattern.compile("\\d{6,}");

    private BarcodeDetector() {}

    public static boolean isBarcode(String q) {
        if (q == null) return false;
        return BARCODE_PATTERN.matcher(q).find();
    }
}
