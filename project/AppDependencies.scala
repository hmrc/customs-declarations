import sbt._

object AppDependencies {

  private val testScope = Test

  val playSuffix = "-play-30"

  val bootstrapVersion = "9.19.0"
  val hmrcMongoVersion = "2.11.0"
  val scalamockVersion = "7.4.0"

  val bootstrapBackendPlay30        = "uk.gov.hmrc"        %% s"bootstrap-backend$playSuffix" % bootstrapVersion
  val cats                          = "org.typelevel"      %% "cats-core"                     % "2.13.0"
  val hmrcMongo                     = "uk.gov.hmrc.mongo"  %% s"hmrc-mongo$playSuffix"        % hmrcMongoVersion

  val bootstrapBackendTestPlay30    = "uk.gov.hmrc"        %% s"bootstrap-test$playSuffix"    % bootstrapVersion % testScope
  val hmrcMongoTest                 = "uk.gov.hmrc.mongo"  %% s"hmrc-mongo-test$playSuffix"   % hmrcMongoVersion % testScope
  val scalamock                     = "org.scalamock"      %% "scalamock"                     % scalamockVersion % "test"
}
