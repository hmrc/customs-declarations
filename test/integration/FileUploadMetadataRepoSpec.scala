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

package integration

import org.mockito.ArgumentMatchers.any

import java.net.URL
import org.mockito.Mockito._
import org.mongodb.scala.{MongoClient, MongoDatabase}
import org.mongodb.scala.model.Filters.equal
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsXml
import play.api.test.Helpers
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.FileUploadConfig
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{HasConversationId, ValidatedHeadersRequest}
import uk.gov.hmrc.customs.declaration.model.upscan.{CallbackFields, FileUploadMetadata}
import uk.gov.hmrc.customs.declaration.repo.{FileUploadMetadataMongoRepo, FileUploadMetadataRepoErrorHandler}
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.HeaderCarrier
import util.ApiSubscriptionFieldsTestData.subscriptionFieldsId
import util.MockitoPassByNameHelper.PassByNameVerifier
import util.TestData.{FileMetadataWithFileOne, _}
import org.scalatest.matchers.should.Matchers.{an, be, convertToAnyShouldWrapper, thrownBy}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import org.mongodb.scala.model._
import uk.gov.hmrc.mongo.MongoComponent

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
import org.mongodb.scala.bson.conversions.Bson

import scala.collection.script.Index
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.model.Sorts._
import org.scalactic.Equality
import org.scalatest.matchers.dsl.MatcherFactory1
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{ExecutionContext, Future}

class FileUploadMetadataRepoSpec extends AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with MockitoSugar {

  implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext

  trait SetUp {
    val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    val mockErrorHandler: FileUploadMetadataRepoErrorHandler = mock[FileUploadMetadataRepoErrorHandler]
    val mockConfigService: DeclarationsConfigService = mock[DeclarationsConfigService]
    val mockFileUploadConfig: FileUploadConfig = mock[FileUploadConfig]
    lazy implicit val emptyHC: HeaderCarrier = HeaderCarrier()
    implicit val implicitVHR: ValidatedHeadersRequest[AnyContentAsXml] = TestValidatedHeadersRequest
    when(mockConfigService.fileUploadConfig).thenReturn(mockFileUploadConfig)
    when(mockFileUploadConfig.ttlInSeconds).thenReturn(10000)
    val repository = new FileUploadMetadataMongoRepo(mongoComponent, mockErrorHandler, mockConfigService, mockLogger)
  }

//  override implicit lazy val mongoConnectorForTest: MongoConnector = MongoConnector(mongoUri)

  private val mongoComponent:   MongoComponent =
    new MongoComponent {
      override def client: MongoClient = ???

      override def database: MongoDatabase = ???
    }

  override def beforeEach() {
    // TODO what to do here?
//    dropTestCollection("batchFileUploads")
  }

  override def afterAll() {
    // TODO what to do here?
//    dropTestCollection("batchFileUploads")
  }

  private def collectionSize(repository: FileUploadMetadataMongoRepo): Int = {
    await(repository.collection.countDocuments().toFuture()).toInt
  }

  private def logVerifier(mockLogger: DeclarationsLogger, logLevel: String, logText: String): Unit = {
    PassByNameVerifier(mockLogger, logLevel)
      .withByNameParam(logText)
      .withParamMatcher(any[HasConversationId])
      .verify()
  }

  private def selector(fileReference: String) = {
    org.mongodb.scala.model.Filters.equal( "files.reference" , play.api.libs.json.Json.toJsFieldJsValueWrapper(fileReference))
  }

