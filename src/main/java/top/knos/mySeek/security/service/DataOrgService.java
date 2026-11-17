package top.knos.mySeek.security.service;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import top.knos.mySeek.security.domain.DataOrg;
import top.knos.mySeek.security.repository.DataOrgRepository;
import top.knos.mySeek.security.repository.ResourceRepository;
import top.knos.mySeek.security.vo.req.DataOrgAddReq;
import top.knos.mySeek.security.vo.req.DataOrgQueryReq;
import top.knos.mySeek.security.vo.req.DeleteReq;
import top.knos.mySeek.security.vo.res.DataOrgQueryRes;
import top.knos.mySeek.support.vo.PageVo;
import top.knos.mySeek.support.vo.Selection;
import top.knos.mySeek.utils.JpaUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author 卢文武(01380604)
 * @date 2018/11/16 18:56
 */
@Service
public class DataOrgService {
    @Autowired
    private DataOrgRepository dataOrgRepository;
    @Autowired
    private ResourceRepository resourceRepository;

    public void add(DataOrgAddReq dataOrgAddReq) {
        DataOrg dataOrg = new DataOrg();
        notExistDataOrg(dataOrgAddReq);
        BeanUtils.copyProperties(dataOrgAddReq, dataOrg);
        dataOrgRepository.save(dataOrg);
    }

    public void delete(DeleteReq deleteReq) {
        List<Integer> ids = deleteReq.getIds();
        dataOrgRepository.deleteAllByIdInBatch(ids);
    }

    private void notExistDataOrg(DataOrgAddReq dataOrgAddReq) {
        DataOrg dbNameDataOrg = dataOrgRepository.findByName(dataOrgAddReq.getName());
        if (dbNameDataOrg != null)
            throw new IllegalArgumentException("该组织名已经存在");
    }

    @Transactional
    public void update(DataOrgAddReq dataOrgAddReq) {
        existDataOrg(dataOrgAddReq.getId());
        Assert.hasText(dataOrgAddReq.getName(), "组织名不能为空");

        DataOrg dataOrg = dataOrgRepository.getReferenceById(dataOrgAddReq.getId());
        dataOrg.setName(dataOrgAddReq.getName());
        dataOrg.setStatus(dataOrgAddReq.getStatus());
        dataOrg.setDescription(dataOrgAddReq.getDescription());
        dataOrg.setUpdatedBy(dataOrgAddReq.getUpdatedBy());
        dataOrg.setUpdatedDate(dataOrgAddReq.getUpdatedDate());
        dataOrgRepository.save(dataOrg);
    }

    public PageVo<DataOrgQueryRes> query(DataOrgQueryReq dataOrgQueryReq) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        DataOrg dataOrg = new DataOrg();
        BeanUtils.copyProperties(dataOrgQueryReq, dataOrg);
        Example<DataOrg> dataOrgExample = JpaUtils.createExample(dataOrgQueryReq, DataOrg.class);
        Page<DataOrg> page = dataOrgRepository.findAll(dataOrgExample, JpaUtils.createPage(dataOrgQueryReq));
        List<DataOrgQueryRes> queryResList = page.getContent().stream().map(u -> {
            DataOrgQueryRes dataOrgQueryRes = new DataOrgQueryRes();
            BeanUtils.copyProperties(u, dataOrgQueryRes);
            return dataOrgQueryRes;
        }).collect(Collectors.toList());
        PageVo<DataOrgQueryRes> pageVo = new PageVo<>();
        pageVo.setPageSize(dataOrgQueryReq.getPageSize());
        pageVo.setCurrent(dataOrgQueryReq.getCurrent());
        pageVo.setTotal(page.getTotalElements());
        pageVo.setData(queryResList);
        return pageVo;
    }

    private void existDataOrg(Integer id) {
        Optional<DataOrg> dbIdDataOrgOptional = dataOrgRepository.findById(id);
        if (dbIdDataOrgOptional.isEmpty())
            throw new IllegalArgumentException("该角色不存在");
    }

    public List<Selection> querySelect(DataOrgQueryReq dataOrgQueryReq) {
        String name = dataOrgQueryReq.getName();
        List<DataOrg> dataOrgs ;
        if(StringUtils.hasText(dataOrgQueryReq.getName())){
            dataOrgs = dataOrgRepository.findAllByNameLike("%"+name+"%");
        }else {
            dataOrgs = dataOrgRepository.findAll();
        }
        return dataOrgs.stream().map(dataOrg -> {
            Selection selection = new Selection();
            selection.setLabel(dataOrg.getName());
            selection.setValue(dataOrg.getId().toString());
            return selection;
        }).toList();
    }
}
