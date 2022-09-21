package org.assimbly.integrationrestdt;

import org.apache.camel.builder.ThreadPoolProfileBuilder;
import org.apache.camel.spi.Registry;
import org.apache.camel.util.concurrent.ThreadPoolRejectedPolicy;
import org.assimbly.dil.blocks.beans.CustomHttpBinding;
import org.assimbly.dil.blocks.beans.UuidExtensionFunction;
import org.assimbly.util.mail.ExtendedHeaderFilterStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.camel.*;
import world.dovetail.aggregate.AggregateStrategy;
import world.dovetail.cookies.CookieStore;
import world.dovetail.enrich.EnrichStrategy;
import world.dovetail.multipart.processor.MultipartProcessor;
import world.dovetail.throttling.QueueMessageChecker;
import world.dovetail.xmltojson.CustomXmlJsonDataFormat;


public class Dovetail {

	protected Logger log = LoggerFactory.getLogger(getClass());

	private CamelContext context;

	public Dovetail(Object context){
		this.context = (CamelContext) context;
	}

	public void setComponents() throws Exception {

		System.out.println("Set components");

		Registry registry = context.getRegistry();

		//Start Dovetail specific beans
		registry.bind("CurrentAggregateStrategy", new AggregateStrategy());
		registry.bind("CurrentEnrichStrategy", new EnrichStrategy());
		registry.bind("ExtendedHeaderFilterStrategy", new ExtendedHeaderFilterStrategy());
		registry.bind("flowCookieStore", new CookieStore());
		registry.bind("multipartProcessor", new MultipartProcessor());
		registry.bind("QueueMessageChecker", new QueueMessageChecker());
		//End Dovetail specific beans

		//Start Dovetail services
		context.addService(new CustomXmlJsonDataFormat());
		//End Dovetail services

		// Start Dovetail components
		context.addComponent("aleris", new world.dovetail.aleris.AlerisComponent());
		context.addComponent("amazon", new world.dovetail.amazon.AmazonComponent());
		// End Dovetail components

	}

	public void setThreadProfile(int poolSize, int maxPoolSize, int maxQueueSize) {

		ThreadPoolProfileBuilder builder = new ThreadPoolProfileBuilder("wiretapProfile");
		builder.poolSize(poolSize).maxPoolSize(maxPoolSize).maxQueueSize(maxQueueSize).rejectedPolicy(ThreadPoolRejectedPolicy.DiscardOldest).keepAliveTime(10L);
		context.getExecutorServiceManager().registerThreadPoolProfile(builder.build());

	}

}