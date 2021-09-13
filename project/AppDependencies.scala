import sbt._

object AppDependencies {

  private val scalatestplusVersion = "5.1.0"
  private val customsApiCommonVersion = "1.57.0"
  private val playJsonJodaVersion = "2.9.2"
  private val simpleReactiveMongoVersion = "8.0.0-play-28"
  private val reactiveMongoTestVersion = "5.0.0-play-28"
  private val testScope = "test,it"

  val scalaTestPlusPlay = "org.scalatestplus.play" %% "scalatestplus-play" % scalatestplusVersion % testScope

  val flexmark = "com.vladsch.flexmark" % "flexmark-all" % "0.35.10"  % testScope

  val mockito = "org.scalatestplus" %% "mockito-3-4" % "3.2.9.0" % testScope

  val wireMock = "com.github.tomakehurst" % "wiremock-standalone" % "2.27.1" % testScope

  val customsApiCommon = "uk.gov.hmrc" %% "customs-api-common" % customsApiCommonVersion withSources()

  val customsApiCommonTests = "uk.gov.hmrc" %% "customs-api-common" % customsApiCommonVersion % testScope classifier "tests"

  val playJsonJoda = "com.typesafe.play" %% "play-json-joda" % playJsonJodaVersion
  
  val simpleReactiveMongo = "uk.gov.hmrc" %% "simple-reactivemongo" % simpleReactiveMongoVersion

  val reactiveMongoTest = "uk.gov.hmrc" %% "reactivemongo-test" % reactiveMongoTestVersion % testScope

  val silencerPlugin = compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.5" cross CrossVersion.full)
  val silencerLib = "com.github.ghik" % "silencer-lib" % "1.7.5" % Provided cross CrossVersion.full

}
