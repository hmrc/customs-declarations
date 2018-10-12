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

package uk.gov.hmrc.customs.declaration.repo

import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import play.api.libs.json.Json
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.JsObjectDocumentWriter
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId
import uk.gov.hmrc.customs.declaration.model.{BatchFileUploadMetadata, CallbackFields, FileReference}
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@ImplementedBy(classOf[BatchFileUploadMetadataMongoRepo])
trait BatchFileUploadMetadataRepo {

  def create(batchFileUploadMetadata: BatchFileUploadMetadata)(implicit r: HasConversationId): Future[Boolean]

  def fetch(reference: FileReference)(implicit r: HasConversationId): Future[Option[BatchFileUploadMetadata]]

  def delete(clientNotification: BatchFileUploadMetadata)(implicit r: HasConversationId): Future[Unit]

  def update(reference: FileReference, callbackFields: CallbackFields)(implicit r: HasConversationId): Future[Option[BatchFileUploadMetadata]]
}

@Singleton
class BatchFileUploadMetadataMongoRepo @Inject()(mongoDbProvider: MongoDbProvider,
                                                 errorHandler: BatchFileUploadMetadataRepoErrorHandler,
                                                 logger: DeclarationsLogger)
  extends ReactiveRepository[BatchFileUploadMetadata, BSONObjectID](
    collectionName = "batchFileUploads",
    mongo = mongoDbProvider.mongo,
    domainFormat = BatchFileUploadMetadata.batchFileUploadMetadataJF
  ) with BatchFileUploadMetadataRepo {

  private implicit val format = BatchFileUploadMetadata.batchFileUploadMetadataJF

  override def indexes: Seq[Index] = Seq(
    Index(
      key = Seq("batchId" -> IndexType.Ascending),
      name = Some("batch-id"),
      unique = true
    ),
    Index(
      key = Seq("files.reference" -> IndexType.Ascending),
      name = Some("file-reference"),
      unique = true
    )
  )

  override def create(batchFileUploadMetadata: BatchFileUploadMetadata)(implicit r: HasConversationId): Future[Boolean] = {
    logger.debug(s"saving batchFileUploadMetadata: $batchFileUploadMetadata")
    lazy val errorMsg = s"Batch file meta data not inserted for $batchFileUploadMetadata"

    collection.insert(batchFileUploadMetadata).map {
      writeResult => errorHandler.handleSaveError(writeResult, errorMsg)
    }
  }

  override def fetch(reference: FileReference)(implicit r: HasConversationId): Future[Option[BatchFileUploadMetadata]] = {
    logger.debug(s"fetching batch file upload metadata with file reference: $reference")

    val selector = Json.obj("files.reference" -> reference)
    collection.find(selector).one[BatchFileUploadMetadata]
  }

  override def delete(batchFileUploadMetadata: BatchFileUploadMetadata)(implicit r: HasConversationId): Future[Unit] = {
    logger.debug(s"deleting batchFileUploadMetadata: $batchFileUploadMetadata")

    val selector = Json.obj("batchId" -> batchFileUploadMetadata.batchId)
    lazy val errorMsg = s"Could not delete entity for selector: $selector"
    collection.remove(selector).map(errorHandler.handleDeleteError(_, errorMsg))
  }

  def update(reference: FileReference, cf: CallbackFields)(implicit r: HasConversationId): Future[Option[BatchFileUploadMetadata]] = {
    logger.debug(s"updating batch file upload metadata with file reference: $reference with callbackField=$cf")

    val selector = Json.obj("files.reference" -> reference.toString)
    val update = Json.obj("$set" -> Json.obj("files.$.maybeCallbackFields" -> Json.obj("name" -> cf.name, "mimeType" -> cf.mimeType, "checksum" -> cf.checksum)))

    val updateOp = collection.updateModifier(
      update = update,
      fetchNewObject = true,
      upsert = false
    )

    collection.findAndModify(selector, updateOp).map(findAndModifyResult =>
      findAndModifyResult.value match {
        case None => None
        case Some(jsonDoc) =>
          val record = jsonDoc.as[BatchFileUploadMetadata]
          Some(record)
      })
  }

}
