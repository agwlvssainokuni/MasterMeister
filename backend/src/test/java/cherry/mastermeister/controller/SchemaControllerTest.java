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

import cherry.mastermeister.config.SecurityConfig;
import cherry.mastermeister.model.ColumnMetadata;
import cherry.mastermeister.model.SchemaMetadata;
import cherry.mastermeister.model.TableMetadata;
import cherry.mastermeister.service.SchemaUpdateService;
import cherry.mastermeister.service.UserDetailsServiceImpl;
import cherry.mastermeister.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SchemaController.class)
@Import({SecurityConfig.class, JwtUtil.class})
class SchemaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SchemaUpdateService schemaUpdateService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testReadSchema() throws Exception {
        // Setup test data
        ColumnMetadata column = new ColumnMetadata(
                "ID", "BIGINT", 19, null, false, null, "ID column",
                true, true, 1
        );

        TableMetadata table = new TableMetadata(
                "PUBLIC", "TEST_TABLE", "TABLE", "Test table", List.of(column)
        );

        SchemaMetadata schema = new SchemaMetadata(
                1L, "testdb", List.of("PUBLIC"), List.of(table), LocalDateTime.now()
        );

        when(schemaUpdateService.getSchema(1L, "user")).thenReturn(schema);

        // Execute and verify
        mockMvc.perform(get("/api/admin/schema/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.connectionId").value(1))
                .andExpect(jsonPath("$.data.databaseName").value("testdb"))
                .andExpect(jsonPath("$.data.schemas[0]").value("PUBLIC"))
                .andExpect(jsonPath("$.data.tables[0].schema").value("PUBLIC"))
                .andExpect(jsonPath("$.data.tables[0].tableName").value("TEST_TABLE"))
                .andExpect(jsonPath("$.data.tables[0].tableType").value("TABLE"))
                .andExpect(jsonPath("$.data.tables[0].columns[0].columnName").value("ID"))
                .andExpect(jsonPath("$.data.tables[0].columns[0].dataType").value("BIGINT"))
                .andExpect(jsonPath("$.data.tables[0].columns[0].primaryKey").value(true))
                .andExpect(jsonPath("$.data.tables[0].columns[0].autoIncrement").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testReadSchemaForbiddenForUser() throws Exception {
        mockMvc.perform(get("/api/admin/schema/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testReadSchemaUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/schema/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
