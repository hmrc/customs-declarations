import sbt._

object AppDependencies {

  private val testScope = "test,it"

  val playSuffix = "-play-30"

  val bootstrapVersion = "8.0.0"
  val hmrcMongoVersion = "1.7.0"
  val scalamockVersion = "5.2.0"

  val bootstrapBackendPlay30        = "uk.gov.hmrc"                                %% s"bootstrap-backend$playSuffix" % bootstrapVersion
  val cats                          = "org.typelevel"                              %% "cats-core"                    % "2.10.0"
  val hmrcMongo                     = "uk.gov.hmrc.mongo"                          %% s"hmrc-mongo$playSuffix"        % hmrcMongoVersion

  val bootstrapBackendTestPlay30    = "uk.gov.hmrc"                                %% s"bootstrap-test$playSuffix"    % bootstrapVersion                % testScope
  val hmrcMongoTest                 = "uk.gov.hmrc.mongo"                          %% s"hmrc-mongo-test$playSuffix"   % "1.8.0"                         % testScope
  val scalamock                     = "org.scalamock"                              %% "scalamock"                    % scalamockVersion                % "test"
}
