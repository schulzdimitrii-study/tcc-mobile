package com.pedroaba.tccmobile.features.home.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.pedroaba.tccmobile.ui.components.AppScreenScaffold
import com.pedroaba.tccmobile.ui.components.AppSecondary
import com.pedroaba.tccmobile.ui.components.FeatureCard
import com.pedroaba.tccmobile.ui.components.MetricCard
import com.pedroaba.tccmobile.ui.components.MetricStrip
import com.pedroaba.tccmobile.ui.components.PanelDivider
import com.pedroaba.tccmobile.ui.components.StatusPill
import com.pedroaba.tccmobile.ui.components.StatusPillTone
import com.pedroaba.tccmobile.ui.components.TopIdentityHeader
import com.pedroaba.tccmobile.ui.components.ListPanel
import com.pedroaba.tccmobile.ui.components.ListRow

@Composable
fun HomeScreenWithModal(
    onDismissModal: () -> Unit = {},
    onTabSelected: (String) -> Unit = {}
) {
    AppScreenScaffold {
        TopIdentityHeader(
            title = "Boa noite, Pedro",
            subtitle = "Nível 12 · 37 hordas sobrevividas"
        )

        MetricStrip {
            MetricCard(value = "12", label = "sessões concluídas")
            MetricCard(value = "37", label = "hordas vencidas")
        }

        FeatureCard(
            status = "SMARTWATCH OFFLINE",
            statusTone = StatusPillTone.Alert,
            title = "Como iniciar a sessão?",
            body = "Sem smartwatch, a sessão registra tempo, distância e calorias estimadas. Conecte o relógio para liberar BPM, zona cardíaca e ritmo ao vivo.",
            primaryAction = "Conectar relógio",
            onPrimaryAction = onDismissModal,
            secondaryAction = "Continuar sem BPM",
            onSecondaryAction = onDismissModal,
            footer = {
                PanelDivider()
                Column(verticalArrangement = Arrangement.spacedBy(com.pedroaba.tccmobile.theme.AppTheme.spacing.xs)) {
                    StatusPill(text = "Ritmo ao vivo", tone = StatusPillTone.Neutral)
                    AppSecondary("Você mantém um fluxo de 28 min e escapa da zona de risco em 3 sprints.")
                }
            }
        )

        ListPanel(title = "Ranking dos amigos", actionLabel = "Top 3") {
            ListRow(title = "Pedro Augusto", subtitle = "1º lugar", trailingTop = "100 km")
            PanelDivider()
            ListRow(title = "Rafael Augusto", subtitle = "2º lugar", trailingTop = "87 km")
        }
    }
}
