package org.evomaster.e2etests.spring.rest.bb


import org.apache.commons.io.FileUtils
import org.evomaster.client.java.instrumentation.shared.ClassName
import org.evomaster.core.EMConfig.TestSuiteSplitType
import org.evomaster.core.output.OutputFormat
import org.evomaster.e2etests.utils.BlackBoxUtils
import org.evomaster.e2etests.utils.CoveredTargets
import org.evomaster.e2etests.utils.RestTestBase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.io.File
import java.nio.file.Paths
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.collections.Collection


abstract class SpringTestBase : RestTestBase() {


    @BeforeEach
    fun clearTargets(){
        CoveredTargets.reset()
    }

    protected fun addBlackBoxOptions(
        args: MutableList<String>,
        outputFormat: OutputFormat
    ) {
        setOption(args, "blackBox", "true")
        setOption(args, "bbTargetUrl", baseUrlOfSut)
        setOption(args, "bbSwaggerUrl", "$baseUrlOfSut/v3/api-docs")
        setOption(args, "problemType", "REST")
        setOption(args, "outputFormat", outputFormat.toString())

        //this is deprecated
        setOption(args, "bbExperiments", "false")
    }

    fun executeAndEvaluateBBTest(
        outputFormat: OutputFormat,
        outputFolderName: String,
        iterations: Int,
        timeoutMinutes: Int,
        targetLabel: String,
        lambda: Consumer<MutableList<String>>
    ){
        executeAndEvaluateBBTest(outputFormat, outputFolderName, iterations, timeoutMinutes,
            listOf(targetLabel),
            lambda)
    }

    fun executeAndEvaluateBBTest(
        outputFormat: OutputFormat,
        outputFolderName: String,
        iterations: Int,
        timeoutMinutes: Int,
        targetLabels: Collection<String>,
        lambda: Consumer<MutableList<String>>
    ){
        assertFalse(CoveredTargets.areCovered(targetLabels))
        runBlackBoxEM(outputFormat, outputFolderName, iterations, timeoutMinutes, lambda)
        BlackBoxUtils.checkCoveredTargets(targetLabels)

        CoveredTargets.reset()
        runGeneratedTests(outputFormat, outputFolderName)
        BlackBoxUtils.checkCoveredTargets(targetLabels)
    }


    fun runBlackBoxEM(
        outputFormat: OutputFormat,
        outputFolderName: String,
        iterations: Int,
        timeoutMinutes: Int,
        lambda: Consumer<MutableList<String>>
    ){
        val baseLocation = when {
            outputFormat.isJavaScript() -> BlackBoxUtils.baseLocationForJavaScript
            // TODO Python here
            else -> throw IllegalArgumentException("Not supported output type $outputFormat")
        }
        runTestForNonJVM(outputFormat, baseLocation, outputFolderName, iterations, timeoutMinutes, lambda)
    }

    fun runGeneratedTests(outputFormat: OutputFormat, outputFolderName: String){

        when{
            outputFormat.isJavaScript() -> BlackBoxUtils.runNpmTests(BlackBoxUtils.relativePath(outputFolderName))
            //TODO Python here
            else -> throw IllegalArgumentException("Not supported output type $outputFormat")
        }
    }


    private fun runTestForNonJVM(
        outputFormat: OutputFormat,
        rootOutputFolderBasePath: String,
        outputFolderName: String,
        iterations: Int,
        timeoutMinutes: Int,
        lambda: Consumer<MutableList<String>>
    ) {

        val folder = Paths.get(rootOutputFolderBasePath, outputFolderName)

        assertTimeoutPreemptively(Duration.ofMinutes(timeoutMinutes.toLong())) {
            FileUtils.deleteDirectory(folder.toFile())

            handleFlaky {
                val args = getArgsWithCompilation(
                    iterations,
                    outputFolderName,
                    ClassName("FIXME"),
                    true,
                    TestSuiteSplitType.FAULTS.toString(),
                    "FALSE"
                )
                setOption(args, "outputFolder", folder.toString())
                setOption(args, "testSuiteFileName", "")
                addBlackBoxOptions(args, outputFormat)

                defaultSeed++
                lambda.accept(ArrayList(args))
            }
        }
    }

}