package com.studyvault.storage;

import com.studyvault.common.error.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Upload validation: extension whitelist, size caps and magic-byte content sniffing.
 * Original filenames are never used as storage keys; content types are derived from
 * the validated extension rather than trusting the client.
 */
@Service
public class FileValidationService {

    private static final long MAX_DIGITAL_BYTES = 200L * 1024 * 1024; // 200 MB
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;     // 5 MB
    private static final Set<String> DIGITAL_EXTENSIONS = Set.of("pdf", "zip");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");

    public record ValidatedFile(String extension, String contentType, String safeOriginalName, byte[] data) {
    }

    public ValidatedFile validateDigital(MultipartFile file) {
        ValidatedFile vf = validate(file, DIGITAL_EXTENSIONS, MAX_DIGITAL_BYTES);
        byte[] data = vf.data();
        boolean pdf = startsWith(data, new byte[]{'%', 'P', 'D', 'F', '-'});
        boolean zip = startsWith(data, new byte[]{0x50, 0x4B, 0x03, 0x04})
                || startsWith(data, new byte[]{0x50, 0x4B, 0x05, 0x06})
                || startsWith(data, new byte[]{0x50, 0x4B, 0x07, 0x08});
        if (!pdf && !zip) {
            throw new BadRequestException("File content does not match its type.");
        }
        return vf;
    }

    public ValidatedFile validateImage(MultipartFile file) {
        ValidatedFile vf = validate(file, IMAGE_EXTENSIONS, MAX_IMAGE_BYTES);
        byte[] data = vf.data();
        boolean ok = switch (vf.extension()) {
            case "png" -> startsWith(data, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
            case "jpg", "jpeg" -> startsWith(data, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case "webp" -> data.length > 11 && startsWith(data, new byte[]{'R', 'I', 'F', 'F'})
                    && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P';
            default -> false;
        };
        if (!ok) {
            throw new BadRequestException("Image content does not match its type.");
        }
        return vf;
    }

    private ValidatedFile validate(MultipartFile file, Set<String> allowed, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file provided.");
        }
        if (file.getSize() > maxBytes) {
            throw new BadRequestException("File is too large.");
        }
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            throw new BadRequestException("File name is missing.");
        }
        // Strip any path components from client-supplied names.
        String safeName = new java.io.File(original.replace('\\', '/')).getName();
        if (safeName.isBlank() || safeName.length() > 255) {
            throw new BadRequestException("Invalid file name.");
        }
        String extension = extensionOf(safeName);
        if (!allowed.contains(extension)) {
            throw new BadRequestException("File type not allowed. Allowed: " + String.join(", ", allowed));
        }
        byte[] data;
        try (InputStream in = file.getInputStream()) {
            data = in.readNBytes((int) Math.min(file.getSize(), maxBytes + 1));
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file.");
        }
        if (data.length != file.getSize()) {
            throw new BadRequestException("File could not be read completely.");
        }
        String contentType = switch (extension) {
            case "pdf" -> "application/pdf";
            case "zip" -> "application/zip";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
        return new ValidatedFile(extension, contentType, safeName, data);
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    public static String randomKey(String prefix, String extension) {
        return prefix + UUID.randomUUID().toString().replace("-", "") + "." + extension;
    }
}
