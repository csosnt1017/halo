package run.halo.app.controller.admin.api;

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import run.halo.app.model.dto.CategoryDTO;
import run.halo.app.service.PixivService;

/**
 * Copyright © 2021年 halo. All rights reserved.
 *
 * @author 古今
 * <p>
 * 百度api
 * @date 2021/10/12
 * <p>
 * Modification History:
 * Date     Author    Version      Description
 * ---------------------------------------------------------*
 * 2021/10/12   古今   v1.0.0       新增
 */
@RequestMapping("/api/pixiv")
@RestController
public class PixivController {
    @Autowired
    private PixivService pixivService;
    @GetMapping
    public void downloadPixiv() {
        pixivService.generatePixivImg();
    }
}
