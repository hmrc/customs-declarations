/*
 * Copyright 2023 HM Revenue & Customs
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

import java.time.LocalDate
import org.mockito.ArgumentMatchers.{any, eq => ameq}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.auth.core.AuthProvider.{GovernmentGateway, PrivilegedApplication}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{AgentInformation, Credentials, EmptyRetrieval, ItmpAddress, ItmpName, LoginTimes, MdtpInformation, Name, Retrieval, ~}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.customs.declaration.model.Eori
import uk.gov.hmrc.http.HeaderCarrier
import util.TestData._
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.{ExecutionContext, Future}
import org.scalatest.matchers.should.Matchers

trait AuthConnectorStubbing extends AnyWordSpecLike with GuiceOneAppPerSuite with MockitoSugar with Matchers{
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  private val apiScope = "write:customs-declaration"
  private val customsEnrolmentName = "HMRC-CUS-ORG"
  private val eoriIdentifier = "EORINumber"
  private val cspAuthPredicate = Enrolment(apiScope) and AuthProviders(PrivilegedApplication)
  private val nonCspAuthPredicate = Enrolment(customsEnrolmentName) and AuthProviders(GovernmentGateway)

  def authoriseCsp(): Unit = {
    when(mockAuthConnector.authorise(ameq(cspAuthPredicate), ameq(nrsRetrievalData))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(TestData.nrsReturnData))
  }

  def authoriseCspButDontFetchRetrievals(): Unit = {
    when(mockAuthConnector.authorise(ameq(cspAuthPredicate), ameq(EmptyRetrieval))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(()))
  }

  def authoriseCspError(): Unit = {
    when(mockAuthConnector.authorise(ameq(cspAuthPredicate), ameq(nrsRetrievalData))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.failed(TestData.emulatedServiceFailure))
  }

  def authoriseCspErrorButDontFetchRetrievals(): Unit = {
    when(mockAuthConnector.authorise(ameq(cspAuthPredicate), ameq(EmptyRetrieval))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.failed(TestData.emulatedServiceFailure))
  }

  def unauthoriseCsp(authException: AuthorisationException = new InsufficientEnrolments): Unit = {
    when(mockAuthConnector.authorise(ameq(cspAuthPredicate), ameq(nrsRetrievalData))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.failed(authException))
  }

  type NrsDataType = Option[String] ~ Option[String] ~ Option[String] ~ Option[Credentials] ~ ConfidenceLevel ~ Option[String] ~ Option[String] ~ Option[Name] ~ Option[LocalDate] ~ Option[String] ~ AgentInformation ~ Option[String] ~ Option[CredentialRole] ~ Option[MdtpInformation] ~ Option[ItmpName] ~ Option[LocalDate] ~ Option[ItmpAddress] ~ Option[AffinityGroup] ~ Option[String] ~ LoginTimes
  type CspRetrievalDataType = Retrieval[NrsDataType]
  type NrsRetrievalDataTypeWithEnrolments = Retrieval[NrsDataType ~ Enrolments]


  def authoriseNonCsp(maybeEori: Option[Eori]): Unit = {
    unauthoriseCsp()
    val customsEnrolment = maybeEori.fold(ifEmpty = Enrolment(customsEnrolmentName)) { eori =>
      Enrolment(customsEnrolmentName).withIdentifier(eoriIdentifier, eori.value)
    }
    when(mockAuthConnector.authorise(ameq(nonCspAuthPredicate), ameq(nrsRetrievalData and Retrievals.authorisedEnrolments))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(new ~( nrsReturnData, Enrolments(Set(customsEnrolment)))))
  }

  def authoriseNonCspButDontRetrieveCustomsEnrolment(): Unit = {
    unauthoriseCsp()
    when(mockAuthConnector.authorise(ameq(nonCspAuthPredicate), ameq(nrsRetrievalData and Retrievals.authorisedEnrolments))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(new ~( nrsReturnData, Enrolments(Set.empty))))
  }

  def unauthoriseNonCspOnly(authException: AuthorisationException = new InsufficientEnrolments): Unit = {
    when(mockAuthConnector.authorise(ameq(nonCspAuthPredicate), ameq(nrsRetrievalData and Retrievals.authorisedEnrolments))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.failed(authException))
  }

  def authoriseNonCspOnlyError(): Unit = {
    when(mockAuthConnector.authorise(ameq(nonCspAuthPredicate), ameq(nrsRetrievalData and Retrievals.authorisedEnrolments))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.failed(TestData.emulatedServiceFailure))
  }


  def verifyCspAuthorisationCalled(numberOfTimes: Int): Future[NrsDataType] = {
    verify(mockAuthConnector, times(numberOfTimes))
      .authorise(ameq(cspAuthPredicate), ameq(nrsRetrievalData))(any[HeaderCarrier], any[ExecutionContext])
  }

  def verifyNonCspAuthorisationCalled(numberOfTimes: Int): Future[NrsDataType ~ Enrolments] = {
    verify(mockAuthConnector, times(numberOfTimes))
      .authorise(ameq(nonCspAuthPredicate), ameq(nrsRetrievalData and Retrievals.authorisedEnrolments))(any[HeaderCarrier], any[ExecutionContext])
  }

  def verifyNonCspAuthorisationNotCalled: Future[NrsDataType ~ Enrolments] = verifyNonCspAuthorisationCalled(0)

}
