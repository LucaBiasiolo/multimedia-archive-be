package biasiolo.luca.archive;

import java.io.File;
import java.sql.*;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NewFileService {

    private static NewFileService newFileServiceInstance = null;
    private final Logger logger = Logger.getLogger("New-file-service-logger");
    private NewFileService() {}

    public static NewFileService getInstance() {
        if (newFileServiceInstance == null) {
            newFileServiceInstance = new NewFileService();
        }
        return newFileServiceInstance;
    }

    public boolean checkFileExtension(NewFile newFile) throws SQLException {
        boolean okay = true;
        if (!Arrays.asList(ArchiveFile.ADMITTED_FILE_EXTENSIONS).contains(newFile.fileExtension)) {
            logger.log(Level.INFO,"Impossibile processare " + newFile.getName() + " in quanto estensione non valida");
            okay = false;
        }
        return okay;
    }

    public void renameAndMoveFile(NewFile newFile) throws SQLException {
        if (newFile.getName().split("\\.")[0].length() == 10) {
            renameTimestampFile(newFile);
        } else if (newFile.getName().startsWith("IMG-") || newFile.getName().startsWith("VID-")) {
            renameIMGVIDFile(newFile);
        } else if (newFile.getName().startsWith("WP-")) {
            renameWPFile(newFile);
        } else if (newFile.getName().split("\\.")[0].length() == 15) {
            renameYearMonthDayHourFile(newFile);
        } else {
            renameMDateFile(newFile);
        }
        // databaseService.getProgNumber(day,month,year);
        Connection connection = DriverManager.getConnection("jdbc:sqlite:archive.db");
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(String.format(
                "select max (prog_number) from files where day =%s and month=%s and year=%s", newFile.day, newFile.month, newFile.year
        ));
        if (resultSet.next()) {
            String maxProgressiveNumber = resultSet.getString("prog_number");
            if (maxProgressiveNumber.equals("NULL")) {
                newFile.progressiveNumber = Integer.parseInt("1");
            } else {
                newFile.progressiveNumber = Integer.parseInt(maxProgressiveNumber) + 1;
            }
        }
        String newFileName = newFile.day + "-" + newFile.month + "-" + Integer.toString(newFile.year).substring(2) + "-" + newFile.progressiveNumber + "." + newFile.fileExtension;
        // TODO: implementare rinominazione file e spostamento nella newFileella opportuna dell'archivio
        // TODO: implementare creazione newFileella dell'anno e del mese se queste ancora non esistono
        Statement insertStatement = connection.createStatement();
        insertStatement.executeQuery(String.format(
                "insert into files values (%s,%s,%d,%d,%d,%d,%s,%s,%d)",
                "NULL", newFileName, newFile.day, newFile.month, newFile.year, newFile.progressiveNumber, newFile.fileExtension, newFile.getAbsolutePath(), newFile.hashCode
        ));
        connection.close();
    }

    private void renameYearMonthDayHourFile(NewFile newFile) {
        String[] pieces = newFile.getName().split("_");
        newFile.year = Integer.parseInt(pieces[0].substring(0, 4));
        newFile.month = Integer.parseInt(pieces[0].substring(4, 6).replaceFirst("^0+(?!$)", ""));
        newFile.day = Integer.parseInt(pieces[0].substring(6).replaceFirst("^0+(?!$)", ""));
    }

    private void renameTimestampFile(NewFile newFile) {
        Timestamp timestamp = new Timestamp(Integer.parseInt(newFile.getName().split("\\.")[0]));
        newFile.day = timestamp.getDay();
        newFile.month = timestamp.getMonth();
        newFile.year = timestamp.getYear();
    }

    private void renameIMGVIDFile(NewFile newFile) {
        String[] pieces = newFile.getName().split("-");
        newFile.year = Integer.parseInt(pieces[1].substring(0, 4));
        newFile.month = Integer.parseInt(pieces[1].substring(4, 6).replaceFirst("^0+(?!$)", ""));
        newFile.day = Integer.parseInt(pieces[1].substring(6).replaceFirst("^0+(?!$)", ""));
    }

    private void renameWPFile(NewFile newFile) {
        String[] pieces = newFile.getName().split("_");
        newFile.year = Integer.parseInt(pieces[1].substring(0, 4));
        newFile.month = Integer.parseInt(pieces[1].substring(4, 6).replaceFirst("^0+(?!$)", ""));
        newFile.day = Integer.parseInt(pieces[1].substring(6).replaceFirst("^0+(?!$)", ""));
    }

    private void renameMDateFile(NewFile newFile) {
        Timestamp lastModifiedTimestamp = new Timestamp(new File(newFile.getAbsolutePath()).lastModified());
        newFile.day = lastModifiedTimestamp.getDay();
        newFile.month = lastModifiedTimestamp.getMonth();
        newFile.year = lastModifiedTimestamp.getYear();
    }
}