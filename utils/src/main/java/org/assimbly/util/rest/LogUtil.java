package org.assimbly.util.rest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LogUtil {

	/**
	 * The last lines of a UTF-8 file.
	 *
	 * @return the lines, or null when the file can't be read
	 */
	public static String tail(File file, int lines) {

	    try (java.io.RandomAccessFile fileHandler = new java.io.RandomAccessFile( file, "r" )) {

	        long fileLength = fileHandler.length() - 1;
	        // the bytes from the end of the file backwards; decoded as a whole, as a character can take several bytes
	        ByteArrayOutputStream reversed = new ByteArrayOutputStream();
	        int line = 0;

	        for(long filePointer = fileLength; filePointer != -1; filePointer--){
	            fileHandler.seek( filePointer );
	            int readByte = fileHandler.readByte();

	             if( readByte == 0xA ) {
	                if (filePointer < fileLength) {
	                    line = line + 1;
	                }
	            } else if( readByte == 0xD && filePointer < fileLength-1) {
	               line = line + 1;
	            }

	            if (line >= lines) {
	                break;
	            }

	            reversed.write( readByte );
	        }

	        byte[] bytes = reversed.toByteArray();
	        for (int i = 0, j = bytes.length - 1; i < j; i++, j--) {
	            byte swap = bytes[i];
	            bytes[i] = bytes[j];
	            bytes[j] = swap;
	        }

	        return new String(bytes, StandardCharsets.UTF_8);
	    } catch( IOException _ ) {
	        return null;
	    }
	}
}
