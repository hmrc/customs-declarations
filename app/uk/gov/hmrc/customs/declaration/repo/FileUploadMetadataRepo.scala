/*
 * Copyright 2024 HM Revenue & Customs
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
import com.mongodb.client.model.Indexes.{ascending, descending}
import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.model.{FindOneAndUpdateOptions, IndexModel, IndexOptions, ReturnDocument}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.SubscriptionFieldsId
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId
import uk.gov.hmrc.customs.declaration.model.upscan.{CallbackFields, FileReference, FileUploadMetadata}
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@ImplementedBy(classOf[FileUploadMetadataMongoRepo])
trait FileUploadMetadataRepo {

  def create(fileUploadMetadata: FileUploadMetadata)(implicit r: HasConversationId): Future[Boolean]

  def fetch(reference: FileReference)(implicit r: HasConversationId): Future[Option[FileUploadMetadata]]

  def delete(clientNotification: FileUploadMetadata)(implicit r: HasConversationId): Future[Unit]

  def update(csId: SubscriptionFieldsId, reference: FileReference, callbackFields: CallbackFields)(implicit r: HasConversationId): Future[Option[FileUploadMetadata]]

  def deleteAll(): Future[Unit]
}

@Singleton
class FileUploadMetadataMongoRepo @Inject()(mongoComponent: MongoComponent,
                                            configService: DeclarationsConfigService,
                                            logger: DeclarationsLogger)
                                           (implicit ec: ExecutionContext)
  extends PlayMongoRepository[FileUploadMetadata](
    collectionName = "batchFileUploads",
    mongoComponent = mongoComponent,
    domainFormat = FileUploadMetadata.format,
    indexes = Seq(
      IndexModel(descending("createdAt"), IndexOptions().unique(false).name("createdAt-Index").expireAfter(configService.fileUploadConfig.ttlInSeconds.toLong,TimeUnit.SECONDS)),
      IndexModel(ascending("batchId"), IndexOptions().unique(true).name("batch-id")),
      IndexModel(ascending("files.reference", "csId"), IndexOptions().unique(true).name("csId-and-file-reference"))
    )
  ) with FileUploadMetadataRepo {

  override def create(fileUploadMetadata: FileUploadMetadata)(implicit r: HasConversationId): Future[Boolean] = {
    logger.debug(s"saving fileUploadMetadata: $fileUploadMetadata")
    lazy val errorMsg = s"File meta data not inserted for $fileUploadMetadata"

    collection.insertOne(fileUploadMetadata).toFuture().transformWith {
      case Success(result) =>
        result.wasAcknowledged()
        Future.successful(true)
      case Failure(exception) =>
        logger.error(errorMsg)
        Future.failed(new IllegalStateException(exception))
    }

  }

  override def fetch(reference: FileReference)(implicit r: HasConversationId): Future[Option[FileUploadMetadata]] = {
    logger.debug(s"fetching file upload metadata with file reference: $reference")

    val selector = equal("files.reference", reference.toString)
    collection.find(selector).toFuture().map(_.headOption)
  }

  override def delete(fileUploadMetadata: FileUploadMetadata)(implicit r: HasConversationId): Future[Unit] = {
    logger.debug(s"deleting fileUploadMetadata: $fileUploadMetadata")

    val selector = equal("batchId", fileUploadMetadata.batchId.toString)
    lazy val errorMsg = s"Could not delete entity for selector: $selector"
    collection.deleteOne(selector).toFuture().transformWith {
      case Success(_) => Future.successful((): Unit)
      case Failure(exception) => {
        logger.error(errorMsg)
        Future.failed(new RuntimeException(exception))
      }
    }
  }

  def update(csId: SubscriptionFieldsId, reference: FileReference, cf: CallbackFields)(implicit r: HasConversationId): Future[Option[FileUploadMetadata]] = {
    logger.debug(s"updating file upload metadata with file reference: $reference with callbackField=$cf")

    val selector = and(equal("files.reference", reference.toString), equal("csId", csId.toString))

    val update = combine(
      set("files.$.maybeCallbackFields.name", cf.name),
      set("files.$.maybeCallbackFields.mimeType", cf.mimeType),
      set("files.$.maybeCallbackFields.checksum", cf.checksum),
      set("files.$.maybeCallbackFields.uploadTimestamp", cf.uploadTimestamp.toString),
      set("files.$.maybeCallbackFields.outboundLocation", cf.outboundLocation.toString))

    collection.findOneAndUpdate(selector, update,
      FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER))
      .toFutureOption()
  }

  override def deleteAll(): Future[Unit] = {
    logger.debugWithoutRequestContext(s"deleting all file upload metadata")

    collection.deleteMany(Document.empty).toFuture().map { result =>
      logger.debugWithoutRequestContext(s"deleted ${result.getDeletedCount} file upload metadata")
    }

  }

}
