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

package uk.gov.hmrc.customs.declaration.services.queue

import org.apache.pekko.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import org.apache.pekko.event.Logging
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.customs.declaration.config.ServiceConfiguration
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService

import java.util.concurrent.{Executors, TimeUnit}
import javax.inject.Inject
import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}
import scala.util.{Failure, Success}

class WorkItemProcessingScheduler @Inject()(
  queueProcessor: WorkItemService,
  configuration : ServiceConfiguration,
  configService : DeclarationsConfigService
)(using
  actorSystem         : ActorSystem,
  applicationLifecycle: ApplicationLifecycle
) {

  private val pollingInterval: FiniteDuration =
    FiniteDuration(configuration.queuePollingInterval.toMillis, MILLISECONDS)

  private val retryAfterFailureInterval: FiniteDuration =
    FiniteDuration(configuration.queueRetryAfterFailureInterval.toMillis, MILLISECONDS)

  private case object Poll

  private class ContinuousPollingActor extends Actor {
    import context.dispatcher

    private val log = Logging(context.system, this)

    override def receive: Receive = {
      case Poll =>
        queueProcessor.processOne().andThen {
          case Success(true) =>
            self ! Poll
          case Success(false) =>
            context.system.scheduler.scheduleOnce(pollingInterval, self, Poll)
          case Failure(f) =>
            log.error(f, s"Queue processing failed")
            context.system.scheduler.scheduleOnce(retryAfterFailureInterval, self, Poll)
        }
    }
  }

  private val pollingActorOpt: Option[ActorRef] =
    if (!configService.fileTransmissionEnabled)
      Some(actorSystem.actorOf(Props(new ContinuousPollingActor())))
    else
      None

  pollingActorOpt.foreach { pollingActor =>
    Executors.newScheduledThreadPool(1).schedule(
      new Runnable { override def run(): Unit = pollingActor ! Poll },
      pollingInterval.toMillis,
      TimeUnit.MILLISECONDS
    )
  }

  private def shutDown(): Unit =
    pollingActorOpt.foreach(_ ! PoisonPill)

  applicationLifecycle.addStopHook { () =>
    shutDown()
    Future.successful(())
  }
}
