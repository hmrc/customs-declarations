import sbt._

object AppDependencies {

  private val scalatestplusVersion = "4.0.3"
  private val mockitoVersion = "3.5.9"
  private val wireMockVersion = "2.27.2"
  private val customsApiCommonVersion = "1.54.0"
  private val playJsonJodaVersion = "2.8.1"
  private val simpleReactiveMongoVersion = "7.30.0-play-27"
  private val reactiveMongoTestVersion = "4.21.0-play-27"
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
