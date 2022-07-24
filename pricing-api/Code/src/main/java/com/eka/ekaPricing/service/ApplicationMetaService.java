package com.eka.ekaPricing.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.eka.ekaPricing.pojo.ApplicationMeta;
import com.eka.ekaPricing.repository.ApplicationMetaRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// TODO: Auto-generated Javadoc
/**
 * The Class MetaService.
 * Service Class which queries the repo to retrieve the Application Meta 
 */
@Service
public class ApplicationMetaService {
	
	/** The app meta repo. */
	@Autowired
	private ApplicationMetaRepository applicationMetaRepository;
	

	/**
	 * List.
	 *
	 * @return the flux
	 */
	public Flux<ApplicationMeta> list() {	 			
	return applicationMetaRepository.findAll();
	}

	/**
	 * Gets the.
	 *
	 * @param name the name
	 * @return the mono
	 */
	public Mono<ApplicationMeta> get(String name) {	 			
		return applicationMetaRepository.findByName(name);
	}
	
	/**
	 * Save.
	 *
	 * @param appMeta the app meta
	 * @return the mono
	 */
	public Mono<ApplicationMeta> save(ApplicationMeta appMeta) {		
		return applicationMetaRepository.insert(appMeta);
	}
	
	/**
	 * Delete.
	 *
	 * @param name the name
	 * @return the mono
	 */
	public  Mono<Void> delete(String name) {
		Mono<ApplicationMeta> meta=applicationMetaRepository.findByName(name);
		return applicationMetaRepository.deleteById(meta.block().getId());
	}
	
	/**
	 * Update.
	 *
	 * @param appMeta the app meta
	 * @param name the name
	 * @return the mono
	 */
	
	public Mono<ApplicationMeta> update(ApplicationMeta appMeta,String name) {	
		Mono<ApplicationMeta> meta=applicationMetaRepository.findByName(name);		
		appMeta.setId(meta.block().getId());
		return applicationMetaRepository.save(appMeta);
	}
}
