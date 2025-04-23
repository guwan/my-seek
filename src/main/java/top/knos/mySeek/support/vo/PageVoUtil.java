package top.knos.mySeek.support.vo;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import top.knos.mySeek.support.GenericType;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 *
 * @Author andy
 * @Date 2022/3/12 16:37
 */
public class PageVoUtil {
    public static <S, T> PageVo<T> covertPageToPageVo(Page<S> dbPage) {
        PageVo<T> voPageVo = new PageVo<>();
        voPageVo.setTotal(dbPage.getTotalElements());
        Pageable page = dbPage.getPageable();
        List<S> dbs = dbPage.getContent();
        voPageVo.setCurrent(page.getPageNumber());
        voPageVo.setPageSize(page.getPageSize());
        List<T> vos = new ArrayList<>();
        for (S db : dbs) {
            T vo = new GenericType<T>().getInstanceOfT();
            BeanUtils.copyProperties(db, vo);
            vos.add(vo);
        }
        voPageVo.setData(vos);
        return voPageVo;
    }
    public static <S, T> PageVo<T> covertPageToPageVo(Page<S> dbPage, Class<T> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        PageVo<T> voPageVo = new PageVo<>();
        voPageVo.setTotal(dbPage.getTotalElements());
        Pageable page = dbPage.getPageable();
        List<S> dbs = dbPage.getContent();
        voPageVo.setCurrent(page.getPageNumber());
        voPageVo.setPageSize(page.getPageSize());
        List<T> vos = new ArrayList<>();
        for (S db : dbs) {
            T classObj = clazz.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(db, classObj);
            vos.add(classObj);
        }
        voPageVo.setData(vos);
        return voPageVo;
    }

    public static <S, T> PageVo<T> covertProPageToPageVo(Page<S> projections, Class<T> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<T> blogVoForLists = new ArrayList<>();
        for (S blogProjection : projections.getContent()) {
            T blogVoForList = clazz.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(blogProjection, blogVoForList);
            blogVoForLists.add(blogVoForList);
        }

        PageVo<T> voPageVo = new PageVo<>();
        voPageVo.setTotal(projections.getTotalElements());
        Pageable page = projections.getPageable();
        voPageVo.setCurrent(page.getPageNumber());
        voPageVo.setPageSize(page.getPageSize());
        voPageVo.setData(blogVoForLists);
        return voPageVo;
    }
}
