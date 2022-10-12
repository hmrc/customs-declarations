/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.customs.declaration.repo

import org.mongodb.scala.result.InsertOneResult

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId

@Singleton
class FileUploadMetadataRepoErrorHandler @Inject()(logger: DeclarationsLogger) {

  //TODO correct this whole file
  def handleDeleteError(result: Throwable, exceptionMsg: => String)(implicit r: HasConversationId): Boolean = {
    logger.error(exceptionMsg)
    throw new RuntimeException(result)
//    handleError(result, databaseAltered, exceptionMsg)
  }

  def handleSaveError(result: Throwable, exceptionMsg: String)(implicit r: HasConversationId): Boolean = {

    logger.error(exceptionMsg)
    throw new IllegalStateException(result)
//    def handleSaveError(result: InsertOneResult): Boolean =
//      if (databaseAltered(result)) {
//        true
//      }
//      else {
//        throw new IllegalStateException(exceptionMsg)
//      }
//
//    handleError(writeResult, handleSaveError, exceptionMsg)
  }
//
//  private def handleError[T](result: InsertOneResult, f: InsertOneResult => T, exceptionMsg: => String)(implicit r: HasConversationId): T = {
//    result.writeConcernError.fold(f(result)) {
//      errMsg => {
//        val errorMsg = s"$exceptionMsg. $errMsg"
//        logger.error(errorMsg)
//        throw new RuntimeException(errorMsg)
//      }
//    }
//  }
//
//  private def databaseAltered(writeResult: InsertOneResult): Boolean = writeResult.wasAcknowledged()
//  private def databaseAltered(writeResult: WriteResult): Boolean = writeResult.n > 0

}
