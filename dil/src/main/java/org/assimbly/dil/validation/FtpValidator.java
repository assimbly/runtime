package org.assimbly.dil.validation;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.assimbly.dil.validation.beans.FtpSettings;
import org.assimbly.dil.validation.jsch.JschConfig;
import org.assimbly.util.error.ValidationErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class FtpValidator {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private static final int TIMEOUT = 3000;
    private static final ValidationErrorMessage UNREACHABLE_ERROR =
            new ValidationErrorMessage("Cannot login into FTP Server!");
    private static final ValidationErrorMessage INSECURE_PROTOCOL_ERROR =
            new ValidationErrorMessage("FTP is an insecure protocol and is not supported. Use SFTP or FTPS.");

    public ValidationErrorMessage validate(FtpSettings ftpSettings) {

        String protocol = ftpSettings.getProtocol();

        if ("sftp".equalsIgnoreCase(protocol))
            return checkSFtpConnection(ftpSettings.getUser(), ftpSettings.getPwd(), ftpSettings.getHost(), ftpSettings.getPort(), ftpSettings.getPkf(), ftpSettings.getPkfd());
        else if ("ftps".equalsIgnoreCase(protocol))
            return checkFtpsConnection(ftpSettings.getUser(), ftpSettings.getPwd(), ftpSettings.getHost(), ftpSettings.getPort(), ftpSettings.getImplicit());
        else if ("ftp".equalsIgnoreCase(protocol))
            return INSECURE_PROTOCOL_ERROR;
        else
            return new ValidationErrorMessage("Unsupported protocol: " + protocol + ". Use SFTP or FTPS.");
    }

    private Session setupDefaultSession(JSch jsch, String userName, String host, int port) throws JSchException {
        Session session = jsch.getSession(userName, host, port);
        session.setConfig("StrictHostKeyChecking", "no");
        JschConfig.enableExtraConfigOnJsch();

        return session;
    }

    private ValidationErrorMessage checkSFtpConnection(String userName, String password, String host, int port, String privateKeyFilePath, String privateKeyFileData) {
        Session session = null;
        Channel channel = null;
        JSch jsch;

        try {
            jsch = new JSch();

            session = setupDefaultSession(jsch, userName, host, port);

            if (password != null && !password.isEmpty())
                session.setPassword(password);

            if (privateKeyFileData != null && !privateKeyFileData.isEmpty())
                jsch.addIdentity("validation", privateKeyFileData.getBytes(StandardCharsets.UTF_8), null, null);
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

    private ValidationErrorMessage checkFtpsConnection(String userName, String password, String host, int port, boolean implicitMode) {
        FTPSClient ftp = null;

        try {
            ftp = new FTPSClient(implicitMode);
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
        } catch (ConnectException _){
            return new ValidationErrorMessage("Connection refused");
        } catch (UnknownHostException _) {
            return new ValidationErrorMessage("Host name could not be resolved");
        } catch (Exception e) {
            return new ValidationErrorMessage(e.getMessage());
        } finally {
            if (ftp != null && ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException _) {
                    // Do nothing
                }
            }
        }

        return null;
    }
}
