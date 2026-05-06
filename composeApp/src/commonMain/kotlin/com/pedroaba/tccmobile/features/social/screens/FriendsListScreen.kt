package com.pedroaba.tccmobile.features.social.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.pedroaba.tccmobile.ui.components.AppButton
import com.pedroaba.tccmobile.ui.components.AppCaption
import com.pedroaba.tccmobile.ui.components.AppScreenScaffold
import com.pedroaba.tccmobile.ui.components.AppSecondary
import com.pedroaba.tccmobile.ui.components.AppTitle
import com.pedroaba.tccmobile.ui.components.FeatureCard
import com.pedroaba.tccmobile.ui.components.IconBadge
import com.pedroaba.tccmobile.ui.components.ListPanel
import com.pedroaba.tccmobile.ui.components.ListRow
import com.pedroaba.tccmobile.ui.components.MetricCard
import com.pedroaba.tccmobile.ui.components.MetricStrip
import com.pedroaba.tccmobile.ui.components.PanelDivider
import com.pedroaba.tccmobile.ui.components.StatusPill
import com.pedroaba.tccmobile.ui.components.StatusPillTone

@Composable
fun FriendsListScreen(
    onAddFriends: () -> Unit = {},
    onHistory: () -> Unit = {},
    onTabSelected: (String) -> Unit = {}
) {
    AppScreenScaffold {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppTitle("Sua rede de fuga")
            AppCaption("Busque usuários, envie solicitações e acompanhe o status das conexões ativas.")
        }

        MetricStrip {
            MetricCard(value = "38", label = "na rede")
            MetricCard(value = "12", label = "aliados ativos")
        }

        FeatureCard(
            title = "Adicionar por tag",
            body = "Busque por nome, email ou ID do sobrevivente e envie um convite rapido.",
            primaryAction = "Adicionar amigo",
            onPrimaryAction = onAddFriends,
            secondaryAction = "Ver histórico",
            onSecondaryAction = onHistory
        )

        ListPanel(title = "Sobreviventes sugeridos", actionLabel = "convites ativos") {
            FriendRow(name = "Ravi Ferraz", subtitle = "Disponível para zona industrial", online = true)
            PanelDivider()
            FriendRow(name = "Lia Storm", subtitle = "Convite pendente", online = false)
            PanelDivider()
            FriendRow(name = "Caio Norte", subtitle = "Prefere squad pequeno", online = false)
        }

        ListPanel(title = "Convites em rota") {
            AppSecondary("Acompanhe convites pendentes e compartilhe seu ID apenas com quem estiver na sua rede.")
        }
    }
}

@Composable
private fun FriendRow(
    name: String,
    subtitle: String,
    online: Boolean
) {
    ListRow(
        title = name,
        subtitle = subtitle,
        trailing = {
            StatusPill(
                text = if (online) "online" else "offline",
                tone = if (online) StatusPillTone.Success else StatusPillTone.Neutral
            )
        },
        leading = {
            IconBadge(icon = Icons.Filled.Person)
        }
    )
}
