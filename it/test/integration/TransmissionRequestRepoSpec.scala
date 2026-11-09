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

package integration

import org.bson.types.ObjectId
import org.mockito.Mockito.*
import org.mongodb.scala.*
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.customs.declaration.model.transmission.*
import uk.gov.hmrc.customs.declaration.repo.TransmissionRequestRepo
import uk.gov.hmrc.customs.declaration.services.{DateTimeService, DeclarationsConfigService}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}
import util.ApiSubscriptionFieldsTestData.subscriptionFieldsId
import util.TestData.{FileReferenceOne, FileReferenceTwo, fileUploadConfig}

import java.net.URL
import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

class TransmissionRequestRepoSpec extends AnyWordSpec
  with Matchers
  with MockitoSugar
  with OptionValues
  with ScalaFutures
  with IntegrationPatience
  with DefaultPlayMongoRepositorySupport[WorkItem[TransmissionRequestEnvelope]] {

  private val fixedNow = Instant.parse("2026-01-01T10:00:00Z")

  private val mockConfigService = mock[DeclarationsConfigService]
  private val mockDateTimeService = mock[DateTimeService]
  when(mockConfigService.fileUploadConfig).thenReturn(fileUploadConfig)
  when(mockDateTimeService.nowUtc()).thenReturn(fixedNow)

  override val repository: TransmissionRequestRepo = new TransmissionRequestRepo(mongoComponent, mockConfigService, mockDateTimeService)

  private def envelope(fileReference: String): TransmissionRequestEnvelope =
    TransmissionRequestEnvelope(
      request = TransmissionRequest(
        Batch("48400000-8cf0-11bd-b23e-10b96e4ef001", 2),
        Interface("DEC64", "1.0.0"),
        File(
          fileReference,
          new URL("https://bucketName.s3.eu-west-2.amazonaws.com/xxx"),
          "someFileN.ame",
          "application/pdf",
          "asdrfgvbhujk13579",
          1,
          1024,
          Instant.parse("2026-01-01T09:00:00Z")
        ),
        Seq(Property("DeclarationId", "decId123"), Property("Eori", "123")),
        new URL("http://localhost:9000/file-transmission-notify"),
        Some(4.hours)
      ),
      serviceName = "customs-declarations",
      csId = subscriptionFieldsId
    )

  private val RequestOne = envelope(FileReferenceOne.toString)
  private val RequestTwo = envelope(FileReferenceTwo.toString)

  private def pullOne(availableBefore: Instant = fixedNow.plusSeconds(1)): Option[WorkItem[TransmissionRequestEnvelope]] =
    repository.pullOutstanding(failedBefore = fixedNow.plusSeconds(1), availableBefore = availableBefore).futureValue

  "TransmissionRequestRepo" should {

    "use the TransmissionRequest collection" in {
      repository.collectionName shouldBe "TransmissionRequest"
    }

    "create a unique index on file reference and a TTL index" in {
      val indexes = repository.collection.listIndexes().toFuture().futureValue

      val fileReferenceIndex = indexes.find(_.getString("name") == "fileReference-unique").value
      fileReferenceIndex.get("key").map(_.asDocument().keySet().asScala.toSet).value shouldBe Set("body.request.file.reference")
      fileReferenceIndex.get("unique").map(_.asBoolean().getValue).value shouldBe true

      val ttlIndex = indexes.find(_.getString("name") == "createdAt-ttl").value
      ttlIndex.get("key").map(_.asDocument().keySet().asScala.toSet).value shouldBe Set("modifiedDetails.createdAt")
      ttlIndex.get("expireAfterSeconds").map(_.asNumber().longValue()).value shouldBe fileUploadConfig.ttlInSeconds.toLong
    }

    "store a new request as ToDo with a BSON date for the TTL index" in {
      repository.pushNewIfAbsent(RequestOne).futureValue shouldBe true

      val stored = repository.findByFileReference(FileReferenceOne.toString).futureValue.value
      stored.item shouldBe RequestOne
      stored.status shouldBe ProcessingStatus.ToDo
      stored.failureCount shouldBe 0
      stored.receivedAt shouldBe fixedNow

      val raw = repository.collection.find[org.bson.BsonDocument](Filters.equal("_id", stored.id)).head().futureValue
      raw.getDocument("modifiedDetails").get("createdAt").isDateTime shouldBe true
    }

    "not create a duplicate when the same file reference is queued twice" in {
      repository.pushNewIfAbsent(RequestOne).futureValue shouldBe true
      val duplicate = RequestOne.copy(request = RequestOne.request.copy(file = RequestOne.request.file.copy(name = "other.pdf")))
      repository.pushNewIfAbsent(duplicate).futureValue shouldBe false

      repository.count(ProcessingStatus.ToDo).futureValue shouldBe 1
      repository.findByFileReference(FileReferenceOne.toString).futureValue.value.item shouldBe RequestOne
    }

    "accept requests for different file references" in {
      repository.pushNewIfAbsent(RequestOne).futureValue shouldBe true
      repository.pushNewIfAbsent(RequestTwo).futureValue shouldBe true

      repository.count(ProcessingStatus.ToDo).futureValue shouldBe 2
    }

    "return None for an unknown file reference" in {
      repository.findByFileReference(FileReferenceTwo.toString).futureValue shouldBe None
    }

    "pull an outstanding item and mark it InProgress" in {
      repository.pushNewIfAbsent(RequestOne).futureValue

      val pulled = pullOne().value
      pulled.item shouldBe RequestOne
      pulled.status shouldBe ProcessingStatus.InProgress

      pullOne() shouldBe None
    }

    "not pull an item marked for retry until its delay has passed" in {
      repository.pushNewIfAbsent(RequestOne).futureValue
      val pulled = pullOne().value
      val retryAt = fixedNow.plus(2, ChronoUnit.MINUTES)

      repository.markAs(pulled.id, ProcessingStatus.Failed, Some(retryAt)).futureValue shouldBe true

      pullOne(availableBefore = fixedNow.plusSeconds(60)) shouldBe None
      val retried = pullOne(availableBefore = retryAt.plusSeconds(1)).value
      retried.id shouldBe pulled.id
      retried.failureCount shouldBe 1
    }

    "complete an InProgress item" in {
      repository.pushNewIfAbsent(RequestOne).futureValue
      val pulled = pullOne().value

      repository.complete(pulled.id, ProcessingStatus.Succeeded).futureValue shouldBe true

      repository.findById(pulled.id).futureValue.value.status shouldBe ProcessingStatus.Succeeded
      pullOne() shouldBe None
    }

    "store the outcome on the item without changing status" in {
      repository.pushNewIfAbsent(RequestOne).futureValue
      val item = repository.findByFileReference(FileReferenceOne.toString).futureValue.value

      repository.setOutcome(item.id, TransmissionOutcome.Failure("MDG returned 400")).futureValue shouldBe true
      repository.findById(item.id).futureValue.value.item.outcome shouldBe Some(TransmissionOutcome.Failure("MDG returned 400"))

      repository.setOutcome(item.id, TransmissionOutcome.Success).futureValue shouldBe true
      val updated = repository.findById(item.id).futureValue.value
      updated.item.outcome shouldBe Some(TransmissionOutcome.Success)
      updated.status shouldBe ProcessingStatus.ToDo
    }

    "return false when storing the outcome on an unknown id" in {
      repository.setOutcome(new ObjectId(), TransmissionOutcome.Success).futureValue shouldBe false
    }

    "append failed delivery attempts to the item" in {
      repository.pushNewIfAbsent(RequestOne).futureValue
      val item = repository.findByFileReference(FileReferenceOne.toString).futureValue.value
      val first = FailedDeliveryAttempt(fixedNow, "MDG returned 503")
      val second = FailedDeliveryAttempt(fixedNow.plusSeconds(60), "MDG returned 502")

      repository.addFailedDeliveryAttempt(item.id, first).futureValue shouldBe true
      repository.addFailedDeliveryAttempt(item.id, second).futureValue shouldBe true

      repository.findById(item.id).futureValue.value.item.deliveryAttempts shouldBe Seq(first, second)
    }

    "round-trip a request without the optional fields" in {
      val minimal = RequestOne.copy(request = RequestOne.request.copy(properties = Nil, deliveryWindowDuration = None))
      repository.pushNewIfAbsent(minimal).futureValue
      repository.findByFileReference(FileReferenceOne.toString).futureValue.value.item shouldBe minimal
    }
  }
}
