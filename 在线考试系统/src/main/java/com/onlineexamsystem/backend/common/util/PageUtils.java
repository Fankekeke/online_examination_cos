package com.onlineexamsystem.backend.common.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlineexamsystem.backend.common.model.PageData;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

public final class PageUtils {

    private PageUtils() {
    }

    public static long resolvePage(HttpServletRequest request) {
        return resolvePositiveLong(request.getParameter("page"), 1L);
    }

    public static long resolvePageSize(HttpServletRequest request) {
        return resolvePositiveLong(request.getParameter("pageSize"), 10L);
    }

    public static <T> Page<T> createPage(HttpServletRequest request) {
        return new Page<>(resolvePage(request), resolvePageSize(request));
    }

    public static <T> PageData<T> toPageData(List<T> items, long total) {
        return new PageData<>(items, total);
    }

    private static long resolvePositiveLong(String rawValue, long fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }
        try {
            long value = Long.parseLong(rawValue.trim());
            return value > 0 ? value : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
