package noe.bootstrap

import groovy.util.logging.Slf4j
import noe.common.utils.Library
import noe.common.utils.Platform
import noe.workspace.IWorkspace
/**
 * Abstract layer for tasks. Every tasks must inherit from it
 *
 * @author Jan Stefl   <jstefl@redhat.com>
 *
 */
@Slf4j
abstract class TaskAbstract {
  IWorkspace workspace /// Abstraction of task playground. Workspace is easy to share, please use it, as much as possible!
  static Platform platform = new Platform()
  static sep = platform.sep

  TaskAbstract() {
  }

  /**
   * Register tasks
   */
  static def loadTasks() {
    log.debug("Loading tasks started.")
    def tasks = []

    def tasksString = Library.getUniversalProperty('TASKS', '')
    tasksString.toString().tokenize(',').each { taskString ->

      // Try to create task intance
      try {
        def instance = getClass().classLoader.loadClass(taskString)?.newInstance();
        if (instance instanceof TaskAbstract) {
          tasks.add(instance)
        }
      }
      catch (java.lang.ClassNotFoundException e) {
        log.warn("$taskString is not task, skipping ...", e)
      }
      catch (e) {
        log.warn("For $taskString thrown exception", e)
      }
    }

    log.debug("Loading tasks finished.")
    // return registered tasks
    return tasks
  }

  /**
   * Prepare task stuff.
   * DIVIDE
   *
   * Workspace is easy to share, please use it, as much as possible!
   */
  def prepare() {
    workspace.prepare()
  }

  /**
   * Main method for task.
   * AND RULE
   */
  abstract def execute()

  /**
   * Clean the stuff.
   * AND CLEAN THE MESS
   *
   * WORKSPACE is easy to share, please use it, as much as possible!
   */
  def destroy() {
    workspace.destroy()
  }

}
