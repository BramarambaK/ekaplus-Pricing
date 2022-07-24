package com.eka.ekaPricing.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.eka.ekaPricing.pojo.ApplicationMeta;
import com.eka.ekaPricing.service.ApplicationMetaService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The Class ApplicationController.
 */
@CrossOrigin(origins = "http://localhost:4200")
@RestController	
@RequestMapping("/app")
public class ApplicationController {
	
	/** The application meta service. */
	@Autowired
	private ApplicationMetaService applicationMetaService;

	/**
	 * List meta.
	 *
	 * @return the flux
	 */
	@GetMapping
	public @ResponseBody Flux<ApplicationMeta> listMeta() {		
		return applicationMetaService.list();
	}
	
	/**
	 * Gets the meta.
	 *
	 * @param name the name
	 * @return the meta
	 */
	@GetMapping("/{name}")
	public @ResponseBody Mono<ApplicationMeta> getMeta(@PathVariable("name")String name) {		
		return applicationMetaService.get(name);
	}
	
	/**
	 * Save application meta.
	 *
	 * @param appMeta the app meta
	 * @return the mono
	 */
	@PostMapping
	public @ResponseBody Mono<ApplicationMeta> saveApplicationMeta(@RequestBody ApplicationMeta appMeta){		
		return applicationMetaService.save(appMeta);
	}
	
	/**
	 * Update application meta.
	 *
	 * @param appMeta the app meta
	 * @param id the id
	 * @return the mono
	 */
	@PutMapping("/{name}")
	public @ResponseBody Mono<ApplicationMeta> updateApplicationMeta(@RequestBody ApplicationMeta appMeta,@PathVariable("name") String name){		
		return applicationMetaService.update(appMeta,name);
	}
	
	/**
	 * Delete application meta.
	 *
	 * @param id the id
	 * @return the mono
	 */
	@DeleteMapping("/{name}")
	public @ResponseBody Mono<Void> deleteApplicationMeta(@PathVariable("name")String name){		
		return applicationMetaService.delete(name);
	}
}
