package com.eka.ekaPricing.repository;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import com.eka.ekaPricing.pojo.ApplicationMeta;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The Interface AppObjectMetaRepository.
 */
@Repository
public interface ApplicationMetaRepository extends ReactiveMongoRepository<ApplicationMeta, String>  {

	/**
	 * Find by ApplicationName.
	 *
	 * @param name the name
	 * @return the mono
	 */
	@Query("{'name':?0}")
	  Mono<ApplicationMeta> findByName(String name);
	
	/**
	 * List All Application.
	 * @return the Flux
	 */
	  Flux<ApplicationMeta> findAll();
	  

}
