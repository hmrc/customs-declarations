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
import com.mongodb.client.model.Indexes.{ascending, descending}
import org.mongodb.scala.model.{IndexModel, IndexOptions}


import org.mongodb.scala.model.Filters._

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.SubscriptionFieldsId
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId
import uk.gov.hmrc.customs.declaration.model.upscan.{CallbackFields, FileReference, FileUploadMetadata}
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoFormats


import org.mongodb.scala._
import org.mongodb.scala.model._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.model.UpdateOptions
import org.mongodb.scala.bson.BsonObjectId

import scala.collection.script.Index
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.model.Sorts._
import play.shaded.ahc.io.netty.util.concurrent.FastThreadLocal.removeAll

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
class FileUploadMetadataMongoRepo @Inject()(mongoComponent: MongoComponent,
                                            errorHandler: FileUploadMetadataRepoErrorHandler,
                                            configService: DeclarationsConfigService,
                                            logger: DeclarationsLogger)
                                           (implicit ec: ExecutionContext)
  extends PlayMongoRepository[FileUploadMetadata](
    collectionName = "batchFileUploads",
    mongoComponent = mongoComponent,
    domainFormat = FileUploadMetadata.format,
    indexes = Seq(
      IndexModel(descending("createdAt"), IndexOptions().unique(false).name("createdAt-Index")
        /** TODO options = BSONDocument("expireAfterSeconds" -> ttlInSeconds) ** */),
      IndexModel(ascending("batchId"), IndexOptions().unique(true).name("batch-id")),
      IndexModel(ascending("files.reference", "csId"), IndexOptions().unique(true).name("csId-and-file-reference"))
    )
    ,
    replaceIndexes = true
  ) with FileUploadMetadataRepo {

  private implicit val format: Format[FileUploadMetadata] = FileUploadMetadata.format

  private val ttlIndexName = "createdAt-Index"
  private val ttlInSeconds = configService.fileUploadConfig.ttlInSeconds


  //  private val ttlIndex = Index(
  //    key = Seq("createdAt" -> IndexType.Descending),
  //    name = Some(ttlIndexName),
  //    unique = false,
  //    options = BSONDocument("expireAfterSeconds" -> ttlInSeconds)
  //  )


  override def create(fileUploadMetadata: FileUploadMetadata)(implicit r: HasConversationId): Future[Boolean] = {
    logger.debug(s"saving fileUploadMetadata: $fileUploadMetadata")
    lazy val errorMsg = s"File meta data not inserted for $fileUploadMetadata"

    collection.insertOne(fileUploadMetadata).toFuture().map {
      writeResult => errorHandler.handleSaveError(writeResult, errorMsg)
    }
  }

  override def fetch(reference: FileReference)(implicit r: HasConversationId): Future[Option[FileUploadMetadata]] = {
    logger.debug(s"fetching file upload metadata with file reference: $reference")

    val selector = equal("files.reference", toJsFieldJsValueWrapper(reference))
    collection.find(selector).toFuture().map(_.headOption)
  }

  override def delete(fileUploadMetadata: FileUploadMetadata)(implicit r: HasConversationId): Future[Unit] = {
    logger.debug(s"deleting fileUploadMetadata: $fileUploadMetadata")

    val selector = equal("batchId", toJsFieldJsValueWrapper(fileUploadMetadata.batchId))
    lazy val errorMsg = s"Could not delete entity for selector: $selector"
    collection.deleteOne(selector).toFuture().map(errorHandler.handleDeleteError(_, errorMsg))
  }

  def update(csId: SubscriptionFieldsId, reference: FileReference, cf: CallbackFields)(implicit r: HasConversationId): Future[Option[FileUploadMetadata]] = {
    logger.debug(s"updating file upload metadata with file reference: $reference with callbackField=$cf")

    val selector = and(equal("files.reference", reference.toString), equal("csId", csId.toString))
    //TODO Check this update
    //    val update = combine(set(Json.obj("files.$.maybeCallbackFields" -> Json.obj("name", cf.name), ("mimeType",cf.mimeType), ("checksum" , cf.checksum), ("uploadTimestamp", cf.uploadTimestamp), ("outboundLocation" , cf.outboundLocation.toString))))
    val update = combine(set("name", cf.name), set("mimeType", cf.mimeType), set("checksum", cf.checksum), set("uploadTimestamp", cf.uploadTimestamp), set("outboundLocation", cf.outboundLocation.toString))

    collection.findOneAndUpdate(selector, update).toFutureOption().map(findAndModifyResult =>
      findAndModifyResult match {
        case None => None
        case Some(jsonDoc) =>
          val record = jsonDoc
          Some(record)
      })
  }

  override def deleteAll(): Future[Unit] = {
    logger.debugWithoutRequestContext(s"deleting all file upload metadata")

    //TODO is this ok?
    //    removeAll().map {result =>
    //      logger.debugWithoutRequestContext(s"deleted ${result.n} file upload metadata")
    //    }

    removeAll()
    logger.debugWithoutRequestContext(s"deleted file upload metadata")
    Future.successful()

  }


}
