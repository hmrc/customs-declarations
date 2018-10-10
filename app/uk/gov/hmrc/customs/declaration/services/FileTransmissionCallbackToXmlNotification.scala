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

import uk.gov.hmrc.customs.declaration.model.{FileTransmissionFailureOutcome, FileTransmissionNotification, FileTransmissionSuccessOutcome}

import scala.xml.NodeSeq

@Singleton
class FileTransmissionCallbackToXmlNotification extends CallbackToXmlNotification[FileTransmissionNotification] {

  override def toXml(callbackResponse: FileTransmissionNotification): NodeSeq = {
    callbackResponse.outcome match {
      case FileTransmissionSuccessOutcome =>
      <Root>
        <FileReference>{callbackResponse.fileReference.toString}</FileReference>
        <BatchId>{callbackResponse.batchId.toString}</BatchId>
        <Outcome>SUCCESS</Outcome>
        <Details>Thank you for submitting your documents. Typical clearance times are 2 hours for air and 3 hours for maritime declarations. During busy periods wait times may be longer.</Details>
      </Root>
      case FileTransmissionFailureOutcome =>
      <Root>
        <FileReference>{callbackResponse.fileReference.toString}</FileReference>
        <BatchId>{callbackResponse.batchId.toString}</BatchId>
        <Outcome>FAILURE</Outcome>
        <Details>A system error has prevented your document from being accepted. Please follow the guidance on www.gov.uk and submit your documents by an alternative method.</Details>
      </Root>
    }
  }
}
