package com.weixf.blog.service;


import com.weixf.blog.dao.pojo.SysUser;
import com.weixf.blog.vo.Result;
import com.weixf.blog.vo.params.LoginParam;
import org.springframework.transaction.annotation.Transactional;


@Transactional
public interface LoginService {

    /**
     * 登陆功能
     */
    Result login(LoginParam loginParam);

    SysUser checkToken(String token);

    /**
     * 退出登陆
     */
    Result logout(String token);


    /**
     * 注册
     */
    Result register(LoginParam loginParam);

}
