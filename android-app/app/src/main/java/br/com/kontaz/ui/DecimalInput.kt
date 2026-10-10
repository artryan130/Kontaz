package br.com.kontaz.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.math.BigDecimal
import java.math.RoundingMode

internal fun normalizeDecimalInput(value: String): String =
    value.filter(Char::isDigit)
        .trimStart('0')
        .ifEmpty { if (value.any(Char::isDigit)) "0" else "" }

internal fun String.toDecimalOrNull(): Double? =
    normalizeDecimalInput(this)
        .toBigDecimalOrNull()
        ?.movePointLeft(2)
        ?.toDouble()
        ?.takeIf(Double::isFinite)

internal fun decimalInput(value: Double): String =
    BigDecimal.valueOf(value)
        .movePointRight(2)
        .setScale(0, RoundingMode.HALF_UP)
        .toBigInteger()
        .toString()

internal class BrazilianNumberVisualTransformation(
    private val currency: Boolean = true
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = normalizeDecimalInput(text.text)
        if (digits.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val paddedDigits = digits.padStart(3, '0')
        val integerPart = paddedDigits.dropLast(2)
            .trimStart('0')
            .ifEmpty { "0" }
        val fractionPart = paddedDigits.takeLast(2)
        val prefix = if (currency) "R$ " else ""
        val transformed = StringBuilder(prefix)
        val digitPositions = IntArray(paddedDigits.length + 1)
        val sourceLeadingZeros = paddedDigits.length - digits.length

        val integerDigitOffset = paddedDigits.length - 2 - integerPart.length
        integerPart.forEachIndexed { index, digit ->
            if (index > 0 && (integerPart.length - index) % 3 == 0) {
                transformed.append('.')
            }
            val paddedDigitIndex = integerDigitOffset + index
            digitPositions[paddedDigitIndex] = transformed.length
            transformed.append(digit)
            digitPositions[paddedDigitIndex + 1] = transformed.length
        }

        val integerDigitsCount = paddedDigits.length - 2
        transformed.append(',')
        digitPositions[integerDigitsCount] = transformed.length
        for (index in 0 until 2) {
            val paddedIndex = integerDigitsCount + index
            transformed.append(fractionPart[index])
            digitPositions[paddedIndex + 1] = transformed.length
        }

        for (index in 0..paddedDigits.length) {
            if (digitPositions[index] == 0) {
                digitPositions[index] = prefix.length
            }
        }

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val paddedOffset = (sourceLeadingZeros + offset.coerceIn(0, digits.length))
                    .coerceIn(0, paddedDigits.length)
                return digitPositions[paddedOffset].coerceIn(0, transformed.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val transformedOffset = offset.coerceIn(0, transformed.length)
                val paddedOffset = digitPositions.indices.minBy { index ->
                    kotlin.math.abs(digitPositions[index] - transformedOffset)
                }
                return (paddedOffset - sourceLeadingZeros).coerceIn(0, digits.length)
            }
        }

        return TransformedText(AnnotatedString(transformed.toString()), mapping)
    }
}
