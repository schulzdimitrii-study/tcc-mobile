package com.pedroaba.tccmobile.features.home.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pedroaba.tccmobile.backend.model.HordeDto
import com.pedroaba.tccmobile.backend.online.HordeCatalogStatus
import com.pedroaba.tccmobile.backend.online.RemoteSessionState
import com.pedroaba.tccmobile.backend.online.RemoteSessionStatus
import com.pedroaba.tccmobile.backend.online.displayDifficulty
import com.pedroaba.tccmobile.backend.online.displayDuration
import com.pedroaba.tccmobile.backend.online.displayPace
import com.pedroaba.tccmobile.ui.components.AppBody
import com.pedroaba.tccmobile.ui.components.AppButton
import com.pedroaba.tccmobile.ui.components.AppButtonVariant
import com.pedroaba.tccmobile.ui.components.AppCaption
import com.pedroaba.tccmobile.ui.components.AppCallout
import com.pedroaba.tccmobile.ui.components.AppScreenScaffold
import com.pedroaba.tccmobile.ui.components.AppSecondary
import com.pedroaba.tccmobile.ui.components.AppSecondarySemiBold
import com.pedroaba.tccmobile.ui.components.AppSelect
import com.pedroaba.tccmobile.ui.components.AppSelectOption
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
import kotlin.math.round

