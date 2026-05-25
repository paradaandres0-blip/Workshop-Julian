package com.pos.lambda;

import java.util.regex.Pattern;

/**
 * Utility class for detecting barcode strings.
 * A barcode is identified by the presence of 7 or more consecutive digits.
 */
public class BarcodeDetector {

    private static final Pattern BARCODE_PATTERN = Pattern.compile("\\d{7,}");

    private BarcodeDetector() {
        // Utility class — no instantiation
    }

    /**
     * Returns {@code true} if {@code q} contains a substring of 7 or more
     * consecutive digits (i.e., it looks like a barcode).
     *
     * @param q the query string to evaluate
     * @return {@code true} if {@code q} matches the barcode pattern
     */
    public static boolean isBarcode(String q) {
        if (q == null) {
            return false;
        }
        return BARCODE_PATTERN.matcher(q).find();
    }
}
