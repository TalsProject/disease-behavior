import java.time.LocalDateTime

import scala.util.parsing.json.JSONObject

/**
  * Created by nirle on 3/30/2017.
  */
case class Position(deviceId: Long, email: String, date: LocalDateTime, speed: Double, distance: Double, totalDistance: Double) {
  def dayPart: DayPart =  if(isDaytime) Day else  Night

  def isDaytime = !(isNightBefore || isNightAfter)

  def isNightBefore = date.getHour < 6

  def isNightAfter = date.getHour >= 22
}
