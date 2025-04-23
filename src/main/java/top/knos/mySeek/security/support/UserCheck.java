package top.knos.mySeek.security.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.knos.mySeek.exception.TipsException;
import top.knos.mySeek.security.repository.UserRepository;
import top.knos.mySeek.security.service.GrecaptchaService;
import top.knos.mySeek.utils.MyAssert;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UserCheck {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GrecaptchaService grecaptchaService;
    public void checkRegister(UserRegisterReq userRegisterReq) {
        if(!Objects.equals("on",userRegisterReq.getAgreeTerms())){
            throw new TipsException("没同意协议，无法注册");
        }
        //利用正则表达式（可改进）验证邮箱是否符合邮箱的格式
        if(userRegisterReq.getEmail()==null&&!userRegisterReq.getEmail().matches("^\\w+@(\\w+\\.)+\\w+$")){
            throw new TipsException("邮箱格式错误");
        }
        checkPhone(userRegisterReq.getPhone());
        MyAssert.hasLength(userRegisterReq.getUsername(),4,15,"用户名长度错误");
        MyAssert.hasLength(userRegisterReq.getPassword(),6,30,"密码长度错误");
        MyAssert.hasLength(userRegisterReq.getJob(),2,30,"职业是我们为你登录后推荐的唯一凭据，不能为空");

        if(!grecaptchaService.validate(userRegisterReq.getUsername(),userRegisterReq.getCaptcha())){
            throw new TipsException("验证码错误");
        }

        //验证数据库是否存在
        if(userRepository.findByUsername(userRegisterReq.getUsername())!=null){
            throw new TipsException("用户名已经存在");
        }
        if(userRepository.findByEmail(userRegisterReq.getEmail())!=null){
            throw new TipsException("邮箱已经存在");
        }
    }
    public  void checkPhone(String phone) {
        String regex = "^((13[0-9])|(14[5,7,9])|(15([0-3]|[5-9]))|(166)|(17[0,1,3,5,6,7,8])|(18[0-9])|(19[8|9]))\\d{8}$";
        if (phone.length() != 11) {
            throw new TipsException("手机号应为11位数");
        } else {
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(phone);
            boolean isMatch = m.matches();
            if (!isMatch) {
                throw new TipsException("请填入正确的手机号");
            }
        }
    }
}
