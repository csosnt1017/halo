package run.halo.app.handler.file;

import static run.halo.app.model.support.HaloConst.DATE_SEPARATOR;
import static run.halo.app.model.support.HaloConst.FILE_SEPARATOR;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;
import run.halo.app.config.properties.HaloProperties;
import run.halo.app.exception.FileOperationException;
import run.halo.app.model.dto.UploadDTO;
import run.halo.app.model.enums.AttachmentType;
import run.halo.app.model.support.UploadResult;
import run.halo.app.service.OptionService;
import run.halo.app.utils.FilenameUtils;
import run.halo.app.utils.HaloUtils;
import run.halo.app.utils.ImageUtils;
import javax.imageio.ImageReader;

/**
 * Local file handler.
 *
 * @author johnniang
 * @author ryanwang
 * @date 2019-03-27
 */
@Slf4j
@Component
public class LocalFileHandler implements FileHandler {
    /**
     * Upload sub directory.
     */
    public static final String UPLOAD_SUB_DIR = "upload/";

    private static final String THUMBNAIL_SUFFIX = "-thumbnail";

    /**
     * Thumbnail width.
     */
    private static final int THUMB_WIDTH = 256;

    /**
     * Thumbnail height.
     */
    private static final int THUMB_HEIGHT = 256;

    private final OptionService optionService;

    public final String workDir;

    public LocalFileHandler(OptionService optionService,
        HaloProperties haloProperties) {
        this.optionService = optionService;

        // Get work dir
        workDir = FileHandler.normalizeDirectory(haloProperties.getWorkDir());
        // Check work directory
        checkWorkDir();
    }

    /**
     * Check work directory.
     */
    private void checkWorkDir() {
        // Get work path
        Path workPath = Paths.get(workDir);

        // Check file type
        if (!Files.isDirectory(workPath)
            || !Files.isReadable(workPath)
            || !Files.isWritable(workPath)) {
            log.warn("Please make sure that {} is a directory, readable and writable!", workDir);
        }
    }

    @NotNull
    @Override
    public UploadResult upload(@NotNull MultipartFile file) {
        Assert.notNull(file, "Multipart file must not be null");
        UploadResult uploadResult = null;
        try {
            UploadDTO uploadDTO = new UploadDTO(file.getInputStream(), file.getOriginalFilename(),
                file.getContentType(), file.getSize(), FILE_TYPE_NORMAL, null, null,null);
            uploadResult = upload(uploadDTO);
            return uploadResult;
        } catch (IOException ex) {
            throw new FileOperationException("上传附件失败").setErrorData(uploadResult);
        }
    }

    /**
     * 上传p站文件
     *
     * @param uploadDTO dto
     */
    public UploadResult uploadPixiv(UploadDTO uploadDTO) {
        Assert.notNull(uploadDTO.getIn(), "InputStream must not be null");
        UploadResult uploadResult;
        try {
            uploadDTO.setType(FILE_TYPE_PIXIV);
            uploadResult = upload(uploadDTO);
        } catch (Exception ex) {
            log.error("p站图片上传失败");
            throw new RuntimeException("p站图片上传失败");
        }
        return uploadResult;
    }

    @NotNull
    public UploadResult upload(UploadDTO uploadDTO){
        String originalFilename = uploadDTO.getOriginalFilename();
        InputStream in = uploadDTO.getIn();
        String originalBasename =
            FilenameUtils.getBasename(Objects.requireNonNull(originalFilename));

        // Get basename
        String basename = originalBasename + '-' + HaloUtils.randomUUIDWithoutDash();

        // Get extension
        String extension = FilenameUtils.getExtension(originalFilename);

        log.debug("Base name: [{}], extension: [{}] of original filename: [{}]", basename,
            extension, originalFilename);

        String subDir = getSubDir(uploadDTO.getType());

        // Build sub file path
        String subFilePath = subDir + basename + '.' + extension;
        // Get upload path
        Path uploadPath = Paths.get(workDir, subFilePath);

        // TODO Synchronize here
        // Create directory
        try {
            Files.createDirectories(uploadPath.getParent());
            Files.createFile(uploadPath);
            // Upload this file
            FileCopyUtils.copy(in, Files.newOutputStream(uploadPath));
        } catch (IOException e) {
            throw new RuntimeException("文件创建失败：",e);
        }

        // Build upload result
        UploadResult uploadResult = new UploadResult();
        uploadResult.setFilename(originalBasename);
        uploadResult.setFilePath(subFilePath);
        uploadResult.setKey(subFilePath);
        uploadResult.setSuffix(extension);
        uploadResult
            .setMediaType(MediaType.valueOf(Objects.requireNonNull(uploadDTO.getContentType())));
        uploadResult.setSize(uploadDTO.getSize());
        uploadResult.setThumbPath(subFilePath);
        if (IMAGE_TYPE.includes(MediaType.valueOf(uploadDTO.getContentType()))) {
            final String thumbnailBasename = basename + THUMBNAIL_SUFFIX;
            final String thumbnailSubFilePath =
                subDir + thumbnailBasename + '.' + extension;
            final Path thumbnailPath = Paths.get(workDir + thumbnailSubFilePath);
            try {
                if (uploadDTO.getWidth() != null && uploadDTO.getHeight() != null) {
                    uploadResult.setWidth(uploadDTO.getWidth());
                    uploadResult.setHeight(uploadDTO.getHeight());
                } else {
                    ImageReader image =
                        ImageUtils.getImageReaderFromFile(in, uploadResult.getSuffix());
                    uploadResult.setWidth(image.getWidth(0));
                    uploadResult.setHeight(image.getHeight(0));
                    // Generate thumbnail
                    BufferedImage originalImage = ImageUtils.getImageFromFile(in, extension);
                    if (originalImage == null) {
                        return uploadResult;
                    }
                    boolean result = generateThumbnail(originalImage, thumbnailPath, extension);
                    if (result) {
                        uploadResult.setThumbPath(thumbnailSubFilePath);
                    }
                }
            } catch (Throwable e) {
                log.warn("Failed to open image file.", e);
            }
        }
        log.info("Uploaded file: [{}] to directory: [{}] successfully",
            originalFilename, uploadPath.toString());
        return uploadResult;
    }

