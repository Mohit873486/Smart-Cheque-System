package com.chequeprint.util;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public class SignatureService {

  private static final String APP_DIR = System.getProperty("user.home") + File.separator + ".chequepro";
  private static final String SIG_FILE = "signature.png";
  private static final String META_FILE = "signature.properties";

  public static Path getAppDir() {
    Path p = Path.of(APP_DIR);
    try {
      if (!Files.exists(p))
        Files.createDirectories(p);
    } catch (IOException ignored) {
    }
    return p;
  }

  public static Path getSignaturePath() {
    return getAppDir().resolve(SIG_FILE);
  }

  public static boolean hasSignature() {
    return Files.exists(getSignaturePath());
  }

  public static String getSignatureUrl() {
    Path p = getSignaturePath();
    if (!Files.exists(p))
      return "";
    return p.toUri().toString();
  }

  public static Image loadSignatureImage() {
    try {
      Path p = getSignaturePath();
      if (!Files.exists(p))
        return null;
      try (InputStream in = Files.newInputStream(p)) {
        return new Image(in);
      }
    } catch (Exception ex) {
      return null;
    }
  }

  public static Path saveSignature(File source) throws IOException {
    if (source == null)
      throw new IllegalArgumentException("source is null");
    Path target = getSignaturePath();
    Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);

    // ensure it's a PNG with transparency preserved: convert if necessary
    try {
      BufferedImage img = ImageIO.read(target.toFile());
      if (img != null) {
        ImageIO.write(img, "png", target.toFile());
      }
    } catch (Exception ignored) {
    }

    return target;
  }

  public static void removeSignature() throws IOException {
    Path p = getSignaturePath();
    if (Files.exists(p))
      Files.delete(p);
  }

  public static Properties loadMetadata() {
    Properties props = new Properties();
    Path meta = getAppDir().resolve(META_FILE);
    if (Files.exists(meta)) {
      try (InputStream in = Files.newInputStream(meta)) {
        props.load(in);
      } catch (Exception ignored) {
      }
    }
    return props;
  }

  public static void saveMetadata(Properties props) {
        Path meta = getAppDir().resolve(META_FILE);
        try (java.io.OutputStream out = Files.newOutputStream(meta)) {
            props.store(out, "Signature metadata");
        } catch (Exception ignored) {
        }
    }
}
