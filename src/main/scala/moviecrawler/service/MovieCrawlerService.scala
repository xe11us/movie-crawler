package moviecrawler.service

import moviecrawler.model.Movie

trait MovieCrawlerService {
  def find(title: String): List[Movie]
}