@Composable
fun HomeScreen(
    userName: String = "Você",
    currentUserId: String = "",
    remoteSessionState: RemoteSessionState = RemoteSessionState(),
    hordes: List<HordeDto> = emptyList(),
    selectedHordeId: String? = null,
    hordeCatalogStatus: HordeCatalogStatus = HordeCatalogStatus.IDLE,
    hordeErrorMessage: String? = null,
    onHordeSelected: (String) -> Unit = {},
    onReloadHordes: () -> Unit = {},
    onStartRun: () -> Unit = {},
    onViewProfile: () -> Unit = {},
    onShowWatchModal: () -> Unit = {},
    onTabSelected: (String) -> Unit = {}
) {
    val selectedHorde = hordes.firstOrNull { it.id == selectedHordeId }
        ?: remoteSessionState.selectedHorde
    val isLoadingHordes = hordeCatalogStatus == HordeCatalogStatus.LOADING
    val canStartHorde = selectedHorde?.id?.isNotBlank() == true && !isLoadingHordes
    val leaderboard = remoteSessionState.leaderboard
    val currentUserEntry = leaderboard?.entries?.firstOrNull { it.userId == currentUserId }
    val activeSessionId = remoteSessionState.sessionId
    val isSessionActive = remoteSessionState.status == RemoteSessionStatus.ACTIVE ||
        remoteSessionState.status == RemoteSessionStatus.CONNECTING ||
        remoteSessionState.status == RemoteSessionStatus.STARTING

    AppScreenScaffold {
        TopIdentityHeader(
            title = "Boa noite, $userName",
            subtitle = if (activeSessionId != null) {
                "Sessão ${activeSessionId.take(8)} · ${remoteSessionState.status.name.lowercase()}"
            } else {
                "Conta conectada · aguardando sessão"
            }
        )

        FeatureCard(
            eyebrow = if (canStartHorde) "HORDA PRONTA" else "HORDA INDISPONIVEL",
            title = selectedHorde?.name ?: "Escolha uma horda do backend.",
            body = selectedHorde?.let {
                "${it.displayDifficulty()} · ${it.displayPace()} · ${it.displayDuration()}"
            } ?: "Busque as hordas no backend para iniciar uma sessão vinculada.",
            primaryAction = if (isLoadingHordes) "Carregando" else "Comecar horda",
            onPrimaryAction = onStartRun,
            primaryActionEnabled = canStartHorde,
            secondaryAction = "Ver perfil",
            onSecondaryAction = onViewProfile,
            footer = {
                PanelDivider()
                if (hordes.isNotEmpty()) {
                    AppSelect(
                        options = hordes.map { horde ->
                            AppSelectOption(
                                label = "${horde.name} · ${horde.displayDifficulty()}",
                                value = horde.id
                            )
                        },
                        value = selectedHordeId,
                        onValueChange = onHordeSelected,
                        placeholder = "Selecionar horda",
                        enabled = !isLoadingHordes
                    )
                    PanelDivider()
                }
                hordeErrorMessage?.let {
                    AppCallout(text = it)
                    PanelDivider()
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        AppSecondarySemiBold("Última sincronização")
                        AppCaption(
                            when (hordeCatalogStatus) {
                                HordeCatalogStatus.LOADED -> "${hordes.size} hordas carregadas"
                                HordeCatalogStatus.LOADING -> "Buscando hordas no backend"
                                HordeCatalogStatus.ERROR -> "Falha ao buscar hordas"
                                HordeCatalogStatus.IDLE -> "Catálogo ainda não carregado"
                            }
                        )
                    }
                    StatusPill(
                        text = when {
                            isLoadingHordes -> "Sincronizando"
                            hordes.isEmpty() -> "Buscar hordas"
                            else -> "Backend ativo"
                        },
                        tone = if (hordes.isEmpty()) StatusPillTone.Alert else StatusPillTone.Primary,
                        modifier = Modifier
                    )
                }
                if (hordes.isEmpty() && !isLoadingHordes) {
                    AppButton(
                        text = "Recarregar hordas",
                        onClick = onReloadHordes,
                        modifier = Modifier.fillMaxWidth(),
                        variant = AppButtonVariant.Outline
                    )
                }
            }
        )

        MetricStrip {
            MetricCard(
                modifier = Modifier.weight(1f),
                value = hordes.size.toString(),
                label = "hordas disponíveis"
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                value = leaderboard?.userRank?.let { "#$it" } ?: "--",
                label = "rank na sessão"
            )
        }

        ListPanel(
            title = "Sessão online",
            actionLabel = remoteSessionState.status.name
        ) {
            if (activeSessionId == null) {
                AppBody("Nenhuma sessão ativa. Escolha uma horda carregada do backend para iniciar.")
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        AppTitle(currentUserEntry?.distanceKm?.let(::formatKm) ?: "--")
                        AppCaption("Sua distância")
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        AppTitle(leaderboard?.hordeVirtualDistanceKm?.let(::formatKm) ?: "--")
                        AppCaption("Distância da horda")
                    }
                }
                AppBody(
                    selectedHorde?.let { "Sessão vinculada à horda ${it.name}." }
                        ?: "Sessão online ativa sem horda selecionada localmente."
                )
            }
        }

        ListPanel(
            title = "Ranking atual",
            actionLabel = if (leaderboard == null) "sem dados" else "tempo real"
        ) {
            if (leaderboard == null) {
                AppBody("O leaderboard aparece aqui assim que o backend transmitir dados da sessão.")
            } else {
                ListRow(
                    title = selectedHorde?.name ?: "Horda",
                    subtitle = "Alvo virtual",
                    trailingTop = leaderboard.hordeVirtualDistanceKm?.let(::formatKm) ?: "--"
                )
                leaderboard.entries.take(3).forEach { entry ->
                    PanelDivider()
                    RankingPreviewRow(
                        position = "#${entry.rank}",
                        name = if (entry.userId == currentUserId) userName else entry.userId.take(8),
                        score = formatKm(entry.distanceKm),
                        highlight = entry.userId == currentUserId
                    )
                }
                if (leaderboard.entries.isEmpty()) {
                    PanelDivider()
                    AppBody("Ainda não há corredores no leaderboard desta sessão.")
                }
            }
        }

        FeatureCard(
            status = if (isSessionActive) "SESSÃO ATIVA" else "PRONTO PARA GPS",
            statusTone = if (isSessionActive) StatusPillTone.Success else StatusPillTone.Neutral,
            title = "Telemetria da corrida",
            body = "A sessão usa localização e sensores do celular; BPM entra quando houver ponte de wearable disponível no runtime.",
            primaryAction = "Ver conexão",
            onPrimaryAction = onShowWatchModal,
            secondaryAction = if (isSessionActive) "Abrir sessão" else "Iniciar com GPS",
            onSecondaryAction = onStartRun,
            secondaryActionEnabled = isSessionActive || canStartHorde
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
        subtitle = if (highlight) "você" else "sessão atual",
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

private fun formatKm(value: Double): String {
    val rounded = round(value * 100.0) / 100.0
    return "$rounded km"
}
