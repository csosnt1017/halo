package run.halo.app.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.InputStream;

/**
 * Copyright © 2021年 halo. All rights reserved.
 *
 * @author 古今
 * <p>
 * 上传文件DTO
 * @date 2021/9/21
 * <p>
 * Modification History:
 * Date     Author    Version      Description
 * ---------------------------------------------------------*
 * 2021/9/21   古今   v1.0.0       新增
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadDTO {
    private InputStream in;
    private String originalFilename;
    private String contentType;
    private Long size;
    private String type;
    private Integer width;
    private Integer height;
    private String diyPath;
}
