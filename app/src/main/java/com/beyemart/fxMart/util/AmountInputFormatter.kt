package com.beyemart.fxMart.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormatSymbols
import java.util.Locale

fun normalizeAmountInput(input: String): String {
    val normalized = input.trim().replace(',', '.')
    if (normalized.isEmpty()) {
        return ""
    }

    val builder = StringBuilder()
    var sawDecimalPoint = false

    normalized.forEach { character ->
        when {
            character.isDigit() -> builder.append(character)
            character == '.' && !sawDecimalPoint -> {
                if (builder.isEmpty()) {
                    builder.append('0')
                }
                builder.append('.')
                sawDecimalPoint = true
            }
        }
    }

    return builder.toString()
}

class AmountInputVisualTransformation(
    private val locale: Locale = Locale.getDefault(),
) : VisualTransformation {

    private val decimalSymbols = DecimalFormatSymbols.getInstance(locale)

    override fun filter(text: AnnotatedString): TransformedText {
        val rawText = text.text
        if (rawText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val decimalIndex = rawText.indexOf('.')
        val hasDecimal = decimalIndex >= 0
        val integerPart = if (hasDecimal) rawText.substring(0, decimalIndex) else rawText
        val fractionPart = if (hasDecimal) rawText.substring(decimalIndex + 1) else ""
        val transformedText = buildLocalizedAmountText(integerPart, fractionPart, hasDecimal)
        val originalToTransformed = IntArray(rawText.length + 1)
        val transformedToOriginal = ArrayList<Int>(transformedText.length + 1)

        transformedToOriginal.add(0)

        var transformedIndex = 0
        var rawCursor = 0
        var integerDigitsProcessed = 0
        val integerLength = integerPart.length

        rawText.forEachIndexed { index, character ->
            originalToTransformed[index] = transformedIndex

            when {
                index < integerLength && character.isDigit() -> {
                    if (integerDigitsProcessed > 0 && (integerLength - integerDigitsProcessed) % 3 == 0) {
                        transformedIndex++
                        transformedToOriginal.add(rawCursor)
                    }

                    transformedIndex++
                    rawCursor++
                    integerDigitsProcessed++
                    transformedToOriginal.add(rawCursor)
                }
                character == '.' -> {
                    transformedIndex++
                    rawCursor++
                    transformedToOriginal.add(rawCursor)
                }
                else -> {
                    transformedIndex++
                    rawCursor++
                    transformedToOriginal.add(rawCursor)
                }
            }
        }

        originalToTransformed[rawText.length] = transformedIndex

        val transformedToOriginalArray = IntArray(transformedToOriginal.size) { transformedToOriginal[it] }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return originalToTransformed[offset.coerceIn(0, rawText.length)]
            }

            override fun transformedToOriginal(offset: Int): Int {
                return transformedToOriginalArray[offset.coerceIn(0, transformedToOriginalArray.lastIndex)]
            }
        }

        return TransformedText(AnnotatedString(transformedText), offsetMapping)
    }

    private fun buildLocalizedAmountText(
        integerPart: String,
        fractionPart: String,
        hasDecimal: Boolean,
    ): String {
        val safeIntegerPart = integerPart.ifEmpty { "0" }
        val groupedIntegerPart = safeIntegerPart.reversed()
            .chunked(3)
            .joinToString(decimalSymbols.groupingSeparator.toString())
            .reversed()

        return buildString {
            append(groupedIntegerPart)
            if (hasDecimal) {
                append(decimalSymbols.decimalSeparator)
                append(fractionPart)
            }
        }
    }
}

