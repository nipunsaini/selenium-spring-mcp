package com.example.mcp;

import com.example.mcp.service.BrowserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringMcpServerApplication {

	private static final Logger logger = LoggerFactory.getLogger(SpringMcpServerApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(SpringMcpServerApplication.class, args);
		logger.info("Server started");
	}

	@Bean
	public ToolCallbackProvider getTools(BrowserService browserService) {
		return MethodToolCallbackProvider.builder().toolObjects(browserService).build();
	}

}
