package com.drone.platform.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private long total;
    private long pages;
    private long current;
    private long size;
    private List<T> records;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> r = new PageResult<>();
        r.setTotal(page.getTotal());
        r.setPages(page.getPages());
        r.setCurrent(page.getCurrent());
        r.setSize(page.getSize());
        r.setRecords(page.getRecords());
        return r;
    }
}
