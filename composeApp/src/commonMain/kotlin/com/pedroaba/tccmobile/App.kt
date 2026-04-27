package com.pedroaba.tccmobile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.pedroaba.tccmobile.features.auth.screens.LoginScreen
import com.pedroaba.tccmobile.theme.TccMobileTheme

@Composable
@Preview
fun App() {
    TccMobileTheme {
        LoginScreen()
    }
}