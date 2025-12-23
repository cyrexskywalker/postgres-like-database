package memory.io;

public interface DirtyPageWriter {
    void startBackgroundWriter();
    void startCheckPointer();
}
