package com.holidaycheck.office.poznan

import akka.actor.ActorSystem
import com.holidaycheck.office.poznan.SearchClient.{Price, Offer, Search}
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormatter, ISODateTimeFormat}
import spray.client.pipelining._
import spray.http.HttpRequest
import spray.httpx.SprayJsonSupport._
import spray.json._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object Config {
  lazy val servicePath = "http://search.holidaycheck.lan"
}

object SearchClient {

  case class Search(hotelId: String, from: DateTime, to: DateTime)

  case class Offer(hotelId: String, from: DateTime, to: DateTime, price: Price)

  case class Price(value: BigDecimal, currency: String)

  def apply(system: ActorSystem): SearchClient = new DefaultSearchClient()(system)

}

sealed trait SearchClient {

  import com.holidaycheck.office.poznan.SearchClient._

  trait ResponseHandler {
    def onSuccess(offers: Seq[Offer])
    def onError(exception: MPGException)
  }

  def offers(search: Search, filter: Offer => Boolean, handler: ResponseHandler): Unit

}

private object SearchJsonProtocol extends DefaultJsonProtocol {

  implicit object DateJsonFormat extends RootJsonFormat[DateTime] {
    private val parserISO: DateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis()

    override def write(obj: DateTime) = JsString(parserISO.print(obj))

    override def read(json: JsValue): DateTime = json match {
      case JsString(s) => parserISO.parseDateTime(s)
      case _ => throw new DeserializationException("DateTime deserialization exception")
    }
  }

  implicit val priceFormat = jsonFormat2(Price)
  implicit val searchFormat = jsonFormat3(Search)
  implicit val offerFormat = jsonFormat4(Offer)

}

import com.holidaycheck.office.poznan.SearchJsonProtocol._

private class DefaultSearchClient(implicit val system: ActorSystem) extends SearchClient {
  override def offers(search: Search, filter: (Offer) => Boolean, handler: ResponseHandler) = {
    import system.dispatcher
    val pipeline: HttpRequest => Future[Seq[Offer]] = sendReceive ~> unmarshal[Seq[Offer]]
    val request = Post(Config.servicePath, search)
    pipeline(request) onComplete {
      case Success(offers) => handler onSuccess offers.filter(filter)
      case Failure(e) => handler.onError(e)
    }
  }
}

sealed class MPGException(message: String, cause: Throwable) extends RuntimeException(message, cause)

object MPGException {
  implicit def apply(t: Throwable): MPGException = new MPGException("MPG exception", t)
}