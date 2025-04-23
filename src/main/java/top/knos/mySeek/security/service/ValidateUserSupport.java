package top.knos.mySeek.security.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import top.knos.mySeek.security.domain.User;
import top.knos.mySeek.security.repository.UserRepository;
import top.knos.mySeek.utils.MyAssert;

import java.util.Collection;
import java.util.Optional;

/**
 * TODO
 *
 * @Author andy
 * @Date 2022/3/5 20:21
 */
@Service
public class ValidateUserSupport {

    private final static int username_length_min=4;
    private final static int username_length_max=20;
    private final static int password_length_min=6;
    private final static int password_length_max=20;


    private final UserRepository userRepository;

    public ValidateUserSupport(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public void validSaveUser(User user) {
        Assert.hasText(user.getUsername(),"用户名不能为空");
        MyAssert.hasLengthMin(user.getUsername(),username_length_min,String.format("用户名不能小于%d",username_length_min));
        MyAssert.hasLengthMax(user.getUsername(),username_length_max,String.format("用户名必须不超过%d字节",username_length_max));
        Assert.hasText(user.getPassword(),"密码不能为空");
        MyAssert.hasLengthMin(user.getPassword(),password_length_min,String.format("密码不能小于%d",password_length_min));
        MyAssert.hasLengthMax(user.getPassword(),password_length_max,String.format("密码必须不超过%d字节",password_length_max));

        User dbUser = userRepository.findByUsername(user.getUsername());
        if(dbUser != null){
            throw new IllegalArgumentException("用户名已经存在");
        }
        validateAuthorities(user.getAuthorities());
    }
    public void validUpdateUser(User user) {
        Assert.notNull(user.getId(),"用户id不能为空");
        Optional<User> userOptional = userRepository.findById(user.getId());
        if(!userOptional.isPresent()){
            throw new IllegalArgumentException("该用户不存在");
        }
        User dbUser = userOptional.get();
        user.setPassword(dbUser.getPassword());
        Assert.isTrue(dbUser.getUsername().equals(user.getUsername()),"用户名不能修改");
        Assert.hasText(user.getUsername(),"用户名不能为空");
        MyAssert.hasLengthMin(user.getUsername(),username_length_min,String.format("用户名不能小于%d",username_length_min));
        MyAssert.hasLengthMax(user.getUsername(),username_length_max,String.format("用户名必须不超过%d字节",username_length_max));
        validateAuthorities(user.getAuthorities());
    }

    public void validateAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Assert.notNull(authorities, "权限不能为空");

        for (GrantedAuthority authority : authorities) {
            Assert.notNull(authority, "权限不能为空");
            Assert.hasText(authority.getAuthority(), "权限不能为空");
        }
    }

    public void validPassword(String password) {
        Assert.hasText(password,"密码不能为空");
        MyAssert.hasLengthMin(password,password_length_min,String.format("密码不能小于%d",password_length_min));
        MyAssert.hasLengthMax(password,password_length_max,String.format("密码必须不超过%d字节",password_length_max));
    }

    public User validUserExist(Long userId) {
        Assert.notNull(userId,"用户id不能为空");
        Optional<User> userOptional = userRepository.findById(userId);
        if(!userOptional.isPresent()){
            throw new IllegalArgumentException("该用户不存在");
        }
        return userOptional.get();
    }

}
