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

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.{Format, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.JsObjectDocumentWriter
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.SubscriptionFieldsId
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId
import uk.gov.hmrc.customs.declaration.model.upscan.{CallbackFields, FileReference, FileUploadMetadata}
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[FileUploadMetadataMongoRepo])
trait FileUploadMetadataRepo {

  def create(fileUploadMetadata: FileUploadMetadata)(implicit r: HasConversationId): Future[Boolean]

  def fetch(reference: FileReference)(implicit r: HasConversationId): Future[Option[FileUploadMetadata]]

  def delete(clientNotification: FileUploadMetadata)(implicit r: HasConversationId): Future[Unit]

  def update(csId: SubscriptionFieldsId, reference: FileReference, callbackFields: CallbackFields)(implicit r: HasConversationId): Future[Option[FileUploadMetadata]]

  def deleteAll(): Future[Unit]
}

@Singleton
class FileUploadMetadataMongoRepo @Inject()(reactiveMongoComponent: ReactiveMongoComponent,
                                            errorHandler: FileUploadMetadataRepoErrorHandler,
                                            configService: DeclarationsConfigService,
                                            logger: DeclarationsLogger)
                                           (implicit ec: ExecutionContext)
  extends ReactiveRepository[FileUploadMetadata, BSONObjectID](
    collectionName = "batchFileUploads",
    mongo = reactiveMongoComponent.mongoConnector.db,
    domainFormat = FileUploadMetadata.fileUploadMetadataJF
  ) with FileUploadMetadataRepo {

  private implicit val format: Format[FileUploadMetadata] = FileUploadMetadata.fileUploadMetadataJF

  private val ttlIndexName = "createdAt-Index"
  private val ttlInSeconds = configService.fileUploadConfig.ttlInSeconds
  private val ttlIndex = Index(
    key = Seq("createdAt" -> IndexType.Descending),
    name = Some(ttlIndexName),
    unique = false,
    options = BSONDocument("expireAfterSeconds" -> ttlInSeconds)
  )

  dropInvalidIndexes.flatMap { _ =>
    collection.indexesManager.ensure(ttlIndex)
  }

  override def indexes: Seq[Index] = Seq(
    Index(
      key = Seq("batchId" -> IndexType.Ascending),
      name = Some("batch-id"),
      unique = true
    ),
    Index(
      key = Seq("files.reference" -> IndexType.Ascending, "csId" -> IndexType.Ascending),
      name = Some("csId-and-file-reference"),
      unique = true
    ),
    ttlIndex
  )

  override def create(fileUploadMetadata: FileUploadMetadata)(implicit r: HasConversationId): Future[Boolean] = {
    logger.debug(s"saving fileUploadMetadata: $fileUploadMetadata")
    lazy val errorMsg = s"File meta data not inserted for $fileUploadMetadata"

    collection.insert(ordered = false).one(fileUploadMetadata).map {
      writeResult => errorHandler.handleSaveError(writeResult, errorMsg)
    }
  }

  override def fetch(reference: FileReference)(implicit r: HasConversationId): Future[Option[FileUploadMetadata]] = {
    logger.debug(s"fetching file upload metadata with file reference: $reference")

    val selector = "files.reference" -> toJsFieldJsValueWrapper(reference)
    find(selector).map (_.headOption)
  }

  override def delete(fileUploadMetadata: FileUploadMetadata)(implicit r: HasConversationId): Future[Unit] = {
    logger.debug(s"deleting fileUploadMetadata: $fileUploadMetadata")

    val selector = "batchId" -> toJsFieldJsValueWrapper(fileUploadMetadata.batchId)
    lazy val errorMsg = s"Could not delete entity for selector: $selector"
    remove(selector).map(errorHandler.handleDeleteError(_, errorMsg))
  }

  def update(csId: SubscriptionFieldsId, reference: FileReference, cf: CallbackFields)(implicit r: HasConversationId): Future[Option[FileUploadMetadata]] = {
    logger.debug(s"updating file upload metadata with file reference: $reference with callbackField=$cf")

    val selector = Json.obj("files.reference" -> reference.toString, "csId" -> csId.toString)
    val update = Json.obj("$set" -> Json.obj("files.$.maybeCallbackFields" -> Json.obj("name" -> cf.name, "mimeType" -> cf.mimeType, "checksum" -> cf.checksum, "uploadTimestamp" -> cf.uploadTimestamp, "outboundLocation" -> cf.outboundLocation.toString)))

    findAndUpdate(selector, update, fetchNewObject = true).map(findAndModifyResult =>
      findAndModifyResult.value match {
        case None => None
        case Some(jsonDoc) =>
          val record = jsonDoc.as[FileUploadMetadata]
          Some(record)
      })
  }

  override def deleteAll(): Future[Unit] = {
    logger.debugWithoutRequestContext(s"deleting all file upload metadata")

    removeAll().map {result =>
      logger.debugWithoutRequestContext(s"deleted ${result.n} file upload metadata")
    }
  }

  private def dropInvalidIndexes: Future[_] =
    collection.indexesManager.list().flatMap { indexes =>
      indexes
        .find { index =>
          index.name.contains(ttlIndexName) &&
            !index.options.getAs[Int]("expireAfterSeconds").contains(ttlInSeconds)
        }
        .map { _ =>
          logger.debugWithoutRequestContext(s"dropping $ttlIndexName index as ttl value is incorrect")
          collection.indexesManager.drop(ttlIndexName)
        }
        .getOrElse(Future.successful(()))
    }

}
