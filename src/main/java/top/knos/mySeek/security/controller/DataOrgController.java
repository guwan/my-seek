package top.knos.mySeek.security.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.knos.mySeek.common.Result;
import top.knos.mySeek.security.service.DataOrgService;
import top.knos.mySeek.security.vo.req.DataOrgAddReq;
import top.knos.mySeek.security.vo.req.DataOrgQueryReq;
import top.knos.mySeek.security.vo.req.DeleteReq;
import top.knos.mySeek.security.vo.res.DataOrgQueryRes;
import top.knos.mySeek.support.vo.PageVo;
import top.knos.mySeek.support.vo.Selection;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@RestController
@RequestMapping("/api/dataOrg")
@Slf4j
public class DataOrgController {

    @Autowired
    private DataOrgService dataOrgorityService;

    @PostMapping("add")
    public Result<Object> add(@RequestBody DataOrgAddReq dataOrgAddReq) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        dataOrgorityService.add(dataOrgAddReq);
        return Result.ok();
    }

    @PostMapping("delete")
    public Result<Object> delete(@RequestBody DeleteReq deleteReq) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        dataOrgorityService.delete(deleteReq);
        return Result.ok();
    }

    @PostMapping("update")
    public Result<Object> update(@RequestBody DataOrgAddReq dataOrgAddReq) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        dataOrgorityService.update(dataOrgAddReq);
        return Result.ok();
    }

    @PostMapping("query")
    public PageVo<DataOrgQueryRes> query(@RequestBody DataOrgQueryReq dataOrgQueryReq) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        return dataOrgorityService.query(dataOrgQueryReq);
    }
    @PostMapping("querySelect")
    public List<Selection> querySelect(@RequestBody DataOrgQueryReq dataOrgQueryReq) {
        return dataOrgorityService.querySelect(dataOrgQueryReq);
    }

}
