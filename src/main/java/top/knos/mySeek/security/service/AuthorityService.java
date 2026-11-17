package top.knos.mySeek.security.service;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import top.knos.mySeek.common.MenuType;
import top.knos.mySeek.security.domain.Authority;
import top.knos.mySeek.security.domain.Resource;
import top.knos.mySeek.security.repository.AuthorityRepository;
import top.knos.mySeek.security.repository.ResourceRepository;
import top.knos.mySeek.security.vo.TreeData;
import top.knos.mySeek.security.vo.req.DeleteReq;
import top.knos.mySeek.security.vo.req.RoleAddReq;
import top.knos.mySeek.security.vo.req.RoleQueryReq;
import top.knos.mySeek.security.vo.req.TreeDataUpdateReq;
import top.knos.mySeek.security.vo.res.AllTreeConfigRes;
import top.knos.mySeek.security.vo.res.RoleQueryRes;
import top.knos.mySeek.security.vo.res.SelectKeysRes;
import top.knos.mySeek.support.vo.PageVo;
import top.knos.mySeek.utils.JpaUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 卢文武(01380604)
 * @date 2018/11/16 18:56
 */
@Service
public class AuthorityService {
    @Autowired
    private AuthorityRepository authorityRepository;
    @Autowired
    private ResourceRepository resourceRepository;

    public void add(RoleAddReq roleAddReq) {
        Authority authority = new Authority();
        notExistRole(roleAddReq);
        BeanUtils.copyProperties(roleAddReq, authority);
        authorityRepository.save(authority);
    }

    public void delete(DeleteReq deleteReq) {
        List<Integer> ids = deleteReq.getIds();
        authorityRepository.deleteAllByIdInBatch(ids);
    }

    private void notExistRole(RoleAddReq roleAddReq) {
        Authority dbAuthority = authorityRepository.findByAuthority(roleAddReq.getAuthority());
        if (dbAuthority != null)
            throw new IllegalArgumentException("该角色已经存在");
        Authority dbNameAuthority = authorityRepository.findByName(roleAddReq.getName());
        if (dbNameAuthority != null)
            throw new IllegalArgumentException("该角色名已经存在");
    }

    @Transactional
    public void update(RoleAddReq roleAddReq) {
        existRole(roleAddReq.getId());
        Assert.hasText(roleAddReq.getAuthority(), "角色不能为空");
        Assert.hasText(roleAddReq.getName(), "角色名不能为空");

        Authority authority = authorityRepository.getReferenceById(roleAddReq.getId());
        authority.setName(roleAddReq.getName());
        authority.setAuthority(roleAddReq.getAuthority());
        authority.setMark(roleAddReq.getMark());
        authority.setSort(roleAddReq.getSort());
        authorityRepository.save(authority);
    }

    public PageVo<RoleQueryRes> query(RoleQueryReq roleQueryReq) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Authority authority = new Authority();
        BeanUtils.copyProperties(roleQueryReq, authority);
        Example<Authority> authorityExample = JpaUtils.createExample(roleQueryReq, Authority.class);
        Page<Authority> page = authorityRepository.findAll(authorityExample, JpaUtils.createPage(roleQueryReq));
        List<RoleQueryRes> queryResList = page.getContent().stream().map(u -> {
            RoleQueryRes roleQueryRes = new RoleQueryRes();
            BeanUtils.copyProperties(u, roleQueryRes);
            return roleQueryRes;
        }).collect(Collectors.toList());
        PageVo<RoleQueryRes> pageVo = new PageVo<>();
        pageVo.setPageSize(roleQueryReq.getPageSize());
        pageVo.setCurrent(roleQueryReq.getCurrent());
        pageVo.setTotal(page.getTotalElements());
        pageVo.setData(queryResList);
        return pageVo;
    }

    private void existRole(Integer id) {
        Optional<Authority> dbIdAuthorityOptional = authorityRepository.findById(id);
        if (dbIdAuthorityOptional.isEmpty())
            throw new IllegalArgumentException("该角色不存在");
    }

    public AllTreeConfigRes queryAlllMenu() {
        AllTreeConfigRes allTreeConfigRes = new AllTreeConfigRes();
        List<Resource> allResources = resourceRepository.findAll(Sort.by("sort"));
        List<TreeData> treeData = allResources.stream().map(a -> {
            TreeData data = new TreeData();
            data.setId(a.getId());
            String type = a.getType().equals(MenuType.MENU.getCode()) ? MenuType.MENU.getName() : MenuType.INTERFACE.getName();
            data.setTitle("[" + type + "]" + a.getShowName());
            data.setParentId(a.getParent() == null ? null : a.getParent().getId());
            data.setKey(data.getId());
            return data;
        }).collect(Collectors.toList());

        treeData = makeMenuTree(null, treeData);
        allTreeConfigRes.setTreeData(treeData);

        return allTreeConfigRes;
    }

    @Transactional(readOnly = true)
    public SelectKeysRes queryRoleMenu(Integer id) {
        SelectKeysRes selectKeysRes = new SelectKeysRes();

        Authority authority = authorityRepository.getReferenceById(id);
        Collection<Resource> resources = authority.getResources();
        List<Integer> checkedKeys = new ArrayList<>();
        if (!CollectionUtils.isEmpty(resources)) {
            checkedKeys = resources.stream().map(Resource::getId).collect(Collectors.toList());
        }
        selectKeysRes.setCheckedKeys(checkedKeys);
        return selectKeysRes;
    }

    @Transactional
    public void updateRoleMenu(TreeDataUpdateReq req) {
        Authority authority = authorityRepository.getReferenceById(req.getId());

        List<Integer> checkedKeys = req.getCheckedKeys();
        List<Resource> authorities = resourceRepository.findAllById(checkedKeys);
        authority.setResources(new LinkedHashSet<>(authorities));
        authorityRepository.save(authority);
    }


    private List<TreeData> makeMenuTree(Integer parentId, List<TreeData> treeDatas) {
        List<TreeData> children = new ArrayList<>();
        for (TreeData treeData : treeDatas) {
            if (Objects.equals(treeData.getParentId(), parentId)) {
                children.add(treeData);
                List<TreeData> currentChildren = makeMenuTree(treeData.getId(), treeDatas);
                treeData.setChildren(currentChildren);
            }
        }

        return children;
    }

    public List<String> findRolesByUrl(String url) {
        return authorityRepository.findRolesByUrl(url);
    }
}
