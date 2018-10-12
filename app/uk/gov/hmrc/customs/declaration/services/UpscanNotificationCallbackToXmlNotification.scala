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

import javax.inject.Singleton

import uk.gov.hmrc.customs.declaration.model.UploadedFailedCallbackBody

import scala.xml.NodeSeq

@Singleton
class UpscanNotificationCallbackToXmlNotification extends CallbackToXmlNotification[UploadedFailedCallbackBody] {

  override def toXml(failed: UploadedFailedCallbackBody): NodeSeq =
    <root>
      <reference>{failed.reference.toString}</reference>
      <fileStatus>FAILED</fileStatus>
      <failureDetails>
        <failureReason>{failed.failureDetails.failureReason}</failureReason>
        <message>{failed.failureDetails.message}</message>
      </failureDetails>
    </root>

}
