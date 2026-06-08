package com.pedroaba.tccmobile.features.game.screens.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameResultDialogContentTest {

    @Test
    fun `does not create dialog content for running game`() {
        assertNull(gameResultDialogContentFor("running"))
    }

    @Test
    fun `creates captured defeat content for caught game`() {
        val content = gameResultDialogContentFor("caught")

        assertEquals("Você foi capturado", content?.title)
        assertEquals("A horda alcançou você. A partida foi perdida.", content?.message)
        assertEquals("Tentar novamente", content?.primaryAction)
    }

    @Test
    fun `creates victory content for escaped game`() {
        val content = gameResultDialogContentFor("escaped")

        assertEquals("Você venceu", content?.title)
        assertEquals("Você escapou da horda e concluiu a corrida.", content?.message)
        assertEquals("Ver resultado", content?.primaryAction)
    }

    @Test
    fun `ignores unknown game result`() {
        assertNull(gameResultDialogContentFor("paused"))
    }
}
