package com.holidaycheck.office.poznan

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import spray.can.Http
import spray.client.pipelining._
import spray.util._

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object BlondeInRed extends App {

  val logger = Logger(LoggerFactory.getLogger("BlondeInRed"))
  implicit val system = ActorSystem("Matrix")
  implicit val timeout = Timeout(5.seconds)

  import system.dispatcher

  def shutdown(): Unit = {
    IO(Http).ask(Http.CloseAll).await
    system.shutdown()
  }

  val pipeline = sendReceive
  val request = Get("http://www.bing.com/images/search?q=blonde+in+red")
  val pattern = "imgurl:&quot;(.+?)&quot;".r

  pipeline(request) onComplete {
    case Success(data) =>
      pattern.findAllIn(data.entity.asString).matchData.foreach {
        data => logger.info(data.group(1))
      }
      shutdown()
    case Failure(e) =>
      logger.error("Error in Matrix", e)
      shutdown()
  }
}