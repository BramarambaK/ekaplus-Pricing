package com.eka.ekaPricing.pojo;

public class Auth2AccessToken
{
    private String scope;

    private String jti;

    private String expires_in;

    private String token_type;

    private String refresh_token;

    private String access_token;

    public String getScope ()
    {
        return scope;
    }

    public void setScope (String scope)
    {
        this.scope = scope;
    }

    public String getJti ()
    {
        return jti;
    }

    public void setJti (String jti)
    {
        this.jti = jti;
    }

    public String getExpires_in ()
    {
        return expires_in;
    }

    public void setExpires_in (String expires_in)
    {
        this.expires_in = expires_in;
    }

    public String getToken_type ()
    {
        return token_type;
    }

    public void setToken_type (String token_type)
    {
        this.token_type = token_type;
    }

    public String getRefresh_token ()
    {
        return refresh_token;
    }

    public void setRefresh_token (String refresh_token)
    {
        this.refresh_token = refresh_token;
    }

    public String getAccess_token ()
    {
        return access_token;
    }

    public void setAccess_token (String access_token)
    {
        this.access_token = access_token;
    }

    @Override
    public String toString()
    {
        return "Auth2AccessToken [scope = "+scope+", jti = "+jti+", expires_in = "+expires_in+", token_type = "+token_type+", refresh_token = "+refresh_token+", access_token = "+access_token+"]";
    }
}