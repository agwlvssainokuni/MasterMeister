# Data Access Feature - Detailed Requirements

This document outlines the detailed requirements for Data Access feature UX design and UI implementation.

## 📋 Screen Layout Overview

- **Left Pane**: DB/Table selection tree view (collapsible)
- **Right Pane**: Detail view based on selection
  - DB selected: Database connection info display
  - Table selected: Tabbed view with:
    - Record data display
    - Column definition display (types, defaults, constraints, etc.)

## 1. Permissions & Access Control

### Basic Policy
- **Leverage existing permission management system**
- **Tree View**: Display all DBs, schemas, and tables
- **Permission-based UI control**: Frontend restricts operations based on permissions

### Column-Level Permission Control
- **READ permission granted**: Display column content
- **READ permission denied**: Show permission denied marker (🔒 or ❌)
- **Even without permissions**: Column name, type, and constraints are visible

### Record Operation Permissions
| Operation | Required Conditions |
|-----------|-------------------|
| **CREATE** | At least one column with WRITE permission |
| **UPDATE** | Primary key READ permission + at least one column with WRITE permission |
| **DELETE** | Primary key READ permission + table-level DELETE permission |

### UI Placement
- **Create link**: Above record list
- **Edit/Delete links**: Left side of each record row (per-record basis)

### Form Permission Control
- **WRITE permission granted**: Input allowed
- **WRITE permission denied**:
  - Create: Use DEFAULT value
  - Update: No change
- **READ permission granted**: Display-only
- **READ & WRITE permission denied**: Show permission denied marker (🔒 or ❌)

## 2. Data Operations

### Operations to Implement
- ✅ **Single Record CRUD** (Create, Read, Update, Delete)
- 🔄 **Bulk Operations** (CSV/TSV upload)
- 📥 **Data Export** (CSV/TSV download)

### Create/Edit UI Approach
**Under consideration**: Modal editing vs Full-screen transition

**Modal Editing Benefits:**
- Preserves context
- Easy comparison with list
- Responsive operation feel

**Full-screen Benefits:**
- Better for many columns
- Complex validation display
- Mobile-friendly

### Delete Confirmation
- **Confirmation modal required**

### Implementation Timeline
1. **MVP**: Record display + action buttons (non-functional)
2. **Phase 2**: Complete record display with permission control
3. **Phase 3**: CRUD implementation
4. **Phase 4**: Bulk operations & export

### Bulk Operations
- **CSV/TSV Upload**: Create, update, delete operations
- **Template Download**: Header-only CSV/TSV files
- **Data Export**: All records matching current filter conditions

## 3. Data Display & Navigation

### Pagination
- **Required feature**
- **Page size**: Configurable options from settings file

### Sorting
- **Simple sort**: Click column header icons
- **Advanced sort**: Direct ORDER BY clause input (no permission check required)

### Filtering
- **UI approach**: Column selection + operator selection + value input + apply button
- **Target columns**: READ permission required
- **Logic operators**: Multiple conditions with AND/OR
- **Advanced filter**: Direct WHERE clause input (no permission check required)

## 4. Multi-DB Connection Management

### DB Connection Policy
- **Temporary connections**: Connect/disconnect per API call only
- **No persistence**: UI state and DB connections are independent
- **Schema info**: Leverage existing schema management system

## 5. UI State Management

### Basic Policy
- **Cross-session persistence**: Not required
- **Page reload restoration**: Not required

### Possible State Persistence Methods (Reference)
1. **localStorage**: Browser-local persistence
2. **sessionStorage**: Tab-session only
3. **URL state management**: Route parameters & query parameters
4. **Server state**: Save as user preferences in DB

## 6. Responsive Design

### Small Screen Support
- **Left pane**: Hide functionality required
- **Overlay display**: Slide-in side panel
  - Slides in from left
  - Background overlay
  - Close on outside tap
  - Mobile app-like UX

### Recommended Implementation
```
[☰] Data Access Screen
```
↓ Tap hamburger menu
```
[████████████] [×] ← Overlay + close button
[DB Tree     ] [  ]
[- Database1 ] [  ] 
[  - Table1  ] [  ]
[  - Table2  ] [  ]
[+ Database2 ] [  ]
[████████████] [  ]
```

## 7. Error & Exception Handling

### Notification Method
- **Unified**: Use existing Notification system

### Error Types & Responses
| Error Type | Timing | Response |
|------------|--------|----------|
| **DB Connection Error** | API call | Show notification, prompt retry |
| **Permission Denied** | Rare (invalid operations) | Show notification, simple message |
| **Timeout** | Almost never | Show notification, leave retry to user |

### Permission Control Philosophy
- **Preventive control**: Frontend restricts operations
- **Backend errors rare**: Indicates potential misuse

---

## 🤔 Considerations & Questions

### 1. Create/Edit UI Approach
**Question**: Modal editing vs Full-screen transition - which is preferred?

### 2. Implementation Timeline
**Question**: When should CRUD implementation begin?

### 3. Permission Denied Markers
**Proposal**: 🔒 (lock icon) or ❌ (X mark)

### 4. Overlay Display
**Explanation**: Mobile app-style side panel display method

---

## 💭 Recommendations

### 1. **Create/Edit UI Approach**
**Recommended: Modal editing**
- Comparison with list data is important for data access
- Modal suits administrative interface nature
- Responsive support possible (full-screen modal on small screens)

### 2. **Permission Markers**
**Proposal**: 
- 🔒 (READ denied)
- 👁️‍🗨️ (READ granted, WRITE denied)  
- ✏️ (WRITE granted)

### 3. **Implementation Phases**
**Recommended**:
1. **MVP**: Tree view + record display + empty action buttons
2. **Phase 2**: Complete record display with permission control
3. **Phase 3**: CRUD implementation
4. **Phase 4**: Bulk operations & export

### 4. **State Persistence Methods (Reference)**
- **Tree expand/collapse**: `localStorage` (UX improvement)
- **Selection state**: URL parameters (bookmarkable/shareable)
- **Display settings**: `localStorage` (page size, etc.)

---

*This document will be updated as implementation progresses*
