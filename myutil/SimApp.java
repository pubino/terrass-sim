package myutil;

/**
 * Interface for simulation applications.
 * Replaces java.applet.Applet lifecycle methods.
 */
public interface SimApp {
    void init();
    void start();
    default void stop() {}
}
