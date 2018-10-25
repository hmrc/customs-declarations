/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.customs.declaration.services

import javax.inject.{Inject, Singleton}
import org.joda.time.LocalDate
import play.api.http.Status
import play.api.http.Status.UNAUTHORIZED
import uk.gov.hmrc.auth.core.AuthProvider.{GovernmentGateway, PrivilegedApplication}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorInternalServerError, UnauthorizedCode}
import uk.gov.hmrc.customs.declaration.connectors.GoogleAnalyticsConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{HasAnalyticsValues, HasConversationId}
import uk.gov.hmrc.customs.declaration.model.{Eori, NonCsp, NrsRetrievalData}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Left
import scala.util.control.NonFatal

@Singleton
class CustomsAuthService @Inject()(override val authConnector: AuthConnector,
                                   googleAnalyticsConnector: GoogleAnalyticsConnector,
                                   logger: DeclarationsLogger) extends AuthorisedFunctions {

  private val hmrcCustomsEnrolment = "HMRC-CUS-ORG"

  private type NrsRetrievalDataType = Option[String] ~ Option[String] ~ Option[String] ~ Credentials ~ ConfidenceLevel ~ Option[String] ~
    Option[String] ~ Name ~ Option[LocalDate] ~ Option[String] ~ AgentInformation ~ Option[String] ~
    Option[CredentialRole] ~ Option[MdtpInformation] ~ ItmpName ~ Option[LocalDate] ~ ItmpAddress ~
    Option[AffinityGroup] ~ Option[String] ~ LoginTimes

  private type CspRetrievalDataType = Retrieval[NrsRetrievalDataType]
  private type NonCspRetrievalDataType = Retrieval[NrsRetrievalDataType ~ Enrolments]

  private val cspRetrievals: CspRetrievalDataType =
    Retrievals.internalId and Retrievals.externalId and Retrievals.agentCode and
      Retrievals.credentials and Retrievals.confidenceLevel and Retrievals.nino and
      Retrievals.saUtr and Retrievals.name and Retrievals.dateOfBirth and
      Retrievals.email and Retrievals.agentInformation and Retrievals.groupIdentifier and
      Retrievals.credentialRole and Retrievals.mdtpInformation and Retrievals.itmpName and
      Retrievals.itmpDateOfBirth and Retrievals.itmpAddress and Retrievals.affinityGroup and
      Retrievals.credentialStrength and Retrievals.loginTimes

  private val nonCspRetrievals: NonCspRetrievalDataType = cspRetrievals and Retrievals.authorisedEnrolments

  private lazy val errorResponseEoriNotFoundInCustomsEnrolment =
    ErrorResponse(UNAUTHORIZED, UnauthorizedCode, "EORI number not found in Customs Enrolment")
  private val errorResponseUnauthorisedGeneral =
    ErrorResponse(Status.UNAUTHORIZED, UnauthorizedCode, "Unauthorised request")

  type IsCsp = Boolean


  /*
  Wrapper around HMRC authentication library authorised function for CSP authentication
  */
  def authAsCsp(isNrs: Boolean)(implicit vhr: HasConversationId with HasAnalyticsValues, hc: HeaderCarrier): Future[Either[ErrorResponse, (IsCsp, Option[NrsRetrievalData])]] = {
    val eventualAuth: Future[Either[ErrorResponse, (IsCsp, Option[NrsRetrievalData])]] =
      if (isNrs) {
        authorised(Enrolment("write:customs-declaration") and AuthProviders(PrivilegedApplication)).retrieve(cspRetrievals) {
          case internalId ~ externalId ~ agentCode ~ credentials ~ confidenceLevel ~ nino ~ saUtr ~ name ~ dateOfBirth ~ email ~ agentInformation ~ groupIdentifier ~
            credentialRole ~ mdtpInformation ~ itmpName ~ itmpDateOfBirth ~ itmpAddress ~ affinityGroup ~ credentialStrength ~ loginTimes =>
            Future.successful {
              val retrievalData = NrsRetrievalData(internalId, externalId, agentCode, credentials, confidenceLevel, nino, saUtr,
                name, dateOfBirth, email, agentInformation, groupIdentifier, credentialRole, mdtpInformation, itmpName,
                itmpDateOfBirth, itmpAddress, affinityGroup, credentialStrength, loginTimes)

              Right((true, Some(retrievalData)))
            }
        }
      }
      else {
        authorised(Enrolment("write:customs-declaration") and AuthProviders(PrivilegedApplication)) {
          Future.successful[Either[ErrorResponse, (IsCsp, Option[NrsRetrievalData])]] {
            Right((true, None))
          }
        }
      }

    eventualAuth.recover{
      case NonFatal(_: AuthorisationException) =>
        logger.debug("Not authorised as CSP")
        Right((false, None))
      case NonFatal(e) =>
        logger.error("Error authorising CSP", e)
        Left(ErrorInternalServerError)
    }
  }

  /*
    Wrapper around HMRC authentication library authorised function for NON CSP authentication
    */
  def authAsNonCsp(isNrs: Boolean)(implicit vhr: HasConversationId with HasAnalyticsValues, hc: HeaderCarrier): Future[Either[ErrorResponse, NonCsp]] = {
    val eventualAuth: Future[(Enrolments, Option[NrsRetrievalData])] =
      if (isNrs) {
        authorised(Enrolment(hmrcCustomsEnrolment) and AuthProviders(GovernmentGateway)).retrieve(nonCspRetrievals) {
          case internalId ~ externalId ~ agentCode ~ credentials ~ confidenceLevel ~ nino ~ saUtr ~ name ~ dateOfBirth ~ email ~ agentInformation ~ groupIdentifier ~
            credentialRole ~ mdtpInformation ~ itmpName ~ itmpDateOfBirth ~ itmpAddress ~ affinityGroup ~ credentialStrength ~ loginTimes ~ authorisedEnrolments =>

            val retrievalData = NrsRetrievalData(internalId, externalId, agentCode, credentials, confidenceLevel, nino, saUtr,
              name, dateOfBirth, email, agentInformation, groupIdentifier, credentialRole, mdtpInformation, itmpName,
              itmpDateOfBirth, itmpAddress, affinityGroup, credentialStrength, loginTimes)

            Future.successful((authorisedEnrolments, Some(retrievalData)))
        }
      } else {
        authorised(Enrolment(hmrcCustomsEnrolment) and AuthProviders(GovernmentGateway)).retrieve(Retrievals.authorisedEnrolments) {
          enrolments =>
            Future.successful((enrolments, None))
        }
      }

    eventualAuth.map{ enrolmentsAndMaybeNrsData =>
      val enrolments: Enrolments = enrolmentsAndMaybeNrsData._1
      val maybeNrsData: Option[NrsRetrievalData] = enrolmentsAndMaybeNrsData._2
      val maybeEori: Option[Eori] = findEoriInCustomsEnrolment(enrolments, hc.authorization)
      logger.debug(s"EORI from Customs enrolment for non-CSP request: $maybeEori")
      maybeEori.fold[Either[ErrorResponse, NonCsp]]{
        googleAnalyticsConnector.failure(errorResponseEoriNotFoundInCustomsEnrolment.message)
        Left(errorResponseEoriNotFoundInCustomsEnrolment)
      }{ eori =>
        logger.debug("Authorising as non-CSP")
        Right(NonCsp(eori, maybeNrsData))
      }
    }.recover{
      case NonFatal(_: AuthorisationException) =>
        googleAnalyticsConnector.failure(errorResponseUnauthorisedGeneral.message)
        Left(errorResponseUnauthorisedGeneral)
      case NonFatal(e) =>
        logger.error("Error authorising non-CSP", e)
        Left(ErrorInternalServerError)
    }
  }

  private def findEoriInCustomsEnrolment[A](enrolments: Enrolments, authHeader: Option[Authorization])(implicit vhr: HasConversationId with HasAnalyticsValues, hc: HeaderCarrier): Option[Eori] = {
    val maybeCustomsEnrolment = enrolments.getEnrolment(hmrcCustomsEnrolment)
    if (maybeCustomsEnrolment.isEmpty) {
      logger.warn(s"Customs enrolment $hmrcCustomsEnrolment not retrieved for authorised non-CSP call")
    }
    for {
      customsEnrolment <- maybeCustomsEnrolment
      eoriIdentifier <- customsEnrolment.getIdentifier("EORINumber")
    } yield Eori(eoriIdentifier.value)
  }

}
