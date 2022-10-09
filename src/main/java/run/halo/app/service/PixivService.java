package run.halo.app.service;

import run.halo.app.model.dto.PixivImgRankDTO;
import run.halo.app.model.support.UploadResult;
import java.util.List;
import java.util.Map;

/**
 * Copyright © 2021年 halo. All rights reserved.
 *
 * @author 古今
 * <p>
 * xx
 * @date 2021/9/16
 * <p>
 * Modification History:
 * Date     Author    Version      Description
 * ---------------------------------------------------------*
 * 2021/9/16   古今   v1.0.0       新增
 */
public interface PixivService {
    /**
     * 生成p站图片
     */
    void generatePixivImg();

    /**
     * 下载p站图片
     *
     * @return 下载结果集合
     */
    List<UploadResult> downloadPixivImg();

    /**
     * 删除过期P站图片
     */
    void deletePixivImg();

    /**
     * 复制p站图片到上传文件夹
     *
     * @param pixivImgMap p站图片map
     * @return p站图片map
     */
    Map<String, String> copyPixivPhoto(Map<String, String> pixivImgMap);
}
