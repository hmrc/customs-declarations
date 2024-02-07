/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.customs.declaration.config

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.implicits._
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration

/**
  * <p>
  * We need a wrapper for ServicesConfig as the current API rely's on throwing exceptions for signalling failure -
  * this is fail fast behaviour. What we want is to aggregate errors (on application startup).
  * The `ValidatedNel` class in the `cats` library gives a type that allows us aggregate errors OR apply valid results
  * to a function (such as a case class constructor).
  * It's declaration is `type ValidatedNel[+E, +A] = Validated[NonEmptyList[E], A]`
  * `Nel` is short for `NonEmptyList` which is simply a `List` that is guaranteed to contain at least 1 item.
  * </p>
  * Example usage:
  *
  * {{{
  * // you will need this import for mapN
  * import cats.implicits._
  *
  * @Singleton
  * class CustomsConfigService @Inject()(configValidatedNel: ConfigValidatedNelAdaptor) {
  *   private val root = configValidatedNel.root
  *   private val validatedCustomsEnrolmentConfig: ValidatedNel[String, CustomsEnrolmentConfig] = (
  *     root.string("customs.enrolment.name"),
  *     root.string("customs.enrolment.eori-identifier")
  *   ).mapN(CustomsEnrolmentConfig)
  * ...
  * }
  * }}}
  *
  * Note that the `ValidatedNel` behaviour is derived from it being an Applicative Functor
  * (see https://typelevel.org/cats/typeclasses/applicative.html)
  *
  * @param servicesConfig HMRC services config lib class
  */
@Singleton
class ConfigValidatedNelAdaptor @Inject()(servicesConfig: ServicesConfig, configuration: Configuration) {

  trait ValidatedNelAdaptor {
    def string(key: String): CustomsValidatedNel[String]
    def int(key: String): CustomsValidatedNel[Int]
    def boolean(key: String): CustomsValidatedNel[Boolean]
    def duration(key: String): CustomsValidatedNel[Duration]
  }

  trait RootValidatedNelAdaptor extends ValidatedNelAdaptor {
    def maybeString(key: String): CustomsValidatedNel[Option[String]]
    def maybeBoolean(key: String): CustomsValidatedNel[Option[Boolean]]
    def stringSeq(key: String): CustomsValidatedNel[Seq[String]]
  }

  trait UrlNelAdaptor {
    def baseUrl: CustomsValidatedNel[String]
    def serviceUrl: CustomsValidatedNel[String]
  }

  def root: RootValidatedNelAdaptor = RootConfigReader

  def service(serviceName: String): ValidatedNelAdaptor with UrlNelAdaptor = ServiceConfigReader(serviceName)

  private object RootConfigReader extends RootValidatedNelAdaptor {

    override def string(key: String): CustomsValidatedNel[String] =
      validatedNel(servicesConfig.getString(key))

    override def int(key: String): CustomsValidatedNel[Int] =
      validatedNel(servicesConfig.getInt(key))

    override def boolean(key: String): CustomsValidatedNel[Boolean] =
      validatedNel(servicesConfig.getBoolean(key))

    override def duration(key: String): CustomsValidatedNel[Duration] =
      validatedNel(servicesConfig.getDuration(key))

    def maybeString(key: String): CustomsValidatedNel[Option[String]] = {
      Valid(configuration.getOptional[String](key))
    }

    def maybeBoolean(key: String): CustomsValidatedNel[Option[Boolean]] = {
      Valid(configuration.getOptional[Boolean](key))
    }

    override def stringSeq(key: String): CustomsValidatedNel[Seq[String]] = {
      val seq: Seq[String] = configuration.getOptional[Seq[String]](key).getOrElse(Nil)
      Valid(seq)
    }
  }

  private def validatedNel[T](f: => T): CustomsValidatedNel[T] = {
    Validated.catchNonFatal(f).leftMap[String](e => e.getMessage).toValidatedNel
  }

  private case class ServiceConfigReader(serviceName: String) extends ValidatedNelAdaptor with UrlNelAdaptor {

    override def string(key: String): CustomsValidatedNel[String] =
      validatedNel(readConfig(key, servicesConfig.getConfString))

    override def int(key: String): CustomsValidatedNel[Int] =
      validatedNel(readConfig(key, servicesConfig.getConfInt))

    override def boolean(key: String): CustomsValidatedNel[Boolean] =
      validatedNel(readConfig(key, servicesConfig.getConfBool))

    override def duration(key: String): CustomsValidatedNel[Duration] =
      validatedNel(readConfig(key, servicesConfig.getConfDuration))

    override def baseUrl: CustomsValidatedNel[String] =
      validatedNel(servicesConfig.baseUrl(serviceName))

    override def serviceUrl: CustomsValidatedNel[String] = {
      def url(base: String, context: String) = s"$base$context"

      val contextNel: CustomsValidatedNel[String] = string("context") andThen { context =>
        if (context.startsWith("/")) Valid(context).toValidatedNel else Invalid(s"For service '$serviceName' context '$context' does not start with '/'").toValidatedNel
      }

      (baseUrl, contextNel).mapN(url)
    }

    private def readConfig[T](key: String, f: (String, => T) => T) = {
      val serviceKey = serviceName + "."  + key
      f(serviceKey, throw new IllegalStateException(s"Service configuration not found for key: $serviceKey"))
    }
  }

}
