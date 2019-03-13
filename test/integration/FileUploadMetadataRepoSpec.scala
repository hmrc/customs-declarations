/*
 * Copyright 2019 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import reactivemongo.api.DB
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId
import uk.gov.hmrc.customs.declaration.repo.{FileUploadMetadataMongoRepo, FileUploadMetadataRepoErrorHandler, MongoDbProvider}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.UnitSpec
import util.ApiSubscriptionFieldsTestData.subscriptionFieldsId
import util.MockitoPassByNameHelper.PassByNameVerifier
import util.TestData.{FileMetadataWithFileOne, _}

import scala.language.postfixOps

class FileUploadMetadataRepoSpec extends UnitSpec
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with MockitoSugar
  with MongoSpecSupport { self =>

  private val mockLogger = mock[DeclarationsLogger]
  private val mockErrorHandler = mock[FileUploadMetadataRepoErrorHandler]
  private lazy implicit val emptyHC: HeaderCarrier = HeaderCarrier()
  private implicit val implicitVHR = TestValidatedHeadersRequest

  private val mongoDbProvider = new MongoDbProvider{
    override val mongo: () => DB = self.mongo
  }

  private val repository = new FileUploadMetadataMongoRepo(mongoDbProvider, mockErrorHandler, mockLogger)

  override def beforeEach() {
    dropTestCollection("batchFileUploads")
    Mockito.reset(mockErrorHandler, mockLogger)
  }

  override def afterAll() {
    dropTestCollection("batchFileUploads")
  }

  private def collectionSize: Int = {
    await(repository.count(Json.obj()))
  }

  private def logVerifier(logLevel: String, logText: String) = {
    PassByNameVerifier(mockLogger, logLevel)
      .withByNameParam(logText)
      .withParamMatcher(any[HasConversationId])
      .verify()
  }

  private def selector(fileReference: String) = {
    "files.reference" -> play.api.libs.json.Json.toJsFieldJsValueWrapper(fileReference)
  }

  "repository" should {
    "successfully save a single file metadata" in {
      when(mockErrorHandler.handleSaveError(any(), any())(any())).thenReturn(true)
      val saveResult = await(repository.create(FileMetadataWithFileOne))
      saveResult shouldBe true
      collectionSize shouldBe 1

      val findResult = await(repository.find(selector(BatchFileOne.reference.toString)).head)

      findResult shouldBe FileMetadataWithFileOne
      logVerifier("debug", "saving fileUploadMetadata: FileUploadMetadata(1,123,327d9145-4965-4d28-a2c5-39dedee50334,48400000-8cf0-11bd-b23e-10b96e4ef001,1,List(BatchFile(31400000-8ce0-11bd-b23e-10b96e4ef00f,Some(CallbackFields(name1,application/xml,checksum1)),https://a.b.com,1,1,Some(Document Type 1))))")
    }

    "successfully save when create is called multiple times" in {
      await(repository.create(FileMetadataWithFileOne))
      await(repository.create(FileMetadataWithFileTwo))
      collectionSize shouldBe 2

      val findResult1 = await(repository.find(selector(BatchFileOne.reference.toString)).head)

      findResult1 shouldBe FileMetadataWithFileOne

      val findResult2 = await(repository.find(selector(BatchFileTwo.reference.toString)).head)

      findResult2 shouldBe FileMetadataWithFileTwo
    }

    "successfully update checksum, searching by reference" in {
      await(repository.create(FileMetadataWithFilesOneAndThree))
      await(repository.create(FileMetadataWithFileTwo))
      val updatedFileOne = BatchFileOne.copy(maybeCallbackFields = Some(CallbackFields("UPDATED_NAME", "UPDATED_MIMETYPE", "UPDATED_CHECKSUM")))
      val expectedRecord = FileMetadataWithFilesOneAndThree.copy(files = Seq(updatedFileOne, BatchFileThree))

      val maybeActual = await(repository.update(subscriptionFieldsId, FileReferenceOne, CallbackFieldsUpdated))

      maybeActual shouldBe Some(expectedRecord)
      await(repository.fetch(BatchFileOne.reference)) shouldBe Some(expectedRecord)
      await(repository.fetch(BatchFileTwo.reference)) shouldBe Some(FileMetadataWithFileTwo)
      logVerifier("debug", "updating file upload metadata with file reference: 31400000-8ce0-11bd-b23e-10b96e4ef00f with callbackField=CallbackFields(UPDATED_NAME,UPDATED_MIMETYPE,UPDATED_CHECKSUM)")
    }

    "not update checksum, when searching by reference fails" in {
      await(repository.create(FileMetadataWithFileTwo))

      val maybeActual = await(repository.update(subscriptionFieldsId, FileReferenceOne, CallbackFieldsUpdated))

      maybeActual shouldBe None
    }

    "return Some when fetch by file reference is successful" in {
      await(repository.create(FileMetadataWithFileOne))
      await(repository.create(FileMetadataWithFileTwo))

      val maybeFoundRecordOne = await(repository.fetch(BatchFileOne.reference))

      maybeFoundRecordOne shouldBe Some(FileMetadataWithFileOne)

      val maybeFoundRecordTwo = await(repository.fetch(BatchFileTwo.reference))

      maybeFoundRecordTwo shouldBe Some(FileMetadataWithFileTwo)
      logVerifier("debug", "fetching file upload metadata with file reference: 31400000-8ce0-11bd-b23e-10b96e4ef00f")
    }

    "return None when fetch by file reference is un-successful" in {
      await(repository.create(FileMetadataWithFileTwo))

      val maybeFoundRecord = await(repository.fetch(BatchFileOne.reference))

      maybeFoundRecord shouldBe None
    }

    "successfully delete a record" in {
      await(repository.create(FileMetadataWithFileOne))
      await(repository.create(FileMetadataWithFileTwo))
      collectionSize shouldBe 2

      val maybeFoundRecordOne = await(repository.fetch(BatchFileOne.reference))

      maybeFoundRecordOne shouldBe Some(FileMetadataWithFileOne)

      await(repository.delete(maybeFoundRecordOne.get))
      collectionSize shouldBe 1

      val maybeFoundRecordTwo = await(repository.fetch(BatchFileTwo.reference))

      maybeFoundRecordTwo shouldBe Some(FileMetadataWithFileTwo)
      logVerifier("debug", "deleting fileUploadMetadata: FileUploadMetadata(1,123,327d9145-4965-4d28-a2c5-39dedee50334,48400000-8cf0-11bd-b23e-10b96e4ef001,1,List(BatchFile(31400000-8ce0-11bd-b23e-10b96e4ef00f,Some(CallbackFields(name1,application/xml,checksum1)),https://a.b.com,1,1,Some(Document Type 1))))")
    }

    "collection should be same size when deleting non-existent record" in {
      await(repository.create(FileMetadataWithFileOne))
      collectionSize shouldBe 1

      await(repository.delete(FileMetadataWithFileTwo))

      collectionSize shouldBe 1
    }

  }
}