  "repository" should {
    "successfully save a single file metadata" in new SetUp {
      when(mockErrorHandler.handleSaveError(any(), any())(any())).thenReturn(true)
      val saveResult = await(repository.create(FileMetadataWithFileOne))
      saveResult shouldBe true
      collectionSize(repository) shouldBe 1

      val findResult = await(repository.collection.find(selector(BatchFileOne.reference.toString)).toFuture()).head

      findResult shouldBe FileMetadataWithFileOne
      logVerifier(mockLogger, "debug", "saving fileUploadMetadata: FileUploadMetadata(1,123,327d9145-4965-4d28-a2c5-39dedee50334,48400000-8cf0-11bd-b23e-10b96e4ef001,1,2018-04-24T09:30:00.000Z,List(BatchFile(31400000-8ce0-11bd-b23e-10b96e4ef00f,Some(CallbackFields(name1,application/xml,checksum1,2018-04-24T09:30:00Z,https://outbound.a.com)),https://a.b.com,1,1,Some(Document Type 1))))")
    }

    "successfully save when create is called multiple times" in new SetUp {
      await(repository.create(FileMetadataWithFileOne))
      await(repository.create(FileMetadataWithFileTwo))
      collectionSize(repository) shouldBe 2

      val findResult1 = await(repository.collection.find(selector(BatchFileOne.reference.toString)).toFuture()).head

      findResult1 shouldBe FileMetadataWithFileOne

      val findResult2 = await(repository.collection.find(selector(BatchFileTwo.reference.toString)).toFuture()).head

      findResult2 shouldBe FileMetadataWithFileTwo
    }

    "successfully update checksum, searching by reference" in new SetUp {
      await(repository.create(FileMetadataWithFilesOneAndThree))
      await(repository.create(FileMetadataWithFileTwo))
      val updatedFileOne = BatchFileOne.copy(maybeCallbackFields = Some(CallbackFields("UPDATED_NAME", "UPDATED_MIMETYPE", "UPDATED_CHECKSUM", InitiateDate, new URL("https://outbound.a.com"))))
      val expectedRecord = FileMetadataWithFilesOneAndThree.copy(files = Seq(updatedFileOne, BatchFileThree))

      val maybeActual = await(repository.update(subscriptionFieldsId, FileReferenceOne, CallbackFieldsUpdated))

      maybeActual shouldBe Some(expectedRecord)
      await(repository.fetch(BatchFileOne.reference)) shouldBe Some(expectedRecord)
      await(repository.fetch(BatchFileTwo.reference)) shouldBe Some(FileMetadataWithFileTwo)
      logVerifier(mockLogger, "debug", "updating file upload metadata with file reference: 31400000-8ce0-11bd-b23e-10b96e4ef00f with callbackField=CallbackFields(UPDATED_NAME,UPDATED_MIMETYPE,UPDATED_CHECKSUM,2018-04-24T09:30:00Z,https://outbound.a.com)")
    }

    "not update checksum, when searching by reference fails" in new SetUp {
      await(repository.create(FileMetadataWithFileTwo))

      val maybeActual = await(repository.update(subscriptionFieldsId, FileReferenceOne, CallbackFieldsUpdated))

      maybeActual shouldBe None
    }

    "return Some when fetch by file reference is successful" in new SetUp {
      await(repository.create(FileMetadataWithFileOne))
      await(repository.create(FileMetadataWithFileTwo))

      val maybeFoundRecordOne = await(repository.fetch(BatchFileOne.reference))

      maybeFoundRecordOne shouldBe Some(FileMetadataWithFileOne)

      val maybeFoundRecordTwo = await(repository.fetch(BatchFileTwo.reference))

      maybeFoundRecordTwo shouldBe Some(FileMetadataWithFileTwo)
      logVerifier(mockLogger, "debug", "fetching file upload metadata with file reference: 31400000-8ce0-11bd-b23e-10b96e4ef00f")
    }

    "return None when fetch by file reference is un-successful" in new SetUp {
      await(repository.create(FileMetadataWithFileTwo))

      val maybeFoundRecord = await(repository.fetch(BatchFileOne.reference))

      maybeFoundRecord shouldBe None
    }

    "successfully delete a record" in new SetUp {
      await(repository.create(FileMetadataWithFileOne))
      await(repository.create(FileMetadataWithFileTwo))
      collectionSize(repository) shouldBe 2

      val maybeFoundRecordOne = await(repository.fetch(BatchFileOne.reference))

      maybeFoundRecordOne shouldBe Some(FileMetadataWithFileOne)

      await(repository.delete(maybeFoundRecordOne.get))
      collectionSize(repository) shouldBe 1

      val maybeFoundRecordTwo = await(repository.fetch(BatchFileTwo.reference))

      maybeFoundRecordTwo shouldBe Some(FileMetadataWithFileTwo)
      logVerifier(mockLogger, "debug", "deleting fileUploadMetadata: FileUploadMetadata(1,123,327d9145-4965-4d28-a2c5-39dedee50334,48400000-8cf0-11bd-b23e-10b96e4ef001,1,2018-04-24T09:30:00.000Z,Vector(BatchFile(31400000-8ce0-11bd-b23e-10b96e4ef00f,Some(CallbackFields(name1,application/xml,checksum1,2018-04-24T09:30:00Z,https://outbound.a.com)),https://a.b.com,1,1,Some(Document Type 1))))")
    }

    "collection should be same size when deleting non-existent record" in new SetUp {
      await(repository.create(FileMetadataWithFileOne))
      collectionSize(repository) shouldBe 1

      await(repository.delete(FileMetadataWithFileTwo))

      collectionSize(repository) shouldBe 1
    }

    "successfully delete all file metadata" in new SetUp {
      await(repository.create(FileMetadataWithFileOne))
      await(repository.create(FileMetadataWithFileTwo))
      collectionSize(repository) shouldBe 2

      await(repository.deleteAll())

      collectionSize(repository) shouldBe 0
    }
  }
}
