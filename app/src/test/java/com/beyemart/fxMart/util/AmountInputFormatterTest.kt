package com.beyemart.fxMart.util

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AmountInputFormatterTest {

    @Test
    fun normalizeAmountInput_treatsCommaAsDecimalPoint() {
        assertEquals("1234.5", normalizeAmountInput("1234,5"))
    }

    @Test
    fun normalizeAmountInput_treatsDotAsDecimalPoint() {
        assertEquals("1234.5", normalizeAmountInput("1234.5"))
    }

    @Test
    fun normalizeAmountInput_keepsTrailingDecimalPoint() {
        assertEquals("1234.", normalizeAmountInput("1234."))
    }

    @Test
    fun visualTransformation_addsThousandsSeparatorsUsingLocale() {
        val transformation = AmountInputVisualTransformation(Locale.GERMANY)
        val transformed = transformation.filter(AnnotatedString("1234567.89")).text.text

        assertEquals("1.234.567,89", transformed)
    }
}

