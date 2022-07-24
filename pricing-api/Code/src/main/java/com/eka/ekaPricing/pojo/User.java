package com.eka.ekaPricing.pojo;

public class User
{
    private String sessionTimeoutInSeconds;

    private Auth2AccessToken auth2AccessToken;

    public String getSessionTimeoutInSeconds ()
    {
        return sessionTimeoutInSeconds;
    }

    public void setSessionTimeoutInSeconds (String sessionTimeoutInSeconds)
    {
        this.sessionTimeoutInSeconds = sessionTimeoutInSeconds;
    }

    public Auth2AccessToken getAuth2AccessToken ()
    {
        return auth2AccessToken;
    }

    public void setAuth2AccessToken (Auth2AccessToken auth2AccessToken)
    {
        this.auth2AccessToken = auth2AccessToken;
    }

    @Override
    public String toString()
    {
        return "User [sessionTimeoutInSeconds = "+sessionTimeoutInSeconds+", auth2AccessToken = "+auth2AccessToken+"]";
    }
}
