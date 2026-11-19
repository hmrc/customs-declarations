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

import uk.gov.hmrc.customs.declaration.model.transmission.*
import util.TestData.{BatchIdOne, FileReferenceOne, InitiateDate}

import java.net.URL

object TransmissionRequestTestData {
  
  val ValidChecksumHex = "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100"

  val PresignedUrl = new URL("https://object-store.s3.example/dec64/file?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Expires=900&X-Amz-Signature=abc123")
  val FileSize: Int = 2048

  val TransmissionRequestOne: TransmissionRequest = TransmissionRequest(
    batch = Batch(BatchIdOne.toString, 2),
    interface = Interface("DEC64", "1.0.0"),
    file = File(
      reference = FileReferenceOne.toString,
      location = PresignedUrl,
      name = "someFile.pdf",
      mimeType = "application/pdf",
      checksum = ValidChecksumHex,
      sequenceNumber = 1,
      size = FileSize,
      uploadTimestamp = InitiateDate
    ),
    properties = Seq(Property("DeclarationId", "decId123"), Property("Eori", "ZZ123456789000"), Property("DocumentType", "Document Type 1")),
    callbackUrl = new URL("https://customs-declarations.example/callback"),
    deliveryWindowDuration = None
  )

  def withFile(f: File => File): TransmissionRequest =
    TransmissionRequestOne.copy(file = f(TransmissionRequestOne.file))
}
