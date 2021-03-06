package com.jsherz.cloudfrontlogstoes

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.alpakka.elasticsearch.scaladsl.ElasticsearchFlow
import akka.stream.alpakka.elasticsearch.{
  ApiVersion,
  ElasticsearchWriteSettings,
  RetryWithBackoff,
  WriteMessage
}
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.Sink
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient

import scala.concurrent.duration._

object ProcessCurrentFiles extends App {

  private val bucket = sys.env("LOGS_BUCKET")

  private val numWorkers = 10

  private implicit val system: ActorSystem = ActorSystem("ProcessCurrentFiles")
  private implicit val materializer: Materializer = Materializer.matFromSystem

  private val downloader = new FileDownloader

  system.registerOnTermination(downloader.close())

  private val source = S3.listBucket(bucket, None)

  import LogEntryJsonProtocol._

  private implicit val esClient: RestClient =
    RestClient
      .builder(HttpHost.create(sys.env("ELASTICSEARCH_HOST")))
      .build()

  system.registerOnTermination(esClient.close())

  private val esSettings = ElasticsearchWriteSettings()
    .withApiVersion(ApiVersion.V7)
    .withBufferSize(100) // scalastyle:ignore magic.number
    .withRetryLogic(RetryWithBackoff(3, 100 millis, 5 seconds))

  source
    .mapAsync(numWorkers)(downloader.downloadAndGunzip)
    .via(new ParseLogFileFlow)
    .map(entry =>
      WriteMessage
        .createIndexMessage[LogEntry](LogEntryUtil.id(entry), entry)
        .withIndexName(LogEntryUtil.indexName(entry))
    )
    .via(
      ElasticsearchFlow.create[LogEntry](
        "unused-index-name",
        typeName = "_doc",
        settings = esSettings
      )
    )
    .map(res =>
      res.error
        .foreach(error => system.log.error(s"Failed to index document: $error"))
    )
    .runWith(Sink.ignore)

}
