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

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    /*
     * Cache configuration is handled by application.properties:
     *
     * spring.cache.type=caffeine
     * spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=5m,recordStats
     * spring.cache.cache-names=tablePermissions,readPermissions,deletePermissions,readableColumns,writableColumns
     *
     * This class only provides @EnableCaching annotation to activate caching functionality.
     */
}
