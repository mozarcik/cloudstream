package com.lagradost.cloudstream3.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsAccountTest {
    @Test
    fun `hides placeholder scan qr pseudo code`() {
        assertEquals(false, shouldShowDevicePinCode("SCAN QR"))
    }

    @Test
    fun `shows real device code`() {
        assertEquals(true, shouldShowDevicePinCode("YJTSKL"))
    }

    @Test
    fun `specific pin error suppresses fallback action`() {
        assertEquals(
            PinRequestFailureAction.None,
            resolvePinRequestFailureAction(
                hasOAuth2 = false,
                hasSpecificPinErrorMessage = true,
            )
        )
    }

    @Test
    fun `oauth providers open local auth when pin request fails without specific message`() {
        assertEquals(
            PinRequestFailureAction.OpenOAuth,
            resolvePinRequestFailureAction(
                hasOAuth2 = true,
                hasSpecificPinErrorMessage = false,
            )
        )
    }

    @Test
    fun `non oauth providers show generic auth failure when pin request fails`() {
        assertEquals(
            PinRequestFailureAction.ShowGenericAuthFailure,
            resolvePinRequestFailureAction(
                hasOAuth2 = false,
                hasSpecificPinErrorMessage = false,
            )
        )
    }
}
