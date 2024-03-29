package run.halo.app.handler.file;

import static run.halo.app.model.support.HaloConst.FILE_SEPARATOR;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;
import javax.imageio.ImageReader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;
import run.halo.app.exception.FileOperationException;
import run.halo.app.model.dto.UploadDTO;
import run.halo.app.model.enums.AttachmentType;
import run.halo.app.model.support.UploadResult;
import run.halo.app.utils.ImageUtils;

/**
 * File handler interface.
 *
 * @author johnniang
 * @date 2019-03-27
 */
public interface FileHandler {

    MediaType IMAGE_TYPE = MediaType.valueOf("image/*");

    /**
     * 普通附件
     */
    String FILE_TYPE_NORMAL = "normal";

    /**
     * p站图片
     */
    String FILE_TYPE_PIXIV = "pixiv";

    /**
     * Normalize directory full name, ensure the end path separator.
     *
     * @param dir directory full name must not be blank
     * @return normalized directory full name with end path separator
     */
    @NonNull
    static String normalizeDirectory(@NonNull String dir) {
        Assert.hasText(dir, "Directory full name must not be blank");

        return StringUtils.appendIfMissing(dir, FILE_SEPARATOR);
    }

    /**
     * Uploads file.
     *
     * @param file multipart file must not be null
     * @return upload result
     * @throws FileOperationException throws when fail to upload the file
     */
    @NonNull
    UploadResult upload(@NonNull MultipartFile file);

    /**
     * Uploads file.
     *
     * @param uploadDTO 上传DTO
     * @return upload result
     * @throws FileOperationException throws when fail to upload the file
     */
    @NonNull
    UploadResult upload(@NonNull UploadDTO uploadDTO);

    /**
     * Check if the current file is an image.
     *
     * @param file multipart file must not be null
     * @return true if the current file is an image, false otherwise
     */
    default boolean isImageType(@NonNull MultipartFile file) {
        String mediaType = file.getContentType();
        return isImageType(mediaType);
    }

    default boolean isImageType(@NonNull String mediaType) {
        return IMAGE_TYPE.includes(MediaType.valueOf(mediaType));
    }

    /**
     * @param uploadResult updated result must not be null
     * @param file multipart file must not be null
     * @param thumbnailSupplier thumbnail supplier
     */
    default void handleImageMetadata(@NonNull MultipartFile file,
        @NonNull UploadResult uploadResult,
        @Nullable Supplier<String> thumbnailSupplier) {
        if (isImageType(file)) {
            // Handle image
            try (InputStream is = file.getInputStream()) {
                ImageReader image = ImageUtils.getImageReaderFromFile(is, uploadResult.getSuffix());
                uploadResult.setWidth(image.getWidth(0));
                uploadResult.setHeight(image.getHeight(0));
                if (thumbnailSupplier != null) {
                    uploadResult.setThumbPath(thumbnailSupplier.get());
                }
            } catch (IOException | OutOfMemoryError e) {
                // ignore IOException and OOM
                LoggerFactory.getLogger(getClass()).warn("Failed to fetch image meta data", e);
            }
        }
        if (StringUtils.isBlank(uploadResult.getThumbPath())) {
            uploadResult.setThumbPath(uploadResult.getFilePath());
        }
    }

    default void handleImageMetadata(@NonNull InputStream is, @NonNull String mediaType,
    @NonNull UploadResult uploadResult,
        @Nullable Supplier<String> thumbnailSupplier) throws IOException {
        if (isImageType(mediaType)) {
            // Handle image
            try  {
                ImageReader image = ImageUtils.getImageReaderFromFile(is, uploadResult.getSuffix());
                uploadResult.setWidth(image.getWidth(0));
                uploadResult.setHeight(image.getHeight(0));
                if (thumbnailSupplier != null) {
                    uploadResult.setThumbPath(thumbnailSupplier.get());
                }
            } catch (IOException | OutOfMemoryError e) {
                // ignore IOException and OOM
                LoggerFactory.getLogger(getClass()).warn("Failed to fetch image meta data", e);
            }finally {
                is.close();
            }
        }
        if (StringUtils.isBlank(uploadResult.getThumbPath())) {
            uploadResult.setThumbPath(uploadResult.getFilePath());
        }
    }

    /**
     * Deletes file.
     *
     * @param key file key must not be null
     * @throws FileOperationException throws when fail to delete the file
     */
    void delete(@NonNull String key);

    /**
     * Deletes files.
     *
     * @param prefix file key prefix
     */
    void deleteByPrefix(String prefix);

    /**
     * Get attachment type is supported.
     *
     * @return attachment type
     */
    AttachmentType getAttachmentType();

}