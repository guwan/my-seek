package top.knos.mySeek.security.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.knos.mySeek.common.Result;
import top.knos.mySeek.security.service.ResourceService;
import top.knos.mySeek.security.vo.req.DeleteReq;
import top.knos.mySeek.security.vo.req.MenuAddReq;
import top.knos.mySeek.security.vo.req.MenuQueryReq;
import top.knos.mySeek.security.vo.res.MenuQueryRes;
import top.knos.mySeek.support.vo.PageVo;

import java.lang.reflect.InvocationTargetException;

/**
 * @author 卢文武(01380604)
 * @date 2018/11/16 18:40
 */
@RestController
@RequestMapping("/api/resource")
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    @PostMapping("add")
    public Result<Object> add(@RequestBody MenuAddReq menuAddReq) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        resourceService.add(menuAddReq);
        return Result.ok();
    }
    @PostMapping("intelligenceAdd")
    public Result<Object> intelligenceAdd(@RequestBody MenuAddReq menuAddReq) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        resourceService.intelligenceAdd(menuAddReq);
        return Result.ok();
    }

    @PostMapping("delete")
    public Result<Object> deleteMenu(@RequestBody DeleteReq deleteReq) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        resourceService.delete(deleteReq);
        return Result.ok();
    }

    @PostMapping("update")
    public Result<Object> updateMenu(@RequestBody MenuAddReq menuAddReq) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        resourceService.update(menuAddReq);
        return Result.ok();
    }

    @PostMapping("query")
    public PageVo<MenuQueryRes> queryMenu(@RequestBody MenuQueryReq menuQueryReq) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        return resourceService.query(menuQueryReq);
    }

}
