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

package cherry.mastermeister.controller.dto;

import cherry.mastermeister.model.DatabaseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record DatabaseConnectionRequest(
        @NotBlank(message = "Name is required")
        String name,
        
        @NotNull(message = "Database type is required")
        DatabaseType dbType,
        
        @NotBlank(message = "Host is required")
        String host,
        
        @NotNull(message = "Port is required")
        @Min(value = 1, message = "Port must be greater than 0")
        @Max(value = 65535, message = "Port must be less than 65536")
        Integer port,
        
        @NotBlank(message = "Database name is required")
        String databaseName,
        
        @NotBlank(message = "Username is required")
        String username,
        
        @NotBlank(message = "Password is required")
        String password,
        
        String connectionParams,
        
        boolean active
) {
}