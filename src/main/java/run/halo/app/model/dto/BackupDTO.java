package run.halo.app.model.dto;

import lombok.Data;
import java.nio.file.Path;

/**
 * @author ryanwang
 * @date 2019-05-25
 */
@Data
public class BackupDTO {

    private String downloadLink;

    private String filename;

    private Long updateTime;

    private Long fileSize;

    private Path markdownZipPath;
}
