package com.pedroaba.tccmobile.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun AppDateInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "DD / MM / AAAA"
) {
    AppTextInput(
        value = formatDateInput(value),
        onValueChange = { onValueChange(formatDateInput(it)) },
        modifier = modifier,
        placeholder = placeholder,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

private fun formatDateInput(rawValue: String): String {
    val digits = rawValue.filter { it.isDigit() }.take(8)
    val day = digits.take(2)
    val month = digits.drop(2).take(2)
    val year = digits.drop(4).take(4)

    return buildString {
        append(day)
        if (month.isNotEmpty()) {
            append(" / ")
            append(month)
        }
        if (year.isNotEmpty()) {
            append(" / ")
            append(year)
        }
    }
}
