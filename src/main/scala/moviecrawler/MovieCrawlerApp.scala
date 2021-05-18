package moviecrawler

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import akka.actor.ActorSystem
import moviecrawler.service.MovieCrawlerServiceImpl
import scala.concurrent.ExecutionContext
import scala.io.StdIn.readLine

object MovieCrawlerApp {
  implicit val ac: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext = ac.dispatcher

  def main(args: Array[String]): Unit = {
    print("Введите название фильма: ")
    val title = readLine()
    val movies = MovieCrawlerServiceImpl().
      find(URLEncoder.encode(title, StandardCharsets.UTF_8).replace("+", "%20"))

    if (movies.isEmpty) {
      println("Ничего не найдено")
    } else {
      movies.foreach(movie => movie.print())
    }
  }
}