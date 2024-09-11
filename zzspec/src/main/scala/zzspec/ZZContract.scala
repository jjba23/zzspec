package zzspec

import zio._

import java.io.FileWriter
import java.nio.file.{Files, Path, Paths}

object ZZContract {

  def mkContractName: String => String = (x: String) =>
    x.strip
      .toLowerCase()
      .replaceAll("\\s", "-")
      .replaceAll("[^A-Za-z0-9\\-]", "-")
      .replaceAll("-{2,}", "-")

  def contractFromTestName(
    name: String,
    modulePath: String,
    extension: String = "json",
    orElse: Option[String] = None
  ): ZIO[Any with Scope, Throwable, String] = {
    val newName =
      s"${mkContractName(name)}-${mkContractName(this.getClass().toString())}.${extension}"
        .replace(s"-.$extension", s".$extension")
    readOrCreateContract(
      name = newName,
      modulePath = modulePath,
      orElse = orElse
    )
  }

  private def writeContractToFile(
    fileName: String
  )(content: String): ZIO[Any with Scope, Throwable, Unit] =
    for {
      w <-
        ZIO.acquireRelease(ZIO.attempt(new FileWriter(fileName)))(w =>
          ZIO.attempt(w.close()).orDie
        )
      _ <- ZIO.attempt(w.write(content))
    } yield ()

  private def readOrCreateContract(
    name: String,
    modulePath: String,
    orElse: Option[String] = None,
  ): ZIO[Any with Scope, Throwable, String] = {
    val basePath = Paths.get("src", "main", "resources", "contracts")
    val path     = Path.of(s"$modulePath/" + basePath.toString() + s"/$name")

    val readFileContents    = ZIO.attempt(Files.readString(path))
    val writeFallbackToFile =
      orElse.map(writeContractToFile(path.toString())).getOrElse(ZIO.unit)

    for {
      fileExists <- ZIO.attempt(Files.exists(path)).orElseSucceed(false)
      contents   <-
        if (fileExists) readFileContents
        else
          ZIO.attempt(Files.createFile(path)) *>
          writeFallbackToFile *>
          readFileContents
    } yield contents
  }
}
