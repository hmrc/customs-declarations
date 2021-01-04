/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.customs.declaration.controllers.actionbuilders

import javax.inject.{Inject, Singleton}
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status.SERVICE_UNAVAILABLE
import play.api.mvc.{ActionRefiner, _}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.ErrorAcceptHeaderInvalid
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{ApiVersionRequest, ConversationIdRequest}
import uk.gov.hmrc.customs.declaration.model.{ApiVersion, VersionOne, VersionThree, VersionTwo}
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService

import scala.concurrent.{ExecutionContext, Future}

/** Action builder that validates headers.
  * <ol>
  * <li/>Input - `ConversationIdRequest`
  * <li/>Output - `ApiVersionRequest`
  * <li/>Error - If Accept header is missing or invalid then return a 406. If requested version is shuttered then return a 503. This terminates the action builder pipeline.
  * </ol>
  */
@Singleton
class ShutterCheckAction @Inject()(logger: DeclarationsLogger,
                                   config: DeclarationsConfigService)
                                  (implicit ec: ExecutionContext)
  extends ActionRefiner[ConversationIdRequest, ApiVersionRequest] {
    actionName =>

    private val errorResponseVersionShuttered: Result = ErrorResponse(SERVICE_UNAVAILABLE, "SERVER_ERROR", "Service Unavailable").XmlResult

    private lazy val v1Shuttered: Boolean = config.declarationsShutterConfig.v1Shuttered.getOrElse(false)
    private lazy val v2Shuttered: Boolean = config.declarationsShutterConfig.v2Shuttered.getOrElse(false)
    private lazy val v3Shuttered: Boolean = config.declarationsShutterConfig.v3Shuttered.getOrElse(false)

    protected val versionsByAcceptHeader: Map[String, ApiVersion] = Map(
      "application/vnd.hmrc.1.0+xml" -> VersionOne,
      "application/vnd.hmrc.2.0+xml" -> VersionTwo,
      "application/vnd.hmrc.3.0+xml" -> VersionThree
    )

    override def executionContext: ExecutionContext = ec

    override def refine[A](cir: ConversationIdRequest[A]): Future[Either[Result, ApiVersionRequest[A]]] = Future.successful {
     implicit val id: ConversationIdRequest[A] = cir
     val acceptErrorResult = Left(ErrorAcceptHeaderInvalid.XmlResult.withConversationId)

     cir.request.headers.get(ACCEPT) match {
       case None =>
         logger.error(s"Error - header '$ACCEPT' not present")
         acceptErrorResult
       case Some(v) =>
         if (!versionsByAcceptHeader.keySet.contains(v)) {
           logger.error(s"Error - header '$ACCEPT' value '$v' is not valid")
           acceptErrorResult
         } else {
           val apiVersion: ApiVersion = versionsByAcceptHeader(v)
           versionShuttered(apiVersion)
         }
     }
  }

  private def versionShuttered[A](apiVersion: ApiVersion)(implicit cir: ConversationIdRequest[A]): Either[Result, ApiVersionRequest[A]] = {

    val serviceUnavailableResult = Left(errorResponseVersionShuttered)

    def unavailableWithLog(apiVersion: ApiVersion) = {
      logger.warn(s"version ${apiVersion.toString} requested but is shuttered")
      serviceUnavailableResult
    }

    apiVersion match {
      case VersionOne if v1Shuttered =>
        unavailableWithLog(VersionOne)
      case VersionTwo if v2Shuttered =>
        unavailableWithLog(VersionTwo)
      case VersionThree if v3Shuttered =>
        unavailableWithLog(VersionThree)
      case _ =>
        logger.debug(s"$ACCEPT header passed validation with: $apiVersion")
        Right(ApiVersionRequest(cir.conversationId, cir.start, apiVersion, cir.request))
    }
  }

}
