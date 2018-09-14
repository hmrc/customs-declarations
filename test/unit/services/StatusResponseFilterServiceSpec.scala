package unit.services

import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.services.{DeclarationsConfigService, StatusResponseFilterService}
import uk.gov.hmrc.customs.declaration.xml.StatusResponseCreator
import uk.gov.hmrc.play.test.UnitSpec
import util.TestXMLData

import scala.xml.NodeSeq

class StatusResponseFilterServiceSpec extends UnitSpec with MockitoSugar {

  trait SetUp {

    val mockStatusResponseCreator = mock[StatusResponseCreator]
    val mockDeclarationsLogger = mock[DeclarationsLogger]
    val mockDeclarationsConfigService = mock[DeclarationsConfigService]

    val service = new StatusResponseFilterService(mockStatusResponseCreator, mockDeclarationsLogger, mockDeclarationsConfigService)
  }

  //TODO more on this test and others
  "StatusResponseFilterService" should {
    "return filtered values" in new SetUp() {
      val statusResponse: NodeSeq = service.filter(TestXMLData.ValidStatusResponse)

      statusResponse should not be 'empty

    }
  }

}
