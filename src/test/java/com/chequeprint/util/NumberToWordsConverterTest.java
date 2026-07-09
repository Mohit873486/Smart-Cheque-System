package com.chequeprint.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

public class NumberToWordsConverterTest {

    @Test
    public void testZeroAndOne() {
        assertEquals("Zero Only", NumberToWordsConverter.convert(BigDecimal.ZERO));
        assertEquals("One Only", NumberToWordsConverter.convert(BigDecimal.ONE));
    }

    @Test
    public void testNegativeRejection() {
        assertThrows(IllegalArgumentException.class, () -> {
            NumberToWordsConverter.convert(BigDecimal.valueOf(-1));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            NumberToWordsConverter.convert(-99.99);
        });
    }

    @Test
    public void testAmountsWithPaise() {
        assertEquals("One Hundred and Five Paise Only", NumberToWordsConverter.convert(BigDecimal.valueOf(100.05)));
        assertEquals("Nine Lakh Ninety Nine Thousand Nine Hundred Ninety Nine and Ninety Nine Paise Only", 
            NumberToWordsConverter.convert(BigDecimal.valueOf(999999.99)));
    }

    @Test
    public void testLakhAndCroreBoundaries() {
        assertEquals("Ninety Nine Thousand Nine Hundred Ninety Nine Only", NumberToWordsConverter.convert(BigDecimal.valueOf(99999)));
        assertEquals("One Lakh Only", NumberToWordsConverter.convert(BigDecimal.valueOf(100000)));
        
        assertEquals("Ninety Nine Lakh Ninety Nine Thousand Nine Hundred Ninety Nine Only", NumberToWordsConverter.convert(BigDecimal.valueOf(9999999)));
        assertEquals("One Crore Only", NumberToWordsConverter.convert(BigDecimal.valueOf(10000000)));
    }

    @Test
    public void testFloatingPointTrapRounding() {
        // 0.1 + 0.2 in double is 0.30000000000000004
        double trapValue = 0.1 + 0.2;
        assertEquals("Thirty Paise Only", NumberToWordsConverter.convert(trapValue));
        
        // 100.045 with HALF_UP should round to 100.05
        assertEquals("One Hundred and Five Paise Only", NumberToWordsConverter.convert(BigDecimal.valueOf(100.045)));
    }
}
