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

package uk.gov.hmrc.customs.declaration.logging

import javax.inject.Singleton

import com.google.inject.Inject
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.logging.LoggingHelper._
import uk.gov.hmrc.customs.declaration.model.{Ids, SeqOfHeader}
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class DeclarationsLogger @Inject()(logger: CdsLogger) {

  def debug(s: => String, payload: => String)(implicit hc: HeaderCarrier): Unit = logger.debug(formatDebug(s, Some(payload)))
  def debug(s: => String)(implicit hc: HeaderCarrier): Unit = logger.debug(formatDebug(s))
  def debug(s: => String, headers: => SeqOfHeader): Unit = logger.debug(formatDebug(msg = s, headers = headers))
  def info(s: => String)(implicit hc: HeaderCarrier): Unit = logger.info(formatInfo(s))
  def info(s: => String, ids: => Ids)(implicit hc: HeaderCarrier): Unit = logger.info(formatInfo(s, Some(ids)))
  def info(s: => String, headers: => SeqOfHeader): Unit = logger.info(formatInfo(msg = s, headers = headers))
  def warn(s: => String)(implicit hc: HeaderCarrier): Unit = logger.warn(formatWarn(s))
  def error(s: => String, e: => Throwable)(implicit hc: HeaderCarrier): Unit = logger.error(formatError(s), e)
  def error(s: => String)(implicit hc: HeaderCarrier): Unit = logger.error(formatError(s))
  def errorWithoutHeaderCarrier(s: => String): Unit = logger.error(s)

}
