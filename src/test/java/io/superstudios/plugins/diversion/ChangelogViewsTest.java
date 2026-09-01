package io.superstudios.plugins.diversion;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ChangelogViewsTest {

    @Test
    void changeLogSetDigestJellyExists() {
        assertNotNull(
                DiversionChangeLogSet.class.getResource("DiversionChangeLogSet/digest.jelly"),
                "Job/build page includes DiversionChangeLogSet/digest.jelly; missing view 500s the page");
    }

    @Test
    void changeLogSetIndexJellyExists() {
        assertNotNull(DiversionChangeLogSet.class.getResource("DiversionChangeLogSet/index.jelly"));
    }

    @Test
    void changeLogEntryDigestJellyExists() {
        assertNotNull(DiversionChangeLogEntry.class.getResource("DiversionChangeLogEntry/digest.jelly"));
    }
}
