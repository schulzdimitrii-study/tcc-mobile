package com.pedroaba.tccmobile.features.home.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pedroaba.tccmobile.ui.components.AppBody
import com.pedroaba.tccmobile.ui.components.AppCaption
import com.pedroaba.tccmobile.ui.components.AppScreenScaffold
import com.pedroaba.tccmobile.ui.components.AppSecondary
import com.pedroaba.tccmobile.ui.components.AppSecondarySemiBold
import com.pedroaba.tccmobile.ui.components.FeatureCard
import com.pedroaba.tccmobile.ui.components.ListPanel
import com.pedroaba.tccmobile.ui.components.ListRow
import com.pedroaba.tccmobile.ui.components.MetricCard
import com.pedroaba.tccmobile.ui.components.MetricStrip
import com.pedroaba.tccmobile.ui.components.PanelDivider
import com.pedroaba.tccmobile.ui.components.StatusPill
import com.pedroaba.tccmobile.ui.components.StatusPillTone
import com.pedroaba.tccmobile.ui.components.SurvivorAvatar
import com.pedroaba.tccmobile.ui.components.TopIdentityHeader
import com.pedroaba.tccmobile.ui.components.AppTitle

@Composable
fun HomeScreen(
    userName: String = "Pedro",
    level: Int = 12,
    totalHordes: Int = 37,
    lastRunDistance: String = "6.4 km",
    lastRunDuration: String = "42 min",
    onStartRun: () -> Unit = {},
    onViewProfile: () -> Unit = {},
    onShowWatchModal: () -> Unit = {},
    onTabSelected: (String) -> Unit = {}
) {
    AppScreenScaffold {
        TopIdentityHeader(
            title = "Boa noite, $userName",
            subtitle = "Nível $level · $totalHordes hordas sobrevividas"
        )

        FeatureCard(
            eyebrow = "HORDA PRONTA",
            title = "Inicie uma nova sessão.",
            body = "Crie uma sessão agora. Sem smartwatch, você ainda registra distância, duração e calorias estimadas.",
            primaryAction = "Comecar horda",
            onPrimaryAction = onStartRun,
            secondaryAction = "Ver perfil",
            onSecondaryAction = onViewProfile,
            footer = {
                PanelDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        AppSecondarySemiBold("Última sincronização")
                        AppCaption("Sem smartwatch ativo")
                    }
                    StatusPill(
                        text = "Conectar relógio",
                        tone = StatusPillTone.Alert,
                        modifier = Modifier
                    )
                }
            }
        )

        MetricStrip {
            MetricCard(value = "12", label = "sessões concluídas")
            MetricCard(value = "37", label = "hordas vencidas")
        }

        ListPanel(
            title = "Última sessão",
            actionLabel = "SOBREVIVEU"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    AppTitle(lastRunDistance)
                    AppCaption("Distância")
                }
                Column(horizontalAlignment = Alignment.End) {
                    AppTitle(lastRunDuration)
                    AppCaption("Duração")
                }
            }
            AppBody("Sessão vinculada à horda Distrito Industrial, com distância total, duração e calorias estimadas registradas.")
        }

        ListPanel(
            title = "Ranking atual",
            actionLabel = "Periodo semanal"
        ) {
            ListRow(
                title = "Alvo",
                subtitle = "Ativo",
                trailingTop = "18.4 km"
            )
            PanelDivider()
            RankingPreviewRow(position = "#1", name = "Ravi", score = "23.0k pts")
            RankingPreviewRow(position = "#2", name = "Maya", score = "18.8k pts")
            RankingPreviewRow(position = "#18", name = "Pedro Barbosa", score = "1.2k pts", highlight = true)
        }

        FeatureCard(
            status = "SMARTWATCH OFFLINE",
            statusTone = StatusPillTone.Alert,
            title = "Como iniciar a sessão?",
            body = "Sem smartwatch, a sessão registra tempo, distância e calorias estimadas. Conecte o relógio para liberar BPM, zona cardíaca e ritmo ao vivo.",
            primaryAction = "Conectar relógio",
            onPrimaryAction = onShowWatchModal,
            secondaryAction = "Continuar sem BPM",
            onSecondaryAction = onStartRun
        )
    }
}

@Composable
private fun RankingPreviewRow(
    position: String,
    name: String,
    score: String,
    highlight: Boolean = false
) {
    ListRow(
        title = name,
        subtitle = if (highlight) "você" else "top semanal",
        trailingTop = score,
        leading = {
            SurvivorAvatar(initials = position.replace("#", ""))
        },
        trailing = {
            if (highlight) {
                StatusPill(
                    text = position,
                    tone = StatusPillTone.Alert
                )
            } else {
                AppSecondary(position)
            }
        }
    )
}
