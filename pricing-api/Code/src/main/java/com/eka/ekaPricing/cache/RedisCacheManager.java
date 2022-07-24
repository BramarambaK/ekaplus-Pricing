package com.eka.ekaPricing.cache;

import java.util.LinkedHashMap;
import java.util.Map;

import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.eka.ekaPricing.exception.PricingException;
import com.eka.ekaPricing.pojo.PricingProperties;
import com.eka.ekaPricing.util.ContextProvider;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

@Component
public class RedisCacheManager implements ICacheManager{
	final static Logger logger = ESAPI.getLogger(RedisCacheManager.class);

	private static JedisPool pool = null;
	private static JedisPoolConfig jedisPoolConfig = null;
	private Jedis jedis = null;
	
	@Autowired
	ContextProvider context;

	/**
	 * Usage:The <i>getJedis</i> method creates pool of jedis and return it for use.
	 * 
	 * @return Jedis This method returns an instance of jedis from the pool.
	 * @throws PricingException 
	 */
	public Jedis getJedis() throws PricingException {
		PricingProperties pricingProps = context.getCurrentContext().getPricingProperties();
		logger.debug(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Method getJedis starts"));
		Jedis jedis = null;
		// configure our pool connection
		// Set timeout to 30 Min
		try {
			if (null == pool) {
				String ekaRedisHost = pricingProps.getEka_redis_host();
				String ekaRedisPort = pricingProps.getEka_redis_port();
				String ekaRedisTimeout = pricingProps.getEka_redis_timeout();
				String ekaRedisMaxPool = pricingProps.getEka_redis_pool_size();
				String ekaRedisPassword = pricingProps.getEka_redis_password();
				logger.debug(Logger.EVENT_SUCCESS,
						ESAPI.encoder().encodeForHTML("ekaRedisHost : " + ekaRedisHost + " ekaRedisPort:" + ekaRedisPort
								+ " ekaRedisTimeout:" + ekaRedisTimeout + " ekaRedisMaxPool:" + ekaRedisMaxPool));
				jedisPoolConfig = new JedisPoolConfig();
				jedisPoolConfig.setMaxTotal(Integer.parseInt(ekaRedisMaxPool));
				pool = new JedisPool(jedisPoolConfig, ekaRedisHost, Integer.parseInt(ekaRedisPort),
						Integer.parseInt(ekaRedisTimeout), ekaRedisPassword);
			}
			jedis = pool.getResource();
		} catch (NumberFormatException e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("Exception while calling getJedis   " + e.getLocalizedMessage()));
			closeRedisResource(jedis);
			throw new PricingException("Exception in  getJedis method :" + e.getLocalizedMessage());
		} catch (JedisConnectionException e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("Exception while callinggetJedis   " + e.getLocalizedMessage()));
			closeRedisResource(jedis);
			throw new PricingException("Exception in  getJedis method :" + e.getLocalizedMessage());
		} catch (JedisException e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("Exception while calling getJedis   " + e.getLocalizedMessage()));
			closeRedisResource(jedis);
			throw new PricingException("Exception in  getJedis method:" + e.getLocalizedMessage());
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("Exception while calling getJedis   " + e.getLocalizedMessage()));
			closeRedisResource(jedis);
			throw new PricingException("Exception in  getJedis method :" + e.getLocalizedMessage());
		}
		logger.debug(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Method getJedis ends"));
		return jedis;
	}

	/**
	 * Usage : The <i>closeRedisResource</i> method closes an instance of jedis.
	 * 
	 * @param jedis An instance of jedis to be closed and returned to pool.
	 * @throws PricingException 
	 */
	public void closeRedisResource(Jedis jedis) throws PricingException {
		logger.debug(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Method closeRedisResource starts"));
		try {
			if (null != jedis)
				jedis.close();
			jedis = null;
		} catch (JedisException e) {
			logger.error(Logger.EVENT_FAILURE, ESAPI.encoder()
					.encodeForHTML("Exception while calling closeRedisResource   " + e.getLocalizedMessage()));
			throw new PricingException("Exception in  closeRedisResource method:" + e.getLocalizedMessage());
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE, ESAPI.encoder()
					.encodeForHTML("Exception while calling closeRedisResource   " + e.getLocalizedMessage()));
			throw new PricingException("Exception in  closeRedisResource method:" + e.getLocalizedMessage());
		}
		logger.debug(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Method closeRedisResource ends"));
	}

	/*
	 * Usage : The <i>evict</i> flushes all the keys from jedis and closes the
	 * connection. (non-Javadoc)
	 * 
	 * @see com.eka.ekaconnect.cache.ICacheManager#evict()
	 */
	@Override
	public void evict() throws PricingException, JedisException {
		logger.debug(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Method evict starts"));
		try {
			Jedis jedis = getJedis();
			jedis.flushAll();
		} catch (JedisException e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("Exception while calling evict   " + e.getLocalizedMessage()));
			throw new PricingException("Exception in  evict method:" + e.getLocalizedMessage());
		} catch (PricingException e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("Exception while calling evict   " + e.getLocalizedMessage()));
			throw new PricingException("Exception in  evict method:" + e.getLocalizedMessage());
		}
		finally {
			closeRedisResource(jedis);
		}
		
		logger.debug(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Method evict ends"));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.eka.ekaconnect.cache.ICacheManager#addInCache(java.util.Map,
	 * java.lang.String, java.lang.String)
	 * 
	 * Usage : The <i>addInCache</i> adds content to the cache for the duration of
	 * the ttl(time to live) param.
	 */
	@Override
	public boolean addInCache(Map<String, Object> resultMap, String cacheKey, String ttl) throws PricingException {
		logger.debug(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Method addInCache starts"));
		// Check if cache is not enabled in properties
		String cacheEnabled = "Y";

		if (StringUtils.isEmpty(cacheEnabled) || cacheEnabled.equals("N")) {
			return false;
		}
		Jedis jedis = null;
		try {
			jedis = getJedis();
			jedis.getSet(cacheKey, new Gson().toJson(resultMap.get(cacheKey)));
			int tte = Integer.parseInt(ttl);
			if (tte > 0) {
				jedis.expire(cacheKey, tte);
			}
		} catch (JedisConnectionException e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("Exception while calling addInCache   " + e.getLocalizedMessage()));
			closeRedisResource(jedis);
			throw new PricingException("Exception in  addInCache method :" + e.getLocalizedMessage());
		} catch (JedisException e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("Exception while calling addInCache   " + e.getLocalizedMessage()));
			closeRedisResource(jedis);
			throw new PricingException("Exception in  addInCache method:" + e.getLocalizedMessage());
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("Exception while calling addInCache   " + e.getLocalizedMessage()));
			closeRedisResource(jedis);
			throw new PricingException("Exception in  addInCache method:" + e.getLocalizedMessage());
		} finally {
			closeRedisResource(jedis);
		}
		logger.debug(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Method addInCache ends"));
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.eka.ekaconnect.cache.ICacheManager#addInCache(java.util.Map,
	 * java.lang.String, java.lang.String)
	 * 
	 * Usage : The <i>retrieveFromCache</i> retrieves content from the cache based
	 * on the cache key.
	 */
	@Override
	public Map<String, Object> retrieveFromCache(String cacheKey, String ttl) throws PricingException {
		logger.debug(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Method retrieveFromCache starts"));
		Map<String, Object> resultMap = new LinkedHashMap<>();
		String cacheEnabled = "Y";
		// Check if cache is not enabled in properties
		if (StringUtils.isEmpty(cacheEnabled) || cacheEnabled.equals("N")) {
			return resultMap;
		}
		Jedis jedis = null;
		try {
			jedis = getJedis();
			if (jedis.exists(cacheKey)) {
				Object mapList = new Gson().fromJson(jedis.get(cacheKey), new TypeToken<Object>() {
				}.getType());
				resultMap.put(cacheKey, mapList);
			}
		} catch (JedisConnectionException e) {
			logger.error(Logger.EVENT_FAILURE, ESAPI.encoder()
					.encodeForHTML("Exception while calling retrieveFromCache   " + e.getLocalizedMessage()));
			closeRedisResource(jedis);
			throw new PricingException("Exception in  retrieveFromCache method :" + e.getLocalizedMessage());
		} catch (JedisException e) {
			logger.error(Logger.EVENT_FAILURE, ESAPI.encoder()
					.encodeForHTML("Exception while calling retrieveFromCache   " + e.getLocalizedMessage()));
			closeRedisResource(jedis);
			throw new PricingException("Exception in  retrieveFromCache method:" + e.getLocalizedMessage());
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE, ESAPI.encoder()
					.encodeForHTML("Exception while calling retrieveFromCache   " + e.getLocalizedMessage()));
			closeRedisResource(jedis);
			throw new PricingException("Exception in  retrieveFromCache method: " + e.getLocalizedMessage());
		} finally {
			closeRedisResource(jedis);
		}
		logger.debug(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Method retrieveFromCache ends"));
		return resultMap;
	}

	@Override
	public void setIgniteCacheName(String cacheName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, Object> joinFromCache(String cacheKey, String ttl) {
		// TODO Auto-generated method stub
		return null;
	}

}
