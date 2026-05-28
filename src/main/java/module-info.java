/**
 * The org.mavai.outcome library.
 *
 * <p>A small sealed-type domain for representing the success-or-failure
 * outcome of a computation as data rather than as exceptions, plus
 * helpers for boundary failure classification, retry policies, and
 * operational reporting (log4j, slf4j, slack, teams, metrics).
 *
 * <p>Note: this {@code module-info.java} is present in the working copy
 * to support punit's JPMS experiment. Releasing it requires a new
 * outcome version.
 */
module org.mavai.outcome {
    exports org.mavai.outcome;
    exports org.mavai.outcome.boundary;
    exports org.mavai.outcome.ops;
    exports org.mavai.outcome.ops.log4j;
    exports org.mavai.outcome.ops.metrics;
    exports org.mavai.outcome.ops.slack;
    exports org.mavai.outcome.ops.teams;
    exports org.mavai.outcome.retry;

    requires java.net.http;
    requires java.sql;
    requires static org.slf4j;
}
