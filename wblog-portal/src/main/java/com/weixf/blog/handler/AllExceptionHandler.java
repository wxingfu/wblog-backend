package com.weixf.blog.handler;


import com.weixf.blog.common.exception.GlobalException;
import com.weixf.blog.utils.ExceptionUtil;
import com.weixf.blog.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@ControllerAdvice
public class AllExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result doException(Exception ex) {
        //e.printStackTrace();是打印异常的堆栈信息，指明错误原因，
        // 其实当发生异常时，通常要处理异常，这是编程的好习惯，所以e.printStackTrace()可以方便你调试程序！
        ex.printStackTrace();
        return Result.fail(-999, "系统异常");

    }

    @ExceptionHandler(GlobalException.class)
    public Result error(GlobalException e) {
        log.error(ExceptionUtil.getMessage(e));
        return Result.fail(e.getCode(), e.getMsg());
    }
}
