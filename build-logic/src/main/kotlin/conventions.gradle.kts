import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
    val desc = "${System.getenv("GITHUB_REPOSITORY")}/${System.getenv("GITHUB_WORKFLOW")}#${System.getenv("GITHUB_RUN_NUMBER")}/${project.name}/${System.getenv("MATRIX_OS")}"

    val bodyJson = """{"data":{"description":"$desc"}}"""
    val auth     = Base64.getEncoder().encodeToString("$user:$pass".toByteArray())

    val request  = HttpRequest.newBuilder()
      .uri(URI.create("https://ossrh-staging-api.central.sonatype.com/service/local/staging/profiles/$pid/start"))
      .header("Content-Type", "application/json")
      .header("Authorization", "Basic $auth")
      .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
      .build()

    val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())

    println("HTTP Response Code: ${response.statusCode()}:\n${response.body()}")

    if (response.statusCode() !in 200..299)
      throw GradleException("Failed to start staging repository")

    /* -------- Parse the JSON -------- */
    val mapper  = jacksonObjectMapper()
    val node    = mapper.readTree(response.body())
    val repoId  = node["data"]["stagedRepositoryId"].asText()

    if (repoId == null || repoId.isBlank())
      throw GradleException("========= Failed to parse staging repository ID from response: =========\n${response.body()}")

    // 1. Expose as a Gradle extra property
    println("-------------- Exposing Repo ID: $repoId in ${project.name} --------------")
    project.extra["stagingRepoId"] = repoId
  }
}


data class Repo(
  val key: String,
  val state: String,
  val description: String? = null,
  val portal_deployment_id: String? = null
) {
  val encodedKey get() = java.net.URLEncoder.encode(key, java.nio.charset.StandardCharsets.UTF_8)
  val showName get() = description?.let { "${it}-${key}" } ?: key
}

data class Wrapper(val repositories: List<Repo>) {
  val repositoriesSorted = repositories.sortedBy { it.showName }
}

fun HttpClient.listStagingRepos(user: String, pass: String): Wrapper {
  val pid   = "io.exoquery"
  val auth     = Base64.getEncoder().encodeToString("$user:$pass".toByteArray())
  val request  = HttpRequest.newBuilder()
    .uri(URI.create("https://ossrh-staging-api.central.sonatype.com/manual/search/repositories?profile_id=$pid&ip=any"))
    .header("Content-Type", "application/json")
    .header("Authorization", "Basic $auth")
    .GET()
    .build()

  val mapper = jacksonObjectMapper()

  fun tryPrintJson(json: String) {
    try {
      mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(json))
    } catch (e: Exception) {
      json
    }
  }
  val response = http.send(request, HttpResponse.BodyHandlers.ofString())
  println("================ /manual/search/repositories Response Code: ${response.statusCode()}: ================\n${tryPrintJson(response.body())}")

  /* 1.  Sanity-check the HTTP call */
  if (response.statusCode() !in 200..299) {
    val msg = "================ OSS RH search failed Code:${response.statusCode()} ================\n${response.body()}"
    logger.error(msg)
    throw GradleException("Search request was not successful because of:\n${msg}")
  }

  val payload: Wrapper = mapper.readValue<Wrapper>(response.body())
  return payload
}

val publishSonatypeStaging by tasks.registering {
  description = "Creates a new OSSRH staging repository and records its ID"

  doLast {
    /* ---- gather inputs exactly as before ---- */
    val user  = System.getenv("SONATYPE_USERNAME")   ?: error("SONATYPE_USERNAME not set")
    val pass  = System.getenv("SONATYPE_PASSWORD")   ?: error("SONATYPE_PASSWORD not set")
    val desc = "${System.getenv("GITHUB_REPOSITORY")}/${System.getenv("GITHUB_WORKFLOW")}#${System.getenv("GITHUB_RUN_NUMBER")}"
    val http = HttpClient.newHttpClient()

    /* Pick the repositories whose description matches `desc` */
    val matching = http.listStagingRepos(user, pass).repositoriesSorted.filter { it.description?.startsWith(desc) ?: false }

    if (matching.isEmpty()) {
      logger.lifecycle("No repositories found with description “$desc”.")
      return@doLast
    } else {
      println("---------------- Found ${matching.size} repositories matching description “$desc” ---------------\n${matching.joinToString("\n")}")
    }

    var ok = 0
    var failed = 0

    matching.forEach { repo ->
      println("==== Processing Repo: ${repo.showName} ====")

      // Encode the key exactly like `jq -sRr @uri`
      val enc = repo.encodedKey
      val promoteRequest = HttpRequest.newBuilder()
        .uri(URI.create("https://ossrh-staging-api.central.sonatype.com/manual/upload/repository/$enc?publishing_type=user_managed"))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("Authorization", "Basic $auth")
        .POST(HttpRequest.BodyPublishers.ofString("{}"))
        .build()

      val promoteResp = http.send(promoteRequest, HttpResponse.BodyHandlers.ofString())
      if (promoteResp.statusCode() in 200..299) {
        println("--- Promoted staging repo ${repo.showName} ---")
        ok++
      } else {
        println("--- Failed to promote repo ${repo.showName} - HTTP Code ${promoteResp.statusCode()} ---\n${promoteResp.body()}")
        failed++
      }
    }
    println("==== Processing of Repos Completed ====")

    if (failed > 0) {
      throw GradleException("Some repositories failed to publish: $failed of ${matching.size}")
    } else {
      println("All $ok staging repositories successfully switched to user-managed.")
    }

    /* List the repos again with the deployment ID */
    val updated = http.listStagingRepos(user, pass).repositoriesSorted.filter { it.description?.startsWith(desc) ?: false }
    if (updated.isEmpty()) {
      logger.lifecycle("No repositories found with description “$desc”.")
    } else {
      println("---------------- Completed Repositories (${updated.size}): ----------------\n${updated.withIndex().map { (i, it) -> "${i}) ${it.showName}\n   - ${it.portal_deployment_id}\n" }.joinToString("\n")}")
    }
  }
}
