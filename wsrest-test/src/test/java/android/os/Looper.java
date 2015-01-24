package android.os;

public class Looper {
    public static Looper getMainLooper() {
        return new Looper();
    }

    public Thread getThread() {
        return null;
    }
}
