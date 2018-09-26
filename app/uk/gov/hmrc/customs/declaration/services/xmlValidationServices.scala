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
import play.api.Configuration

@Singleton
class SubmissionXmlValidationService @Inject()(configuration: Configuration) extends XmlValidationService(configuration, "xsd.locations.submit")

@Singleton
class FileUploadXmlValidationService @Inject()(configuration: Configuration) extends XmlValidationService(configuration, "xsd.locations.fileupload")

@Singleton
//TODO add post-parsing validation rules here?
class BatchFileUploadXmlValidationService @Inject()(configuration: Configuration) extends XmlValidationService(configuration, "xsd.locations.batchfileupload")

@Singleton
class CancellationXmlValidationService @Inject()(configuration: Configuration) extends XmlValidationService(configuration, "xsd.locations.cancel")

@Singleton
class ClearanceXmlValidationService @Inject()(configuration: Configuration) extends XmlValidationService(configuration, "xsd.locations.clearance")

@Singleton
class AmendXmlValidationService @Inject()(configuration: Configuration) extends XmlValidationService(configuration, "xsd.locations.submit")

@Singleton
class ArrivalNotificationXmlValidationService @Inject()(configuration: Configuration) extends XmlValidationService(configuration, "xsd.locations.submit")
