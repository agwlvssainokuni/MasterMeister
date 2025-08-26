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

package cherry.mastermeister.service;

import cherry.mastermeister.entity.PermissionTemplateEntity;
import cherry.mastermeister.entity.PermissionTemplateItemEntity;
import cherry.mastermeister.model.PermissionTemplate;
import cherry.mastermeister.model.PermissionTemplateItem;
import cherry.mastermeister.repository.PermissionTemplateItemRepository;
import cherry.mastermeister.repository.PermissionTemplateRepository;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PermissionTemplateService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PermissionTemplateRepository templateRepository;
    private final PermissionTemplateItemRepository itemRepository;

    public PermissionTemplateService(
            PermissionTemplateRepository templateRepository,
            PermissionTemplateItemRepository itemRepository
    ) {
        this.templateRepository = templateRepository;
        this.itemRepository = itemRepository;
    }

    /**
     * Create new permission template
     */
    public PermissionTemplate createTemplate(String name, String description, Long connectionId,
                                           List<PermissionTemplateItem> items) {
        logger.info("Creating permission template: {} for connection: {}", name, connectionId);

        // Check if template name already exists
        if (templateRepository.existsByNameAndConnectionIdAndIsActiveTrue(name, connectionId)) {
            throw new RuntimeException("Template with name '" + name + "' already exists for this connection");
        }

        PermissionTemplateEntity entity = new PermissionTemplateEntity();
        entity.setName(name);
        entity.setDescription(description);
        entity.setConnectionId(connectionId);
        entity.setCreatedBy(getCurrentUserEmail());
        entity.setIsActive(true);

        PermissionTemplateEntity savedTemplate = templateRepository.save(entity);

        // Create template items
        List<PermissionTemplateItemEntity> itemEntities = items.stream()
                .map(item -> toItemEntity(item, savedTemplate))
                .toList();
        
        List<PermissionTemplateItemEntity> savedItems = itemRepository.saveAll(itemEntities);
        savedTemplate.setItems(savedItems);

        logger.info("Created permission template with ID: {} and {} items", savedTemplate.getId(), savedItems.size());
        return toModel(savedTemplate);
    }

    /**
     * Update permission template
     */
    public Optional<PermissionTemplate> updateTemplate(Long templateId, String name, String description,
                                                      List<PermissionTemplateItem> items) {
        logger.info("Updating permission template ID: {}", templateId);

        Optional<PermissionTemplateEntity> existing = templateRepository.findById(templateId);
        if (existing.isEmpty()) {
            logger.warn("Permission template not found: {}", templateId);
            return Optional.empty();
        }

        PermissionTemplateEntity entity = existing.get();
        entity.setName(name);
        entity.setDescription(description);
        entity.setUpdatedBy(getCurrentUserEmail());

        // Remove existing items
        itemRepository.deleteByTemplateId(templateId);

        // Create new items
        List<PermissionTemplateItemEntity> itemEntities = items.stream()
                .map(item -> toItemEntity(item, entity))
                .toList();
        
        List<PermissionTemplateItemEntity> savedItems = itemRepository.saveAll(itemEntities);
        entity.setItems(savedItems);

        PermissionTemplateEntity updated = templateRepository.save(entity);
        logger.info("Updated permission template with {} items", savedItems.size());
        
        return Optional.of(toModel(updated));
    }

    /**
     * Get permission template by ID
     */
    @Transactional(readOnly = true)
    public Optional<PermissionTemplate> getTemplate(Long templateId) {
        logger.debug("Retrieving permission template ID: {}", templateId);

        return templateRepository.findById(templateId)
                .map(this::toModel);
    }

    /**
     * Get active templates for connection
     */
    @Transactional(readOnly = true)
    public List<PermissionTemplate> getActiveTemplatesForConnection(Long connectionId) {
        logger.debug("Retrieving active templates for connection: {}", connectionId);

        return templateRepository.findByConnectionIdAndIsActiveTrueOrderByNameAsc(connectionId)
                .stream()
                .map(this::toModel)
                .toList();
    }

    /**
     * Get all templates for connection (including inactive)
     */
    @Transactional(readOnly = true)
    public List<PermissionTemplate> getAllTemplatesForConnection(Long connectionId) {
        logger.debug("Retrieving all templates for connection: {}", connectionId);

        return templateRepository.findByConnectionIdWithItemsOrderByCreatedAtDesc(connectionId)
                .stream()
                .map(this::toModel)
                .toList();
    }

    /**
     * Deactivate template
     */
    public boolean deactivateTemplate(Long templateId) {
        logger.info("Deactivating permission template ID: {}", templateId);

        Optional<PermissionTemplateEntity> template = templateRepository.findById(templateId);
        if (template.isPresent()) {
            PermissionTemplateEntity entity = template.get();
            entity.setIsActive(false);
            entity.setUpdatedBy(getCurrentUserEmail());
            templateRepository.save(entity);
            
            logger.info("Template deactivated successfully");
            return true;
        }

        logger.warn("Template not found for deactivation: {}", templateId);
        return false;
    }

    /**
     * Delete template and all items
     */
    public boolean deleteTemplate(Long templateId) {
        logger.info("Deleting permission template ID: {}", templateId);

        if (templateRepository.existsById(templateId)) {
            templateRepository.deleteById(templateId);
            logger.info("Template deleted successfully");
            return true;
        }

        logger.warn("Template not found for deletion: {}", templateId);
        return false;
    }

    /**
     * Get current user email from security context
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }
        return "system";
    }

    /**
     * Convert entity to model
     */
    private PermissionTemplate toModel(PermissionTemplateEntity entity) {
        // Force Hibernate to initialize the collection
        Hibernate.initialize(entity.getItems());
        
        List<PermissionTemplateItemEntity> itemEntities = entity.getItems();
        if (itemEntities == null) {
            itemEntities = new ArrayList<>();
        }
        
        List<PermissionTemplateItem> items = itemEntities.stream()
                .map(this::toItemModel)
                .toList();

        return new PermissionTemplate(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getConnectionId(),
                entity.getIsActive(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt(),
                items
        );
    }

    /**
     * Convert item entity to model
     */
    private PermissionTemplateItem toItemModel(PermissionTemplateItemEntity entity) {
        return new PermissionTemplateItem(
                entity.getId(),
                entity.getTemplate().getId(),
                entity.getScope(),
                entity.getPermissionType(),
                entity.getSchemaName(),
                entity.getTableName(),
                entity.getColumnName(),
                entity.getGranted(),
                entity.getComment()
        );
    }

    /**
     * Convert item model to entity
     */
    private PermissionTemplateItemEntity toItemEntity(PermissionTemplateItem model, PermissionTemplateEntity template) {
        PermissionTemplateItemEntity entity = new PermissionTemplateItemEntity();
        entity.setTemplate(template);
        entity.setScope(model.scope());
        entity.setPermissionType(model.permissionType());
        entity.setSchemaName(model.schemaName());
        entity.setTableName(model.tableName());
        entity.setColumnName(model.columnName());
        entity.setGranted(model.granted());
        entity.setComment(model.comment());
        return entity;
    }
}