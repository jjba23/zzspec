package io.github.jjba23.zzspec

import zio._

import java.io.FileWriter
import java.nio.file.{Files, Path, Paths}

/** ZZContract provides useful functions to perform contract testing and to more
  * easily persist (to /resources) the data from effectful calls.
  */
object ZZContract {

  /** Transform a name into a dash-separated, no white-space name, suitable for
    * file names.
    *
    * @return
    */
  def mkContractName: String => String = (x: String) =>
    x.strip
      .toLowerCase()
      .replaceAll("\\s", "-")
      .replaceAll("[^A-Za-z0-9\\-]", "-")
      .replaceAll("-{2,}", "-")

  /** Retrive a contract's data from the filesystem, optionally creating it when
    * it doesn't exist.
    *
    * @param name
    * @param modulePath
    * @param extension
    * @param orElse
    * @return
    */
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

  /** Write a contract's data to the wanted destination in the filesystem.
    *
    * @param fileName
    * @param content
    * @return
    */
  def writeContractToFile(
    fileName: String
  )(content: String): ZIO[Any with Scope, Throwable, Unit] =
    for {
      w <-
        ZIO.acquireRelease(ZIO.attempt(new FileWriter(fileName)))(w =>
          ZIO.attempt(w.close()).orDie
        )
      _ <- ZIO.attempt(w.write(content))
    } yield ()

  /** Retrieve a contract from the filesystem based on the test's name and
    * module, or create a new file with the expected data and retrieve that.
    *
    * @param name
    * @param modulePath
    * @param orElse
    * @return
    */
  def readOrCreateContract(
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
