package com.weixf.blog.common.exception;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GlobalException extends RuntimeException {

    @ApiModelProperty(value = "状态码", example = "2000")
    private Integer code;

    @ApiModelProperty(value = "错误消息")
    private String msg;

}