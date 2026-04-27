package com.pedroaba.tccmobile.features.home.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pedroaba.tccmobile.theme.AppTheme
import com.pedroaba.tccmobile.theme.TccMobileTheme
import com.pedroaba.tccmobile.ui.components.AppBadge
import com.pedroaba.tccmobile.ui.components.AppBody
import com.pedroaba.tccmobile.ui.components.AppCard
import com.pedroaba.tccmobile.ui.components.AppCardContent
import com.pedroaba.tccmobile.ui.components.AppCardHeader
import com.pedroaba.tccmobile.ui.components.AppRootContainer
import com.pedroaba.tccmobile.ui.components.AppSubtitle
import com.pedroaba.tccmobile.ui.components.AppTitle

@Composable
fun HomeScreen(
    userName: String = "sobrevivente",
    level: Int = 12,
    totalHours: Int = 37,
    lastRunDistance: String = "6.4 km",
    lastRunDuration: String = "18 min",
    onStartRun: () -> Unit = {},
    onViewProfile: () -> Unit = {},
    onContinueMission: () -> Unit = {}
) {
    val firstName = userName.trim().split(" ".toRegex()).firstOrNull() ?: "sobrevivente"

    AppRootContainer {
        Column(
            modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
        ) {
            Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                            .size(48.dp)
                            .background(
                                AppTheme.colors.primary,
                                MaterialTheme.shapes.medium
                            ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Avatar",
                        tint = AppTheme.colors.primaryForeground
                    )
                }

                Column(modifier = Modifier.padding(start = AppTheme.spacing.md)) {
                    AppTitle("Boa noite, $firstName!")
                    AppSubtitle("Nvel $level - $totalHours horas sobrevividas")
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

            AppCard {
                AppCardHeader {
                    AppBadge("HORDA PRONTA")
                }

                AppCardContent {
                    AppTitle("Comece uma nova uga agora.")
                    AppBody(
                        "Seu smartwatch estÃ¡desconectado. Incie mesmo assim ou conecte para liber BPM e sensores em Tempo Real.",
                        modifier = Modifier.padding(top = AppTheme.spacing.xs)
                    )

                    Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)
                    ) {
                        Button(
                            onClick = onStartRun,
                            modifier = Modifier.weight(2f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.DirectionsRun,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "Comear horda",
                                    modifier = Modifier.padding(start = AppTheme.spacing.xs)
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onViewProfile,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ver perfil")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Nvel atual",
                    value = level.toString()
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Hordas",
                    value = totalHours.toString()
                )
            }

            AppCard {
                AppCardHeader {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppTitle("ãšltima horda", modifier = Modifier.weight(1f))
                        AppBadge("SOREVIVEU")
                    }
                }

                AppCardContent {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                lastRunDistance,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Distncia", style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                lastRunDuration,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Tempo", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    AppBody(
                        "Voc mantve ritmo forte e ecou da zona de risco em 3 sprints.",
                        modifier = Modifier.padding(top = AppTheme.spacing.md)
                    )
                }
            }

            AppCard {
                AppCardHeader {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppTitle("Ranking dos amigos")
                        Text(
                            "Top 3",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTheme.colors.glow
                        )
                    }
                }

                AppCardContent(
                    modifier = Modifier.padding(top = AppTheme.spacing.md)
                ) {
                    RankingItem(position = 1, name = "Pedro Augusto", distance = "100 km")
                    RankingItem(position = 2, name = "Rafael Augusto", distance = "87 km")
                    RankingItem(position = 3, name = "Gabriel Augusto", distance = "65 km")
                }
            }

            Spacer(modifier = Modifier.height(80.dp))

            Button(
                onClick = onContinueMission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continuar misso")
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.card
        )
    ) {
    AppCardContent {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.textSecondary
            )
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RankingItem(
    position: Int,
    name: String,
    distance: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
        ) {
            Text(
                "${position}º",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                        .size(40.dp)
                        .background(
                            AppTheme.colors.input,
                            MaterialTheme.shapes.small
                        ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = AppTheme.colors.textSecondary
                )
            }
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            distance,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    TccMobileTheme {
        HomeScreen()
    }
}