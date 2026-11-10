package ru.itmo.ipkn.signalscope.client;

/** Wait for confirmed, continuously empty snapshots instead of flashing while waiting for data. */
final class EmptySignalNotice {
    private static final long DELAY_NANOS = 500_000_000L;
    private boolean empty;
    private long emptySince;

    void accept(boolean isEmpty, long now) {
        if (!isEmpty) {
            clear();
        } else if (!empty) {
            empty = true;
            emptySince = now;
        }
    }

    boolean isVisible(long now) {
        return empty && now - emptySince >= DELAY_NANOS;
    }

    void clear() {
        empty = false;
        emptySince = 0;
    }
}
