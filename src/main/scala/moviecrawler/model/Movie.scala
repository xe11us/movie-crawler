package moviecrawler.model

case class Movie(service: String, title: String, year: String, prices: List[(String, Double)]) {
  def print(): Unit = {
    println(title)
    println("год: " + year)
    println("сервис: " + service)
    println("=".repeat(24))
    prices.foreach(price => println(price._1 + ": " + price._2 + " RUB"))
    println("")
  }
}