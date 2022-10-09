package run.halo.app.handler.file;

import static run.halo.app.model.support.HaloConst.URL_SEPARATOR;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.DeleteObjectsResult;
import com.aliyun.oss.model.DeleteVersionsRequest;
import com.aliyun.oss.model.DeleteVersionsResult;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.ListVersionsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.OSSVersionSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.PutObjectResult;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.aliyun.oss.model.VersionListing;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;
import run.halo.app.exception.FileOperationException;
import run.halo.app.model.dto.UploadDTO;
import run.halo.app.model.enums.AttachmentType;
import run.halo.app.model.properties.AliOssProperties;
import run.halo.app.model.support.UploadResult;
import run.halo.app.service.OptionService;
import run.halo.app.utils.FilenameUtils;
import run.halo.app.utils.ImageUtils;

/**
 * Ali oss file handler.
 *
 * @author MyFaith
 * @author ryanwang
 * @date 2019-04-04
 */
@Slf4j
@Component
public class AliOssFileHandler implements FileHandler {

    private final OptionService optionService;

    public AliOssFileHandler(OptionService optionService) {
        this.optionService = optionService;
    }

    @Override
    public @NonNull
    UploadResult upload(@NonNull MultipartFile file) {
        Assert.notNull(file, "Multipart file must not be null");
        try {
            UploadDTO uploadDTO = new UploadDTO(file.getInputStream(), file.getOriginalFilename(),
                file.getContentType(), file.getSize(), FILE_TYPE_NORMAL, null, null, null);
            return upload(uploadDTO);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("上传附件 " + file.getOriginalFilename() + " 到阿里云失败 ");
        }
    }

    @NotNull
    public UploadResult upload(UploadDTO uploadDTO) {
        String originalFilename = uploadDTO.getOriginalFilename();
        InputStream in = uploadDTO.getIn();
        String contentType = uploadDTO.getContentType();
        // Get config
        String bucketName =
            optionService.getByPropertyOfNonNull(AliOssProperties.OSS_BUCKET_NAME).toString();
        String styleRule =
            optionService.getByPropertyOrDefault(AliOssProperties.OSS_STYLE_RULE, String.class, "");
        String thumbnailStyleRule = optionService
            .getByPropertyOrDefault(AliOssProperties.OSS_THUMBNAIL_STYLE_RULE, String.class, "");

        OSS ossClient = createOSSClient();

        String basePath = getBasePath();

        try {
            final String basename =
                FilenameUtils.getBasename(Objects.requireNonNull(originalFilename));
            final String extension = FilenameUtils.getExtension(originalFilename);
            final String timestamp = String.valueOf(System.currentTimeMillis());

            StringBuilder upFilePath = getUpFilePathPrefix(uploadDTO.getDiyPath());
            upFilePath.append(basename.replace("/", ""))
                .append("_")
                .append(timestamp)
                .append(".")
                .append(extension);

            String filePath = StringUtils.join(basePath, upFilePath.toString());

            log.info(basePath);

            // Upload
            final PutObjectResult putObjectResult = ossClient.putObject(bucketName,
                upFilePath.toString(), uploadDTO.getIn());

            if (putObjectResult == null) {
                throw new FileOperationException("上传附件 " + originalFilename + " 到阿里云失败 ");
            }

            // Response result
            final UploadResult uploadResult = new UploadResult();
            uploadResult.setFilename(basename);
            uploadResult
                .setFilePath(StringUtils.isBlank(styleRule) ? filePath : filePath + styleRule);
            uploadResult.setKey(upFilePath.toString());
            uploadResult
                .setMediaType(MediaType.valueOf(Objects.requireNonNull(contentType)));
            uploadResult.setSuffix(extension);
            uploadResult.setSize(uploadDTO.getSize());

            Integer height = uploadDTO.getHeight();
            Integer width = uploadDTO.getWidth();
            if (width != null && height != null) {
                uploadResult.setHeight(height);
                uploadResult.setWidth(width);
                uploadResult.setThumbPath(StringUtils.isBlank(thumbnailStyleRule) ? filePath :
                    filePath + thumbnailStyleRule);
            } else {
                handleImageMetadata(in, contentType, uploadResult, () -> {
                    if (ImageUtils.EXTENSION_ICO.equals(extension)) {
                        return filePath;
                    } else {
                        return StringUtils.isBlank(thumbnailStyleRule) ? filePath :
                            filePath + thumbnailStyleRule;
                    }
                });
            }

            log.info("Uploaded file: [{}] successfully", originalFilename);
            return uploadResult;
        } catch (Exception e) {
            throw new FileOperationException("上传附件 " + originalFilename + " 到阿里云失败 ", e)
                .setErrorData(originalFilename);
        } finally {
            ossClient.shutdown();
        }
    }


