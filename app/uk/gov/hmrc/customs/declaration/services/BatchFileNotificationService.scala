package uk.gov.hmrc.customs.declaration.services

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.connectors.BatchFileNotificationConnector
import uk.gov.hmrc.customs.declaration.model.{CallbackResponse, CallbackToXmlNotification}

import scala.concurrent.Future

@Singleton
class BatchFileNotificationService @Inject()(notificationConnector: BatchFileNotificationConnector,
                                             cdsLogger: CdsLogger) {

  def sendMessage[T <: CallbackResponse](callbackResponse: T, clientSubscriptionId: String)(implicit callback: CallbackToXmlNotification[T]): Future[Unit] = {

    notificationConnector.send(
      FileTransmissionCustomsNotification(
        clientSubscriptionId, callbackResponse.reference.value, callback.toXml(callbackResponse)
      )
    )
  }

}
