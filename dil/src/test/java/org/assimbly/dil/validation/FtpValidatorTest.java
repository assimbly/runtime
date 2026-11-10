package org.assimbly.dil.validation;

import org.assimbly.dil.validation.beans.FtpSettings;
import org.assimbly.util.error.ValidationErrorMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FtpValidatorTest {

    private final FtpValidator validator = new FtpValidator();

    @Test
    void validateRejectsFtpAsInsecure() {
        ValidationErrorMessage result = validator.validate(settings("ftp"));
        assertNotNull(result);
        assertEquals("FTP is an insecure protocol and is not supported. Use SFTP or FTPS.", result.getError());
    }

    @Test
    void validateRejectsUnknownProtocol() {
        ValidationErrorMessage result = validator.validate(settings("scp"));
        assertNotNull(result);
        assertEquals("Unsupported protocol: scp. Use SFTP or FTPS.", result.getError());
    }

    @Test
    void validateSftpReturnsErrorForInvalidInMemoryKey() {
        FtpSettings settings = settings("sftp");
        settings.setPkfd("not-a-private-key");

        assertNotNull(validator.validate(settings));
    }

    private static FtpSettings settings(String protocol) {
        FtpSettings settings = new FtpSettings();
        settings.setProtocol(protocol);
        settings.setUser("user");
        settings.setHost("localhost");
        settings.setPort(22);
        return settings;
    }
}
