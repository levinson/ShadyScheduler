package shady.scheduler

import org.joda.time.DateTime

import scala.collection.mutable
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration.Duration

/**
  * A thread-safe implementation of the scheduler interface.
  * @param tickMs Defines the 'granularity' of the scheduler. Ready tasks are executed after each tick.
  */
class Scheduler(val tickMs: Long = 10) extends SchedulerIF {

  // Container to hold the scheduled task
  case class ScheduledTask(id: String, f: (String) => Unit, startTime: DateTime, frequency: Duration)

  // Define ordering based on earliest start time
  implicit object ScheduledTaskOrdering extends Ordering[ScheduledTask] {
    def compare(a: ScheduledTask, b: ScheduledTask): Int = {
      b.startTime.compareTo(a.startTime)
    }
  }

  // Priority queue which contains tasks in their execution order
  private var tasks = mutable.PriorityQueue[ScheduledTask]()

  // Whether the scheduler is currently running
  private var running = false

  // Used to assign unique IDs to each scheduled task
  private var id: Int = 0

  def schedule(startTime: DateTime, frequency: Duration)(f: (String) => Unit): String = {
    tasks.synchronized {
      id += 1
      val task = ScheduledTask(id.toString, f, startTime, frequency)
      tasks.enqueue(task)
      task.id
    }
  }

  def cancel(id: String): Unit = {
    tasks.synchronized {
      tasks = tasks.filter(task => task.id != id)
    }
  }

  def start()(implicit ec: ExecutionContext): Unit = {

    if (running) {
      println("Scheduler is already running!")
    }
    else {

      running = true

      // Interval start time
      var startTime = System.currentTimeMillis()

      // Run scheduler in background
      Future {

        while (running) {
          // Wait for a tick
          Thread.sleep(tickMs)

          // Interval end time
          val endTime = System.currentTimeMillis()

          // Execute all tasks scheduled during last interval
          while (tasks.nonEmpty && tasks.head.startTime.getMillis <= endTime) {
            val task = tasks.dequeue()
            if (task.startTime.getMillis < startTime) {
              println(s"Skipping task $id since it was scheduled to run in the past!")
            }
            else {
              // Run task in background
              Future {
                // Execute the task
                task.f(task.id)

                // If recurring frequency is valid
                if (task.frequency.isFinite() && task.frequency.gt(Duration.Zero)) {
                  // Queue the next occurrence of the task
                  val nextStartTime = task.startTime.plus(task.frequency.toMillis)
                  tasks.enqueue(task.copy(startTime = nextStartTime))
                }
              }
            }
          }

          // Update the start time for next interval
          startTime = endTime
        }
      }
    }
  }

  def stop(): Unit = {
    running = false
  }
}
