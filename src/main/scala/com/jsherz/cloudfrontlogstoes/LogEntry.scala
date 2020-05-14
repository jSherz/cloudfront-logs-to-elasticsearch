package com.jsherz.cloudfrontlogstoes

import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date

import spray.json.{
  DefaultJsonProtocol,
  JsString,
  JsValue,
  JsonFormat,
  RootJsonFormat
}

/**
  * See: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/AccessLogs.html
  */
case class LogEntry(
    date: Date,
    request: LogEntryRequest,
    response: LogEntryResponse,
    edgeLocation: String,
    edgeResultType: String,
    edgeRequestId: String,
    timeTaken: Double,
    edgeResponseResultType: String,
    timeToFirstByte: Double,
    edgeDetailedResultType: String
)

object LogEntryUtil {
  private val indexDateFormat = new SimpleDateFormat("yyyy.MM.dd")

  def id(logEntry: LogEntry): String = logEntry.edgeRequestId

  def indexName(logEntry: LogEntry): String = {
    s"cloudfront-logs-${indexDateFormat.format(logEntry.date)}"
  }
}

case class LogEntryRequest(
    ip: String,
    method: String,
    host: String,
    uriStem: String,
    referer: Option[String],
    userAgent: String,
    uriQuery: Option[String],
    cookie: Option[String],
    hostHeader: String,
    protocol: String,
    bytes: Int,
    forwardedFor: Option[String],
    sslProtocol: Option[String],
    sslCipher: Option[String],
    protocolVersion: String,
    fleStatus: Option[String],
    fleEncryptedFields: Option[String],
    port: Option[Int]
)

case class LogEntryResponse(
    bytes: Int,
    status: Int,
    contentType: String,
    contentLen: Option[Int],
    rangeStart: Option[Int],
    rangeEnd: Option[Int]
)

object LogEntryJsonProtocol extends DefaultJsonProtocol {
  implicit object DateJsonFormat extends RootJsonFormat[Date] {
    override def read(json: JsValue): Date =
      Date.from(Instant.parse(json.convertTo[String]))

    override def write(obj: Date): JsValue = JsString(obj.toInstant.toString)
  }

  implicit val logEntryRequestFormat: JsonFormat[LogEntryRequest] =
    jsonFormat18(LogEntryRequest)

  implicit val logEntryResponseFormat: JsonFormat[LogEntryResponse] =
    jsonFormat6(LogEntryResponse)

  implicit val logEntryJsonFormat: JsonFormat[LogEntry] = jsonFormat10(LogEntry)
}
