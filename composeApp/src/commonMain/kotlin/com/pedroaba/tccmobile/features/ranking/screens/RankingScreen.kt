package com.pedroaba.tccmobile.features.ranking.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pedroaba.tccmobile.ui.components.AppCaption
import com.pedroaba.tccmobile.ui.components.AppMetric
import com.pedroaba.tccmobile.ui.components.AppOverline
import com.pedroaba.tccmobile.ui.components.AppScreenScaffold
import com.pedroaba.tccmobile.ui.components.AppSecondary
import com.pedroaba.tccmobile.ui.components.AppSecondarySemiBold
import com.pedroaba.tccmobile.ui.components.AppTitle
import com.pedroaba.tccmobile.ui.components.ListPanel
import com.pedroaba.tccmobile.ui.components.ListRow
import com.pedroaba.tccmobile.ui.components.MetricCard
import com.pedroaba.tccmobile.ui.components.PanelDivider
import com.pedroaba.tccmobile.ui.components.StatusPill
import com.pedroaba.tccmobile.ui.components.StatusPillTone
import com.pedroaba.tccmobile.ui.components.SurvivorAvatar
import com.pedroaba.tccmobile.theme.AppTheme

@Composable
fun RankingScreen(
    onTabSelected: (String) -> Unit = {}
) {
    AppScreenScaffold {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppTitle("Ranking da infestação")
            AppCaption("Veja sua posição no sistema e quem domina as hordas hoje.")
        }

        ListPanel(title = "Seu status", actionLabel = "+6 esta semana") {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    AppTitle("#18 no ranking global")
                    AppCaption("1.284 pts")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        value = "92%",
                        label = "consistência"
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        value = "14",
                        label = "hordas do mês"
                    )
                }
            }
        }

        ListPanel(title = "Top 3 do ranking", actionLabel = "período mensal") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PodiumCard(position = "#2", name = "Maya", score = "1.8k pts")
                PodiumCard(position = "#1", name = "Ravi", score = "2.1k pts", active = true)
                PodiumCard(position = "#3", name = "Luna", score = "1.7k pts")
            }
        }

        ListPanel(title = "Ranking geral", actionLabel = "score por período") {
            LeaderboardRow(rank = "4", name = "Caio V.", score = "1.702 pts")
            PanelDivider()
            LeaderboardRow(rank = "5", name = "Lia Storm", score = "1.688 pts")
            PanelDivider()
            LeaderboardRow(rank = "6", name = "Nico Ferraz", score = "1.641 pts")
            PanelDivider()
            LeaderboardRow(rank = "18", name = "Pedro Barbosa", score = "1.284 pts", highlight = true)
        }
    }
}

@Composable
private fun PodiumCard(
    position: String,
    name: String,
    score: String,
    active: Boolean = false
) {
    Surface(
        modifier = Modifier.width(92.dp),
        shape = RoundedCornerShape(18.dp),
        color = AppTheme.colors.card,
        contentColor = AppTheme.colors.textPrimary,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.border)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppOverline(score)
                AppMetric(position)
                AppCaption(name)
            }
            if (active) {
                StatusPill(text = "líder", tone = StatusPillTone.Alert)
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    rank: String,
    name: String,
    score: String,
    highlight: Boolean = false
) {
    ListRow(
        title = name,
        subtitle = if (highlight) "você" else "score semanal",
        trailingTop = score,
        leading = { SurvivorAvatar(initials = rank) },
        trailing = {
            if (highlight) {
                StatusPill(text = "#$rank", tone = StatusPillTone.Alert)
            } else {
                AppSecondarySemiBold("#$rank")
            }
        }
    )
}
