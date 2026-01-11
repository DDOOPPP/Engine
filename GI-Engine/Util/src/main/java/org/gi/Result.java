package org.gi;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Result {
    int code;
    String message;

    public static Result SUCCESS = new Result(1, "Success");
    public static Result FAIL = new Result(-1, "Fail");
    public static Result CONNECTED = new Result(100, "Connected");
    public static Result DISCONNECTED = new Result(101, "Disconnected");

    public static Result NULL = new Result(-9999, "Null");
    public static Result EMPTY = new Result(-9999, "Empty");

    public static Result SUCCESS(int code, String message) {
        return new Result(code, message);
    }

    public static Result SUCCESS(String message) {
        return SUCCESS(100,message);
    }

    public static Result Error(int code, String message){
        return new Result(code, message);
    }

    public static Result Error(String message){
        return Error(-1, message);
    }

    public static Result Exception(int code,Exception e){
        return new Result(code, e.getMessage());
    }

    public static Result Exception(Exception e){
        return Exception(-9999, e);
    }

    public boolean isSuccess(){
        return this.equals(SUCCESS) || this.code == 1;
    }

    public boolean isConnected(){
        return this.equals(CONNECTED);
    }

    public boolean isDisconnected(){
        return this.equals(DISCONNECTED);
    }
}
