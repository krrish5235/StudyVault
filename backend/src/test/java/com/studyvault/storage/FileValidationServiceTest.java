package com.studyvault.storage;

import com.studyvault.common.error.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileValidationServiceTest {

    private final FileValidationService validator = new FileValidationService();

    @Test
    void acceptsRealPdfByMagicBytes() {
        byte[] pdf = "%PDF-1.7 real content".getBytes();
        var result = validator.validateDigital(new MockMultipartFile("file", "notes.pdf", "application/pdf", pdf));
        assertThat(result.extension()).isEqualTo("pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
    }

    @Test
    void rejectsExecutableDisguisedAsPdf() {
        byte[] exe = new byte[]{0x4D, 0x5A, (byte) 0x90, 0x00, 0x03}; // MZ header
        MockMultipartFile file = new MockMultipartFile("file", "evil.pdf", "application/pdf", exe);
        assertThatThrownBy(() -> validator.validateDigital(file)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsDisallowedExtensions() {
        MockMultipartFile file = new MockMultipartFile("file", "payload.html", "text/html", "<h1>hi</h1>".getBytes());
        assertThatThrownBy(() -> validator.validateDigital(file)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsOversizedFiles() {
        byte[] big = new byte[(int) (200L * 1024 * 1024 + 2)];
        big[0] = '%'; big[1] = 'P'; big[2] = 'D'; big[3] = 'F'; big[4] = '-';
        MockMultipartFile file = new MockMultipartFile("file", "huge.pdf", "application/pdf", big);
        assertThatThrownBy(() -> validator.validateDigital(file)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void acceptsPngAndRejectsForgedImage() {
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        assertThat(validator.validateImage(new MockMultipartFile("file", "cover.png", "image/png", png))
                .contentType()).isEqualTo("image/png");

        byte[] fake = "GIF89a-not-really".getBytes();
        assertThatThrownBy(() -> validator.validateImage(new MockMultipartFile("file", "x.png", "image/png", fake)))
                .isInstanceOf(BadRequestException.class);
    }
}
