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

package uk.gov.hmrc.customs.declaration.model

import uk.gov.hmrc.customs.declaration.model.upscan.FileReference


sealed trait CompletionResult
case class Completed(transmitted: Int) extends CompletionResult
case object AlreadyCompleted extends CompletionResult
case object CompletionInProgress extends CompletionResult
case class Pending(references: Seq[FileReference]) extends CompletionResult
case class Invalid(references: Seq[FileReference]) extends CompletionResult
case object BatchNotFound extends CompletionResult