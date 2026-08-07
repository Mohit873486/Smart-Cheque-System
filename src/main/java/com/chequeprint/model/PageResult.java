package com.chequeprint.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PageResult<T> {
    private List<T> content = List.of();
    private long totalElements;
    private int totalPages;
    private int number;
    private int size;
    private boolean first = true;
    private boolean last = true;
    private boolean empty = true;

    public PageResult() {}

    public PageResult(List<T> content, long totalElements, int number, int size) {
        this.content = content != null ? content : List.of();
        this.totalElements = totalElements;
        this.number = number;
        this.size = size;
        this.empty = this.content.isEmpty();
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        this.first = number == 0;
        this.last = number >= totalPages - 1 || totalPages == 0;
    }

    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = content.stream().map(mapper).collect(Collectors.toList());
        return new PageResult<>(mapped, totalElements, number, size);
    }

    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public boolean isFirst() { return first; }
    public void setFirst(boolean first) { this.first = first; }
    public boolean isLast() { return last; }
    public void setLast(boolean last) { this.last = last; }
    public boolean isEmpty() { return empty; }
    public void setEmpty(boolean empty) { this.empty = empty; }
    public boolean hasNext() { return !last; }
    public boolean hasPrevious() { return !first; }
}