package ru.itmo.ipkn.signalscope.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmptySignalNoticeTest {
    private static final long MS = 1_000_000L;
    private final EmptySignalNotice notice = new EmptySignalNotice();

    @Test
    void doesNotShowBeforeTheFirstSnapshotEvenIfTheServerIsSlow() {
        assertFalse(notice.isVisible(10_000 * MS));
    }

    @Test
    void showsAfterHalfASecondOfConfirmedEmptySnapshots() {
        notice.accept(true, 100 * MS);
        assertFalse(notice.isVisible(100 * MS));
        notice.accept(true, 350 * MS);
        assertFalse(notice.isVisible(599 * MS));
        assertTrue(notice.isVisible(600 * MS));
    }

    @Test
    void aPlayerArrivingDuringTheDelayCancelsTheMessage() {
        notice.accept(true, 0);
        notice.accept(false, 250 * MS);
        assertFalse(notice.isVisible(1_000 * MS));
        notice.accept(true, 1_000 * MS);
        assertFalse(notice.isVisible(1_499 * MS));
        assertTrue(notice.isVisible(1_500 * MS));
    }

    @Test
    void puttingTheScopeAwayOrChangingWorldResetsTheDelay() {
        notice.accept(true, 0);
        assertTrue(notice.isVisible(500 * MS));
        notice.clear();
        assertFalse(notice.isVisible(1_000 * MS));
        notice.accept(true, 1_000 * MS);
        assertFalse(notice.isVisible(1_000 * MS));
        assertTrue(notice.isVisible(1_500 * MS));
    }
}
