package shady.scheduler

import org.joda.time.DateTime
import org.scalatest._
import org.scalatest.concurrent.Eventually._
import org.scalatest.Matchers._
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Tests for the 'Scheduler' class.
  */
class SchedulerTest extends WordSpec with BeforeAndAfter {

  var scheduler: Scheduler = _

  before {
    scheduler = new Scheduler()
    scheduler.start()
  }

  after {
    scheduler.stop()
  }

  "A scheduler" should {
    "run a single task at scheduled time" in {

      val startTime = DateTime.now().plusSeconds(1)

      // When the scheduled task is executed
      var taskExecutionMs: Long = 0

      scheduler.schedule(startTime, Duration.Zero) { id =>
        taskExecutionMs = System.currentTimeMillis()
      }

      eventually(timeout(2 seconds)) {
        taskExecutionMs should equal (startTime.getMillis +- scheduler.tickMs)
      }
    }
    "run multiple scheduled tasks at their scheduled times" in {
      val now = DateTime.now()

      // Define task start times
      val startTimes = List(now.plusSeconds(1), now.plusSeconds(1), now.plusSeconds(2), now.plusSeconds(2))

      // Array to store execution times
      val executionTimes: Array[Long] = new Array[Long](startTimes.length)

      // Schedule all the tasks
      for (i <- executionTimes.indices) {
        scheduler.schedule(startTimes(i), Duration.Zero) { id =>
          executionTimes(i) = System.currentTimeMillis()
        }
      }

      eventually(timeout(3 seconds)) {
        for (i <- executionTimes.indices) {
          executionTimes(i) should equal (startTimes(i).getMillis +- scheduler.tickMs)
        }
      }
    }
    "run a scheduled task with given frequency" in {
      val startTime = DateTime.now().plusSeconds(1)
      val frequency = 100 millis

      val executionTimes = mutable.ListBuffer[Long]()

      scheduler.schedule(startTime, frequency) { id =>
        executionTimes.append(System.currentTimeMillis())
      }

      // Wait for recurring task to execute 10 times
      eventually(timeout(3 seconds)) {
        executionTimes.length should equal (10)
      }

      // Verify the actual execution times
      executionTimes.zipWithIndex.foreach {
        case (executionTimeMs, index) =>
          val expectedTimeMs = startTime.getMillis + index * frequency.toMillis
          executionTimeMs should equal (expectedTimeMs +- scheduler.tickMs)
      }
    }
    "cancel a scheduled task" in {
      val startTime = DateTime.now().plusSeconds(1)

      // When the scheduled task is executed
      var taskExecutionMs: Long = 0

      val id = scheduler.schedule(startTime, Duration.Zero) { id =>
        taskExecutionMs = System.currentTimeMillis()
      }

      scheduler.cancel(id)

      // Wait for two seconds
      Thread.sleep(2000)

      // Verify task was never executed
      taskExecutionMs should equal (0)
    }
  }
}
