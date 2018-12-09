package ch.usi.inf.omicron.background;

public class BackgroundSemaphore {

    private int permits;

    public BackgroundSemaphore() {

        permits = 100;

    }

    public boolean acquire() {

        if (permits > 0) {
            permits++;
            return true;
        } else return false;

    }

    public void release() {

        if (permits + 1 < 100)
            permits++;

    }

}
