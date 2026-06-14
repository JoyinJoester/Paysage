package joyin.takgi.paysage.esim

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EsimSgp22SafetyPolicyTest {
    @Test
    fun allowsImplementedDiagnosticCommand() {
        val command = EsimSgp22CommandCatalog.commands.first { it.id == "select_isd_r" }

        val decision = EsimSgp22SafetyPolicy.evaluate(command, defaultContext(isdRSelected = false))

        assertTrue(decision.allowed)
        assertTrue(decision.message.contains("只读诊断"))
    }

    @Test
    fun allowsSystemEuiccInfoWithoutIsdR() {
        val command = EsimSgp22CommandCatalog.commands.first { it.id == "get_euicc_info" }

        val decision = EsimSgp22SafetyPolicy.evaluate(command, defaultContext(isdRSelected = false))

        assertTrue(decision.allowed)
        assertTrue(decision.message.contains("只读诊断"))
    }

    @Test
    fun blocksPlannedProfileReadBeforeIsdR() {
        val command = EsimSgp22CommandCatalog.commands.first { it.id == "list_profiles" }

        val decision = EsimSgp22SafetyPolicy.evaluate(command, defaultContext(isdRSelected = false))

        assertFalse(decision.allowed)
        assertTrue(decision.message.contains("ISD-R"))
    }

    @Test
    fun blocksPlannedProfileReadEvenAfterIsdRUntilImplemented() {
        val command = EsimSgp22CommandCatalog.commands.first { it.id == "list_profiles" }

        val decision = EsimSgp22SafetyPolicy.evaluate(
            command,
            defaultContext(isdRSelected = true, allowExperimentalReadOnly = true)
        )

        assertFalse(decision.allowed)
        assertTrue(decision.message.contains("尚未实现"))
    }

    @Test
    fun blocksDestructiveCommandsInCommercialBuild() {
        val command = EsimSgp22CommandCatalog.commands.first { it.id == "delete_profile" }

        val decision = EsimSgp22SafetyPolicy.evaluate(
            command,
            defaultContext(
                isdRSelected = true,
                privilegedOrAuthorized = true,
                userConfirmedSensitiveAction = true
            )
        )

        assertFalse(decision.allowed)
        assertTrue(decision.message.contains("破坏性") || decision.message.contains("授权"))
    }

    private fun defaultContext(
        isdRSelected: Boolean,
        allowExperimentalReadOnly: Boolean = false,
        privilegedOrAuthorized: Boolean = false,
        userConfirmedSensitiveAction: Boolean = false
    ): EsimSgp22SafetyContext =
        EsimSgp22SafetyContext(
            isdRSelected = isdRSelected,
            allowExperimentalReadOnly = allowExperimentalReadOnly,
            privilegedOrAuthorized = privilegedOrAuthorized,
            userConfirmedSensitiveAction = userConfirmedSensitiveAction
        )
}
