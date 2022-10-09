package run.halo.app.model.dto;

/**
 * Copyright © 2019年 erciyuanboot. All rights reserved.
 *
 * @author 古今
 * <p>
 * xxxxx类
 * @date 2019/09/29
 * <p>
 * Modification History:
 * Date     Author    Version      Description
 * ---------------------------------------------------------*
 * 2019/09/29   古今   v1.0.0       新增
 */
public class PixivImgRankDTO {
    private String userId;

    private String userName;

    private String illustId;

    private String title;

    private String url;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getIllustId() {
        return illustId;
    }

    public void setIllustId(String illustId) {
        this.illustId = illustId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public PixivImgRankDTO(String userId, String userName, String illustId, String title, String url) {
        this.userId = userId;
        this.userName = userName;
        this.illustId = illustId;
        this.title = title;
        this.url = url;
    }

    public PixivImgRankDTO(){}
}