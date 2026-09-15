package uk.gov.hmrc.customs.declaration.model

import uk.gov.hmrc.customs.declaration.model.upscan.FileReference


sealed trait CompletionResult
case class Completed(transmitted: Int) extends CompletionResult
case object AlreadyCompleted extends CompletionResult
case object CompletionInProgress extends CompletionResult
case class Pending(references: Seq[FileReference]) extends CompletionResult
case class Invalid(references: Seq[FileReference]) extends CompletionResult
case object BatchNotFound extends CompletionResult