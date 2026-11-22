package app.ondea.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.ondea.R
import app.ondea.domain.Temperature
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import android.text.format.DateFormat

/** Locale-appropriate pattern for the given skeleton, e.g. "d MMM" in French, "MMM d" in English. */
private fun best(skeleton: String): DateTimeFormatter {
    val locale = Locale.getDefault()
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
}

fun LocalDate.long(): String = toJavaLocalDate().format(best("EEEEdMMMM")).replaceFirstChar { it.titlecase() }
fun LocalDate.short(): String = toJavaLocalDate().format(best("EEEdMMM"))
fun LocalDate.dayMonth(): String = toJavaLocalDate().format(best("dMMM"))
fun LocalDate.medium(): String = toJavaLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
/** Today in the phone's time zone. */
fun today(): LocalDate = java.time.LocalDate.now().toKotlinLocalDate()

/** The date picker works in UTC milliseconds. */
private const val DAY_MILLIS = 86_400_000L

/** Temperature with 2 decimals in the user's locale ("36.45" or "36,45"). */
fun Temperature.format(c: Double, fahrenheit: Boolean, locale: Locale = Locale.getDefault()): String =
    String.format(locale, "%.2f", toDisplay(c, fahrenheit))

fun java.time.YearMonth.title(): String = format(best("MMMMyyyy")).replaceFirstChar { it.titlecase() }

/** Material date picker restricted to dates up to [maxDate]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastDatePickerDialog(
    initial: LocalDate,
    maxDate: LocalDate,
    onPicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val maxMillis = maxDate.toEpochDays() * DAY_MILLIS
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.toEpochDays() * DAY_MILLIS,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= maxMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onPicked(LocalDate.fromEpochDays(it / DAY_MILLIS))
                }
                onDismiss()
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) {
        DatePicker(state = state)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Asks for a backup password; when [confirmTwice] the password must be typed twice and be 8+ chars. */
@Composable
fun PasswordDialog(
    title: String,
    confirmTwice: Boolean,
    onDone: (CharArray) -> Unit,
    onDismiss: () -> Unit,
) {
    var pw by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
    val tooShort = confirmTwice && pw.length < 8
    val mismatch = confirmTwice && pw != pw2
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (confirmTwice) {
                    Text(stringResource(R.string.backup_password_explain))
                    Spacer(Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = pw,
                    onValueChange = { pw = it },
                    label = { Text(stringResource(R.string.password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    supportingText = if (confirmTwice && pw.isNotEmpty() && tooShort) {
                        { Text(stringResource(R.string.password_too_short)) }
                    } else null,
                )
                if (confirmTwice) {
                    OutlinedTextField(
                        value = pw2,
                        onValueChange = { pw2 = it },
                        label = { Text(stringResource(R.string.password_repeat)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = pw2.isNotEmpty() && mismatch,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = pw.isNotEmpty() && !tooShort && !mismatch,
                onClick = { onDone(pw.toCharArray()); onDismiss() },
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
