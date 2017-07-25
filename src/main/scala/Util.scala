/**
  * Created by yarik on 25.07.17.
  */
object Util {
  def millisToMinutes(milliseconds: Long): Int = ((milliseconds / 60000) % 60).toInt
}
