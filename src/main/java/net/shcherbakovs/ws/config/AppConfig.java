package net.shcherbakovs.ws.config;

import java.util.Date;
import java.util.List;

import net.shcherbakovs.ws.domain.Quote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import reactor.R;
import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.fn.Event;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@ComponentScan(basePackages = { "net.shcherbakovs.ws.service" })
@EnableScheduling
@EnableBatchProcessing
public class AppConfig extends MapRepositoryBatchConfigurer {
	private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

	
	@Autowired
	private JobBuilderFactory jobBuilder;

	@Autowired
	private StepBuilderFactory stepBuilder;

	@Bean
	public Job importJob() {
		return jobBuilder.get("importJob").start(readFileStep()).build();
	}
	
	@Bean
	public Step readFileStep() {
		return stepBuilder.get("readFile").<Quote, Quote>chunk(byDateCompletionPoicy())
				.reader(flatFileReader()).writer(publisherWriter()).listener(byDateCompletionPoicy()).build();
	}

	@Bean
	public Reactor reactor() {
		return R.reactor().using(new Environment()).dispatcher(Environment.RING_BUFFER).get();
	}

//	@Autowired
//	private QuotesMessageSource publisher;
	
	@Bean
	public ItemWriter<Quote> publisherWriter() {
		return new ItemWriter<Quote>() {
			public void write(List<? extends Quote> items) throws Exception {
				Thread.sleep(1000L);
				log.trace("Publishing {} quotes", items.size());
				
				for(Quote quote : items) {
//					publisher.processMessage(quote);
					reactor().notify(quote.getSymbol(), Event.wrap(quote));
				}
			}
		};
	}

	public FlatFileItemReader<Quote> flatFileReader() {
		FlatFileItemReader<Quote> itemReader = new FlatFileItemReader<Quote>();
		DefaultLineMapper<Quote> lineMapper = new DefaultLineMapper<Quote>();
		DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
		tokenizer.setNames(new String[] {"symbol", "timestamp", "open", "high", "low", "close", "volume"});
		lineMapper.setLineTokenizer(tokenizer);
		BeanWrapperFieldSetMapper<Quote> fieldSetMapper = new BeanWrapperFieldSetMapper<Quote>();
		fieldSetMapper.setTargetType(Quote.class);
		lineMapper.setFieldSetMapper(fieldSetMapper);
		itemReader.setLineMapper(lineMapper);
		itemReader.setResource(new ClassPathResource("quotes/quotes.csv"));
		return itemReader;
	}
	
	@Bean
	public ByDateCompletionPolicy byDateCompletionPoicy() {
		return new ByDateCompletionPolicy();
	}

	static class ByDateCompletionPolicy implements CompletionPolicy, ItemReadListener<Quote> {
		private Date lastSeenDate = null;
		private boolean isComplete = false;
		
		public void afterRead(Quote item) {
			if( !item.getTimestamp().equals(lastSeenDate) ) {
				lastSeenDate = item.getTimestamp();
				isComplete = true;
			}
		}
		
		public RepeatContext start(RepeatContext context) {
			isComplete = false;
			return context;
		}

		public boolean isComplete(RepeatContext context, RepeatStatus result) {
			return (result == null || result == RepeatStatus.FINISHED || isComplete);
		}

		public boolean isComplete(RepeatContext context) {
			return isComplete;
		}

		public void update(RepeatContext context) {}
		public void beforeRead() {}
		public void onReadError(Exception ex) {}
	}

	@Bean
	public ObjectMapper objectMapper() {
		Jackson2ObjectMapperFactoryBean objectMapperFactoryBean = objectMapperFactoryBean();
		return (ObjectMapper) objectMapperFactoryBean.getObject();
	}

	@Bean
	public Jackson2ObjectMapperFactoryBean objectMapperFactoryBean() {
		Jackson2ObjectMapperFactoryBean objectMapperFactoryBean = new Jackson2ObjectMapperFactoryBean();
		objectMapperFactoryBean.setAutoDetectGettersSetters(true);
		objectMapperFactoryBean.setAutoDetectFields(true);
		objectMapperFactoryBean.afterPropertiesSet();
		return objectMapperFactoryBean;
	}
	
	@Autowired
	private JobLauncher jobLauncher; 
		
	@Scheduled(fixedDelay=1000L)
	public void onStartup() throws Exception {
		log.info("Starting main background import job");
		jobLauncher.run(importJob(), new JobParametersBuilder().addDate("time", new Date()).toJobParameters());
	}

}
