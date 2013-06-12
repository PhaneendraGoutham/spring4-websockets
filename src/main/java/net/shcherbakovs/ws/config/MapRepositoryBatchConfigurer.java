/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.shcherbakovs.ws.config;

import javax.annotation.PostConstruct;

import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

public class MapRepositoryBatchConfigurer implements BatchConfigurer {

	private JobRepository jobRepository;
	private JobLauncher jobLauncher;
	private PlatformTransactionManager transactionManager;

	protected MapRepositoryBatchConfigurer() {}

	public JobRepository getJobRepository() {
		return jobRepository;
	}

	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	public JobLauncher getJobLauncher() {
		return jobLauncher;
	}

	@PostConstruct
	public void initialize() throws Exception {
		this.jobRepository = createJobRepository();
		this.jobLauncher = createJobLauncher();
	}

	private JobLauncher createJobLauncher() throws Exception {
		SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
		jobLauncher.setJobRepository(jobRepository);
		jobLauncher.afterPropertiesSet();
		return jobLauncher;
	}

	protected JobRepository createJobRepository() throws Exception {
		transactionManager = new ResourcelessTransactionManager();
		MapJobRepositoryFactoryBean factoryBean = new MapJobRepositoryFactoryBean(transactionManager);
		factoryBean.afterPropertiesSet();
		return factoryBean.getJobRepository();
	}

}
