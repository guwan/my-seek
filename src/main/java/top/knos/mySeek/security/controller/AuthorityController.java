package top.knos.mySeek.security.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.knos.mySeek.common.Result;
import top.knos.mySeek.security.service.AuthorityService;
import top.knos.mySeek.security.vo.req.DeleteReq;
import top.knos.mySeek.security.vo.req.RoleAddReq;
import top.knos.mySeek.security.vo.req.RoleQueryReq;
import top.knos.mySeek.security.vo.req.TreeDataUpdateReq;
import top.knos.mySeek.security.vo.res.AllTreeConfigRes;
import top.knos.mySeek.security.vo.res.RoleQueryRes;
import top.knos.mySeek.security.vo.res.SelectKeysRes;
import top.knos.mySeek.support.vo.PageVo;

import java.lang.reflect.InvocationTargetException;

/**
 * @author 卢文武(01380604)
 * @date 2018/11/16 18:40
 */
@RestController
@RequestMapping("/api/role")
public class AuthorityController {

    @Autowired
    private AuthorityService authorityService;

    @PostMapping("add")
    public Result<Object> add(@RequestBody RoleAddReq roleAddReq) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        authorityService.add(roleAddReq);
        return Result.ok();
    }

    @PostMapping("delete")
    public Result<Object> delete(@RequestBody DeleteReq deleteReq) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        authorityService.delete(deleteReq);
        return Result.ok();
    }

    @PostMapping("update")
    public Result<Object> update(@RequestBody RoleAddReq roleAddReq) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        authorityService.update(roleAddReq);
        return Result.ok();
    }

    @PostMapping("query")
    public PageVo<RoleQueryRes> query(@RequestBody RoleQueryReq roleQueryReq) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        return authorityService.query(roleQueryReq);
    }

    @PostMapping("queryAllMenu")
    public AllTreeConfigRes queryRoleMenu() throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        return authorityService.queryAlllMenu();
    }

    @PostMapping("queryRoleMenu")
    public SelectKeysRes queryRoleMenu(@RequestParam Integer id) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        return authorityService.queryRoleMenu(id);
    }

    @PostMapping("updateRoleMenu")
    public Result<Object> updateRoleMenu(@RequestBody TreeDataUpdateReq treeDataUpdateReq) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        authorityService.updateRoleMenu(treeDataUpdateReq);
        return Result.ok();
    }
}
