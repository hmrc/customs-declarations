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

package uk.gov.hmrc.customs.declaration.poc

import java.util.UUID

import akka.stream.Materializer
import com.kenshoo.play.metrics.MetricsFilter
import javax.inject.{Inject, Singleton}
import play.api.http.{DefaultHttpFilters, Status}
import play.api.mvc.{Filter, RequestHeader, Result}
import uk.gov.hmrc.customs.declaration.connectors.GoogleAnalyticsConnector
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.GoogleAnalyticsValues.HasAnalyticsValues
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId
import uk.gov.hmrc.customs.declaration.model.{ConversationId, GoogleAnalyticsValues}
import uk.gov.hmrc.play.bootstrap.filters.{AuditFilter, CacheControlFilter, LoggingFilter}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


//TODO MC unit test missing for this (component tests passed)
@Singleton
class GoogleAnalyticsFilter @Inject()(logger: DeclarationsLogger, googleAnalyticsConnector: GoogleAnalyticsConnector)(implicit val mat: Materializer) extends Filter {

  private val endpointMapping = Map("/" -> GoogleAnalyticsValues.Submit,
    "/cancellation-requests" -> GoogleAnalyticsValues.Cancel,
    "/file-upload" -> GoogleAnalyticsValues.Fileupload,
    "/clearance" -> GoogleAnalyticsValues.Clearance
  )


  override def apply(nextFilter: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {

    val eventualResult = nextFilter.apply(rh)


    debugLog(s"in GoogleAnalyticsFilter", None)

    val maybeAnalyticValues = endpointMapping.get(rh.uri)

    debugLog(s"Analytic values: $maybeAnalyticValues", None)


    if (maybeAnalyticValues.isDefined) {

      eventualResult.map { result =>

        val maybeHasConversationId: Option[HasConversationId] = result.header.headers.get(CustomHeaderNames.XConversationIdHeaderName).map(cid => new HasConversationId {
          override val conversationId: ConversationId = ConversationId(UUID.fromString(cid))
        })
        implicit val data = new HasConversationId with HasAnalyticsValues {
          override val conversationId: ConversationId = maybeHasConversationId.get.conversationId
          override val analyticsValues: GoogleAnalyticsValues = maybeAnalyticValues.get
        }

        result.header.status match {
          case stat: Int if stat == Status.ACCEPTED || stat == Status.OK =>
            googleAnalyticsConnector.success
          case stat: Int if stat >= Status.BAD_REQUEST && stat < Status.INTERNAL_SERVER_ERROR =>
            result.body.consumeData.map { bs =>
              val message = (scala.xml.XML.loadString(bs.decodeString("UTF-8")) \ "message").head.text
              googleAnalyticsConnector.failure(message)
            }
        }

        result
      }.recoverWith {
        case ex: Throwable =>
          logger.errorWithoutRequestContext(ex.getMessage)
          eventualResult
      }
    } else {
      eventualResult
    }
  }

  private def debugLog(message: String, maybeConversationId: Option[HasConversationId]) = {
    maybeConversationId.fold {
      logger.debugWithoutRequestContext(message)
    } { cid =>
      logger.debug(message)(cid)
    }
  }

}

@Singleton
class DeclarationsFilters @Inject()(
                                     metricsFilter: MetricsFilter,
                                     auditFilter: AuditFilter,
                                     loggingFilter: LoggingFilter,
                                     cacheFilter: CacheControlFilter,
                                     googleAnalyticsFilter: GoogleAnalyticsFilter
                                   ) extends DefaultHttpFilters(metricsFilter, auditFilter, loggingFilter, cacheFilter, googleAnalyticsFilter)
