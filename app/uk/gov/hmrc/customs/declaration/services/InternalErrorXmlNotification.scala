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

import uk.gov.hmrc.customs.declaration.model.FileReference

import scala.xml.NodeSeq

@Singleton
class InternalErrorXmlNotification extends CallbackToXmlNotification[FileReference] {

  override def toXml(fileReference: FileReference): NodeSeq =
    <errorResponse>
      <code>INTERNAL_SERVER_ERROR</code>
      <message>File upload for file reference {fileReference.toString} failed. A system error has prevented your document from being accepted. Please follow the guidance on www.gov.uk and submit your documents by an alternative method.</message>
    </errorResponse>
}
