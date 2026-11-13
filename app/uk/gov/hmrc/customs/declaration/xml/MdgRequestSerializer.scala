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

package uk.gov.hmrc.customs.declaration.xml

import uk.gov.hmrc.customs.declaration.model.dec64.{MimeTypes, Dec64Submission}

import java.net.URL
import javax.inject.Singleton
import scala.xml.NodeSeq

@Singleton
class MdgRequestSerializer {

  import MdgRequestSerializer.*

  def serialize(submission: Dec64Submission, sourceLocation: URL, fileSize: Long): String = {

    val propertiesXml = for (property <- submission.properties)
      yield <mdg:property>
        <mdg:name>{property.name}</mdg:name>
        <mdg:value>{property.value}</mdg:value>
      </mdg:property>

    val filename =
      if (submission.fileName == FilenamePlaceholder)
        FilenamePlaceholder + MimeTypes.extensionFor(submission.mimeType).getOrElse("")
      else
        submission.fileName

    val xml =
      <mdg:BatchFileInterfaceMetadata
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xmlns:mdg="http://www.hmrc.gsi.gov.uk/mdg/batchFileInterfaceMetadataSchema"
      xsi:schemaLocation="http://www.hmrc.gsi.gov.uk/mdg/batchFileInterfaceMetadataSchema BatchFileInterfaceMetadata-1.0.7.xsd"
      >
        <mdg:sourceSystem>MDTP</mdg:sourceSystem>
        <mdg:sourceSystemType>AWS</mdg:sourceSystemType>
        <mdg:interfaceName>{InterfaceName}</mdg:interfaceName>
        <mdg:interfaceVersion>{InterfaceVersion}</mdg:interfaceVersion>
        <mdg:correlationID>{submission.fileReference.toString}</mdg:correlationID>
        <mdg:sequenceNumber>{submission.sequenceNumber.value}</mdg:sequenceNumber>
        <mdg:batchID>{submission.batchId.toString}</mdg:batchID>
        <mdg:batchSize>{submission.fileCount}</mdg:batchSize>
        <mdg:batchCount>{submission.sequenceNumber.value}</mdg:batchCount>
        <mdg:extractEndDateTime>{submission.uploadTimestamp.toString}</mdg:extractEndDateTime>
        <mdg:checksum>{submission.checksum}</mdg:checksum>
        <mdg:checksumAlgorithm>SHA-256</mdg:checksumAlgorithm>
        <mdg:fileSize>{fileSize}</mdg:fileSize>
        <mdg:compressed>false</mdg:compressed>
        <mdg:encrypted>false</mdg:encrypted>
        {if (propertiesXml.isEmpty) NodeSeq.Empty else <mdg:properties>{propertiesXml}</mdg:properties>}
        <mdg:sourceLocation>{sourceLocation.toString}</mdg:sourceLocation>
        <mdg:sourceFileName>{filename}</mdg:sourceFileName>
        <mdg:sourceFileMimeType>{submission.mimeType}</mdg:sourceFileMimeType>
        <mdg:destinations>
          <mdg:destination>
            <mdg:destinationSystem>{DestinationSystem}</mdg:destinationSystem>
          </mdg:destination>
        </mdg:destinations>
      </mdg:BatchFileInterfaceMetadata>

    xml.toString()
  }
}

object MdgRequestSerializer {
  private val InterfaceName = "DEC64"
  private val InterfaceVersion = "1.0.0"
  private val DestinationSystem = "CDS"
  val FilenamePlaceholder: String = "$" + "{filename}"
}
