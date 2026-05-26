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
import java.util.Locale;
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

  public static Path getSignaturePath(String userKey) {
    if (userKey == null || userKey.isBlank()) {
      return getSignaturePath();
    }
    String safe = userKey.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "_");
    return getAppDir().resolve("signature_" + safe + ".png");
  }

  public static boolean hasSignature() {
    return Files.exists(getSignaturePath());
  }

  public static boolean hasSignature(String userKey) {
    return Files.exists(getSignaturePath(userKey));
  }

  public static String getSignatureUrl() {
    Path p = getSignaturePath();
    if (!Files.exists(p))
      return "";
    return p.toUri().toString();
  }

  public static String getSignatureUrl(String userKey) {
    Path p = getSignaturePath(userKey);
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

  public static Image loadSignatureImage(String userKey) {
    try {
      Path p = getSignaturePath(userKey);
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
    validateSignatureImage(source);
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

  public static Path saveSignature(File source, String userKey) throws IOException {
    if (source == null)
      throw new IllegalArgumentException("source is null");
    validateSignatureImage(source);
    Path target = getSignaturePath(userKey);
    Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
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

  public static void removeSignature(String userKey) throws IOException {
    Path p = getSignaturePath(userKey);
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

  public static Properties loadMetadata(String userKey) {
    if (userKey == null || userKey.isBlank()) {
      return loadMetadata();
    }
    Properties props = new Properties();
    Path meta = getAppDir().resolve("signature_" + userKey.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "_") + ".properties");
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

  public static void saveMetadata(Properties props, String userKey) {
    if (userKey == null || userKey.isBlank()) {
      saveMetadata(props);
      return;
    }
    Path meta = getAppDir().resolve("signature_" + userKey.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "_") + ".properties");
    try (java.io.OutputStream out = Files.newOutputStream(meta)) {
      props.store(out, "Signature metadata");
    } catch (Exception ignored) {
    }
  }

  public static void validateSignatureImage(File source) throws IOException {
    String name = source.getName().toLowerCase(Locale.ROOT);
    if (!name.endsWith(".png")) {
      throw new IllegalArgumentException("Only PNG signature files are allowed.");
    }

    BufferedImage img = ImageIO.read(source);
    if (img == null) {
      throw new IllegalArgumentException("Selected file is not a readable image.");
    }
    if (!img.getColorModel().hasAlpha()) {
      throw new IllegalArgumentException("Signature must have transparent background (alpha channel missing).");
    }
  }
}
