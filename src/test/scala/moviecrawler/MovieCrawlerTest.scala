package moviecrawler

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import moviecrawler.model.Movie
import moviecrawler.service.MovieCrawlerServiceImpl
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.concurrent.ExecutionContext

class MovieCrawlerTest extends AnyFlatSpec with should.Matchers {
  implicit val ac: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext = ac.dispatcher

  "Client example movie test" should "find two scarface movies with years 1932 and 1983" in {
    val movies: List[Movie] = MovieCrawlerServiceImpl().find("scarface")
    movies.size >= 2 should be(true)

    val first: Movie = movies.
      find(movie => movie.year == "1932").
      getOrElse(Movie("", "", "0", List.empty[(String, Double)]))

    val second: Movie = movies.
      find(movie => movie.year == "1983").
      getOrElse(Movie("", "", "0", List.empty[(String, Double)]))

    first.year should be("1932")
    second.year should be("1983")
  }

  "Client non existing movie title test" should "return empty list" in {
    val movies = MovieCrawlerServiceImpl().find("non-existing movie title")
    movies.isEmpty should be(true)
  }

  "Client correct http request test" should "return response with status code 200" in {
    val response = MovieCrawlerServiceImpl().getSafetyResponse("https://www.google.com")
    response.status should be(StatusCodes.OK)
  }

  "Client bad http request test" should "return response with 404" in {
    val response = MovieCrawlerServiceImpl().getSafetyResponse("https://www.bad-request.com")
    response.status should be(StatusCodes.BadRequest)
  }

  "Client find non-existing movie" should "return empty list" in {
    val movies = MovieCrawlerServiceImpl().find("very%20long%non-existing%20movie%20title")
    movies.isEmpty should be(true)
  }
}