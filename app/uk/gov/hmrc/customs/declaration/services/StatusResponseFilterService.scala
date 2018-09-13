package uk.gov.hmrc.customs.declaration.services

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger

import scala.xml.NodeSeq

@Singleton
class StatusResponseFilterService @Inject() (declarationsLogger: DeclarationsLogger, declarationsConfigService: DeclarationsConfigService) {

  def filter(xml: NodeSeq): NodeSeq = ???


}
