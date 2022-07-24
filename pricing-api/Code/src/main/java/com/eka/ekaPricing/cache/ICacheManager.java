package com.eka.ekaPricing.cache;

import java.util.Map;

import com.eka.ekaPricing.exception.PricingException;

import redis.clients.jedis.exceptions.JedisException;

public interface ICacheManager {
	/**
	 * Usage : The <i>evict</i> flushes all the keys from jedis and closes the connection.
	 * @throws JedisException 
	 * @throws PricingException 
	 */
	public void evict() throws PricingException, JedisException;
	/**
	 * Usage : The <i>addInCache</i> adds content to the cache for the duration of the ttl(time to live) param.
	 * @param resultMap	The content to be stored in cache againt a key.
	 * @param cacheKey	The key against which content to be stored in the cache.
	 * @param ttl		The time to live parameter to be passed in seconds. Once the ttl expires
	 * 					content stored in the cache against the key will be cleared.
	 * @return
	 * @throws PricingException 
	 */
	public boolean addInCache(Map<String, Object> resultMap, String cacheKey,String ttl) throws PricingException;
	/**
	 * Usage : The <i>retrieveFromCache</i> retrieves content from the cache based on the cache key.
	 * @param cacheKey	The cacheKey against which content may or maynot be stored
	 * @param ttl		The time to live parameter for the content
	 * @return
	 * @throws PricingException 
	 */
	public Map<String, Object> retrieveFromCache( String cacheKey,String ttl) throws PricingException;
	 
	public  void setIgniteCacheName(String cacheName);
	public Map<String, Object> joinFromCache( String cacheKey,String ttl);
		
	
}