    @Override
    public void delete(String key) {
        Assert.hasText(key, "File key must not be blank");
        // Get path
        Path path = Paths.get(workDir, key);

        // Delete the file key
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new FileOperationException("附件 " + key + " 删除失败", e);
        }

        // Delete thumb if necessary
        String basename = FilenameUtils.getBasename(key);
        String extension = FilenameUtils.getExtension(key);

        // Get thumbnail name
        String thumbnailName = basename + THUMBNAIL_SUFFIX + '.' + extension;

        // Get thumbnail path
        Path thumbnailPath = Paths.get(path.getParent().toString(), thumbnailName);

        // Delete thumbnail file
        try {
            boolean deleteResult = Files.deleteIfExists(thumbnailPath);
            if (!deleteResult) {
                log.warn("Thumbnail: [{}] may not exist", thumbnailPath.toString());
            }
        } catch (IOException e) {
            throw new FileOperationException("附件缩略图 " + thumbnailName + " 删除失败", e);
        }
    }

    /**
     * Deletes files.
     *
     * @param prefix file key prefix
     */
    @Override
    public void deleteByPrefix(String prefix) {

    }

    @Override
    public AttachmentType getAttachmentType() {
        return AttachmentType.LOCAL;
    }

    private boolean generateThumbnail(BufferedImage originalImage, Path thumbPath,
        String extension) {
        Assert.notNull(originalImage, "Image must not be null");
        Assert.notNull(thumbPath, "Thumb path must not be null");

        boolean result = false;
        // Create the thumbnail
        try {
            Files.createFile(thumbPath);
            // Convert to thumbnail and copy the thumbnail
            log.debug("Trying to generate thumbnail: [{}]", thumbPath.toString());
            Thumbnails.of(originalImage).size(THUMB_WIDTH, THUMB_HEIGHT).keepAspectRatio(true)
                .toFile(thumbPath.toFile());
            log.info("Generated thumbnail image, and wrote the thumbnail to [{}]",
                thumbPath.toString());
            result = true;
        } catch (Throwable t) {
            // Ignore the error
            log.warn("Failed to generate thumbnail: " + thumbPath, t);
        } finally {
            // Disposes of this graphics context and releases any system resources that it is
            // using.
            originalImage.getGraphics().dispose();
        }
        return result;
    }

    private String getSubDir(String type) {
        StringBuilder stringBuilder = new StringBuilder(UPLOAD_SUB_DIR);
        // Get current time
        Calendar current = Calendar.getInstance(optionService.getLocale());
        // Get month and day of month
        int year = current.get(Calendar.YEAR);
        int month = current.get(Calendar.MONTH) + 1;
        int day = current.get(Calendar.DAY_OF_MONTH);

        String monthString = month < 10 ? "0" + month : String.valueOf(month);
        switch (type) {
            case FILE_TYPE_PIXIV:
                stringBuilder.append(FILE_TYPE_PIXIV)
                    .append(FILE_SEPARATOR)
                    .append(year)
                    .append(DATE_SEPARATOR)
                    .append(monthString)
                    .append(DATE_SEPARATOR)
                    .append(day)
                    .append(FILE_SEPARATOR);
                break;
            default:
                stringBuilder.append(year)
                    .append(DATE_SEPARATOR)
                    .append(monthString)
                    .append(FILE_SEPARATOR);
                break;
        }
        return stringBuilder.toString();
    }
}
