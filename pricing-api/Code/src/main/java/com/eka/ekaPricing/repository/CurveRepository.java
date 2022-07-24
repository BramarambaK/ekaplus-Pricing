package com.eka.ekaPricing.repository;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.eka.ekaPricing.util.ContextProvider;

@Component
public class CurveRepository {
	@Autowired
	ContextProvider contextProvider;

	/** The mongo template. */
	@Autowired
	MongoTemplate mongoTemplate;
	
	@Value("${eka.pricing.udid}")
	private String pricingUDID;
	
	private String resovleCollectionName() {
		return contextProvider.getCurrentContext().getTenantID() + "_Data";
	}
	/**
	 * Add Or Update Base Curve names.
	 *
	 * @param id
	 *            the id
	 * @param data
	 *            the data
	 * @param name
	 *            the name
	 * @param appName
	 *            the app name
	 * @return the object
	 */
	public Object upsertBaseCurves(String id,Map<String, Object> data, String objName, String appName) {
		Query query = new Query(Criteria.where("_id").is(id));

		if (!data.containsKey("object")) {
			data.put("object", objName);
		}
		if (!data.containsKey("app")) {
			data.put("app", appName);
		}
		data.put("refTypeId", pricingUDID);
		Update update = new Update();
		Iterator<Entry<String, Object>> it = data.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Object> pair = it.next();
			update.set((String) pair.getKey(), pair.getValue());
		}
		mongoTemplate.upsert(query, update, resovleCollectionName());
		return update.getUpdateObject();
	}
	
}
