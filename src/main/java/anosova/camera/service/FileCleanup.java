package anosova.camera.service;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
@Slf4j
public class FileCleanup {
    static final private String folderPath = "src/tmp";

    public static void deleteFilesOlderThanOneMonth() {
        log.info("Start deleting files older than one month - {}", LocalDate.now());
        try {
            // Преобразуем дату 1 месяц назад
//            LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
            //для проверки установил 1 день. Вернуть обратно
            LocalDate oneMonthAgo = LocalDate.now().minusDays(1);
            Instant threshold = oneMonthAgo.atStartOfDay(ZoneId.systemDefault()).toInstant();

            // Проходим по всем файлам в указанной папке и удаляем те, которые старше 1 месяца
            Files.walk(Paths.get(folderPath), FileVisitOption.FOLLOW_LINKS)
                    .filter(path -> path.toFile().isFile())
                    .filter(path -> {
                        try {
                            return Files.readAttributes(path, BasicFileAttributes.class).creationTime().toInstant().isBefore(threshold);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            log.info("File deleted: {}", path);
                        } catch (Exception e) {
                            log.info("Unable to delete file: {}", path);
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("Finish deleting files older than one month - {}", LocalDate.now());
    }
}