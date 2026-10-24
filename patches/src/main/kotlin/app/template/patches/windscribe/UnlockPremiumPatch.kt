package app.template.patches.windscribe

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.WINDSCRIBE_COMPATIBILITY

@Suppress("unused")
val windscribeUnlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium",
    description = "Unlocks Windscribe premium account.",
    default = true
) {
    compatibleWith(WINDSCRIBE_COMPATIBILITY)

    execute {
        // Integer-returning getters -> return Integer.valueOf(1)
        listOf(
            LoginPremiumFingerprint,
            LoginBillingPlanFingerprint,
            RegistrationPremiumFingerprint,
            SessionPremiumFingerprint,
            SessionBillingPlanFingerprint,
            SessionUserClassFingerprint,
            SessionReBillFingerprint
        ).forEach { fingerprint ->
            fingerprint.match(classDefBy(fingerprint.definingClass!!)).method.addInstructions(
                0,
                """
                const/4 v0, 0x1
                invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
                move-result-object p0
                return-object p0
                """.trimIndent()
            )
        }

        // String-returning getters -> return "-1" (unlimited traffic)
        listOf(
            LoginTrafficMaxFingerprint,
            RegistrationTrafficMaxFingerprint,
            SessionTrafficMaxFingerprint
        ).forEach { fingerprint ->
            fingerprint.match(classDefBy(fingerprint.definingClass!!)).method.addInstructions(
                0,
                """
                const-string p0, "-1"
                return-object p0
                """.trimIndent()
            )
        }

        // Premium expiry -> far future
        SessionPremiumExpiryFingerprint
            .match(classDefBy(SessionPremiumExpiryFingerprint.definingClass!!))
            .method
            .addInstructions(0, "const-string p0, \"2099-12-31\"\nreturn-object p0")

        // int-returning getters -> return 0 (unlock all locations/servers)
        listOf(
            LocationPremiumOnlyFingerprint,
            LocationPremiumOnlyComponentFingerprint,
            DatacenterPremiumFingerprint,
            DatacenterPremiumComponentFingerprint
        ).forEach { fingerprint ->
            fingerprint.match(classDefBy(fingerprint.definingClass!!)).method.addInstructions(
                0,
                "const/4 v0, 0x0\nreturn v0"
            )
        }
    }
}