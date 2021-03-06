import java.io.{FileWriter, PrintWriter}
import java.time.temporal.ChronoUnit
import java.time.{Duration, ZonedDateTime}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by nirle on 7/2/2017.
  */
object locationExtractor extends App {
  val averageWalkingSpeed: Double = 5000d / Duration.of(1, ChronoUnit.HOURS).getSeconds // 5 km/h in m/s
  val speedThreshold = averageWalkingSpeed
  val timeThreshold = Duration.of(10, ChronoUnit.MINUTES)
  val visitTimeThreshold = Duration.of(15, ChronoUnit.MINUTES)
  val radiusThreshold = 400

  val dataPoints: Seq[DataPoint] = DAO.loadDataPoints()

  val visits: mutable.Map[Long, ListBuffer[Visit]] = mutable.Map.empty
  val locations: mutable.Map[Long, ListBuffer[Location]] = mutable.Map.empty

  var locationOpt: Option[Location] = None
  var arrivalOpt: Option[ZonedDateTime] = None
  var departureOpt: Option[ZonedDateTime] = None

  dataPoints.groupBy(dp => dp.deviceId).foreach{ case (deviceId, byDevice) => {
    visits += deviceId -> ListBuffer.empty
    locations += deviceId -> ListBuffer.empty

    (locationOpt, arrivalOpt, departureOpt)  match {
      case (Some(location), Some(arrival), Some(departure)) => {
        if(arrival.until(departure, ChronoUnit.SECONDS) >= visitTimeThreshold.getSeconds) {
          val mergedLocation = location.merge(locations(deviceId))
          visits(deviceId) += Visit(mergedLocation, arrival, departure)
        }
        locationOpt = None
        arrivalOpt = None
        departureOpt = None
      }
      case _ => ()
    }

    byDevice.sortBy(_.date.toEpochSecond).foreach(dataPoint => {
      (locationOpt, arrivalOpt, departureOpt) match {
        case (Some(location), Some(arrival), Some(departure)) => {
          val time = departure.until(dataPoint.date, ChronoUnit.SECONDS)
          if(time <= timeThreshold.getSeconds && location.distanceTo(dataPoint.geoPosition) / time <= speedThreshold) {
            val candidateLocation = location + dataPoint.geoPosition
            if(candidateLocation.radius <= radiusThreshold) {
              locationOpt = Some(candidateLocation)
              departureOpt = Some(dataPoint.date)
            } else {
              if(arrival.until(departure, ChronoUnit.SECONDS) >= visitTimeThreshold.getSeconds) {
                val mergedLocation = location.merge(locations(deviceId))
                visits(deviceId) += Visit(mergedLocation, arrival, departure)
              }
              locationOpt = None
              arrivalOpt = None
              departureOpt = None
            }
          } else {
            if(arrival.until(departure, ChronoUnit.SECONDS) >= visitTimeThreshold.getSeconds) {
              val mergedLocation = location.merge(locations(deviceId))
              visits(deviceId) += Visit(mergedLocation, arrival, departure)
            }
            locationOpt = None
            arrivalOpt = None
            departureOpt = None
          }
        }
        case _ => {
          locationOpt = Some(Location(dataPoint.geoPosition))
          arrivalOpt = Some(dataPoint.date)
          departureOpt = Some(dataPoint.date)
        }
      }
    })
  }}

  val writer: PrintWriter = new PrintWriter(new FileWriter("visits.json"))
  writer.println(visits.toString())
  writer.close()
}
