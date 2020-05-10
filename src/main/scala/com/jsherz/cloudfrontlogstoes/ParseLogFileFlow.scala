package com.jsherz.cloudfrontlogstoes

import java.net.URLDecoder
import java.text.SimpleDateFormat

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

/**
  * Takes a log file as an input and emits one or more [[LogEntry]] as a result of parsing it.
  */
class ParseLogFileFlow extends GraphStage[FlowShape[String, LogEntry]] {

  val in: Inlet[String] = Inlet[String]("ParseLogFileFlow.in")
  val out: Outlet[LogEntry] = Outlet[LogEntry]("ParseLogFileFlow.out")

  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  override val shape: FlowShape[String, LogEntry] = FlowShape.of(in, out)

  private def noneIfHyphen(input: String): Option[String] = {
    if (input == "-") None
    else Some(input)
  }

  private def noneIfHyphenInt(input: String): Option[Int] = {
    if (input == "-") None
    else Some(input.toInt)
  }

  private def parseFile(input: String): Iterator[LogEntry] = {
    val lines = input.split("\n")

    if (lines(0) == "#Version: 1.0") {
      // Skip version and header rows
      lines.view
        .slice(2, Integer.MAX_VALUE)
        .map(line => {
          val parts = line.split("\t")

          try {
            LogEntry(
              date = dateFormat.parse(parts(0) + " " + parts(1)),
              request = LogEntryRequest(
                ip = parts(4),
                method = parts(5),
                host = parts(6),
                uriStem = URLDecoder.decode(parts(7), "utf-8"),
                referer = noneIfHyphen(parts(9)),
                userAgent = URLDecoder.decode(parts(10), "utf-8"),
                uriQuery = noneIfHyphen(parts(11)),
                cookie = noneIfHyphen(parts(12)),
                hostHeader = parts(15),
                protocol = parts(16),
                bytes = parts(17).toInt,
                forwardedFor = noneIfHyphen(parts(19)),
                sslProtocol = noneIfHyphen(parts(20)),
                sslCipher = noneIfHyphen(parts(21)),
                protocolVersion = parts(23),
                fleStatus = noneIfHyphen(parts(24)),
                fleEncryptedFields = noneIfHyphen(parts(25)),
                port = noneIfHyphenInt(parts(26))
              ),
              response = LogEntryResponse(
                bytes = parts(3).toInt,
                status = parts(8).toInt,
                contentType = parts(29),
                contentLen = noneIfHyphenInt(parts(30)),
                rangeStart = noneIfHyphenInt(parts(31)),
                rangeEnd = noneIfHyphenInt(parts(32))
              ),
              edgeLocation = parts(2),
              edgeResultType = parts(13),
              edgeRequestId = parts(14),
              timeTaken = parts(18).toDouble,
              edgeResponseResultType = parts(22),
              timeToFirstByte = parts(27).toDouble,
              edgeDetailedResultType = parts(28)
            )
          } catch {
            case ex: Exception =>
              throw new RuntimeException(
                s"Failed to parse log line: $line ($ex)",
                ex
              )
          }
        })
        .iterator
    } else {
      throw new IllegalArgumentException(
        s"Expected first line of file to be a version 1 indicator, was: ${lines(0)}"
      )
    }
  }

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      setHandler(in, new InHandler {

        override def onPush(): Unit = {
          emitMultiple(out, parseFile(grab(in)))
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          pull(in)
        }
      })
    }
}
