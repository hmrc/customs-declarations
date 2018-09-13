package uk.gov.hmrc.customs.declaration.services

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.BadgeIdentifier

import scala.xml.NodeSeq

@Singleton
class StatusResponseValidationService @Inject() (declarationsLogger: DeclarationsLogger, declarationsConfigService: DeclarationsConfigService) {

  //TODO check badgeId & declaration date
  def validate(xml: NodeSeq, badgeIdentifier: BadgeIdentifier): Boolean = ???
}
