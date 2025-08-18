/*
 * Copyright 2025 agwlvssainokuni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cherry.mastermeister.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;
    private final List<String> allowedMethods;
    private final List<String> allowedHeaders;
    private final boolean allowCredentials;
    private final String mappingPath;

    public WebConfig(
            @Value("${mm.cors.allowed-origins}") List<String> allowedOrigins,
            @Value("${mm.cors.allowed-methods}") List<String> allowedMethods,
            @Value("${mm.cors.allowed-headers}") List<String> allowedHeaders,
            @Value("${mm.cors.allow-credentials}") boolean allowCredentials,
            @Value("${mm.cors.mapping-path}") String mappingPath
    ) {
        this.allowedOrigins = allowedOrigins;
        this.allowedMethods = allowedMethods;
        this.allowedHeaders = allowedHeaders;
        this.allowCredentials = allowCredentials;
        this.mappingPath = mappingPath;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(mappingPath)
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods(allowedMethods.toArray(String[]::new))
                .allowedHeaders(allowedHeaders.toArray(String[]::new))
                .allowCredentials(allowCredentials);
    }
}