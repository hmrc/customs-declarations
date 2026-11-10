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

package uk.gov.hmrc.customs.declaration.repo

import com.mongodb.ErrorCategory
import org.bson.types.ObjectId
import org.mongodb.scala._
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Updates}
import uk.gov.hmrc.customs.declaration.model.transmission.{FailedDeliveryAttempt, TransmissionOutcome, TransmissionRequestEnvelope}
import uk.gov.hmrc.customs.declaration.services.{DateTimeService, DeclarationsConfigService}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs
import uk.gov.hmrc.mongo.workitem.{WorkItem, WorkItemFields, WorkItemRepository}

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TransmissionRequestRepo @Inject()(mongoComponent: MongoComponent,
                                        configService: DeclarationsConfigService,
                                        dateTimeService: DateTimeService)
                                       (implicit ec: ExecutionContext)
  extends WorkItemRepository[TransmissionRequestEnvelope](
    collectionName = TransmissionRequestRepo.CollectionName,
    mongoComponent = mongoComponent,
    itemFormat = TransmissionRequestEnvelope.transmissionRequestEnvelopeFormat,
    workItemFields = TransmissionRequestRepo.workItemFields,
    extraIndexes = Seq(
      IndexModel(ascending(TransmissionRequestRepo.FileReferenceField), IndexOptions().unique(true).name("fileReference-unique")),
      IndexModel(ascending(TransmissionRequestRepo.workItemFields.receivedAt), IndexOptions().name("createdAt-ttl")
        .expireAfter(configService.fileUploadConfig.ttlInSeconds.toLong, TimeUnit.SECONDS))
    )
  ) {

  override val inProgressRetryAfter: java.time.Duration = TransmissionRequestRepo.InProgressRetryAfter

  override def now(): Instant = dateTimeService.nowUtc().truncatedTo(ChronoUnit.MILLIS)

  def pushNewIfAbsent(envelope: TransmissionRequestEnvelope): Future[Boolean] =
    pushNew(envelope).map(_ => true).recover {
      case e: MongoWriteException if e.getError.getCategory == ErrorCategory.DUPLICATE_KEY => false
    }

  def findByFileReference(fileReference: String): Future[Option[WorkItem[TransmissionRequestEnvelope]]] =
    collection.find(Filters.equal(TransmissionRequestRepo.FileReferenceField, fileReference)).headOption()

  def addFailedDeliveryAttempt(id: ObjectId, attempt: FailedDeliveryAttempt): Future[Boolean] =
    collection.updateOne(
      filter = Filters.equal(workItemFields.id, id),
      update = Updates.push(s"${workItemFields.item}.deliveryAttempts", Codecs.toBson(attempt))
    ).toFuture().map(_.getMatchedCount > 0)

  def setOutcome(id: ObjectId, outcome: TransmissionOutcome): Future[Boolean] =
    collection.updateOne(
      filter = Filters.equal(workItemFields.id, id),
      update = Updates.set(s"${workItemFields.item}.outcome", Codecs.toBson(outcome))
    ).toFuture().map(_.getMatchedCount > 0)
}

object TransmissionRequestRepo {
  private val CollectionName = "TransmissionRequest"

  private val InProgressRetryAfter: java.time.Duration = java.time.Duration.ofMinutes(5)

  private val workItemFields: WorkItemFields = WorkItemFields(
    id = "_id",
    receivedAt = "modifiedDetails.createdAt",
    updatedAt = "modifiedDetails.lastUpdated",
    availableAt = "modifiedDetails.availableAt",
    status = "status",
    failureCount = "failures",
    item = "body"
  )

  private val FileReferenceField = s"${workItemFields.item}.request.file.reference"
}
