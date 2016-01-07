package shady.scheduler

import org.joda.time.DateTime
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

/**
  * A simple scheduler interface.
  */
trait SchedulerIF {

  /**
    * Schedule a function 'f' to execute at startTime and recur with given frequency.
    * @param startTime the time when the function should be executed.
    * @param frequency the frequency at which the function should be exected after start time.
    * @param f the function to execute at scheduled time(s). The function is applied with the task ID.
    * @return the ID of the scheduled task
    */
  def schedule(startTime: DateTime, frequency: Duration)(f: (String) => Unit): String

  /**
    * Cancel a scheduled task by its ID.
    * @param id the ID of the scheduled task to cancel.
    */
  def cancel(id: String): Unit

  /**
    * Starts the scheduler. Tasks are executed according to start time and frequency.
    * @param ec the execution context used by scheduler and scheduled tasks.
    */
  def start()(implicit ec: ExecutionContext): Unit

  /**
    * Stops the scheduler. No new tasks will be executed (although currently running tasks will run to completion).
    */
  def stop(): Unit
}
