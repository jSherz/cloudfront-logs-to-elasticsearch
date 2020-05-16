package com.jsherz.cloudfrontlogstoes

import java.nio.file.{Files, Path}

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.testkit.TestKit
import com.jsherz.cloudfrontlogstoes.LogEntryJsonProtocol._
import org.scalatest.funspec.AnyFunSpecLike
import spray.json._

import scala.concurrent.Await
import scala.concurrent.duration._

class ParseLogFileFlowSpec
    extends TestKit(ActorSystem("ParseLogFileFlowSpec"))
    with AnyFunSpecLike {

  private val testFile =
    Files.readString(
      Path.of(getClass.getClassLoader.getResource("file").toURI)
    )

  private val parsedEntries =
    Files.readString(
      Path.of(getClass.getClassLoader.getResource("parsed-entries.json").toURI)
    )

  it("parses a file into multiple log entries") {
    val resultFuture = Source
      .single(testFile)
      .viaMat(new ParseLogFileFlow)(Keep.right)
      .runWith(Sink.seq)

    val result = Await.result(resultFuture, 3 seconds)

    assert(result.length === 2)
    assert(result == parsedEntries.parseJson.convertTo[Seq[LogEntry]])
  }

}
