package run.halo.app.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.CopyObjectResult;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.halo.app.handler.file.AliOssFileHandler;
import run.halo.app.handler.file.FileHandler;
import run.halo.app.handler.file.LocalFileHandler;
import run.halo.app.model.dto.UploadDTO;
import run.halo.app.model.entity.Attachment;
import run.halo.app.model.entity.Photo;
import run.halo.app.model.enums.AttachmentType;
import run.halo.app.model.params.AttachmentQuery;
import run.halo.app.model.params.PhotoQuery;
import run.halo.app.model.properties.AliOssProperties;
import run.halo.app.model.properties.AttachmentProperties;
import run.halo.app.model.support.PixivConst;
import run.halo.app.model.support.UploadResult;
import run.halo.app.service.AttachmentService;
import run.halo.app.service.OptionService;
import run.halo.app.service.PhotoService;
import run.halo.app.service.PixivService;
import run.halo.app.utils.HaloUtils;
import run.halo.app.utils.PixivUtil;

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
@Slf4j
@Service
public class PixivServiceImpl implements PixivService {
    @Autowired
    private OptionService optionService;

    @Autowired
    private AliOssFileHandler fileHandlers;

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private PhotoService photoService;

    /**
     * 生成p站图片
     */
    @Override
    @Scheduled(cron = "0 0 11 * * ?")
    public void generatePixivImg() {
        log.info("定时任务--generatePixivImg开始执行");
        try {
            checkConnect();
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            return;
        }
        deletePixivImg();
        List<UploadResult> uploadResultList = downloadPixivImg();
        uploadResultList.forEach(uploadResult -> {
            saveAttachment(uploadResult);
            savePhoto(uploadResult);
        });
        log.info("定时任务--generatePixivImg执行完毕");
    }

