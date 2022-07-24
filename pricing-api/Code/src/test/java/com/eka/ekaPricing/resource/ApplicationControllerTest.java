package com.eka.ekaPricing.resource;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.eka.ekaPricing.pojo.ApplicationMeta;
import com.eka.ekaPricing.service.ApplicationMetaService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ApplicationControllerTest {
	
	@InjectMocks
	ApplicationController applicationController;
	
	@Mock
	public ApplicationMetaService applicationMetaService;
	
	@Test
	public void listMetaTest() {
		ApplicationMeta applicationMeta1 = new ApplicationMeta();
		applicationMeta1.setTitle("exp");
		applicationMeta1.setId("id");
		Flux<ApplicationMeta> expectedValue  = Flux.just(applicationMeta1);
		Mockito.when(applicationMetaService.list()).thenReturn(expectedValue);
		Flux<ApplicationMeta> actualValue = applicationController.listMeta();
		assertEquals(expectedValue,actualValue);
	}
	
	@Test
	public void getMetaTest() {
		ApplicationMeta applicationMeta1 = new ApplicationMeta();
		applicationMeta1.setTitle("exp");
		applicationMeta1.setId("id");
		String name = "valueDem";
		Mono<ApplicationMeta> expectedValue  = Mono.just(applicationMeta1);
		Mockito.when(applicationMetaService.get(name)).thenReturn(expectedValue);
		Mono<ApplicationMeta> actualValue = applicationController.getMeta(name);
		assertEquals(expectedValue,actualValue);	
	}
	@Test
	public void saveApplicationMetaTest() {
		ApplicationMeta applicationMeta1 = new ApplicationMeta();
		applicationMeta1.setTitle("exp");
		applicationMeta1.setId("id");
		ApplicationMeta appMeta = new ApplicationMeta();
		Mono<ApplicationMeta> expectedValue  = Mono.just(applicationMeta1);
		Mockito.when(applicationMetaService.save(appMeta)).thenReturn(expectedValue);
		Mono<ApplicationMeta> actualValue = applicationController.saveApplicationMeta(appMeta);
		assertEquals(expectedValue,actualValue);	
	}
	@Test
	public void updateApplicationMetaTest() {
		ApplicationMeta applicationMeta1 = new ApplicationMeta();
		applicationMeta1.setTitle("exp");
		applicationMeta1.setId("id");
		String name = "valueDem";
		ApplicationMeta appMeta = new ApplicationMeta();
		Mono<ApplicationMeta> expectedValue  = Mono.just(applicationMeta1);
		Mockito.when(applicationMetaService.update(appMeta,name)).thenReturn(expectedValue);
		Mono<ApplicationMeta> actualValue = applicationController.updateApplicationMeta(appMeta,name);
		assertEquals(expectedValue,actualValue);	
	}
	@Test
	public void deleteApplicationMetaTest() {
		ApplicationMeta applicationMeta1 = new ApplicationMeta();
		applicationMeta1.setTitle("exp");
		applicationMeta1.setId("id");
		String name = "valueDem";
		Mono<Void> expectedValue  = Mono.empty();
		Mockito.when(applicationMetaService.delete(name)).thenReturn(expectedValue);
		Mono<Void> actualValue = applicationController.deleteApplicationMeta(name);
		assertEquals(expectedValue,actualValue);
	}
}
