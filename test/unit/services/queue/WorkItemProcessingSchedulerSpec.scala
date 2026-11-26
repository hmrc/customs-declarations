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

package unit.services.queue

import org.apache.pekko.actor.ActorSystem
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.DefaultApplicationLifecycle
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.customs.declaration.config.ServiceConfiguration
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.customs.declaration.services.queue.{WorkItemProcessingScheduler, WorkItemService}

import scala.concurrent.Future
import scala.concurrent.duration.*

class WorkItemProcessingSchedulerSpec extends AnyWordSpecLike
  with Matchers
  with MockitoSugar
  with Eventually
  with IntegrationPatience
  with BeforeAndAfterAll {

  private given actorSystem: ActorSystem = ActorSystem("WorkItemProcessingSchedulerSpec")

  override protected def afterAll(): Unit = {
    await(actorSystem.terminate())
    super.afterAll()
  }

  trait SetUp {
    val mockWorkItemService: WorkItemService = mock[WorkItemService]
    val mockConfiguration: ServiceConfiguration = mock[ServiceConfiguration]
    val mockConfigService: DeclarationsConfigService = mock[DeclarationsConfigService]
    given lifecycle: DefaultApplicationLifecycle = new DefaultApplicationLifecycle()

    when(mockConfiguration.queuePollingInterval).thenReturn(10.millis)
    when(mockConfiguration.queueRetryAfterFailureInterval).thenReturn(10.millis)

    def newScheduler(): WorkItemProcessingScheduler =
      new WorkItemProcessingScheduler(mockWorkItemService, mockConfiguration, mockConfigService)
  }

  "WorkItemProcessingScheduler" should {

    "poll the queue, immediately re-polling after processing an item and retrying after failures" in new SetUp {
      when(mockConfigService.fileTransmissionEnabled).thenReturn(false)
      when(mockWorkItemService.processOne()).thenReturn(
        Future.successful(true),
        Future.failed(new RuntimeException("queue failure")),
        Future.successful(false)
      )

      newScheduler()

      eventually(verify(mockWorkItemService, Mockito.atLeast(4)).processOne())

      await(lifecycle.stop())
    }

    "not poll the queue when file transmission is enabled" in new SetUp {
      when(mockConfigService.fileTransmissionEnabled).thenReturn(true)

      newScheduler()
      Thread.sleep(100)

      verifyNoInteractions(mockWorkItemService)
      await(lifecycle.stop())
    }
  }
}