    @Override
    public void delete(@NonNull String key) {
        Assert.notNull(key, "File key must not be blank");
        String bucketName =
            optionService.getByPropertyOfNonNull(AliOssProperties.OSS_BUCKET_NAME).toString();

        OSS ossClient = createOSSClient();

        try {
            ossClient.deleteObject(new DeleteObjectsRequest(bucketName).withKey(key));
        } catch (Exception e) {
            throw new FileOperationException("附件 " + key + " 从阿里S云删除失败", e);
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public void deleteByPrefix(String prefix) {
        String bucketName =
            optionService.getByPropertyOfNonNull(AliOssProperties.OSS_BUCKET_NAME).toString();

        OSS ossClient = createOSSClient();

        // 删除目录及目录下的所有文件。
        VersionListing versionListing;
        do {
            ListVersionsRequest listVersionsRequest = new ListVersionsRequest()
                .withBucketName(bucketName)
                .withPrefix(prefix);

            versionListing = ossClient.listVersions(listVersionsRequest);
            if (versionListing.getVersionSummaries().size() > 0) {
                List<DeleteVersionsRequest.KeyVersion> keys = new ArrayList<>();
                for (OSSVersionSummary s : versionListing.getVersionSummaries()) {
                    keys.add(new DeleteVersionsRequest.KeyVersion(s.getKey(), s.getVersionId()));
                }
                DeleteVersionsRequest deleteVersionsRequest =
                    new DeleteVersionsRequest(bucketName).withKeys(keys);
                DeleteVersionsResult deleteVersionsResult =
                    ossClient.deleteVersions(deleteVersionsRequest);
                for (DeleteVersionsResult.DeletedVersion version: deleteVersionsResult.getDeletedVersions()) {
                    String deleteObj = URLDecoder.decode(version.getKey(), StandardCharsets.UTF_8);
                    log.info("success delete {}", deleteObj);
                }
            }
        } while (versionListing.isTruncated());
        // 关闭OSSClient。
        ossClient.shutdown();
    }

    @Override
    public AttachmentType getAttachmentType() {
        return AttachmentType.ALIOSS;
    }


    public String getBasePath() {
        String protocol =
            optionService.getByPropertyOfNonNull(AliOssProperties.OSS_PROTOCOL).toString();
        String domain =
            optionService.getByPropertyOrDefault(AliOssProperties.OSS_DOMAIN, String.class, "");
        StringBuilder basePath = new StringBuilder(protocol);
        String bucketName =
            optionService.getByPropertyOfNonNull(AliOssProperties.OSS_BUCKET_NAME).toString();
        String endPoint =
            optionService.getByPropertyOfNonNull(AliOssProperties.OSS_ENDPOINT).toString();
        if (StringUtils.isNotEmpty(domain)) {
            basePath.append(domain)
                .append(URL_SEPARATOR);
        } else {
            basePath.append(bucketName)
                .append(".")
                .append(endPoint)
                .append(URL_SEPARATOR);
        }
        return basePath.toString();
    }

    public StringBuilder  getUpFilePathPrefix(String diyPath) {
        String source =
            optionService.getByPropertyOrDefault(AliOssProperties.OSS_SOURCE, String.class, "");
        StringBuilder upFilePath = new StringBuilder();
        if (StringUtils.isNotEmpty(source)) {
            upFilePath.append(source)
                .append(URL_SEPARATOR);
        }
        if (StringUtils.isNotEmpty(diyPath)) {
            upFilePath.append(diyPath)
                .append(URL_SEPARATOR);
        }
        return upFilePath;
    }

    public OSS createOSSClient() {
        // Get config
        String endPoint =
            optionService.getByPropertyOfNonNull(AliOssProperties.OSS_ENDPOINT).toString();
        String accessKey =
            optionService.getByPropertyOfNonNull(AliOssProperties.OSS_ACCESS_KEY).toString();
        String accessSecret =
            optionService.getByPropertyOfNonNull(AliOssProperties.OSS_ACCESS_SECRET).toString();

        // 创建OSSClient实例。
        return new OSSClientBuilder().build(endPoint, accessKey, accessSecret);
    }
}

