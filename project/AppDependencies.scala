import sbt._

object AppDependencies {

  val bootstrapPlay25Version = "1.5.0"
  val hmrcTestVersion = "3.0.0"
  val scalaTestVersion = "3.0.4"
  val scalatestplusVersion = "2.0.1"
  val mockitoVersion = "2.6.2"
  val pegdownVersion = "1.6.0"
  val wireMockVersion = "2.10.1"
  val customsApiCommonVersion = "1.25.0"
  val circuitBreakerVersion = "3.2.0"
  val testScope = "test,it"

  val xmlResolver = "xml-resolver" % "xml-resolver" % "1.2"

  val bootstrapPlay25 = "uk.gov.hmrc" %% "bootstrap-play-25" % bootstrapPlay25Version

  val hmrcTest = "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % testScope

  val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion % testScope

  val pegDown = "org.pegdown" % "pegdown" % pegdownVersion % testScope

  val scalaTestPlusPlay = "org.scalatestplus.play" %% "scalatestplus-play" % scalatestplusVersion % testScope

  val wireMock = "com.github.tomakehurst" % "wiremock" % wireMockVersion % testScope exclude("org.apache.httpcomponents","httpclient") exclude("org.apache.httpcomponents","httpcore")

  val mockito =  "org.mockito" % "mockito-core" % mockitoVersion % testScope

  val customsApiCommon = "uk.gov.hmrc" %% "customs-api-common" % customsApiCommonVersion withSources()

  val customsApiCommonTests = "uk.gov.hmrc" %% "customs-api-common" % customsApiCommonVersion % testScope classifier "tests"

  val circuitBreaker = "uk.gov.hmrc" %% "reactive-circuit-breaker" % circuitBreakerVersion
}