    /**
     * 下载p站每日排行图片
     */
    @Override
    public List<UploadResult> downloadPixivImg() {
        List<Map<String, Object>> imgList =
            PixivUtil.getImgList(PixivUtil
                .getRankUrl("1", PixivConst.Mode.MODE_DAILY, PixivConst.Content.CONTENT_ILLUST));
        List<UploadResult> uploadResultList = Lists.newArrayListWithExpectedSize(imgList.size());
        imgList.forEach(j -> {
            String illustId = ((Integer) j.get("illust_id")).toString();
            String originalFilename = (String) j.get("title");
            Integer width = (Integer) j.get("width");
            Integer height = (Integer) j.get("height");
            String detailImgUrl = PixivUtil.getImageUrl(PixivUtil.getDetailUrl(illustId));
            InputStream imgInputStream =
                PixivUtil.getImgInputStream(detailImgUrl, PixivUtil.getDetailUrl(illustId));
            String fileType = getFileType(detailImgUrl);
            // Upload file
            try {
                UploadDTO uploadDTO =
                    new UploadDTO(imgInputStream, originalFilename + "." + fileType,
                        "image/" + fileType,
                        (long) imgInputStream.available(), FileHandler.FILE_TYPE_PIXIV, width,
                        height, "pixiv");
                uploadResultList.add(fileHandlers.upload(uploadDTO));
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        });
        log.info("success upload pixiv");
        return uploadResultList;
    }

    /**
     * 删除过期P站图片
     */
    @Override
    @Transactional
    public void deletePixivImg() {

        String path =
            FileHandler.FILE_TYPE_PIXIV + "/";

        // 删除附件(使用本地存储需注意附件比图片的路径多个/)
        AttachmentQuery attachmentQuery = new AttachmentQuery();
        attachmentQuery.setPath(path);
        List<Attachment> attachmentList = attachmentService.listByCondition(attachmentQuery);
        List<String> urlList =
            attachmentList.stream().map(Attachment::getPath)
                .collect(Collectors.toList());
        attachmentService.deleteByList(attachmentList);

        // 删除照片
        PhotoQuery photoQuery = new PhotoQuery();
        photoQuery.setUrlList(urlList);
        List<Photo> photoList = photoService.listByCondition(photoQuery);
        photoService.deleteByList(photoList);


        //删除文件
        fileHandlers
            .deleteByPrefix(fileHandlers.getUpFilePathPrefix("pixiv").toString());
        log.info("success delte pixiv");
    }

    private void deleteFiles(File dirOrFile) {
        if (dirOrFile.isDirectory()) {
            File[] files = dirOrFile.listFiles();
            if (files.length == 0) {
                dirOrFile.delete();
                return;
            }
            for (File file : files) {
                deleteFiles(file);
            }
        } else {
            dirOrFile.delete();
        }
    }

    private void saveAttachment(UploadResult uploadResult) {
        // Build attachment
        Attachment attachment = new Attachment();
        attachment.setName(uploadResult.getFilename());
        // Convert separator
        attachment.setPath(HaloUtils.changeFileSeparatorToUrlSeparator(uploadResult.getFilePath()));
        attachment.setFileKey(uploadResult.getKey());
        attachment.setThumbPath(uploadResult.getThumbPath());
        attachment.setMediaType(uploadResult.getMediaType().toString());
        attachment.setSuffix(uploadResult.getSuffix());
        attachment.setWidth(uploadResult.getWidth());
        attachment.setHeight(uploadResult.getHeight());
        attachment.setSize(uploadResult.getSize());
        attachment.setType(Objects.requireNonNull(optionService
            .getEnumByPropertyOrDefault(AttachmentProperties.ATTACHMENT_TYPE, AttachmentType.class,
                AttachmentType.LOCAL)));

        log.debug("Creating attachment: [{}]", attachment);

        attachmentService.create(attachment);
    }

    private void savePhoto(UploadResult uploadResult) {
        // Build attachment
        Photo photo = new Photo();
        photo.setName(uploadResult.getFilename());
        // Convert separator
        photo.setTakeTime(new Date());
        photo.setUrl(HaloUtils.changeFileSeparatorToUrlSeparator(uploadResult.getFilePath()));
        photo.setThumbnail(uploadResult.getThumbPath());
        photo.setTeam("Pixiv今日排行");
        log.debug("Creating photo: [{}]", photo);

        photoService.create(photo);
    }

    /**
     * 复制p站图片到上传文件夹
     *
     * @param pixivImgMap p站图片map
     * @return p站图片map
     */
    public Map<String, String> copyPixivPhoto(Map<String, String> pixivImgMap) {
        AttachmentQuery attachmentQuery = new AttachmentQuery();
        Map<Integer, String> repeatMap = Maps.newHashMap();
        List<UploadResult> uploadResultList = Lists.newArrayList();
        String bucketName =
            optionService.getByPropertyOfNonNull(AliOssProperties.OSS_BUCKET_NAME).toString();
        Map<String, String> newPixivImgMap = pixivImgMap.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                String value = entry.getValue();
                attachmentQuery.setPath(value.substring(1));
                List<Attachment> attachmentList =
                    attachmentService.listByCondition(attachmentQuery);
                String newFilePath = "";
                Attachment attachment = attachmentList.get(0);
                if (!repeatMap.containsKey(attachment.getId())) {
                    OSS ossClient = fileHandlers.createOSSClient();
                    String destinationKey = attachment.getFileKey().replace("pixiv/", "");
                    // 拷贝文件。
                    ossClient.copyObject(bucketName, attachment.getFileKey(), bucketName,
                        destinationKey);
                    newFilePath = attachment.getPath().replace("pixiv/", "");
                    repeatMap.put(attachment.getId(), newFilePath);
                    UploadResult uploadResult = new UploadResult();
                    uploadResult.setFilename(attachment.getName());
                    uploadResult
                        .setFilePath(newFilePath);
                    uploadResult.setKey(destinationKey);
                    uploadResult
                        .setMediaType(
                            MediaType.valueOf(Objects.requireNonNull(attachment.getMediaType())));
                    uploadResult.setSuffix(attachment.getSuffix());
                    uploadResult.setSize(attachment.getSize());
                    uploadResultList.add(uploadResult);
                } else {
                    newFilePath = repeatMap.get(attachment.getId());
                }
                return newFilePath;
            }));
        uploadResultList.forEach(this::saveAttachment);
        return newPixivImgMap;
    }

    private String getFileType(String url) {
        return url.substring(url.lastIndexOf(".") + 1);
    }

    private void checkConnect() {
        PixivUtil.getImgList(PixivUtil
            .getRankUrl("1", PixivConst.Mode.MODE_DAILY, PixivConst.Content.CONTENT_ILLUST));
    }

}
