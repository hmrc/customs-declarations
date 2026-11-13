/*
 * Copyright 2026 HM Revenue & Customs
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

package util

import uk.gov.hmrc.customs.declaration.model.{Dec64Config, FileSequenceNo}
import uk.gov.hmrc.customs.declaration.model.dec64.{Dec64Property, Dec64Submission}
import util.ApiSubscriptionFieldsTestData.subscriptionFieldsId
import util.TestData.{BatchIdOne, FileReferenceOne, InitiateDate}

import java.net.URL
import scala.concurrent.duration.*

object Dec64TestData {

  val Dec64ConfigOn: Dec64Config = Dec64Config(
    enabled = true,
    mdgDec64Url = "http://localhost:9479/mdg-stub/request",
    mdgDec64BearerToken = "dec64-token",
    pollerInterval = 500.millis,
    initialBackoff = 1.minute,
    deliveryWindow = 4.hours,
    inProgressRetryAfter = 5.minutes
  )
  val Dec64ConfigOff: Dec64Config = Dec64ConfigOn.copy(enabled = false)

  // a real SHA-256 hex (64 chars) so Sha256Checksum.fromHex accepts it
  val ValidChecksumHex = "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100"

  val UpscanDownloadUrl = new URL("https://upscan.example/download/31400000-8ce0-11bd-b23e-10b96e4ef00f")
  val PresignedUrl = new URL("https://object-store.s3.example/dec64/file?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Expires=900&X-Amz-Signature=abc123")
  val ObjectStorePath = s"dec64/$BatchIdOne/$FileReferenceOne"
  val FileSize: Long = 2048L

  val Dec64SubmissionOne: Dec64Submission = Dec64Submission(
    fileReference = FileReferenceOne,
    csId = subscriptionFieldsId,
    batchId = BatchIdOne,
    fileCount = 2,
    sequenceNumber = FileSequenceNo(1),
    fileName = "someFile.pdf",
    mimeType = "application/pdf",
    checksum = ValidChecksumHex,
    size = Some(FileSize),
    uploadTimestamp = InitiateDate,
    upscanDownloadUrl = UpscanDownloadUrl,
    properties = Seq(Dec64Property("DeclarationId", "decId123"), Dec64Property("Eori", "ZZ123456789000"), Dec64Property("DocumentType", "Document Type 1")),
    objectStorePath = None,
    deliveryDeadline = InitiateDate.plusSeconds(4 * 60 * 60)
  )
}
