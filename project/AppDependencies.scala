import sbt._

object AppDependencies {

  private val scalatestplusVersion = "3.1.3"
  private val mockitoVersion = "3.3.3"
  private val wireMockVersion = "2.26.3"
  private val customsApiCommonVersion = "1.50.0"
  private val playJsonJodaVersion = "2.8.1"
  private val simpleReactiveMongoVersion = "7.26.0-play-26"
  private val reactiveMongoTestVersion = "4.16.0-play-26"
  private val testScope = "test,it"

  val scalaTestPlusPlay = "org.scalatestplus.play" %% "scalatestplus-play" % scalatestplusVersion % testScope

  val wireMock = "com.github.tomakehurst" % "wiremock-jre8" % wireMockVersion % testScope

  val mockito = "org.mockito" % "mockito-core" % mockitoVersion % testScope

  val customsApiCommon = "uk.gov.hmrc" %% "customs-api-common" % customsApiCommonVersion withSources()

  val customsApiCommonTests = "uk.gov.hmrc" %% "customs-api-common" % customsApiCommonVersion % testScope classifier "tests"

  val playJsonJoda = "com.typesafe.play" %% "play-json-joda" % playJsonJodaVersion
  
  val simpleReactiveMongo = "uk.gov.hmrc" %% "simple-reactivemongo" % simpleReactiveMongoVersion

  val reactiveMongoTest = "uk.gov.hmrc" %% "reactivemongo-test" % reactiveMongoTestVersion % testScope
}
