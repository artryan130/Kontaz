package br.com.kontaz.ui

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView

@Composable
private fun rememberDismissKeyboard(): () -> Unit {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    return remember(context, focusManager, keyboardController, view) {
        {
            keyboardController?.hide()
            view.post {
                val inputMethodManager =
                    context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
                focusManager.clearFocus(force = true)
                view.clearFocus()
            }
        }
    }
}

@Composable
internal fun rememberDismissKeyboardActions(): KeyboardActions {
    val dismiss = rememberDismissKeyboard()
    return remember(dismiss) {
        KeyboardActions(
            onDone = { dismiss() },
            onGo = { dismiss() },
            onSearch = { dismiss() },
            onSend = { dismiss() }
        )
    }
}

@Composable
internal fun Modifier.dismissKeyboardOnEnter(): Modifier {
    val dismiss = rememberDismissKeyboard()
    return onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown &&
            (event.key == Key.Enter || event.key == Key.NumPadEnter)
        ) {
            dismiss()
            true
        } else {
            false
        }
    }
}
