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

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "tablePermissions",
                "readPermissions",
                "deletePermissions",
                "readableColumns",
                "writableColumns"
        );

        // Configure Caffeine cache with TTL
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)                    // Maximum cache entries
                .expireAfterWrite(Duration.ofMinutes(5))  // TTL: 5 minutes
                .recordStats()                        // Enable cache statistics
        );

        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }
}
