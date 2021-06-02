package au.com.meetup.drawer;


import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileInputStream;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RafflePicker {

    private static Logger logger = Logger.getLogger(RafflePicker.class.getName());

    private static StringBuilder finalOutput = new StringBuilder();

    private static final Row.MissingCellPolicy policy = Row.MissingCellPolicy.CREATE_NULL_AS_BLANK;

    private static final SecureRandom random = new SecureRandom();

    // For you non-programmers, everything starts at 0 instead of 1

    private static final int USER_NAME_FIELD = 0;

    private static final int USER_ID_FIELD = 1;

    private static final int RSVP_TIME = 6;

    private static final int NUMBER_OF_WINNERS = 12;

    public static void main(String [] args) {

        if (args == null || args.length == 0 || StringUtils.isEmpty(args[0])) {
            logger.severe("No file name passed");
            System.exit(-1);
        }

        logOutput(String.format("Running with %s",args[0]));

        File file = new File(args[0]);

        if (!file.canRead()) {
            logOutput("File cannot be read");
        }

        try {

            Map<String,String> participants = new HashMap<>();

            FileInputStream fis = new FileInputStream(file);

            Workbook workbook = new HSSFWorkbook(fis);

            if (workbook.getNumberOfSheets() == 0) {
                throw new Exception("No sheets in the workbook read");
            }

            Sheet sheet = workbook.getSheetAt(0);

            int totalRows = sheet.getLastRowNum();

            // First row appears to be a header row
            for (int i=1;i<=totalRows;i++) {

                Row row = sheet.getRow(i);

                if (row == null || row.getFirstCellNum() == row.getLastCellNum()) {
                    continue;
                }

                String name = row.getCell(USER_NAME_FIELD,policy).getStringCellValue();
                String userId = row.getCell(USER_ID_FIELD,policy).getStringCellValue();
                String rsvpDate = row.getCell(RSVP_TIME,policy).getStringCellValue();

                if (StringUtils.isEmpty(name)) {
                    continue;
                }

                // I can't really win a competition I'm running
                if (name.equalsIgnoreCase("Andrew Crawford")) {
                    continue;
                }

                logOutput(String.format("Adding participant: %s (%s) who entered at %s",name,userId,rsvpDate));

                participants.put(userId,name);
            }

            logOutput(String.format("Participants %d",participants.size()));

            List<String> drawList = participants.keySet().stream().collect(Collectors.toList());

            Set<String> finalWinners = new LinkedHashSet<>();

            if (drawList.size() <= NUMBER_OF_WINNERS) {
                // Just in case. It's still fair at least
                logOutput(String.format("There was less than %d winners. Everyone who entered will win. Congratulations",NUMBER_OF_WINNERS));
                finalWinners.addAll(drawList);
            } else {
                // Let the drawing begin

                do {
                    int nextNumber = Math.abs(random.nextInt(drawList.size()));
                    String winner = drawList.remove(nextNumber);
                    // Remove will remove the entry from the list + return the value it found, so the list gets smaller

                    if (!finalWinners.contains(winner)) {
                        finalWinners.add(winner);
                    }
                } while (finalWinners.size() < NUMBER_OF_WINNERS);

            }

            logOutput("Final winners are:");
            finalWinners.forEach(winner -> {
                logOutput(String.format("Name: %s (%s)",participants.get(winner),winner));
            });
            logOutput("Thanks to everyone who entered");

            logger.info("Final digest of all output (excluding this line): " +
                    Base64.getEncoder().encodeToString(DigestUtils.sha512(finalOutput.toString())));

        } catch (Exception e) {
            logger.log(Level.SEVERE,"Something went wrong: "+ e.getMessage(),e);
            System.exit(-1);
        }



    }

    /**
     * Logs everything sent to the console and keeps track of the characters.
     * Used so the program can print a digest of what was shown.
     *
     * @param str
     */
    private static void logOutput(String str) {
        logger.info(str+"\n");
        finalOutput.append(str+"\n");
    }
}
