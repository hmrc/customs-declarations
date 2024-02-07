import sbt._

object AppDependencies {

  private val testScope = "test,it"


  val bootstrapBackendPlay28 = "uk.gov.hmrc" %% "bootstrap-backend-play-28" % "7.15.0"
  val xmlResolver = "xml-resolver" % "xml-resolver" % "1.2"
  val cats = "org.typelevel" %% "cats-core" % "2.9.0"
  val pegdown = "org.pegdown" % "pegdown" % "1.6.0"
  val hmrcMongo                 = "uk.gov.hmrc.mongo"                          %% "hmrc-mongo-play-28"      % "1.3.0"

  val scalaTestPlusPlay         = "org.scalatestplus.play"                     %% "scalatestplus-play"      % "5.1.0"    % testScope
  val mockito                   = "org.scalatestplus"                          %% "mockito-3-4"             % "3.2.10.0" % testScope
  val wireMock                  = "com.github.tomakehurst"                     %  "wiremock-standalone"     % "2.27.2"   % testScope
  val hmrcMongoTest             = "uk.gov.hmrc.mongo"                          %% "hmrc-mongo-test-play-28" % "1.3.0"    % testScope
  val hmrcBootstrapTest         = "uk.gov.hmrc"                                %% "bootstrap-test-play-28"  % "7.15.0"   % testScope
  val scalaTestPlusMockito      = "org.scalatestplus"                          %% "mockito-4-2"             % "3.2.11.0" % testScope
  val flexmark = "com.vladsch.flexmark" % "flexmark-all" % "0.36.8" % testScope
  val jackson = "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.15.0" % testScope

}
