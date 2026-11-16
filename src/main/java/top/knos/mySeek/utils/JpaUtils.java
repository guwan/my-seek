package top.knos.mySeek.utils;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.*;
import top.knos.mySeek.constant.FrontendSort;
import top.knos.mySeek.support.vo.PageBaseReq;
import top.knos.mySeek.support.vo.SortAndFilter;

import java.lang.reflect.InvocationTargetException;

/**
 * TODO
 *
 * @Author 技术古玩
 * @Date 2021/5/5 19:48
 */
public class JpaUtils {
    public static Pageable createPage() {
        return PageRequest.of(0, 10, Sort.by(Sort.Order.desc("id")));
    }
    public static Pageable createPage(PageBaseReq pageReq) {
        return PageRequest.of(pageReq.getCurrent() - 1, pageReq.getPageSize(), Sort.by(Sort.Order.desc("id")));
    }

    public static Pageable createPageSort(SortAndFilter pageReq) {
        return PageRequest.of(pageReq.getCurrent() - 1, pageReq.getPageSize(), FrontendSort.getSort(pageReq.getSorter()));
    }
    public static Pageable createPageOrderById(PageBaseReq pageReq) {
        return PageRequest.of(pageReq.getCurrent() - 1, pageReq.getPageSize(), Sort.by(Sort.Order.asc("id")));
    }

    /**
     * 忽略 null 值的 Example 匹配器（不可变，线程安全）
     */
    private static final ExampleMatcher DEFAULT_EXAMPLE_MATCHER = ExampleMatcher.matching().withIgnoreNullValues();

    public static ExampleMatcher getExampleMatcher() {
        return DEFAULT_EXAMPLE_MATCHER;
    }

    public static <T> Example<T> createExample(Object userQueryReq, Class<T> targetClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        T targetObj = targetClass.getDeclaredConstructor().newInstance();
        BeanUtils.copyProperties(userQueryReq, targetObj);
        return Example.of(targetObj, JpaUtils.getExampleMatcher());
    }
}
