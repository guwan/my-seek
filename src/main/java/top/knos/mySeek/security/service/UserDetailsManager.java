package top.knos.mySeek.security.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import top.knos.mySeek.security.vo.req.PasswordUpdateReq;

/**
 * An extension of the {@link UserDetailsService} which provides the ability
 * to create new users and update existing ones.
 *
 * @author Luke Taylor
 * @since 2.0
 */
public interface UserDetailsManager extends UserDetailsService {


    void adminChangePassword(PasswordUpdateReq passwordUpdateReq);

    /**
     * Modify the current user's password. This should change the user's password in
     * the persistent user repository (datbase, LDAP etc).
     *
     * @param oldPassword current password (for re-authentication if required)
     * @param newPassword the password to change to
     */
    void changeCurrentUserPassword(String oldPassword, String newPassword);

    /**
     * 修改指定用户密码
     * @param passwordUpdateReq
     */
    void changePassword(PasswordUpdateReq passwordUpdateReq);

    void createUser(UserDetails user);

    /**
     * Check if a user with the supplied login name exists in the system.
     */
    boolean userExists(String username);

}
