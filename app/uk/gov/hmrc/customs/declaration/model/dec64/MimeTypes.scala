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

package uk.gov.hmrc.customs.declaration.model.dec64

object MimeTypes {
  private val mimeToExtension = Map(
    "application/pdf" -> ".pdf",
    "image/png" -> ".png",
    "image/jpeg" -> ".jpg",
    "text/plain" -> ".txt",
    "application/msword" -> ".doc",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx",
    "text/csv" -> ".csv",
    "application/vnd.ms-excel" -> ".xls",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> ".xlsx",
    "application/vnd.oasis.opendocument.spreadsheet" -> ".ods",
    "application/vnd.oasis.opendocument.text" -> ".odt"
  )

  def extensionFor(mimeType: String): Option[String] =
    mimeToExtension.get(mimeType.toLowerCase)
}
