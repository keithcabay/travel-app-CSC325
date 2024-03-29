package com.travel.virtualtravelassistant.User;

public class CurrentUser {
    private static CurrentUser instance;

    private UserInfo userInfo;

    private CurrentUser(){

    }

    public static CurrentUser getInstance(){
        if(instance == null){
            instance = new CurrentUser();
        }
        return instance;
    }

    public UserInfo getUserInfo(){
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo){
        this.userInfo = userInfo;
    }
}
