package com.vaiinilla.app.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.vaiinilla.app.domain.auth.student.StudentAuthMfaFactor
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TotpMfaChallengeFormTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `requires six digits and submits the TOTP code`() {
        var submitted = false
        var code by mutableStateOf("")
        composeTestRule.setContent {
            VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
                TotpMfaChallengeForm(
                    factors = listOf(StudentAuthMfaFactor(uid = "totp-1", displayName = null)),
                    selectedFactorUid = "totp-1",
                    code = code,
                    loading = false,
                    errorMessage = null,
                    onFactorSelected = {},
                    onCodeChange = { code = it },
                    onSubmit = { submitted = true },
                    onCancel = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Verificar código").assertIsNotEnabled()
        composeTestRule.onNodeWithText("El código cambia cada 30 segundos.").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Código de verificación")
            .performTextInput("123456")
        composeTestRule.runOnIdle { assertEquals("123456", code) }
        composeTestRule.onNodeWithText("Verificar código").performClick()

        composeTestRule.runOnIdle { assertEquals(true, submitted) }
    }
}
