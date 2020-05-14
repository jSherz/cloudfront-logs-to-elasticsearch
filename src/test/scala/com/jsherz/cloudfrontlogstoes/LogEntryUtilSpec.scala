package com.jsherz.cloudfrontlogstoes

import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date

import org.scalatest.funspec.AnyFunSpecLike

// scalastyle:off magic.number

class LogEntryUtilSpec extends AnyFunSpecLike {

  private val testEntry = LogEntry(
    date = Date.from(Instant.parse("2020-02-14T18:24:03Z")),
    request = LogEntryRequest(
      ip = "123.123.123.123",
      method = "GET",
      host = "d2o75d34ulysbt.cloudfront.net",
      uriStem = "/feed.xml",
      referer = None,
      userAgent = "-",
      uriQuery = None,
      cookie = None,
      hostHeader = "jsherz.com",
      protocol = "https",
      bytes = 68,
      forwardedFor = None,
      sslProtocol = Some("TLSv1.2"),
      sslCipher = Some("ECDHE-RSA-AES128-GCM-SHA256"),
      protocolVersion = "HTTP/1.1",
      fleStatus = None,
      fleEncryptedFields = None,
      port = Some(59926)
    ),
    response = LogEntryResponse(
      bytes = 203102,
      status = 200,
      contentType = "application/xml",
      contentLen = Some(202601),
      rangeStart = None,
      rangeEnd = None
    ),
    edgeLocation = "HYD50-C1",
    edgeResultType = "Hit",
    edgeRequestId = "LN0-VdP9YQaS0dH4pOQjZzFmNdjpo1Jv9Gwa_VkBm6ApNZS7vWfAUg==",
    timeTaken = 0.083,
    edgeResponseResultType = "Hit",
    timeToFirstByte = 0.079,
    edgeDetailedResultType = "Hit"
  )

  describe("id") {
    it("returns the request ID") {
      val id = LogEntryUtil.id(testEntry)

      assert(id == "LN0-VdP9YQaS0dH4pOQjZzFmNdjpo1Jv9Gwa_VkBm6ApNZS7vWfAUg==")
    }
  }

  describe("indexName") {
    it("returns the correct name") {
      val day1 = Date.from(Instant.parse("2020-02-01T12:24:03Z"))

      assert(
        LogEntryUtil
          .indexName(testEntry.copy(date = day1)) == "cloudfront-logs-2020.02.01"
      )

      val day2 = Date.from(Instant.parse("2019-12-31T18:24:03Z"))

      assert(
        LogEntryUtil
          .indexName(testEntry.copy(date = day2)) == "cloudfront-logs-2019.12.31"
      )
    }
  }

  describe("LogEntry") {
    it("serialises to and deserializes from JSON") {
      import LogEntryJsonProtocol._
      import spray.json._

      assert(
        testEntry.toJson.toString.parseJson.convertTo[LogEntry] == testEntry
      )
    }
  }

}
