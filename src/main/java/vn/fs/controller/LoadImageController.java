package vn.fs.controller;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.*;

@Controller
public class LoadImageController {

    @Value("${upload.path}")
    private String pathUploadImage; // ví dụ: upload/images

    // Ảnh dự phòng nằm trong src/main/resources/static/images/no-image.png
    private static final String FALLBACK_CLASSPATH = "/static/images/no-image.png";

    @GetMapping(value = "loadImage", produces = MediaType.ALL_VALUE)
    @ResponseBody
    public byte[] index(@RequestParam("imageName") String imageName,
                        HttpServletResponse response) throws IOException {

        response.setHeader("Cache-Control", "max-age=86400");

        if (!StringUtils.hasText(imageName)
                || imageName.contains("..")
                || imageName.contains("/") || imageName.contains("\\")
                && (new File(pathUploadImage, imageName)).isDirectory()) {
            return fallbackBytes(response);
        }

        File file = new File(pathUploadImage, imageName);
        if (!file.exists() || !file.isFile() || !file.canRead()) {
            return fallbackBytes(response);
        }

        String lower = imageName.toLowerCase();
        if (lower.endsWith(".png")) {
            response.setContentType(MediaType.IMAGE_PNG_VALUE);
        } else if (lower.endsWith(".gif")) {
            response.setContentType(MediaType.IMAGE_GIF_VALUE);
        } else {
            response.setContentType(MediaType.IMAGE_JPEG_VALUE);
        }

        try (InputStream is = new FileInputStream(file)) {
            return IOUtils.toByteArray(is);
        } catch (Exception ex) {
            return fallbackBytes(response);
        }
    }

    private byte[] fallbackBytes(HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.IMAGE_PNG_VALUE);
        ClassPathResource res = new ClassPathResource(FALLBACK_CLASSPATH);
        if (res.exists()) {
            try (InputStream is = res.getInputStream()) {
                return IOUtils.toByteArray(is);
            }
        }
        // 1x1 GIF
        return new byte[]{
                (byte)0x47,(byte)0x49,(byte)0x46,(byte)0x38,(byte)0x39,(byte)0x61,1,0,1,0,(byte)0x80,0,0,0,0,0,(byte)0xFF,(byte)0xFF,(byte)0xFF,0,0,0,0,0,0,0,0,0,0,0,(byte)0x2C,0,0,0,0,1,0,1,0,0,2,2,68,1,0,59
        };
    }
}
