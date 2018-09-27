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
import play.api.mvc.Result
import uk.gov.hmrc.customs.declaration.connectors.ApiSubscriptionFieldsConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.UpscanInitiateResponsePayload
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedBatchFileUploadPayloadRequest
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class BatchFileUploadBusinessService @Inject()(logger: DeclarationsLogger,
                                               apiSubFieldsConnector: ApiSubscriptionFieldsConnector,
                                               config: DeclarationsConfigService) {

  def send[A](implicit vupr: ValidatedBatchFileUploadPayloadRequest[A], hc: HeaderCarrier): Future[Either[Result, UpscanInitiateResponsePayload]] = ???

}
