import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64

repositories {
    // Do NOT enable this otherwise all kinds of horror ensues. Not exactly sure why. Maybe something in the local repo interferes with the build. with builds.
    // Note that this is also used in the publish plugin althought it is not strictly necessary for it to be there.
    //mavenLocal()
    mavenCentral()
}

group = "io.exoquery"
// Everything inherits the version from here
version = "2.2.0-2.0.0.PL"

object Versions {
    val decomatVersion = "0.3.0"
}

check("$version".isNotBlank() && version != "unspecified")
    { "invalid version $version" }

tasks.withType<Test> {
    testLogging {
        lifecycle {
            events = mutableSetOf(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED)
            exceptionFormat = TestExceptionFormat.FULL

            showExceptions = true
            showCauses = true
            showStackTraces = false
            showStandardStreams = false
        }
        info.events = lifecycle.events
        info.exceptionFormat = lifecycle.exceptionFormat
    }

    val failedTests = mutableListOf<TestDescriptor>()
    val skippedTests = mutableListOf<TestDescriptor>()

    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}

        override fun beforeTest(testDescriptor: TestDescriptor) {}

        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
            when (result.resultType) {
                TestResult.ResultType.FAILURE -> failedTests.add(testDescriptor)
                TestResult.ResultType.SKIPPED -> skippedTests.add(testDescriptor)
                else -> Unit
            }
        }

        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent == null) {
                logger.lifecycle("################ Summary::Start ################")
                logger.lifecycle("Test result: ${result.resultType}")
                logger.lifecycle(
                    "Test summary: ${result.testCount} tests, " +
                      "${result.successfulTestCount} succeeded, " +
                      "${result.failedTestCount} failed, " +
                      "${result.skippedTestCount} skipped")
                failedTests.takeIf { it.isNotEmpty() }?.prefixedSummary("\tFailed Tests")
                skippedTests.takeIf { it.isNotEmpty() }?.prefixedSummary("\tSkipped Tests:")
                logger.lifecycle("################ Summary::End ##################")
            }
        }

        private infix fun List<TestDescriptor>.prefixedSummary(subject: String) {
            logger.lifecycle(subject)
            forEach { test -> logger.lifecycle("\t\t${test.displayName()}") }
        }

        private fun TestDescriptor.displayName() = parent?.let { "${it.name} - $name" } ?: "$name"

    })
}

val startSonatypeStaging by tasks.registering {
  description = "Creates a new OSSRH staging repository and records its ID"

  doLast {
    /* ---- gather inputs exactly as before ---- */
    val pid   = "io.exoquery"
    val user  = System.getenv("SONATYPE_USERNAME")   ?: error("SONATYPE_USERNAME not set")
    val pass  = System.getenv("SONATYPE_PASSWORD")   ?: error("SONATYPE_PASSWORD not set")
    val desc = "${System.getenv("GITHUB_REPOSITORY")}/${System.getenv("GITHUB_WORKFLOW")}#${System.getenv("GITHUB_RUN_NUMBER")}"

    val bodyJson = """{"data":{"description":"$desc"}}"""
    val auth     = Base64.getEncoder().encodeToString("$user:$pass".toByteArray())

    val request  = HttpRequest.newBuilder()
      .uri(URI.create(
        "https://ossrh-staging-api.central.sonatype.com/service/local/staging/profiles/$pid/start"))
      .header("Content-Type", "application/json")
      .header("Authorization", "Basic $auth")
      .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
      .build()

    val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())

    println("HTTP ${response.statusCode()}")
    println(response.body())

    if (response.statusCode() !in 200..299)
      throw GradleException("Failed to start staging repository")

    /* -------- Parse the JSON -------- */
    val mapper  = jacksonObjectMapper()
    val node    = mapper.readTree(response.body())
    val repoId  = node["data"]["stagedRepositoryId"].asText()

    if (repoId == null || repoId.isBlank())
      throw GradleException("========= Failed to parse staging repository ID from response: =========\n${response.body()}")

    // 1. Expose as a Gradle extra property
    project.extra["stagingRepoId"] = repoId
  }
}
