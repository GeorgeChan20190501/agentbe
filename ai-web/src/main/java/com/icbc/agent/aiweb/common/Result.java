package com.icbc.agent.aiweb.common;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Result<T> {
    private Integer code;
    private String msg;
    private T data;

    public Result() {
    }

    public Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data);
    }

    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(0, msg, data);
    }
    public static <T> Result<T> success(Integer code, String msg, T data) {
        return new Result<>(code, msg, data);
    }
    public static <T> Result<T> error(String msg) {
        return new Result<>(1, msg, null);
    }

    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

}
