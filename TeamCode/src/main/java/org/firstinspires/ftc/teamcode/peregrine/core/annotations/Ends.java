package org.firstinspires.ftc.teamcode.peregrine.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <h3>Marks a Task class as one that always finishes on its own.</h3>
 *
 * <p>Put it on a task whose run() is guaranteed to eventually return true, such as Drive or WaitTask.
 * Leave it off tasks that run forever, such as TeleopMovement or Localizer. It is read at runtime by
 * Task.ends(), which TeleopTask uses to check that a task given to it can finish. Compound tasks
 * don't use it; they work out whether they end from their children.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Ends {}

