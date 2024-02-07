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

package util

import org.mockito.{ArgumentMatcher, ArgumentMatchers, Mockito}
import uk.gov.hmrc.http.HeaderCarrier

object MockitoPassByNameHelper {

  sealed trait Builder {
    def withByNameParamMatcher[P](matcher: => P): Builder
    def withParamMatcher[P](matcher: => P)(implicit mp: Manifest[P]): Builder
    def withByNameParam[P](expected: => P): Builder

    @deprecated("use the more general withParamMatcher(any[HeaderCarrier]) instead", "1.24.0")
    def withAnyHeaderCarrierParam(): Builder

    def verify(): Unit
  }

  case class PassByNameParam(clazz: Class[_], paramMatcher: Any)

  /**
   * Work around for the fact that Mockito can not handle Scala pass by name parameters
   * eg `debug(msg: => String)` - see
   * [[https://stackoverflow.com/questions/2152019/how-to-mock-a-method-with-functional-arguments-in-scala
    * solution takes inspiration form this Stack overflow]]. A builder pattern is used to reduce errors when specifying
   * method signatures and parameter values. Usage:
   * {{{
   *  PassByNameVerifier(mockDeclarationLogger, "error")
   *    .withByNameParam[String](s"Call to get api subscription fields failed. url=$expectedUrl")
   *    .withByNameParam[Throwable](caught)
   *    .verify()
   * }}}
   * There is also the ability to apply Mockito ArgumentMatchers to both normal and pass by name parameters:
   * {{{
   *  PassByNameVerifier(mockFoo, "someCall")
   *    .withParamMatcher(any[String])
   *    .withParamMatcher(ameq[String]("some value"))
   *    .withByNameParamMatcher(any[() => Unit])
   *    .verify()
   * }}}
   */
  case class PassByNameVerifier[T](mockedInstance: T, methodName: String, params: Seq[PassByNameParam] = Seq.empty, maybeOngoingVerification: Option[T] = None) extends Builder {

    def withByNameParamMatcher[P](matcher: => P): Builder = {
      initOngoingVerification.copy(params = this.params :+ PassByNameParam(classOf[() => P], matcher))
    }

    def withParamMatcher[P](matcher: => P)(implicit mp: Manifest[P]): Builder = {
      val c = mp.runtimeClass.asInstanceOf[Class[P]]
      initOngoingVerification.copy(params = this.params :+ PassByNameParam(c, matcher))
    }

    def withByNameParam[P](expected: => P): Builder = {
      initOngoingVerification.copy(params = this.params :+ PassByNameParam(classOf[() => P], ArgumentMatchers.argThat(byNameEqArgMatcher(() => expected))))
    }

    def withAnyHeaderCarrierParam(): Builder = {
      withParamMatcher(ArgumentMatchers.any[HeaderCarrier])
    }

    def verify(): Unit = {
      require(params.nonEmpty, "no parameters specified.")
      val method = mockedInstance.getClass.getMethod(methodName, params.map(param => param.clazz): _*)
      method.invoke(maybeOngoingVerification.get, params.map(param => param.paramMatcher.asInstanceOf[AnyRef]): _*)
    }

    private def initOngoingVerification: PassByNameVerifier[T] =
      if (maybeOngoingVerification.isEmpty) {
        this.copy(maybeOngoingVerification = Some(Mockito.verify(mockedInstance)))
      } else {
        this
      }

    private def byNameEqArgMatcher[P](compare: P) = new ArgumentMatcher[P] {
      override def matches(argument: P): Boolean =
        argument.asInstanceOf[() => P].apply() == compare.asInstanceOf[() => P].apply()
    }
  }
}
