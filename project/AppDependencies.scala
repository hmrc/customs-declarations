import sbt._

object AppDependencies {

  private val hmrcTestVersion = "3.9.0-play-26"
  private val scalatestplusVersion = "3.1.3"
  private val mockitoVersion = "3.2.0"
  private val wireMockVersion = "2.25.1"
  private val customsApiCommonVersion = "1.45.0"
  private val circuitBreakerVersion = "3.5.0"
  private val playJsonJodaVersion = "2.8.1"
  private val simpleReactiveMongoVersion = "7.22.0-play-26"
  private val reactiveMongoTestVersion = "4.16.0-play-26"
  private val testScope = "test,it"

  val hmrcTest = "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % testScope

  val scalaTestPlusPlay = "org.scalatestplus.play" %% "scalatestplus-play" % scalatestplusVersion % testScope

  val wireMock = "com.github.tomakehurst" % "wiremock-jre8" % wireMockVersion % testScope

  val mockito =  "org.mockito" % "mockito-core" % mockitoVersion % testScope

  val customsApiCommon = "uk.gov.hmrc" %% "customs-api-common" % customsApiCommonVersion withSources()

  val customsApiCommonTests = "uk.gov.hmrc" %% "customs-api-common" % customsApiCommonVersion % testScope classifier "tests"

  val circuitBreaker = "uk.gov.hmrc" %% "reactive-circuit-breaker" % circuitBreakerVersion

  val  playJsonJoda = "com.typesafe.play" %% "play-json-joda" % playJsonJodaVersion
  
  val simpleReactiveMongo = "uk.gov.hmrc" %% "simple-reactivemongo" % simpleReactiveMongoVersion

  val reactiveMongoTest = "uk.gov.hmrc" %% "reactivemongo-test" % reactiveMongoTestVersion % testScope

}
