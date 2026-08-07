package com.chequeprint.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class PageRequest {
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 25;
    public static final int MAX_SIZE = 100;

    private int page;
    private int size;
    private String sortBy;
    private String sortDir;
    private String searchQuery;
    private final Map<String, String> filters = new LinkedHashMap<>();

    private PageRequest(int page, int size) {
        this.page = Math.max(0, page);
        this.size = Math.max(1, Math.min(size, MAX_SIZE));
    }

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size);
    }

    public static PageRequest firstPage(int size) {
        return new PageRequest(0, size);
    }

    public PageRequest withSort(String by, String dir) {
        this.sortBy = by;
        this.sortDir = dir;
        return this;
    }

    public PageRequest withSearch(String query) {
        this.searchQuery = query;
        return this;
    }

    public PageRequest withFilter(String key, String value) {
        if (value != null && !value.isBlank()) {
            this.filters.put(key, value);
        }
        return this;
    }

    public String toQueryString() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("page", String.valueOf(page));
        params.put("size", String.valueOf(size));
        if (sortBy != null && !sortBy.isBlank()) {
            params.put("sort", sortBy + "," + (sortDir != null ? sortDir : "asc"));
        }
        if (searchQuery != null && !searchQuery.isBlank()) {
            params.put("q", searchQuery);
        }
        params.putAll(filters);
        return params.entrySet().stream()
            .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
            .collect(Collectors.joining("&"));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public int getPage() { return page; }
    public int getSize() { return size; }
    public String getSortBy() { return sortBy; }
    public String getSortDir() { return sortDir; }
    public String getSearchQuery() { return searchQuery; }
    public Map<String, String> getFilters() { return filters; }
}