# Functional requirements (BDD)

## Feature: Manage student news

### Scenario: Create a draft news item
**Given** the API is running and the database is available  
**When** a client sends `POST /api/news` with valid `title` and `content`  
**Then** the API returns `201 Created`  
**And** the response contains a generated `id`  
**And** the created item has status `DRAFT`

### Scenario: Validation fails for empty fields
**Given** the API is running  
**When** a client sends `POST /api/news` with blank `title` and/or `content`  
**Then** the API returns `400 Bad Request`  
**And** no new news item is stored

### Scenario: List all news
**Given** there are one or more news items in the database  
**When** a client sends `GET /api/news`  
**Then** the API returns `200 OK`  
**And** returns all news items

### Scenario: Filter news by status
**Given** there are news items with statuses `DRAFT`, `PUBLISHED`, and `ARCHIVED`  
**When** a client sends `GET /api/news?status=PUBLISHED`  
**Then** the API returns `200 OK`  
**And** every returned item has status `PUBLISHED`

### Scenario: Edit existing news
**Given** a news item exists with id `{id}`  
**When** a client sends `PUT /api/news/{id}` with a new valid `title` and `content`  
**Then** the API returns `200 OK`  
**And** the news item is updated with new values

### Scenario: Edit non-existing news
**Given** no news item exists with id `{id}`  
**When** a client sends `PUT /api/news/{id}`  
**Then** the API returns `404 Not Found`

### Scenario: Publish news
**Given** a news item exists with id `{id}`  
**When** a client sends `PATCH /api/news/{id}/publish`  
**Then** the API returns `200 OK`  
**And** the item's status becomes `PUBLISHED`

### Scenario: Archive news
**Given** a news item exists with id `{id}`  
**When** a client sends `PATCH /api/news/{id}/archive`  
**Then** the API returns `200 OK`  
**And** the item's status becomes `ARCHIVED`

### Scenario: Delete news
**Given** a news item exists with id `{id}`  
**When** a client sends `DELETE /api/news/{id}`  
**Then** the API returns `204 No Content`  
**And** the item is removed from the database

### Scenario: Delete non-existing news
**Given** no news item exists with id `{id}`  
**When** a client sends `DELETE /api/news/{id}`  
**Then** the API returns `404 Not Found`

## Feature: API documentation availability

### Scenario: OpenAPI JSON is exposed
**Given** the backend is running  
**When** a client sends `GET /v3/api-docs`  
**Then** the API returns `200 OK`  
**And** the response is a valid OpenAPI document

### Scenario: Swagger UI is available
**Given** the backend is running  
**When** a client opens `/swagger-ui/index.html`  
**Then** the API documentation UI is displayed
