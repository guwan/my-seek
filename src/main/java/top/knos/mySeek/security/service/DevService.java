package top.knos.mySeek.security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import top.knos.mySeek.AppProfiles;
import top.knos.mySeek.constant.AuthEnum;
import top.knos.mySeek.security.domain.Authority;
import top.knos.mySeek.security.domain.Resource;
import top.knos.mySeek.security.repository.AuthorityRepository;
import top.knos.mySeek.security.repository.ResourceRepository;
import top.knos.mySeek.security.vo.req.AutoGenerateMenuReq;
import top.knos.mySeek.security.vo.req.MenuAddReq;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DevService {

    @Autowired
    private Environment env;
    @Autowired
    private ResourceService resourceService;
    @Autowired
    private ResourceRepository resourceRepository;
    @Autowired
    private AuthorityRepository authorityRepository;

    public void autoGenerateMenu(AutoGenerateMenuReq autoGenerateMenuReq) {

        //只有开发环境才推送允许执行
        if (!env.acceptsProfiles(AppProfiles.dev)) {
            return;
        }

        MenuAddReq menuAddReq = new MenuAddReq();
        menuAddReq.setShowName(autoGenerateMenuReq.getShowName());
        menuAddReq.setParentName(autoGenerateMenuReq.getParentName());
        menuAddReq.setParentShowName(autoGenerateMenuReq.getParentShowName());
        menuAddReq.setPath(autoGenerateMenuReq.getPath());
        menuAddReq.setType(1);
        resourceService.intelligenceAdd(menuAddReq);

        if (autoGenerateMenuReq.getAutoGenerate4Admin()) {
            //为管理员自动添加权限
            Authority authority = authorityRepository.findByAuthority(AuthEnum.ADMIN.getName());

            Resource root = resourceRepository.getReferenceById(menuAddReq.getId());

            Resource resource = resourceRepository.getReferenceById(menuAddReq.getId());
            List<Resource> children = resourceRepository.findAllByParentOrderBySortDesc(resource);
            List<Resource> allResource = new ArrayList<>();
            allResource.add(root);
            allResource.add(resource);
            allResource.addAll(children);

            authority.getResources().addAll(allResource);
            authorityRepository.save(authority);
        }

    }
}
