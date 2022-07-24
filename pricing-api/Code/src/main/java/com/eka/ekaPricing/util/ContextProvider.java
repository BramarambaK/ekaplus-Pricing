package com.eka.ekaPricing.util;

import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.stereotype.Service;

import com.eka.ekaPricing.pojo.ContextInfo;

@Service
public class ContextProvider {

	/** The logger. */
	final static  Logger logger = ESAPI.getLogger(ContextProvider.class);
    
    /** The current tenant. */
    private ThreadLocal<ContextInfo> currentContext = new ThreadLocal<>();
    
    /**
     * Sets the current tenant.
     *
     * @param tenant the new current tenant
     */
    public  void setCurrentContext(ContextInfo tenant) {
        logger.debug(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Setting currentContext to " + currentContext));
        currentContext.set(tenant);
    }
    
    /**
     * Gets the current tenant.
     *
     * @return the current tenant
     */
    public  ContextInfo getCurrentContext() {
        return currentContext.get();
    }
    
    /**
     * remove.
     */
    public  void remove() {
    	currentContext.remove();
    }
}
