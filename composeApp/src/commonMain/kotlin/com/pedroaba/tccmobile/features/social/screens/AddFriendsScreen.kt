package com.pedroaba.tccmobile.features.social.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pedroaba.tccmobile.ui.components.AppButton
import com.pedroaba.tccmobile.ui.components.AppCaption
import com.pedroaba.tccmobile.ui.components.AppForm
import com.pedroaba.tccmobile.ui.components.AppFormField
import com.pedroaba.tccmobile.ui.components.AppFormLabel
import com.pedroaba.tccmobile.ui.components.AppScreenScaffold
import com.pedroaba.tccmobile.ui.components.AppSecondary
import com.pedroaba.tccmobile.ui.components.AppTextInput
import com.pedroaba.tccmobile.ui.components.AppTitle
import com.pedroaba.tccmobile.ui.components.IconBadge
import com.pedroaba.tccmobile.ui.components.ListPanel
import com.pedroaba.tccmobile.ui.components.ListRow
import com.pedroaba.tccmobile.ui.components.MetricCard
import com.pedroaba.tccmobile.ui.components.MetricStrip
import com.pedroaba.tccmobile.ui.components.PanelDivider
import com.pedroaba.tccmobile.ui.components.StatusPill
import com.pedroaba.tccmobile.ui.components.StatusPillTone

@Composable
fun AddFriendsScreen(
    onBack: () -> Unit = {},
    onTabSelected: (String) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }

    AppScreenScaffold {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppTitle("Monte sua fila de fuga")
            AppCaption("Busque usuários, envie solicitações ou use o status das conexões ativas.")
        }

        MetricStrip {
            MetricCard(value = "38", label = "na rede")
            MetricCard(value = "12", label = "aliados ativos")
        }

        ListPanel(title = "Adicionar por tag") {
            AppForm {
                AppFormField {
                    AppFormLabel("Buscar sobrevivente")
                    AppTextInput(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "e-mail, nome ou ID do sobrevivente",
                        leadingContent = { IconBadge(icon = Icons.Filled.Search, modifier = Modifier, tint = com.pedroaba.tccmobile.theme.AppTheme.colors.textSecondary, background = com.pedroaba.tccmobile.theme.AppTheme.colors.background) }
                    )
                }
                AppButton(text = "Enviar solicitacao", onClick = {}, modifier = Modifier.fillMaxWidth())
            }
        }

        ListPanel(title = "Sobreviventes sugeridos") {
            SuggestionRow(name = "Ravi Ferraz", subtitle = "Disponível para zona industrial")
            PanelDivider()
            SuggestionRow(name = "Lia Storm", subtitle = "Convite pendente")
            PanelDivider()
            SuggestionRow(name = "Caio Norte", subtitle = "Prefere squad pequeno")
        }

        ListPanel(title = "Convites em rota") {
            AppSecondary("Acompanhe convites pendentes e compartilhe seu ID apenas com quem estiver na sua rede.")
        }
    }
}

@Composable
private fun SuggestionRow(name: String, subtitle: String) {
    ListRow(
        title = name,
        subtitle = subtitle,
        leading = { IconBadge(icon = Icons.Filled.Person) },
        trailing = {
            StatusPill(text = "convidar", tone = StatusPillTone.Alert)
        }
    )
}
