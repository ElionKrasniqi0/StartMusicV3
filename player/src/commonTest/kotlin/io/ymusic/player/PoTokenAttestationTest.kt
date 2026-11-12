package io.ymusic.player

import io.ymusic.player.mse.PoTokenAttestation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PoTokenAttestationTest {
    @Test
    fun media_or_ok_clears_withheld_rounds() {
        assertEquals(0, PoTokenAttestation.nextWithheldRounds(sps = 2, mediaBytes = 100, withheld = 2))
        assertEquals(0, PoTokenAttestation.nextWithheldRounds(sps = 1, mediaBytes = 0, withheld = 3))
    }

    @Test
    fun pending_sps2_without_media_increments_until_reject() {
        var withheld = 0
        repeat(PoTokenAttestation.MAX_WITHHELD_ROUNDS) {
            withheld = PoTokenAttestation.nextWithheldRounds(2, 0, withheld)
            assertFalse(PoTokenAttestation.shouldReject(2, withheld))
        }
        withheld = PoTokenAttestation.nextWithheldRounds(2, 0, withheld)
        assertTrue(PoTokenAttestation.shouldReject(2, withheld))
    }

    @Test
    fun sps3_rejects_immediately() {
        assertTrue(PoTokenAttestation.shouldReject(sps = 3, withheld = 0))
    }

    @Test
    fun one_shot_reattest_then_fail() {
        assertTrue(PoTokenAttestation.shouldReattest(alreadyAttempted = false))
        assertFalse(PoTokenAttestation.shouldReattest(alreadyAttempted = true))
    }

    @Test
    fun rejected_message_names_actual_sps() {
        assertTrue(PoTokenAttestation.rejectedMessage(2).contains("StreamProtectionStatus=2"))
        assertTrue(PoTokenAttestation.rejectedMessage(3).contains("StreamProtectionStatus=3"))
        assertTrue(PoTokenAttestation.rejectedMessage(null).contains("StreamProtectionStatus=3"))
    }

    @Test
    fun rejected_diag_clears_only_after_reattest_recovery() {
        assertFalse(
            PoTokenAttestation.shouldClearRejectedDiag(
                reattestAttempted = false,
                lastPoRejected = true,
                recovered = true,
            ),
        )
        assertFalse(
            PoTokenAttestation.shouldClearRejectedDiag(
                reattestAttempted = true,
                lastPoRejected = true,
                recovered = false,
            ),
        )
        assertTrue(
            PoTokenAttestation.shouldClearRejectedDiag(
                reattestAttempted = true,
                lastPoRejected = true,
                recovered = true,
            ),
        )
    }
}
