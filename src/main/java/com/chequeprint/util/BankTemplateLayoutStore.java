package com.chequeprint.util;

import com.chequeprint.model.BankTemplateLayout;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class BankTemplateLayoutStore {
    private final Path storePath;

    public BankTemplateLayoutStore() {
        this.storePath = Paths.get(System.getProperty("user.home"), ".chequepro", "bank-template-layouts.bin");
    }

    public Map<String, BankTemplateLayout> loadAll() {
        if (!Files.exists(storePath)) {
            return new HashMap<>();
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(storePath))) {
            Object obj = in.readObject();
            if (obj instanceof Map<?, ?> map) {
                Map<String, BankTemplateLayout> out = new HashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() instanceof String key
                            && entry.getValue() instanceof BankTemplateLayout layout) {
                        layout.ensureAllFields();
                        out.put(key, layout);
                    }
                }
                return out;
            }
        } catch (Exception ignored) {
        }
        return new HashMap<>();
    }

    public void saveAll(Map<String, BankTemplateLayout> layouts) throws IOException {
        Files.createDirectories(storePath.getParent());
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(storePath))) {
            out.writeObject(layouts);
        }
    }
}
