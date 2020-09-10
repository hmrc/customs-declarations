/*
 * Copyright 2020 HM Revenue & Customs
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
import uk.gov.hmrc.customs.declaration.model.{ApiVersion, VersionOne, VersionThree, VersionTwo}
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{ApiVersionRequest, ConversationIdRequest}
import uk.gov.hmrc.http.HttpErrorFunctions

import scala.concurrent.{ExecutionContext, Future}

/** Action builder that validates headers.
  * <ol>
  * <li/>Input - `ConversationIdRequest`
  * <li/>Output - `ApiVersionRequest`
  * <li/>Error - 503 Result if requested version is shuttered, or version unknown and any version is shuttered. This terminates the action builder pipeline.
  * </ol>
  */
@Singleton
class ShutterCheckAction @Inject()(logger: DeclarationsLogger)
                                  (implicit ec: ExecutionContext)
  extends ActionRefiner[ConversationIdRequest, ApiVersionRequest] with HttpErrorFunctions {
    actionName =>

  private val errorResponseVersionShuttered: Result = ErrorResponse(SERVICE_UNAVAILABLE, "SERVER_ERROR", "The 'customs/declarations' API is currently unavailable").XmlResult

  protected val versionsByAcceptHeader: Map[String, ApiVersion] = Map(
    "application/vnd.hmrc.1.0+xml" -> VersionOne,
    "application/vnd.hmrc.2.0+xml" -> VersionTwo,
    "application/vnd.hmrc.3.0+xml" -> VersionThree
  )
  
  //TODO load from config
  private lazy val v1Shuttered = true
  private lazy val v2Shuttered = false
  private lazy val v3Shuttered = false
  override def executionContext: ExecutionContext = ec
  override def refine[A](cr: ConversationIdRequest[A]): Future[Either[Result, ApiVersionRequest[A]]] = Future.successful {
    implicit val id: ConversationIdRequest[A] = cr
    versionShuttered()
  }

  //TODO optimize/simplify
  def versionShuttered[A]()(implicit conversationIdRequest: ConversationIdRequest[A]): Either[Result, ApiVersionRequest[A]] = {
    val anyVersionShuttered = v1Shuttered || v2Shuttered || v3Shuttered

    conversationIdRequest.request.headers.get(ACCEPT) match {
      case None =>
        if (anyVersionShuttered) {
          logger.errorWithoutRequestContext(s"Error - header '$ACCEPT' not present and a version is shuttered, returning unavailable error")
          Left(errorResponseVersionShuttered)
        } else {
          logger.errorWithoutRequestContext(s"Error - header '$ACCEPT' not present")
          Left(ErrorAcceptHeaderInvalid.XmlResult.withConversationId)
        }
      case Some(v) =>
        if (!versionsByAcceptHeader.keySet.contains(v)) {
          if (anyVersionShuttered) {
            logger.errorWithoutRequestContext(s"Error - header '$ACCEPT' value '$v' is not valid")
            Left(errorResponseVersionShuttered)
          } else {
            logger.errorWithoutRequestContext(s"Error - header '$ACCEPT' value '$v' is not valid")
            Left(ErrorAcceptHeaderInvalid.XmlResult.withConversationId)
          }
        } else {
          val apiVersion: ApiVersion = versionsByAcceptHeader(v)
          apiVersion match {
            case VersionOne if v1Shuttered =>
              Left(errorResponseVersionShuttered)
            case VersionTwo if v2Shuttered =>
              Left(errorResponseVersionShuttered)
            case VersionThree if v3Shuttered =>
              Left(errorResponseVersionShuttered)
            case _ =>
              Right(ApiVersionRequest(conversationIdRequest.conversationId, conversationIdRequest.start, apiVersion, conversationIdRequest.request))
          }
        }
    }
  }
}
