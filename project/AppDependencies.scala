import sbt._

object AppDependencies {

  private val testScope = "test,it"


  val bootstrapBackendPlay28    = "uk.gov.hmrc"                                %% "bootstrap-backend-play-30" % "8.5.0"
  val xmlResolver               = "xml-resolver"                               % "xml-resolver"               % "1.2"
  val cats                      = "org.typelevel"                              %% "cats-core"                 % "2.10.0"
  val pegdown                   = "org.pegdown"                                % "pegdown"                    % "1.6.0"
  val hmrcMongo                 = "uk.gov.hmrc.mongo"                          %% "hmrc-mongo-play-30"        % "1.8.0"

  val scalaTestPlusPlay         = "org.scalatestplus.play"                     %% "scalatestplus-play"        % "7.0.1"    % testScope
  val mockito                   = "org.mockito"                                %% "mockito-scala-scalatest"   % "1.17.29"  % testScope
  val wireMock                  = "org.wiremock"                               %  "wiremock-standalone"       % "3.5.2"    % testScope
  val hmrcMongoTest             = "uk.gov.hmrc.mongo"                          %% "hmrc-mongo-test-play-30"   % "1.7.0"    % testScope
  val hmrcBootstrapTest         = "uk.gov.hmrc"                                %% "bootstrap-test-play-30"    % "8.5.0"    % testScope
  val scalaTestPlusMockito      = "org.scalatestplus"                          %% "scalatestplus-mockito"     % "1.0.0-M2" % testScope
  val flexmark                   = "com.vladsch.flexmark"                        % "flexmark-all"                % "0.36.8"   % testScope
  val jackson                   = "com.fasterxml.jackson.module"               %% "jackson-module-scala"      % "2.15.0"   % testScope

}
