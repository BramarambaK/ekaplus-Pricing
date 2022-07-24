package com.eka.ekaPricing.pojo;

public class ValidateToken {
	
	    private String[] permCodes;

	    private String externalUserId;

	    private String expiration;

	    private String userId;

	    private String userName;

	    private String deviceIdentifier;

	    private String[] roleIds;

	    private String userType;

	    public String[] getPermCodes ()
	    {
	        return permCodes;
	    }

	    public void setPermCodes (String[] permCodes)
	    {
	        this.permCodes = permCodes;
	    }

	    public String getExternalUserId ()
	    {
	        return externalUserId;
	    }

	    public void setExternalUserId (String externalUserId)
	    {
	        this.externalUserId = externalUserId;
	    }

	    public String getExpiration ()
	    {
	        return expiration;
	    }

	    public void setExpiration (String expiration)
	    {
	        this.expiration = expiration;
	    }

	    public String getUserId ()
	    {
	        return userId;
	    }

	    public void setUserId (String userId)
	    {
	        this.userId = userId;
	    }

	    public String getUserName ()
	    {
	        return userName;
	    }

	    public void setUserName (String userName)
	    {
	        this.userName = userName;
	    }

	    public String getDeviceIdentifier ()
	    {
	        return deviceIdentifier;
	    }

	    public void setDeviceIdentifier (String deviceIdentifier)
	    {
	        this.deviceIdentifier = deviceIdentifier;
	    }

	    public String[] getRoleIds ()
	    {
	        return roleIds;
	    }

	    public void setRoleIds (String[] roleIds)
	    {
	        this.roleIds = roleIds;
	    }

	    public String getUserType ()
	    {
	        return userType;
	    }

	    public void setUserType (String userType)
	    {
	        this.userType = userType;
	    }

	    @Override
	    public String toString()
	    {
	        return "ValidateToken [permCodes = "+permCodes+", externalUserId = "+externalUserId+", expiration = "+expiration+", userId = "+userId+", userName = "+userName+", deviceIdentifier = "+deviceIdentifier+", roleIds = "+roleIds+", userType = "+userType+"]";
	    }
}
