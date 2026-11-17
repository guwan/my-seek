package top.knos.mySeek.security.service;

import jakarta.transaction.Transactional;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import top.knos.mySeek.exception.TipsException;
import top.knos.mySeek.security.domain.Resource;
import top.knos.mySeek.security.repository.ResourceRepository;
import top.knos.mySeek.security.vo.req.DeleteReq;
import top.knos.mySeek.security.vo.req.MenuAddReq;
import top.knos.mySeek.security.vo.req.MenuQueryReq;
import top.knos.mySeek.security.vo.res.MenuQueryRes;
import top.knos.mySeek.support.vo.PageVo;
import top.knos.mySeek.utils.JpaUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author 卢文武(01380604)
 * @date 2018/11/16 18:41
 */
@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Transactional
    public void add(MenuAddReq menuAddReq) {

        Resource resource = new Resource();
        BeanUtils.copyProperties(menuAddReq, resource);
        if (menuAddReq.getParentId() != null) {
            Resource parent = resourceRepository.getReferenceById(menuAddReq.getParentId());
            resource.setParent(parent);
        }
        if(resource.getDisabled()==null){
            resource.setDisabled(false);
        }
        resourceRepository.save(resource);
    }

    public void delete(DeleteReq deleteReq) {
        resourceRepository.deleteAllByIdIn(deleteReq.getIds());
    }

    @Transactional
    public void update(MenuAddReq menuAddReq) {
        Resource resource = resourceRepository.getReferenceById(menuAddReq.getId());
        BeanUtils.copyProperties(menuAddReq, resource);
        if (menuAddReq.getParentId() != null) {
            Resource parent = resourceRepository.getReferenceById(menuAddReq.getParentId());
            resource.setParent(parent);
        }
        resourceRepository.save(resource);
    }

    public PageVo<MenuQueryRes> query(MenuQueryReq menuQueryReq) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Resource resource = new Resource();
        BeanUtils.copyProperties(menuQueryReq, resource);
        Example<Resource> example = JpaUtils.createExample(menuQueryReq, Resource.class);
        Page<Resource> page = resourceRepository.findAll(example, JpaUtils.createPageSort(menuQueryReq));
        List<MenuQueryRes> queryResList = page.getContent().stream().map(u -> {
            MenuQueryRes queryRes = new MenuQueryRes();
            BeanUtils.copyProperties(u, queryRes);
            Integer parentId = u.getParent() == null ? null : u.getParent().getId();
            queryRes.setParentId(parentId);
            return queryRes;
        }).collect(Collectors.toList());
        PageVo<MenuQueryRes> pageVo = new PageVo<>();
        pageVo.setCurrent(menuQueryReq.getCurrent());
        pageVo.setPageSize(menuQueryReq.getPageSize());
        pageVo.setTotal(page.getTotalElements());
        pageVo.setData(queryResList);
        return pageVo;
    }

    /**
     * 智能添加菜单
     * 如果是顶级目录，则自动排序
     * 如果是页面，则添加响应的接口
     *
     * @param menuAddReq
     */
    @Transactional
    public void intelligenceAdd(MenuAddReq menuAddReq) {
        Resource resource = new Resource();
        BeanUtils.copyProperties(menuAddReq, resource);

        Resource parent = null;
        if (menuAddReq.getParentId() != null) {
            parent = resourceRepository.getReferenceById(menuAddReq.getParentId());
            resource.setParent(parent);
        } else if (menuAddReq.getParentName() != null) {
            parent = resourceRepository.findFirstByName(menuAddReq.getParentName());
            if (parent == null) {
                //不存在这哦上级，则创建一个

                //顶级目录,查找排序最后的目录
                Resource lastResource = resourceRepository.findFirstByParentIsNullOrderBySortDesc();

                parent = new Resource();
                parent.setType(1);
                parent.setSort(lastResource == null ? 100 : lastResource.getSort() + 100);
                parent.setIcon(lastResource == null ? null : lastResource.getIcon());
                parent.setName(menuAddReq.getParentName());
                parent.setShowName(menuAddReq.getParentShowName());
                parent.setDisabled(false);
                parent.setPath("/" + menuAddReq.getParentName());
                resourceRepository.save(parent);
            }
        }
        if (parent != null) {
            menuAddReq.setParentId(parent.getId());
            resource.setParent(parent);
        }

        String path = resource.getPath();
        if (!path.startsWith("/")) {
            throw new TipsException("路径填写错误");
        }

        int pathDeep = stringCount(path, "/");
        if (pathDeep == 1) {
            //顶级目录,查找排序最后的目录
            Resource lastResource = resourceRepository.findFirstByParentIsNullOrderBySortDesc();
            resource.setSort(lastResource == null ? 100 : lastResource.getSort() + 100);
            resource.setIcon(lastResource == null ? null : lastResource.getIcon());

            String resourceName = path.substring(1);
            resource.setName(resourceName);
            resource.setDisabled(false);
            resourceRepository.save(resource);
        } else if (pathDeep == 2) {
            //页面
            Resource parentResource = resourceRepository.getReferenceById(menuAddReq.getParentId());
            Resource lastResource = resourceRepository.findFirstByParentOrderBySortDesc(parentResource);
            if (lastResource == null) {
                resource.setSort(parentResource.getSort() * 100 + 1);
            } else {
                resource.setSort(lastResource.getSort() + 100);
            }
            String uniqueName = path.substring(path.lastIndexOf("/") + 1);
            //String resourceName = path.substring(1).replace("/", ".");
            resource.setName(uniqueName);
            resource.setType(1);
            resource.setDisabled(false);
            resourceRepository.save(resource);

            //新建接口
            List<Resource> apis = new ArrayList<>();
            Resource queryResource = createApiResource(resource, uniqueName, "query", "查询", 1);
            Resource addResource = createApiResource(resource, uniqueName, "add", "新增", 2);
            Resource updateResource = createApiResource(resource, uniqueName, "update", "更新", 3);
            Resource deleteResource = createApiResource(resource, uniqueName, "delete", "删除", 4);
            apis.add(queryResource);
            apis.add(addResource);
            apis.add(updateResource);
            apis.add(deleteResource);

            resourceRepository.saveAll(apis);
        }
        menuAddReq.setId(resource.getId());

    }

    private Resource createApiResource(Resource parentResource, String uniqueName, String operation, String showName, Integer sort) {

        Resource resource = new Resource();
        resource.setName(uniqueName + "." + operation);
        resource.setPath("/api/" + uniqueName + "/" + operation);
        resource.setShowName(parentResource.getShowName() + showName);
        resource.setParent(parentResource);
        resource.setSort(Integer.valueOf(parentResource.getSort().toString() + String.format("%02d",sort)));
        resource.setDisabled(false);
        resource.setType(2);
        return resource;

    }

    public static int stringCount(String string, String subString) {
        int index = 0;
        int count = 0;
        while ((index = string.indexOf(subString, index)) != -1) {
            //在循环控制的条件中将获得的索引值赋给index,不等于-1是因为.在JDK中规定了indexOf查找不到子字符串时就返回-1.在我们这里也就是跳出循环的意思
            index++;//得到索引后,从本位置开始进行下一次循环,所以字符串的索引加一
            count++;//计数器统计出现的次数
        }
        return count;
    }
    @Transactional
    public Resource copy(MenuQueryReq menuQueryReq) {
        Optional<Resource> resourceOptional = resourceRepository.findById(menuQueryReq.getId());
        if (resourceOptional.isEmpty()) {
            throw new RuntimeException("请求参数错误");
        }
        Resource resource = resourceOptional.get();
        Resource newResource = new Resource();
        BeanUtils.copyProperties(resource, newResource);
        newResource.setId(null);
        newResource.setSort(resource.getSort()+10);
        resourceRepository.save(newResource);

        List<Resource> children1 = resourceRepository.findAllByParentOrderBySortDesc(resource);

        if(CollectionUtils.isNotEmpty(children1)){
            //有子元素，一起复制了。
            for (Resource child1 : children1) {

                Resource newChild = new Resource();
                BeanUtils.copyProperties(child1, newChild);
                newChild.setParent(newResource);
                newChild.setId(null);

                resourceRepository.save(newChild);
                //二级元素，菜单最多允许三层，所以这个没有下一层了。
                List<Resource> children2 = resourceRepository.findAllByParentOrderBySortDesc(child1);
                if(CollectionUtils.isNotEmpty(children2)){
                    for (Resource child2 : children2) {
                        Resource newChild2 = new Resource();
                        BeanUtils.copyProperties(child2, newChild2);
                        // 挂到复制出来的新节点下，而不是原节点
                        newChild2.setParent(newChild);
                        newChild2.setId(null);
                        resourceRepository.save(newChild2);
                    }
                }
            }
        }

        return newResource;
    }
}
