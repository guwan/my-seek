package top.knos.mySeek.security.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.knos.mySeek.common.Result;
import top.knos.mySeek.security.service.DevService;
import top.knos.mySeek.security.vo.req.AutoGenerateMenuReq;

import java.lang.reflect.InvocationTargetException;

/**
 * 开放一些只有开发环境才允许使用的接口
 */
@RestController
@RequestMapping("/api/dev")
public class DevController {

    @Autowired
    private DevService devService;
    @PostMapping("autoGenerateMenu")
    public Result<Object> autoGenerateMenu(@RequestBody AutoGenerateMenuReq autoGenerateMenuReq) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        //只有生成环境才生成菜单
        devService.autoGenerateMenu(autoGenerateMenuReq);
        return Result.ok();
    }

}
