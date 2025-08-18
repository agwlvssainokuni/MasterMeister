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

package cherry.mastermeister.controller;

import cherry.mastermeister.controller.dto.ApiResponse;
import cherry.mastermeister.controller.dto.HealthResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * ヘルスチェックAPI
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * ヘルスチェック
     *
     * @return ヘルスチェック結果
     */
    @GetMapping("/health")
    public ApiResponse<HealthResult> health() {
        HealthResult result = new HealthResult(
                "UP",
                LocalDateTime.now(),
                "MasterMeister"
        );
        return ApiResponse.success(result);
    }
}