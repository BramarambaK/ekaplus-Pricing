package com.eka.ekaPricing.service;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.eka.ekaPricing.pojo.ApplicationMeta;
import com.eka.ekaPricing.repository.ApplicationMetaRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ApplicationMetaServiceTest {

	@InjectMocks
	ApplicationMetaService applicationMetaService;

	@Mock
	public ApplicationMetaRepository applicationMetaRepository;

	@Test
	public void listTest() {
		ApplicationMeta applicationMeta1 = new ApplicationMeta();
		applicationMeta1.setTitle("exp");
		applicationMeta1.setId("id");
		Flux<ApplicationMeta> expectedValue = Flux.just(applicationMeta1);
		Mockito.when(applicationMetaRepository.findAll()).thenReturn(expectedValue);
		Flux<ApplicationMeta> actualValue = applicationMetaService.list();
		assertEquals(expectedValue, actualValue);
	}

	@Test
	public void getTest() {
		ApplicationMeta applicationMeta1 = new ApplicationMeta();
		applicationMeta1.setTitle("exp");
		applicationMeta1.setId("id");
		String name = "arbitary";
		Mono<ApplicationMeta> expectedValue = Mono.just(applicationMeta1);
		Mockito.when(applicationMetaRepository.findByName(name)).thenReturn(expectedValue);
		Mono<ApplicationMeta> actualValue = applicationMetaService.get(name);
		assertEquals(expectedValue, actualValue);
	}

	@Test
	public void saveTest() {
		ApplicationMeta applicationMeta1 = new ApplicationMeta();
		applicationMeta1.setTitle("exp");
		applicationMeta1.setId("id");
		ApplicationMeta appMeta = new ApplicationMeta();
		Mono<ApplicationMeta> expectedValue = Mono.just(applicationMeta1);
		Mockito.when(applicationMetaRepository.insert(appMeta)).thenReturn(expectedValue);
		Mono<ApplicationMeta> actualValue = applicationMetaService.save(appMeta);
		assertEquals(expectedValue, actualValue);
	}

	@Test
	public void deleteTest() {
		ApplicationMeta applicationMeta1 = new ApplicationMeta();
		applicationMeta1.setTitle("exp");
		applicationMeta1.setId("id");
		String name = "valueDem";
		Mono<ApplicationMeta> metaValue = Mono.just(applicationMeta1);
		Mono<Void> expectedValue = Mono.empty();
		Mockito.when(applicationMetaRepository.findByName(name)).thenReturn(metaValue);
		Mockito.when(applicationMetaRepository.deleteById(metaValue.block().getId())).thenReturn(expectedValue);
		Mono<Void> actualValue = applicationMetaService.delete(name);
		assertEquals(expectedValue, actualValue);
	}

	@Test
	public void updateTest() {
		ApplicationMeta applicationMeta1 = new ApplicationMeta();
		applicationMeta1.setTitle("exp");
		applicationMeta1.setId("id");
		String name = "valueDem";
		ApplicationMeta appMeta = new ApplicationMeta();
		Mono<ApplicationMeta> meta = Mono.just(applicationMeta1);
		Mono<ApplicationMeta> expectedValue = Mono.just(applicationMeta1);
		Mockito.when(applicationMetaRepository.findByName(name)).thenReturn(meta);
		appMeta.setId(meta.block().getId());
		Mockito.when(applicationMetaRepository.save(appMeta)).thenReturn(expectedValue);
		Mono<ApplicationMeta> actualValue = applicationMetaService.update(appMeta, name);
		assertEquals(expectedValue, actualValue);
	}
}
