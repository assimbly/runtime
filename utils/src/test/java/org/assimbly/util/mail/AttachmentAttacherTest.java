package org.assimbly.util.mail;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AttachmentAttacherTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 2, 3, 4, 5, 6_000_000);

    @Test
    void generatedFileNameHasTheExtensionOfTheMimeType() {
        assertThat(AttachmentAttacher.generatedFileName("application/pdf", NOW)).isEqualTo("2026-01-02-03-04-05-006.pdf");
    }

    @Test
    void generatedFileNameOfAnInvalidMimeTypeHasNoExtension() {
        assertThat(AttachmentAttacher.generatedFileName("not a mime type", NOW)).isEqualTo("2026-01-02-03-04-05-006");
    }

}
