package moviecrawler.service

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpRequest, HttpResponse, StatusCodes}
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import moviecrawler.model.Movie
import org.json4s.native.JsonMethods
import org.json4s.{DefaultFormats, JString, JValue}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

case class MovieCrawlerServiceImpl()(implicit ac: ActorSystem, ec: ExecutionContext) extends LazyLogging
                                                                                        with MovieCrawlerService {
  implicit val formats: DefaultFormats.type = DefaultFormats

  def find(title: String): List[Movie] = {
    try {
      val itunes = Await.result(findItunes(title), Duration.Inf)
      val kinopoisk = Await.result(findKinopoisk(title), Duration.Inf)
      val moretv = Await.result(findMoreTV(title), Duration.Inf)
      itunes.appendedAll(kinopoisk).appendedAll(moretv)
    } catch {
      case _: Throwable => List.empty[Movie]
    }
  }

  private def getField(value: JValue, field: String): JValue = {
    value.findField(entry => entry._1 == field) match {
      case Some(f) => f._2
      case None => JString("")
    }
  }

  private def getResponse(uri: String): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(uri = uri))
  }

  def getSafetyResponse(uri: String): HttpResponse = {
    try {
      Await.result(getResponse(uri), Duration.Inf)
    } catch {
      case e: Throwable =>
        HttpResponse(status = StatusCodes.BadRequest).withEntity(ContentTypes.`text/html(UTF-8)`, e.getMessage)
    }
  }

  private def parseResponseJson(response: HttpResponse)(parse: JValue => List[Movie]): Future[List[Movie]] = {
    response.entity.dataBytes.runWith(Sink.fold(ByteString.empty)(_ ++ _)).map(_.utf8String).map { result =>
      val responseJson: JValue = JsonMethods.parse(result)
      parse(responseJson)
    }
  }

  def findKinopoisk(title: String): Future[List[Movie]] = {
    val uri = "https://api.ott.kinopoisk.ru/v13/suggested-data?query=" + title + "&withPersons=false"

    parseResponseJson(getSafetyResponse(uri))(responseJson => responseJson.findField(s => s._1 == "films") match {
      case Some(v) =>
        val results = v._2.extract[List[JValue]]

        val movies = results.foldLeft(List.empty[Movie])((acc, res) => {
          val title = getField(res, "title").extract[String]
          val year = getField(res, "year").extract[String]
          val option = getField(res, "watchingOption")
          val optionType = getField(option, "type").extract[String]

          if (optionType == "SUBSCRIPTION") {
            val subscription = getField(option, "subscription").extract[String] match {
              case "YA_PLUS_3M" => "подписка Яндекс Плюс (199 RUB в месяц)"
              case "YA_PREMIUM" => "подписка Amediateka (699 RUB в месяц)"
              case "YA_PLUS_SUPER" => "подписка more.tv (399 RUB в месяц)"
              case _ => "Неизвестная подписка"
            }
            acc.appended(Movie("kinopoisk", title, year, List((subscription, 0))))
          } else {
            option.findField(field => field._1.toLowerCase.endsWith("price")) match {
              case Some(value) =>
                val minimumPrice = value._2.extract[String].toDouble
                val discountPriceDetails = getField(option, "discountPriceDetails")
                val discountPrice = getField(discountPriceDetails, "value").extract[String].toInt

                acc.appended(Movie("kinopoisk", title, year, List(("минимальная цена", minimumPrice),
                  ("скидка: первые 3 фильма по подписке Яндекс Плюс (199 RUB в месяц)",
                    discountPrice))))
              case None => acc
            }
          }
        })
        movies
      case None => List.empty[Movie]
    })
  }

  def findMoreTV(title: String): Future[List[Movie]] = {
    val uri = "https://more.tv/api/v2/web/Suggest?q=" + title

    parseResponseJson(getSafetyResponse(uri))(responseJson => responseJson.findField(field => field._1 == "data") match {
      case Some(v) =>
        val results = v._2.extract[List[JValue]]

        val movies: List[Movie] = results.foldLeft(List.empty[Movie])((acc, res) => {
          val title = getField(res, "title").extract[String]
          val year = getField(res, "releaseDate").extract[String].takeWhile(char => char.isDigit)

          res.findField(field => field._1 == "subscriptionType") match {
            case Some(subscription) =>
              val statement: String = subscription._2.extract[String] match {
                case "BASIC" => "подписка (299 RUB в месяц)"
                case _ => "открытый доступ"
              }
              acc.appended(Movie("more.tv", title, year, List((statement, 0))))
            case None => acc
          }
        })
        movies.take(5)
      case None => List.empty[Movie]
    })
  }

  private def findItunes(title: String): Future[List[Movie]] = {
    val uri: String = "https://itunes.apple.com/search?term=" + title + "&media=movie&entity=movie&country=ru"

    parseResponseJson(getSafetyResponse(uri))(responseJson => responseJson.findField(s => s._1 == "results") match {
      case Some(v) =>
        val results = v._2.extract[List[JValue]]

        val movies: List[Movie] = results.foldLeft(List.empty[Movie])((acc, res) => {
          val title = getField(res, "trackName").extract[String]
          val year = getField(res, "releaseDate").extract[String].takeWhile(char => char.isDigit)
          val prices: List[(String, Double)] = res.
            filterField(field => field._1.startsWith("track") && field._1.endsWith("Price")).
            map(field => {
              val statement = field._1 match {
                case "trackPrice" => "цена за SD"
                case "trackRentalPrice" => "цена за аренду SD"
                case "trackHdPrice" => "цена за HD"
                case "trackHdRentalPrice" => "цена за аренду HD"
                case _ => "price"
              }
              (statement, field._2.extract[String].toDouble)
            })

          acc.appended(Movie("itunes", title, year, prices))
        })
        movies
      case None => List.empty
    })
  }
}