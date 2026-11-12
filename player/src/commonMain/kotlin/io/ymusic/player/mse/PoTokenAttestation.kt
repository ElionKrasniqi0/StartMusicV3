package io.ymusic.player.mse

/**
 * MSE stream-protection policy for content PO tokens.
 *
 * googlevideo SPS=2 is attestation-pending and may deliver ~60s of preview media; SPS=3 is a hard
 * reject. The JS host maps either SPS=3 or too many empty SPS=2 rounds to `poRejected`.
 */
internal object PoTokenAttestation {
    const val MAX_WITHHELD_ROUNDS = 3

    fun nextWithheldRounds(sps: Int, mediaBytes: Int, withheld: Int): Int = when {
        mediaBytes > 0 || sps == 1 -> 0
        sps == 2 -> withheld + 1
        else -> withheld
    }

    fun shouldReject(sps: Int, withheld: Int): Boolean =
        sps == 3 || withheld > MAX_WITHHELD_ROUNDS

    /** First rejection for a load re-attests once (fresh BotGuard + mint); a second fails hard. */
    fun shouldReattest(alreadyAttempted: Boolean): Boolean = !alreadyAttempted

    /**
     * Clear the poRejected diagnostic only after a one-shot remint and a positive recovery signal
     * (buffering/ready or buffered media). Keeps the flag set through the remint itself.
     */
    fun shouldClearRejectedDiag(
        reattestAttempted: Boolean,
        lastPoRejected: Boolean,
        recovered: Boolean,
    ): Boolean = lastPoRejected && reattestAttempted && recovered

    fun rejectedMessage(sps: Int?): String = when (sps) {
        2 -> "googlevideo withheld media while PO attestation stayed pending (StreamProtectionStatus=2)"
        3 -> "googlevideo rejected the PO token (StreamProtectionStatus=3)"
        null -> "googlevideo rejected the PO token (StreamProtectionStatus=3)"
        else -> "googlevideo rejected the PO token (StreamProtectionStatus=$sps)"
    }
}
