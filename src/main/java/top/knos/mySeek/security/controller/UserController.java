package top.knos.mySeek.security.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.knos.mySeek.common.Result;
import top.knos.mySeek.security.pojo.UserVo;
import top.knos.mySeek.security.service.impl.UserService;
import top.knos.mySeek.security.support.SecurityUtils;
import top.knos.mySeek.security.vo.req.*;
import top.knos.mySeek.security.vo.res.*;
import top.knos.mySeek.support.vo.PageKeyWords;
import top.knos.mySeek.support.vo.PageVo;

import java.lang.reflect.InvocationTargetException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 卢文武(01380604)
 * @date 2018/11/14 11:14
 */
@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("updatePassword")
    public void updatePassword(@RequestBody PasswordUpdateReq passwordUpdateReq) {
        userService.changePassword(passwordUpdateReq);
    }

    @PostMapping("resetPassword")
    public void resetPassword(@RequestBody PasswordUpdateReq passwordUpdateReq) {
        userService.resetPassword(passwordUpdateReq.getNewPassword());

    }

    @GetMapping("currentUser")
    public Result<Object> currentUser(Principal principal) {
        if (principal == null) {
            return Result.unauthorized();
        }
        return Result.ok(userService.currentUser(principal));
    }

    @PostMapping("/logout")
    public String logout(Principal principal) {
        return userService.logout(principal);
    }

    @GetMapping("myMenu")
    public List<MenuRes> myMenu() {

        var currentUser = SecurityUtils.getUser();
        if (currentUser == null) {
            return new ArrayList<>(0);
        }
        return userService.myMenu(currentUser.getId());
    }

    @PostMapping("add")
    public Result<Object> add(@RequestBody UserAddReq userAddReq)  {
        userService.add(userAddReq);
        return Result.ok();
    }

    @PostMapping("delete")
    public Result<Object> delete(@RequestBody DeleteReq deleteReq) {
        userService.delete(deleteReq);
        return Result.ok();
    }

    @PostMapping("update")
    public Result<Object> update(@RequestBody UserAddReq userAddReq) {
        userService.update(userAddReq);
        return Result.ok();
    }

    @PostMapping("query")
    public PageVo<UserQueryRes> queryUser(@RequestBody UserQueryReq userQueryReq)  {
        return userService.queryUser(userQueryReq);
    }

    @PostMapping("queryAllRole")
    public Result<AllTreeConfigRes> queryAllRole() {
        AllTreeConfigRes res = userService.queryAllRole();
        return Result.ok(res);
    }

    @PostMapping("queryUserRole")
    public Result<SelectKeysRes> queryUserRole(@RequestParam Long id)  {
        SelectKeysRes res = userService.queryUserRole(id);
        return Result.ok(res);
    }
    @PostMapping("queryAllDataOrg")
    public Result<AllTreeConfigRes> queryAllDataOrg()  {
        AllTreeConfigRes res = userService.queryAllDataOrg();
        return Result.ok(res);
    }
    @PostMapping("queryUserDataOrg")
    public Result<SelectKeysRes> queryUserDataOrg(@RequestParam Long id)  {
        SelectKeysRes res = userService.queryUserDataOrg(id);
        return Result.ok(res);
    }

    @PostMapping("updateUserRole")
    public Result<Object> updateUserRole(@RequestBody TreeDataUpdateReq treeDataUpdateReq) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        userService.updateUserRole(treeDataUpdateReq);
        return Result.ok();
    }

    @PostMapping("updateUserDataOrg")
    public Result<Object> updateUserDataOrg(@RequestBody TreeDataUpdateReq treeDataUpdateReq) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        userService.updateUserDataOrg(treeDataUpdateReq);
        return Result.ok();
    }

    @PostMapping("updateUserBySelf")
    public Result<Object> updateUserBySelf(@RequestBody UserVo userVo) {
        userService.updateUserBySelf(userVo);
        return Result.ok();
    }
    @PostMapping("updateAvatarBySelf")
    public Result<Object> updateAvatarBySelf(@RequestBody UserVo userVo) {
        userService.updateAvatarBySelf(userVo);
        return Result.ok();
    }


    /**
     * 分类页
     *
     * @param keyWords 查询参数
     * @return
     */
    @PostMapping(value = "queryUserSelect")
    public Result<List<UserQueryRes>> queryUserSelect(@RequestBody PageKeyWords keyWords) {
        return Result.ok(userService.queryUserSelect(keyWords));
    }
}
