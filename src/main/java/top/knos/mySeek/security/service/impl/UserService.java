package top.knos.mySeek.security.service.impl;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.cache.NullUserCache;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import top.knos.mySeek.exception.TipsException;
import top.knos.mySeek.security.domain.Authority;
import top.knos.mySeek.security.domain.DataOrg;
import top.knos.mySeek.security.domain.Resource;
import top.knos.mySeek.security.domain.User;
import top.knos.mySeek.security.pojo.UserVo;
import top.knos.mySeek.security.repository.AuthorityRepository;
import top.knos.mySeek.security.repository.DataOrgRepository;
import top.knos.mySeek.security.repository.ResourceRepository;
import top.knos.mySeek.security.repository.UserRepository;
import top.knos.mySeek.security.service.UserDetailsManager;
import top.knos.mySeek.security.service.ValidateUserSupport;
import top.knos.mySeek.security.support.SecurityUtils;
import top.knos.mySeek.security.vo.Me;
import top.knos.mySeek.security.vo.TreeData;
import top.knos.mySeek.security.vo.req.*;
import top.knos.mySeek.security.vo.res.*;
import top.knos.mySeek.support.vo.PageKeyWords;
import top.knos.mySeek.support.vo.PageVo;
import top.knos.mySeek.utils.JpaUtils;
import top.knos.mySeek.utils.MyAssert;

