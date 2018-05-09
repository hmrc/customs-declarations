package unit.controllers

import java.util.UUID

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.mvc._
import play.api.test._
import uk.gov.hmrc.customs.declaration.controllers.UpscanNotificationController
import play.api.test.Helpers._

class UpscanNotificationControllerSpec extends PlaySpec with MockitoSugar with Results {

  private val decId = UUID.randomUUID().toString
  private val eori = UUID.randomUUID().toString
  private val docType = "license"
  private val clientSubscriptionId = UUID.randomUUID().toString

  val controller = new UpscanNotificationController()

  "upscan notification controller" should {

    "return 204 when a valid request is received" in  {
      val result = controller.post(decId, eori, docType, clientSubscriptionId).apply(FakeRequest())
      status(result) mustBe 204
      contentAsString(result) mustBe ""
    }
  }
}
