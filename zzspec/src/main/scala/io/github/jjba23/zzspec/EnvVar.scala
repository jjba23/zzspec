package io.github.jjba23.zzspec

final case class EnvVar[T](key: String, value: T) {

  def toPair: (String, String) = (key, value.toString)
}
