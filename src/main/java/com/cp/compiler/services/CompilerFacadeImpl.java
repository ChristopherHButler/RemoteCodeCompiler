package com.cp.compiler.services;

import com.cp.compiler.executions.Execution;
import com.cp.compiler.models.WellKnownUrls;
import com.cp.compiler.repositories.HooksRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * The type Compiler facade.
 */
@Slf4j
@Service
public class CompilerFacadeImpl implements CompilerFacade {
    
    private final CompilerService compilerService;
    
    private final HooksRepository hooksRepository;
    
    private final MeterRegistry meterRegistry;
    
    @Value("${compiler.features.push-notification.enabled}")
    private boolean isPushNotificationEnabled;
    
    private Counter shortRunningExecutionCounter;
    
    private Counter longRunningExecutionCounter;
    
    /**
     * Init.
     */
    @PostConstruct
    public void init() {
        shortRunningExecutionCounter = meterRegistry.counter("short-running-execution.counter");
        longRunningExecutionCounter = meterRegistry.counter("long-running-execution.counter");
    }
    
    /**
     * Instantiates a new Compiler facade.
     *
     * @param compilerService the compiler service
     * @param meterRegistry   the meter registry
     * @param hooksRepository    the hooks storage
     */
    public CompilerFacadeImpl(@Qualifier("proxy") CompilerService compilerService,
                              MeterRegistry meterRegistry,
                              HooksRepository hooksRepository) {
        this.compilerService = compilerService;
        this.meterRegistry = meterRegistry;
        this.hooksRepository = hooksRepository;
    }
    
    @Override
    public ResponseEntity compile(Execution execution, boolean isLongRunning, String url) throws Exception {
        if (isPushNotificationEnabled && isLongRunning) {
            // Long running execution (Push notification)
            longRunningExecutionCounter.increment();
            // Check if the url is valid
            if (!isUrlValid(url)) {
                return ResponseEntity
                        .badRequest()
                        .body("url " + url  + " not valid");
            }
            log.info("The execution is long running and the url is valid");
            hooksRepository.addUrl(execution.getImageName(), url);
        }
        // Short running execution (Long Polling)
        shortRunningExecutionCounter.increment();
        return compilerService.compile(execution);
    }
    
    private boolean isUrlValid(String url) {
        return url.matches(WellKnownUrls.URL_REGEX);
    }
}
