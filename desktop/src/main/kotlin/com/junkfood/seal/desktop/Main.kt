package com.junkfood.seal.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.junkfood.seal.shared.SharedInfo

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Seal Desktop") {
        MaterialTheme {
            Surface {
                Text(SharedInfo.greeting())
            }
        }
    }
}
