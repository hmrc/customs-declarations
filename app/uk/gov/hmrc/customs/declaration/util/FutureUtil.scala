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

package uk.gov.hmrc.customs.declaration.util

import java.util.concurrent.TimeoutException

import akka.actor.ActorSystem

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

object FutureUtil {

  def futureWithTimeout[T](future : Future[T], timeout : FiniteDuration, actorSystem: ActorSystem)(implicit ec: ExecutionContext): Future[T] = {

    val futureTimeout = akka.pattern.after(timeout, using = actorSystem.scheduler)(Future.failed(new TimeoutException()))

    Future.firstCompletedOf(Seq(future, futureTimeout))
  }
}
