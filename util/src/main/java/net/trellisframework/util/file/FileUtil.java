package net.trellisframework.util.file;

import net.trellisframework.core.log.Logger;
import net.trellisframework.http.exception.InsufficientStorageException;
import net.trellisframework.util.constant.Messages;

import java.io.File;
import java.io.IOException;

public class FileUtil {

    public static File createNewFile(String filename) {
        return createNewFile(new File(filename));
    }

    public static File createNewFile(File file) {
        try {
            file.createNewFile();
            return file;
        } catch (IOException e) {
            Logger.error("CreateFileException", e.getMessage(), e);
            throw new InsufficientStorageException(Messages.CAN_NOT_CREATE_FILE);
        }
    }

}
