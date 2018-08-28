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

import org.scalatest._

import scala.concurrent.{Future, TimeoutException}
import scala.concurrent.duration._

class FutureUtilSpec extends AsyncFlatSpec with Matchers with OptionValues with Inside with Inspectors {

  implicit override def executionContext = scala.concurrent.ExecutionContext.Implicits.global

  "FutureUtil" should
    "complete the users future when it returns before the timeout" in {

    def myFuture: Future[Int] = Future[Int] {
      //println(s"starting user future ${System.currentTimeMillis()}")
      Thread.sleep((2 seconds).toMillis)
      //println(s"user future done ${System.currentTimeMillis()}")
      100
    }

    FutureUtil.futureWithTimeout(myFuture, 3 seconds).map {
      result => if(result == 100) succeed else fail
    }
  }

  it should "not complete the future when it returns after the timeout" in {

    lazy val myFuture = Future[Int] {
      //println(s"user future waiting 4 seconds ${System.currentTimeMillis()}")
      Thread.sleep((4 seconds).toMillis)
      //println(s"user future done at ${System.currentTimeMillis()}")
      100
    }

    recoverToSucceededIf[TimeoutException] {
      FutureUtil.futureWithTimeout(myFuture, 2 seconds)
    }
  }
}
