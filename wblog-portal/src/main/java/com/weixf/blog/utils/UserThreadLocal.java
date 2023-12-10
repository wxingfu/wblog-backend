package com.weixf.blog.utils;


import com.weixf.blog.dao.pojo.SysUser;

public class UserThreadLocal {

    private static final ThreadLocal<SysUser> LOCAL = new ThreadLocal<SysUser>();

    private UserThreadLocal() {
    }

    // 放入
    public static void put(SysUser user) {
        LOCAL.set(user);
    }

    // 取出
    public static SysUser get() {
        return LOCAL.get();
    }

    // 删除
    public static void remove() {
        LOCAL.remove();
    }


}
