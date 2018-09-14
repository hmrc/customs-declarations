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

package unit.util

import akka.actor.ActorSystem
import org.scalatest._
import uk.gov.hmrc.customs.declaration.util.FutureUtil

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future, TimeoutException}

class FutureUtilSpec extends AsyncFlatSpec with Matchers with OptionValues with Inside with Inspectors {

  implicit override def executionContext: ExecutionContextExecutor = scala.concurrent.ExecutionContext.Implicits.global

  val actorSystem = ActorSystem("test")

  "FutureUtil" should
    "complete the users future when it returns before the timeout" in {

    def myFuture: Future[Int] = Future[Int] {
      Thread.sleep((2 seconds).toMillis)
      100
    }

    FutureUtil.futureWithTimeout(myFuture, 3 seconds, actorSystem).map {
      result => if(result == 100) succeed else fail
    }
  }

  it should "not complete the future when it returns after the timeout" in {

    lazy val myFuture = Future[Int] {
      Thread.sleep((4 seconds).toMillis)
      100
    }

    recoverToSucceededIf[TimeoutException] {
      FutureUtil.futureWithTimeout(myFuture, 2 seconds, actorSystem)
    }
  }
}
