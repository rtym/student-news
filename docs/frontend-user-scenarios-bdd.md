# Frontend user scenarios (BDD)

## Feature: Manage student news through the web UI

### Scenario: Open the app and see existing news
**Given** the frontend and backend are running  
**When** a user opens `http://localhost:5173` in a browser  
**Then** the page shows the `Student News` title  
**And** a list of existing news items is displayed

### Scenario: Create a draft from the UI
**Given** a user is on the main page  
**When** the user enters a valid title and content in the create form  
**And** clicks `Create draft`  
**Then** a new news item appears in the list  
**And** the new item has status `DRAFT`

### Scenario: Prevent creating an empty news item
**Given** a user is on the main page  
**When** the user submits the form with empty title and/or content  
**Then** an error message is shown  
**And** no new news item appears in the list

### Scenario: Edit an existing news item
**Given** a news item is visible in the list  
**When** the user clicks `Edit` on that item  
**And** updates title/content  
**And** clicks `Save changes`  
**Then** the item displays updated text in the list

### Scenario: Cancel editing mode
**Given** a user clicked `Edit` for an item  
**When** the user clicks `Cancel edit`  
**Then** the form returns to create mode  
**And** no changes are applied to the item

### Scenario: Publish a news item from the UI
**Given** a news item with status `DRAFT` is displayed  
**When** the user clicks `Publish`  
**Then** the item status changes to `PUBLISHED`  
**And** the `Publish` button is no longer shown for that item

### Scenario: Archive a news item from the UI
**Given** a news item is displayed  
**When** the user clicks `Archive`  
**Then** the item status changes to `ARCHIVED`  
**And** the `Archive` button is no longer shown for that item

### Scenario: Delete a news item from the UI
**Given** a news item is displayed  
**When** the user clicks `Delete`  
**Then** the item is removed from the list

### Scenario: Filter list by status
**Given** the list contains items with different statuses  
**When** the user selects `Published` in the status filter  
**Then** only items with status `PUBLISHED` are visible

### Scenario: Return to all items
**Given** a status filter is active  
**When** the user selects `All` in the status filter  
**Then** all news items are visible again

### Scenario: Refresh data manually
**Given** a user is on the main page  
**When** the user clicks `Refresh`  
**Then** the list is reloaded from the backend  
**And** the latest data is displayed

### Scenario: Show loading state while fetching
**Given** the frontend is requesting data from the backend  
**When** the request is in progress  
**Then** the UI displays `Loading...`

### Scenario: Show user-friendly message on backend error
**Given** the backend is unavailable or returns an error  
**When** the user tries to load or modify news  
**Then** an error message is shown in the UI  
**And** the application remains usable for retry actions
