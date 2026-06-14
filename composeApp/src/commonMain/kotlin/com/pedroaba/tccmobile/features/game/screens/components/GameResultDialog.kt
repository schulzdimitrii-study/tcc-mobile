package com.pedroaba.tccmobile.features.game.screens.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

data class GameResultDialogContent(
    val title: String,
    val message: String,
    val primaryAction: String
)

fun gameResultDialogContentFor(result: String): GameResultDialogContent? = when (result) {
    "caught" -> GameResultDialogContent(
        title = "Você foi capturado",
        message = "A horda alcançou você. A partida foi perdida.",
        primaryAction = "Tentar novamente"
    )
    "escaped" -> GameResultDialogContent(
        title = "Você venceu",
        message = "Você escapou da horda e concluiu a corrida.",
        primaryAction = "Ver resultado"
    )
    else -> null
}

@Composable
fun GameResultDialog(
    content: GameResultDialogContent,
    onDismiss: () -> Unit,
    onPrimaryAction: () -> Unit = onDismiss
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = content.title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = content.message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onPrimaryAction) {
                Text(content.primaryAction)
            }
        }
    )
}