import java.security.Principal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService implements UserDetailsManager {

    private final static int username_length_min = 4;
    private final static int username_length_max = 20;
    private final static int password_length_min = 6;
    private final static int password_length_max = 20;


    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final DataOrgRepository dataOrgRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;
    private final ValidateUserSupport validateUserSupport;
    private UserCache userCache = new NullUserCache();

    public UserService(UserRepository userRepository,
                       AuthorityRepository authorityRepository,
                       DataOrgRepository dataOrgRepository, ResourceRepository resourceRepository,
                       PasswordEncoder passwordEncoder,
                       ValidateUserSupport validateUserSupport) {

        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.dataOrgRepository = dataOrgRepository;
        this.resourceRepository = resourceRepository;
        this.passwordEncoder = passwordEncoder;
        this.validateUserSupport = validateUserSupport;
    }


    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 注意：验证码校验属于登录流程（AuthService），此处只负责加载用户，
        // 否则 JWT 鉴权路径（无验证码参数）会被误判为验证码错误。
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        Collection<Authority> authorities = user.getAuthorities();//防止jpa延迟加载
        log.debug("[{}]正在登录,拥有{}个角色", username, authorities.size());
        return user;
    }


    @Override
    public void changePassword(PasswordUpdateReq passwordUpdateReq) {
        User dbUser = validateUserSupport.validUserExist(passwordUpdateReq.getId());
        validateUserSupport.validPassword(passwordUpdateReq.getNewPassword());
        Assert.isTrue(passwordEncoder.matches(passwordUpdateReq.getOldPassword(), dbUser.getPassword()), "你输入的原始密码不正确");
        String newPassword = passwordEncoder.encode(passwordUpdateReq.getNewPassword());
        dbUser.setPassword(newPassword);
        userRepository.save(dbUser);
    }

    @Override
    public void adminChangePassword(PasswordUpdateReq passwordUpdateReq) {
        User dbUser = validateUserSupport.validUserExist(passwordUpdateReq.getId());
        validateUserSupport.validPassword(passwordUpdateReq.getNewPassword());
        String newPassword = passwordEncoder.encode(passwordUpdateReq.getNewPassword());
        dbUser.setPassword(newPassword);
        userRepository.save(dbUser);
    }

    @Override
    public void changeCurrentUserPassword(String oldPassword, String newPassword) throws AuthenticationException {
        Authentication currentUser = SecurityContextHolder.getContext().getAuthentication();
        newPassword = passwordEncoder.encode(newPassword);
        if (currentUser == null) {
            // This would indicate bad coding somewhere
            throw new AccessDeniedException("无法更改密码，因为在当前用户的上下文中找不到身份验证对象。");
        }

        String username = currentUser.getName();

        log.debug("Changing password for user '" + username + "'");

        User reference = userRepository.findByUsername(username);
        reference.setPassword(newPassword);
        reference.setUpdateAt(new Date());
        userRepository.save(reference);

        //userRepository.setPasswordForUsername(newPassword,username);

        SecurityContextHolder.getContext().setAuthentication(createNewAuthentication(currentUser, newPassword));

        userCache.removeUserFromCache(username);
    }

    @Override
    public void createUser(final UserDetails user) {
        User reference = (User) user;
        validateUserDetails(reference);
        userRepository.save(reference);
        insertUserAuthorities(reference);
    }

    private void validateUserDetails(UserDetails user) {
        Assert.hasText(user.getUsername(), "Username may not be empty or null");
        validateAuthorities(user.getAuthorities());
    }

    private void validateAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Assert.notNull(authorities, "Authorities list must not be null");

        for (GrantedAuthority authority : authorities) {
            Assert.notNull(authority, "Authorities list contains a null entry");
            Assert.hasText(authority.getAuthority(), "getAuthority() method must return a non-empty string");
        }
    }

    private void insertUserAuthorities(User reference) {
        userRepository.save(reference);
    }

    public void resetPassword(String newPassword) throws AuthenticationException {
        Authentication currentUser = SecurityContextHolder.getContext().getAuthentication();
        newPassword = passwordEncoder.encode(newPassword);
        if (currentUser == null) {
            // This would indicate bad coding somewhere
            throw new AccessDeniedException("无法更改密码，因为在当前用户的上下文中找不到身份验证对象。");
        }

        String username = currentUser.getName();

        log.debug("Changing password for user '" + username + "'");

        User reference = userRepository.findByUsername(username);
        reference.setPassword(newPassword);
        reference.setUpdateAt(new Date());
        userRepository.save(reference);

        //userRepository.setPasswordForUsername(newPassword,username);

        SecurityContextHolder.getContext().setAuthentication(createNewAuthentication(currentUser, newPassword));

        userCache.removeUserFromCache(username);
    }

    protected Authentication createNewAuthentication(Authentication currentAuth, String newPassword) {
        UserDetails user = loadUserByUsername(currentAuth.getName());
        UsernamePasswordAuthenticationToken newAuthentication =
                new UsernamePasswordAuthenticationToken(user, newPassword, user.getAuthorities());
        newAuthentication.setDetails(currentAuth.getDetails());

        return newAuthentication;
    }

    @Override
    public boolean userExists(String username) {
        User user = userRepository.findByUsername(username);
        return user != null;
    }

    public String getNameById(Long id) {
        return userRepository.getReferenceById(id).getFullName();
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Me currentUser(Principal principal) {
        Me currentUser = new Me();
        User user = (User) ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
        BeanUtils.copyProperties(user, currentUser);
        String access = SecurityUtils.getAccess(user.getAuthorities());
        currentUser.setId(user.getId());
        currentUser.setUsername(user.getUsername());
        currentUser.setAccess(access);
        currentUser.setGroup(user.getBusinessGroup());
        currentUser.setNotifyCount(0);
        currentUser.setUnreadCount(0);
        currentUser.setPhone(user.getPhone());
        currentUser.setDialPhone(user.getDialPhone());

        List<RoleQueryRes> queryResList = user.getAuthorities().stream().map(u -> {
            RoleQueryRes roleQueryRes = new RoleQueryRes();
            BeanUtils.copyProperties(u, roleQueryRes);
            return roleQueryRes;
        }).toList();
        currentUser.setRoles(queryResList);

        return currentUser;
    }

    /**
     * 现在啥也没做，只是为了演示
     *
     * @param principal
     * @return
     */
    public String logout(Principal principal) {
        log.debug("退出登录：{}", principal.getName());
        return "退出登录成功";
    }

    public List<MenuRes> myMenu(User user) {
        return makeMenuTree(user);
        /*Collection<Authority> authorities = user.getAuthorities();
        List<Resource> resources = new ArrayList<>();
        for (Authority authority : authorities) {
            resources.addAll(authority.getResources());
        }
        //去重
        resources = resources
                .stream()
                .filter(resource -> resource.getType().equals(1) && !resource.getDisabled())
                .distinct()
                .sorted(Comparator.comparing(Resource::getSort))
                .collect(Collectors.toList());

        return makeMenuTree(null, resources);*/
    }

    /**
     * 生成菜单树,父级菜单可能没有选中，所以要从子菜单网上找,还要保持顺序
     *
     * @param user
     * @return
     */
    //@Cacheable("user_menu_tree")
    public List<MenuRes> makeMenuTree(User user) {

        Collection<Authority> authorities = user.getAuthorities();
        List<Resource> resources = new ArrayList<>();
        for (Authority authority : authorities) {
            resources.addAll(authority.getResources());
        }
        List<Resource> allMenu = resourceRepository.findAllByTypeOrderBySort(1);
        //List<Resource> allMenu = resourceRepository.findAll();
        //去重
        resources = resources
                .stream()
                .filter(resource -> resource.getType().equals(1) && !resource.getDisabled())
                .distinct()
                .sorted(Comparator.comparing(Resource::getSort))
                .collect(Collectors.toList());
        //可以合理的假设只有两级目录
        Map<Integer, List<Resource>> parentResMap = resources
                .stream()
                .filter(r -> r.getParent() == null)
                .collect(Collectors.groupingBy(Resource::getId));
        Map<Integer, List<Resource>> resMap = resources
                .stream()
                .filter(r -> r.getParent() != null)
                .collect(Collectors.groupingBy(resource -> resource.getParent().getId()));
        List<MenuRes> children = new ArrayList<>();
        for (Resource menu : allMenu) {
            List<Resource> childrenResources = resMap.get(menu.getId());

            if (childrenResources != null) {
                MenuRes menuRes = new MenuRes();
                BeanUtils.copyProperties(menu, menuRes);
                children.add(menuRes);
                List<MenuRes> childrenMenu = childrenResources.stream().map(childrenResource -> {
                    MenuRes childrenMenuRes = new MenuRes();
                    BeanUtils.copyProperties(childrenResource, childrenMenuRes);
                    return childrenMenuRes;
                }).toList();
                menuRes.setChildren(childrenMenu);
            } else if (menu.getParent() == null && parentResMap.get(menu.getId()) != null) {
                //只有一级菜单，没有子目录
                MenuRes menuRes = new MenuRes();
                BeanUtils.copyProperties(menu, menuRes);
                children.add(menuRes);
            }
        }

        return children;

    }

    private List<MenuRes> makeMenuTree(Resource parent, List<Resource> resources) {
        List<MenuRes> children = new ArrayList<>();
        for (Resource resource : resources) {
            if (Objects.equals(resource.getParent(), parent)) {
                MenuRes menuRes = new MenuRes();
                BeanUtils.copyProperties(resource, menuRes);
                children.add(menuRes);
                List<MenuRes> currentChildren = makeMenuTree(resource, resources);
                menuRes.setChildren(currentChildren);
            }
        }

        return children;
    }


    public void add(UserAddReq userAddReq) {
        User user = new User();

        user.setEmail(userAddReq.getEmail());
        user.setEnabled(userAddReq.getEnabled());
        user.setPhone(userAddReq.getPhone());
        user.setLevel(userAddReq.getLevel());
        user.setUsername(userAddReq.getUsername());
        user.setPassword(userAddReq.getPassword());
        Date now = new Date();
        user.setCreatedAt(now);
        user.setUpdateAt(now);
        user.setName(userAddReq.getName());

        List<Authority> authorities = authorityRepository.findAll();
        Set<Authority> newAuths = SecurityUtils.getAuth(userAddReq.getAccess(), authorities);
        user.setAuthorities(newAuths);

        validateAndSave(user);
    }

    public void delete(DeleteReq deleteReq) {
        for (Integer id : deleteReq.getIds()) {
            userRepository.deleteById(id.longValue());
        }
    }

    public void update(UserAddReq userAddReq) {
        Optional<User> userOptional = userRepository.findById(userAddReq.getId());
        if (userOptional.isEmpty()) {
            throw new TipsException("记录不存在");
        }
        User user = userOptional.get();
        user.setEmail(userAddReq.getEmail());
        user.setEnabled(userAddReq.getEnabled());
        user.setPhone(userAddReq.getPhone());
        user.setLevel(userAddReq.getLevel());
        user.setDialPhone(userAddReq.getDialPhone());
        if (StringUtils.isNotEmpty(userAddReq.getPassword())) {
            MyAssert.hasLength(userAddReq.getPassword(), password_length_min, password_length_max, String.format("密码长度范围[%d,%d)", password_length_min, password_length_max));
            user.setPassword(userAddReq.getPassword());
            encodePassowrd(user);
        }

        user.setUpdateAt(new Date());

        validateAndUpdate(user);
    }

    public PageVo<UserQueryRes> queryUser(UserQueryReq userQueryReq) {
        User user = new User();
        BeanUtils.copyProperties(userQueryReq, user);
        //boolean型没法注入null，使用Boolean注入
        user.setEnabled(userQueryReq.getEnabled());
        user.setAccountNonExpired(null);
        user.setAccountNonLocked(null);
        user.setCredentialsNonExpired(null);
        Example<User> userExample = Example.of(user, JpaUtils.getExampleMatcher());
        Page<User> userPage = userRepository.findAll(userExample, JpaUtils.createPage(userQueryReq));
        List<UserQueryRes> userQueryResList = userPage.getContent().stream().map(u -> {
            UserQueryRes userQueryRes = new UserQueryRes();
            BeanUtils.copyProperties(u, userQueryRes);
            userQueryRes.setId(u.getId());
            String access = SecurityUtils.getAccess(u.getAuthorities());
            userQueryRes.setAccess(access);
            return userQueryRes;
        }).toList();
        PageVo<UserQueryRes> pageVo = new PageVo<>();
        pageVo.setPageSize(userPage.getPageable().getPageSize());
        pageVo.setCurrent(userPage.getPageable().getPageNumber() + 1);
        pageVo.setTotal(userPage.getTotalElements());
        pageVo.setData(userQueryResList);

        return pageVo;
    }


    public AllTreeConfigRes queryAllRole() {
        List<Authority> allAuthorities = authorityRepository.findAll();
        List<TreeData> treeData = allAuthorities.stream().map(a -> {
            TreeData data = new TreeData();
            data.setId(a.getId());
            data.setTitle(a.getName());
            data.setKey(data.getId());
            return data;
        }).collect(Collectors.toList());

        AllTreeConfigRes treeConfigRes = new AllTreeConfigRes();
        treeConfigRes.setTreeData(treeData);
        return treeConfigRes;
    }

    public AllTreeConfigRes queryAllDataOrg() {
        List<DataOrg> allDataOrgs = dataOrgRepository.findAll();
        List<TreeData> treeData = allDataOrgs.stream().map(a -> {
            TreeData data = new TreeData();
            data.setId(a.getId());
            data.setTitle(a.getName());
            data.setKey(data.getId());
            return data;
        }).collect(Collectors.toList());

        AllTreeConfigRes treeConfigRes = new AllTreeConfigRes();
        treeConfigRes.setTreeData(treeData);
        return treeConfigRes;
    }

    public void validateAndSave(User user) {
        validSaveUser(user);
        encodePassowrd(user);
        userRepository.save(user);
    }

    public void validateAndUpdate(User user) {
        validUpdateUser(user);
        userRepository.save(user);
    }

    private void encodePassowrd(User user) {
        if (StringUtils.isNotEmpty(user.getPassword())) {
            String password = passwordEncoder.encode(user.getPassword());
            user.setPassword(password);
        }
    }

    private void validSaveUser(User user) {
        Assert.hasText(user.getUsername(), "用户名不能为空");
        MyAssert.hasLengthMin(user.getUsername(), username_length_min, String.format("登录名不能小于%d", username_length_min));
        MyAssert.hasLengthMax(user.getUsername(), username_length_max, String.format("登录名必须不超过%d字节", username_length_max));
        Assert.hasText(user.getPassword(), "密码不能为空");
        MyAssert.hasLengthMin(user.getPassword(), password_length_min, String.format("密码不能小于%d", password_length_min));
        MyAssert.hasLengthMax(user.getPassword(), password_length_max, String.format("密码必须不超过%d字节", password_length_max));

        User dbUser = userRepository.findByUsername(user.getUsername());
        if (dbUser != null) {
            throw new IllegalArgumentException("用户名已经存在");
        }
    }

    private void validUpdateUser(User user) {
        Assert.notNull(user.getId(), "用户id不能为空");
        Optional<User> userOptional = userRepository.findById(user.getId());
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("该用户不存在");
        }
        User dbUser = userOptional.get();
        Assert.isTrue(dbUser.getUsername().equals(user.getUsername()), "用户名不能修改");
        Assert.hasText(user.getUsername(), "用户名不能为空");
        MyAssert.hasLengthMin(user.getUsername(), username_length_min, String.format("用户名不能小于%d", username_length_min));
        MyAssert.hasLengthMax(user.getUsername(), username_length_max, String.format("用户名必须不超过%d字节", username_length_max));
    }

    public SelectKeysRes queryUserRole(Long id) {
        User user = userRepository.getReferenceById(id);

        Collection<Authority> authorities = user.getAuthorities();
        List<Integer> checkedKeys = new ArrayList<>();
        if (!CollectionUtils.isEmpty(authorities)) {
            checkedKeys = authorities.stream().map(Authority::getId).collect(Collectors.toList());
        }


        SelectKeysRes treeConfigRes = new SelectKeysRes();
        treeConfigRes.setCheckedKeys(checkedKeys);
        return treeConfigRes;
    }

    public void updateUserRole(TreeDataUpdateReq req) {
        User user = userRepository.getReferenceById(req.getId().longValue());

        List<Integer> checkedKeys = req.getCheckedKeys();
        List<Authority> authorities = authorityRepository.findAllById(checkedKeys);
        user.setAuthorities(new LinkedHashSet<>(authorities));
        userRepository.save(user);
    }

    public SelectKeysRes queryUserDataOrg(Long id) {
        User user = userRepository.getReferenceById(id);

        Collection<DataOrg> dataOrgs = user.getDataOrgs();
        List<Integer> checkedKeys = new ArrayList<>();
        if (!CollectionUtils.isEmpty(dataOrgs)) {
            checkedKeys = dataOrgs.stream().map(DataOrg::getId).collect(Collectors.toList());
        }

        SelectKeysRes treeConfigRes = new SelectKeysRes();
        treeConfigRes.setCheckedKeys(checkedKeys);
        return treeConfigRes;
    }

    public void updateUserDataOrg(TreeDataUpdateReq treeDataUpdateReq) {
        User user = userRepository.getReferenceById(treeDataUpdateReq.getId().longValue());

        List<Integer> checkedKeys = treeDataUpdateReq.getCheckedKeys();
        List<DataOrg> authorities = dataOrgRepository.findAllById(checkedKeys);
        user.setDataOrgs(new LinkedHashSet<>(authorities));
        userRepository.save(user);
    }

    public Map<Long, User> findUserMap(List<Long> ids) {
        List<User> users = userRepository.findAllById(ids);
        return users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
    }

    public List<UserQueryRes> queryUserSelect(PageKeyWords keyWords) {

        Pageable pageRequest = PageRequest.of(keyWords.getCurrent() - 1, keyWords.getPageSize(), Sort.by(Sort.Order.asc("id")));
        String keywords = StringUtils.strip(keyWords.getKeyWords());
        if (keywords == null) {
            keywords = "%";
        } else {
            keywords = "%" + keywords + "%";
        }
        List<User> users = userRepository.findByNameLike(keywords, pageRequest);
        List<UserQueryRes> resList = new ArrayList<>();
        for (User user : users) {
            UserQueryRes userQueryRes = new UserQueryRes();
            userQueryRes.setId(user.getId());
            userQueryRes.setName(user.getName());
            resList.add(userQueryRes);
        }

        return resList;
    }


    public void updateUserBySelf(UserVo userVo) {
        User currentUser = SecurityUtils.getUser();
        Optional<User> optionalUser = userRepository.findById(currentUser.getId());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setName(userVo.getName());
            user.setPhone(userVo.getPhone());
            user.setEmail(userVo.getEmail());
            user.setSignature(userVo.getSignature());
            user.setDialPhone(userVo.getDialPhone());
            userRepository.save(user);
        }
    }

    public void updateAvatarBySelf(UserVo userVo) {
        Optional<User> optionalUser = userRepository.findById(userVo.getId());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setAvatar(userVo.getAvatar());
            userRepository.save(user);
        }
    }
}
