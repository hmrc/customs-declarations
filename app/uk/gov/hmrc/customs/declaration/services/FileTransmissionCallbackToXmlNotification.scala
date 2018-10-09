package uk.gov.hmrc.customs.declaration.services

import javax.inject.Singleton

import uk.gov.hmrc.customs.declaration.model.{CallbackToXmlNotification, FileTransmissionNotification, FileTransmissionSuccessOutcome}

import scala.xml.NodeSeq

@Singleton
class FileTransmissionCallbackToXmlNotification extends CallbackToXmlNotification[FileTransmissionNotification] {
  override def toXml(callbackResponse: FileTransmissionNotification): NodeSeq = {
    if (callbackResponse.outcome == FileTransmissionSuccessOutcome) {
      fileTransmissionNotificationSuccessXML(callbackResponse)
    } else {
      fileTransmissionNotificationFailureXML(callbackResponse)
    }
  }

  def fileTransmissionNotificationSuccessXML: FileTransmissionNotification => NodeSeq = fileTransmissionNotification =>
    <Root>
      <FileReference>{fileTransmissionNotification.reference.toString}</FileReference>
      <BatchId>{fileTransmissionNotification.batchId.toString}</BatchId>
      <Outcome>SUCCESS</Outcome>
      <Details>Thank you for submitting your documents. Typical clearance times are 2 hours for air and 3 hours for maritime declarations. During busy periods wait times may be longer.</Details>
    </Root>

  def fileTransmissionNotificationFailureXML: FileTransmissionNotification => NodeSeq = fileTransmissionNotification =>
    <Root>
      <FileReference>{fileTransmissionNotification.reference.toString}</FileReference>
      <BatchId>{fileTransmissionNotification.batchId.toString}</BatchId>
      <Outcome>FAILURE</Outcome>
      <Details>A system error has prevented your document from being accepted. Please follow the guidance on www.gov.uk and submit your documents by an alternative method.</Details>
    </Root>
}
