package zzspec

import org.apache.kafka.common.config.TopicConfig
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import zio._
import zio.process.Command
import zio.logging._
import zio.logging.slf4j.bridge.Slf4jBridge
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import zzspec.kafka.NewTopic
import java.nio.file.{Files, Paths}
import zio.test._
import java.nio.file.Path
import java.io.FileWriter

object ZZContract {

  def mkContractName: String => String = (x: String) =>
    x.strip.toLowerCase().replaceAll("\\s", "-").replaceAll("[^A-Za-z0-9\\-]", "-").replaceAll("-{2,}", "-")

  def contractFromTestName(
    name: String,
    className: String = this.getClass().toString(),
    extension: String = ".json",
    orElse: Option[String] = None
  ): ZIO[Any with Scope, Throwable, String] = {
    val newName = mkContractName(name)
    val newClassName = mkContractName(className)
    readContractOrCreate(s"$newName-$newClassName${extension}", orElse)
  }

  def writeContractToFile(fileName: String, content: String): ZIO[Any with Scope, Throwable, Unit] = {
    for {
      w <- ZIO.acquireRelease(ZIO.attempt(new FileWriter(fileName)))(w => ZIO.attempt(w.close()).orDie)
      _ <- ZIO.attempt(w.write(content))
    } yield ()
  }

  def readContractOrCreate(
    fileName: String,
    orElse: Option[String]
  ): ZIO[Any with Scope, Throwable, String] = {
    val filePath = Paths.get("zzspec", "src", "main", "resources", "contracts", fileName)

    for {
      fileExists <- ZIO.attempt(Files.exists(filePath)).orElseSucceed(false)
      contents <-
        if (fileExists) {
          ZIO.attempt(Files.readString(filePath))
        } else {
          ZIO.attempt(Files.createFile(filePath)) *> (if (orElse.isDefined) {
                                                        writeContractToFile(filePath.toString(), orElse.get)
                                                      } else { ZIO.unit }) *> ZIO
            .attempt(Files.readString(filePath))

        }

    } yield contents
  }
}
