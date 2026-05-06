package com.pedroaba.tccmobile.features.profile.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pedroaba.tccmobile.ui.components.AppButton
import com.pedroaba.tccmobile.ui.components.AppButtonVariant
import com.pedroaba.tccmobile.ui.components.AppCard
import com.pedroaba.tccmobile.ui.components.AppCaption
import com.pedroaba.tccmobile.ui.components.AppForm
import com.pedroaba.tccmobile.ui.components.AppFormField
import com.pedroaba.tccmobile.ui.components.AppFormLabel
import com.pedroaba.tccmobile.ui.components.AppScreenScaffold
import com.pedroaba.tccmobile.ui.components.AppSecondary
import com.pedroaba.tccmobile.ui.components.AppSelect
import com.pedroaba.tccmobile.ui.components.AppSelectOption
import com.pedroaba.tccmobile.ui.components.AppTextInput
import com.pedroaba.tccmobile.ui.components.AppTitle
import com.pedroaba.tccmobile.ui.components.TopIdentityHeader

@Composable
fun EditProfileScreen(
    onBack: () -> Unit = {},
    onTabSelected: (String) -> Unit = {}
) {
    var name by remember { mutableStateOf("Pedro Barbosa") }
    var birthDate by remember { mutableStateOf("1998-08-14") }
    var email by remember { mutableStateOf("pedro.bio@survivor.app") }
    var maxHeartRate by remember { mutableStateOf("186") }

    AppScreenScaffold {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppTitle("Editar perfil")
            AppSecondary("Atualize apenas os campos em uso e mantenha sua ficha consistente.")
        }

        AppCard {
            TopIdentityHeader(
                title = "Pedro Barbosa",
                subtitle = "UUID do usuário e e-mail principal"
            )
        }

        AppCard {
            AppForm {
                AppFormField {
                    AppFormLabel("Nome")
                    AppTextInput(value = name, onValueChange = { name = it }, placeholder = "Nome completo")
                }
                AppFormField {
                    AppFormLabel("Data de nascimento")
                    AppTextInput(value = birthDate, onValueChange = { birthDate = it }, placeholder = "AAAA-MM-DD")
                }
                AppFormField {
                    AppFormLabel("Email")
                    AppTextInput(value = email, onValueChange = { email = it }, placeholder = "voce@email.com")
                }
                AppFormField {
                    AppFormLabel("FC máxima")
                    AppSelect(
                        options = listOf(
                            AppSelectOption("186 bpm", "186"),
                            AppSelectOption("188 bpm", "188"),
                            AppSelectOption("190 bpm", "190")
                        ),
                        value = maxHeartRate,
                        onValueChange = { maxHeartRate = it }
                    )
                }
            }
        }

        AppCard {
            AppTitle("Resumo físico")
            AppCaption("Altura: 1,76 m")
            AppCaption("Peso: 74 kg")
            AppSecondary("Perfil pronto para treinos de resistência e sprint.")
        }

        Column(verticalArrangement = Arrangement.spacedBy(com.pedroaba.tccmobile.theme.AppTheme.spacing.sm)) {
            AppButton(text = "Salvar alterações", onClick = onBack, modifier = Modifier.fillMaxWidth())
            AppButton(
                text = "Voltar ao perfil",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                variant = AppButtonVariant.Outline
            )
        }
    }
}
