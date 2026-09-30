package com.studyvault.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SiteSettingsService {

    private final SiteSettingsRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public Map<String, Object> findAllAsMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (SiteSettings row : repository.findAll()) {
            try {
                result.put(row.getKey(), mapper.readValue(row.getValue(), Object.class));
            } catch (Exception e) {
                result.put(row.getKey(), row.getValue());
            }
        }
        return result;
    }

    @Transactional
    public void upsertAll(Map<String, Object> values) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank() || key.length() > 100) {
                continue;
            }
            try {
                String json = mapper.writeValueAsString(entry.getValue());
                SiteSettings row = repository.findById(key).orElseGet(() -> {
                    SiteSettings s = new SiteSettings();
                    s.setKey(key);
                    return s;
                });
                row.setValue(json);
                repository.save(row);
            } catch (Exception ignored) {
                // Skip unserializable values; controller validates input before this.
            }
        }
    }
}
