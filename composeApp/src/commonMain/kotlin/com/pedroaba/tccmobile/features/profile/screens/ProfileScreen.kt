package com.pedroaba.tccmobile.features.profile.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pedroaba.tccmobile.ui.components.AppButton
import com.pedroaba.tccmobile.ui.components.AppButtonVariant
import com.pedroaba.tccmobile.ui.components.AppCaption
import com.pedroaba.tccmobile.ui.components.AppScreenScaffold
import com.pedroaba.tccmobile.ui.components.AppSecondary
import com.pedroaba.tccmobile.ui.components.FeatureCard
import com.pedroaba.tccmobile.ui.components.IconBadge
import com.pedroaba.tccmobile.ui.components.ListPanel
import com.pedroaba.tccmobile.ui.components.ListRow
import com.pedroaba.tccmobile.ui.components.MetricCard
import com.pedroaba.tccmobile.ui.components.MetricStrip
import com.pedroaba.tccmobile.ui.components.PanelDivider
import com.pedroaba.tccmobile.ui.components.StatusPill
import com.pedroaba.tccmobile.ui.components.StatusPillTone
import com.pedroaba.tccmobile.ui.components.TopIdentityHeader

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit = {},
    onTabSelected: (String) -> Unit = {}
) {
    AppScreenScaffold {
        TopIdentityHeader(
            title = "Pedro Barbosa",
            subtitle = "Nível 12 · 37 hordas · Bio Runner",
            badge = "SOBREVIVENTE  RANK #18"
        )

        MetricStrip {
            MetricCard(value = "184 km", label = "distância acumulada")
            MetricCard(value = "42 min", label = "melhor horda")
            MetricCard(value = "92%", label = "aproveitamento")
        }

        FeatureCard(
            title = "Smartwatch e sensores",
            body = "Batimentos, pace e distância da sessão atual estão sendo enviados direto do dispositivo conectado.",
            status = "Conectado",
            statusTone = StatusPillTone.Success,
            primaryAction = "Editar perfil",
            onPrimaryAction = onEditProfile,
            secondaryAction = "Compartilhar ID",
            onSecondaryAction = {},
            footer = {
                PanelDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        AppCaption("Garmin Venue")
                        AppSecondary("último dispositivo pareado")
                    }
                    Column {
                        AppCaption("BPM ao vivo")
                        AppSecondary("biometria e dados ao vivo")
                    }
                }
            }
        )

        ListPanel(title = "Rede social", actionLabel = "amizades e convites") {
            Row(horizontalArrangement = Arrangement.spacedBy(com.pedroaba.tccmobile.theme.AppTheme.spacing.sm)) {
            AppButton(
                text = "Ver aliados",
                onClick = { onTabSelected("social") },
                modifier = Modifier.fillMaxWidth(),
                variant = AppButtonVariant.Outline
            )
            AppButton(
                text = "Convidar amigo",
                onClick = { onTabSelected("social") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        }

        ListPanel(title = "Últimas hordas", actionLabel = "histórico") {
            ListRow(title = "Distrito Industrial", subtitle = "6.4 km · 42 min · Sobreviveu", trailingTop = "Ontem")
            PanelDivider()
            ListRow(title = "Marginal Norte", subtitle = "4.1 km · 28 min · Sprint final", trailingTop = "2 dias")
        }
    }
}
