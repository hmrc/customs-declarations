/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.customs.declaration.controllers

import javax.inject.{Inject, Singleton}
import play.api.http.MimeTypes
import play.api.mvc._
import uk.gov.hmrc.customs.declaration.connectors.CustomsDeclarationsMetricsConnector
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders._
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.CustomsDeclarationsMetricsRequest
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.services.{CancellationDeclarationSubmissionService, DeclarationService, StandardDeclarationSubmissionService}
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext

@Singleton
class Common @Inject() (
  val authAction: AuthAction,
  val validateAndExtractHeadersAction: ValidateAndExtractHeadersAction,
  val logger: DeclarationsLogger
)

@Singleton
class CommonSubmitterHeader @Inject()(
  override val authAction: AuthActionSubmitterHeader,
  validateAndExtractHeadersAction: ValidateAndExtractHeadersAction,
  logger: DeclarationsLogger
) extends Common(authAction, validateAndExtractHeadersAction, logger)

@Singleton
class SubmitDeclarationController @Inject()(common: CommonSubmitterHeader,
                                            businessService: StandardDeclarationSubmissionService,
                                            payloadValidationAction: SubmitPayloadValidationAction,
                                            conversationIdAction: ConversationIdAction,
                                            metricsConnector: CustomsDeclarationsMetricsConnector)
                                           (implicit ec: ExecutionContext)
  extends CustomsDeclarationController(common, businessService, payloadValidationAction, conversationIdAction, Some(metricsConnector))

@Singleton
class CancelDeclarationController @Inject()(common: CommonSubmitterHeader,
                                            businessService: CancellationDeclarationSubmissionService,
                                            payloadValidationAction: CancelPayloadValidationAction,
                                            conversationIdAction: ConversationIdAction)
                                           (implicit ec: ExecutionContext)
  extends CustomsDeclarationController(common, businessService, payloadValidationAction, conversationIdAction)

@Singleton
class ClearanceDeclarationController @Inject()(common: Common,
                                               businessService: StandardDeclarationSubmissionService,
                                               payloadValidationAction: ClearancePayloadValidationAction,
                                               conversationIdAction: ConversationIdAction)
                                              (implicit ec: ExecutionContext)
  extends CustomsDeclarationController(common, businessService, payloadValidationAction, conversationIdAction)

@Singleton
class AmendDeclarationController @Inject()(common: CommonSubmitterHeader,
                                           businessService: StandardDeclarationSubmissionService,
                                           payloadValidationAction: AmendPayloadValidationAction,
                                           conversationIdAction: ConversationIdAction)
                                          (implicit ec: ExecutionContext)
  extends CustomsDeclarationController(common, businessService, payloadValidationAction, conversationIdAction)

@Singleton
class ArrivalNotificationDeclarationController @Inject()(common: Common,
                                                         businessService: StandardDeclarationSubmissionService,
                                                         payloadValidationAction: ArrivalNotificationPayloadValidationAction,
                                                         conversationIdAction: ConversationIdAction)
                                                        (implicit ec: ExecutionContext)
  extends CustomsDeclarationController(common, businessService, payloadValidationAction, conversationIdAction)

abstract class CustomsDeclarationController(val common: Common,
                                            val businessService: DeclarationService,
                                            val payloadValidationAction: PayloadValidationAction,
                                            val conversationIdAction: ConversationIdAction,
                                            val maybeMetricsConnector: Option[CustomsDeclarationsMetricsConnector] = None)
                                           (implicit ec: ExecutionContext)
  extends BaseController {

  private def xmlOrEmptyBody: BodyParser[AnyContent] = BodyParser(rq => parse.xml(rq).map {
    case Right(xml) =>
      Right(AnyContentAsXml(xml))
    case _ =>
      Right(AnyContentAsEmpty)
  })

  def post(): Action[AnyContent] = (
    Action andThen
      conversationIdAction andThen
      common.validateAndExtractHeadersAction andThen
      common.authAction andThen
      payloadValidationAction
    )
    .async(bodyParser = xmlOrEmptyBody) {

      implicit vpr: ValidatedPayloadRequest[AnyContent] =>
        val logger = common.logger

        logger.debug(s"Request received. Payload = ${vpr.body.toString} headers = ${vpr.headers.headers}")

        businessService.send map {
          case Right(maybeNrSubmissionId) =>
            logger.info("Declaration request processed successfully")
            maybeMetricsConnector.map{conn =>
              conn.post(CustomsDeclarationsMetricsRequest(
                "DECLARATION", vpr.conversationId, vpr.start, conversationIdAction.timeService.zonedDateTimeUtc))
            }
            maybeNrSubmissionId match {
              case Some(nrSubmissionId) => Accepted.as(MimeTypes.XML).withConversationId.withNrSubmissionId(nrSubmissionId)
              case None => Accepted.as(MimeTypes.XML).withConversationId
            }
          case Left(errorResult) =>
            errorResult
        }
    }

}
