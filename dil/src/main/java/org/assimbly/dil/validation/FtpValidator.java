package org.assimbly.dil.validation;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.assimbly.dil.validation.beans.FtpSettings;
import org.assimbly.dil.validation.jsch.JschConfig;
import org.assimbly.util.error.ValidationErrorMessage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FtpValidator {

    private static final int TIMEOUT = 3000;
    private static final ValidationErrorMessage UNREACHABLE_ERROR =
            new ValidationErrorMessage("Cannot login into FTP Server!");

    public ValidationErrorMessage validate(FtpSettings ftpSettings) {

        if (ftpSettings.getProtocol().equalsIgnoreCase("ftp"))
            return checkFtpConnection(ftpSettings.getUser(), ftpSettings.getPwd(), ftpSettings.getHost(), ftpSettings.getPort(), false);
        else if (ftpSettings.getProtocol().equalsIgnoreCase("ftps"))
            return checkFtpConnection(ftpSettings.getUser(), ftpSettings.getPwd(), ftpSettings.getHost(), ftpSettings.getPort(), true);
        else
            return checkSFtpConnection(ftpSettings.getUser(), ftpSettings.getPwd(), ftpSettings.getHost(), ftpSettings.getPort(), ftpSettings.getPkf(), ftpSettings.getPkfd());
    }

    private Session setupDefaultSession(JSch jsch, String userName, String host, int port) throws JSchException {
        Session session = jsch.getSession(userName, host, port);
        session.setConfig("StrictHostKeyChecking", "no");
        JschConfig.enableExtraConfigOnJsch(jsch);

        return session;
    }

    private ValidationErrorMessage checkSFtpConnection(String userName, String password, String host, int port, String privateKeyFilePath, String privateKeyFileData) {
        Session session = null;
        Channel channel = null;
        File tempFile = null;
        JSch jsch;

        try {
            jsch = new JSch();

            session = setupDefaultSession(jsch, userName, host, port);

            if (password != null && !password.isEmpty())
                session.setPassword(password);

            if (privateKeyFileData != null && !privateKeyFileData.isEmpty()) {
                tempFile = File.createTempFile("temp", "");
                try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(tempFile.toURI()), StandardCharsets.UTF_8)){
                    writer.write(privateKeyFileData);
                }
                jsch.addIdentity(tempFile.getAbsolutePath());
            }
            else if (privateKeyFilePath != null && !privateKeyFilePath.isEmpty())
                jsch.addIdentity(privateKeyFilePath);

            session.connect(TIMEOUT);

            channel = session.openChannel("sftp");
            channel.connect();

            if (!channel.isConnected())
                return UNREACHABLE_ERROR;

        } catch (Exception e) {
            return handleSftpException(e);
        } finally {
            if (channel != null)
                channel.disconnect();
            if (session != null)
                session.disconnect();
            if (tempFile != null)
                tempFile.delete();
        }

        return null;
    }

    private ValidationErrorMessage handleSftpException (Exception exception) {
        Throwable cause = exception.getCause();

        if (cause != null) {
            return getMessageFromCause(cause);
        } else {
            return getMessageFromException(exception);
        }
    }

    private ValidationErrorMessage getMessageFromCause (Throwable cause) {
        if (cause instanceof ConnectException) {
            return new ValidationErrorMessage("Connection refused");
        } else if (cause instanceof UnknownHostException) {
            return new ValidationErrorMessage("Host name could not be resolved");
        } else {
            return new ValidationErrorMessage(cause.getMessage());
        }
    }

    private ValidationErrorMessage getMessageFromException (Exception exception) {
        if (exception.getMessage().contains("ConnectException")) {
            return new ValidationErrorMessage("Connection refused");
        } else if (exception.getMessage().contains("UnknownHostException")) {
            return new ValidationErrorMessage("Host name could not be resolved");
        } else {
            return new ValidationErrorMessage(exception.getMessage());
        }
    }

    private ValidationErrorMessage checkFtpConnection(String userName, String password, String host, int port, boolean secure) {
        FTPClient ftp = null;

        try {
            if(secure) {
                ftp = new FTPSClient();
            } else {
                ftp = new FTPClient();
            }

            ftp.connect(host, port);

            // After connection attempt, you should check the reply code to verify success.
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();

                return UNREACHABLE_ERROR;
            }

            if (!ftp.login(userName, password)) {
                ftp.logout();

                return UNREACHABLE_ERROR;
            }
        } catch (ConnectException e){
            return new ValidationErrorMessage("Connection refused");
        } catch (UnknownHostException e) {
            return new ValidationErrorMessage("Host name could not be resolved");
        } catch (Exception e) {
            return new ValidationErrorMessage(e.getMessage());
        } finally {
            if (ftp != null && ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                    // Do nothing
                }
            }
        }

        return null;
    }
}
