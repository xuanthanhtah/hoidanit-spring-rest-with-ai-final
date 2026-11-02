package vn.hoidanit.springrestwithai.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Wrapper cho kết quả phân trang: meta (page 1-based) + danh sách result.
 */
public record PageResponse<T>(Meta meta, List<T> result) {

    public record Meta(int page, int pageSize, int pages, long total) {
    }

    public static <T> PageResponse<T> of(Page<?> page, List<T> result) {
        Meta meta = new Meta(page.getNumber() + 1, page.getSize(), page.getTotalPages(), page.getTotalElements());
        return new PageResponse<>(meta, result);
    }
}
