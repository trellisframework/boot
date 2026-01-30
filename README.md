<p align="center">
  <img src="_includes/logo.svg" alt="Trellis Framework" width="300"/>
</p>

<p align="center">
  A comprehensive Java framework for building enterprise microservices with consistent architectural patterns.
</p>

<p align="center">
  <a href="https://openjdk.org/projects/jdk/21/"><img src="https://img.shields.io/badge/Java-21-orange.svg" alt="Java"/></a>
  <a href="https://spring.io/projects/spring-boot"><img src="https://img.shields.io/badge/Spring%20Boot-4.x-green.svg" alt="Spring Boot"/></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License"/></a>
</p>

---

## Table of Contents

- [Overview](#overview)
- [Getting Started](#getting-started)
- [Architecture](#architecture)
- [Core Patterns](#core-patterns)
  - [Action Pattern](#action-pattern)
  - [Task Pattern](#task-pattern)
  - [RepositoryTask Pattern](#repositorytask-pattern)
  - [Repository Pattern](#repository-pattern)
- [Temporal Workflows](#temporal-workflows)
- [Entry Points](#entry-points)
- [Data Layer](#data-layer)
- [Validation](#validation)
- [Configuration](#configuration)
- [Error Handling](#error-handling)
- [Modules](#modules)

---

## Overview

Trellis Framework provides a standardized architecture for building scalable microservices. Key features:

- **Action/Task Pattern** - Clean separation between use cases and atomic operations
- **Stateless Design** - All components are stateless for thread-safety
- **Multi-Database Support** - JPA, MongoDB, Elasticsearch
- **Temporal Integration** - Durable workflows for complex business processes
- **Built-in Validation** - FluentValidator for request validation
- **Caching & Messaging** - Redis, Kafka, RabbitMQ support

---

## Getting Started

### Maven Setup

```xml
<parent>
    <groupId>net.trellisframework</groupId>
    <artifactId>boot</artifactId>
    <version>LATEST</version>
</parent>

<dependencies>
    <dependency>
        <groupId>net.trellisframework</groupId>
        <artifactId>context</artifactId>
    </dependency>
    <dependency>
        <groupId>net.trellisframework</groupId>
        <artifactId>data-sql</artifactId>
    </dependency>
    <dependency>
        <groupId>net.trellisframework</groupId>
        <artifactId>http</artifactId>
    </dependency>
</dependencies>
```

### Project Structure

```
project-root/
├── pom.xml
├── libs/                                # Shared Libraries
│   ├── lib-general/                     # Configuration & utilities
│   ├── lib-sql/                         # Database abstraction
│   └── lib-{domain}/                    # Domain-specific library
│
└── apps/
    └── {service-name}/                  # Microservice Application
        └── com.example.{service}
            ├── user/                    # Feature module
            │   ├── action/
            │   ├── task/
            │   ├── repository/
            │   ├── model/
            │   ├── payload/
            │   ├── constant/
            │   ├── job/
            │   └── api/
            └── common/                  # Shared utilities
```

### Module Structure

Each feature module follows this structure:

```
user/
├── action/                              # Use case handlers
│   ├── AddUserAction.java
│   ├── ReadUserAction.java
│   ├── UpdateUserAction.java
│   ├── DeleteUserAction.java
│   └── ProcessUserAction.java
│
├── task/                                # Atomic operations
│   ├── SaveUserTask.java
│   ├── FindUserByIdTask.java
│   ├── FindUserByEmailTask.java
│   └── SendWelcomeEmailTask.java
│
├── repository/                          # Data access
│   └── UserRepository.java
│
├── model/                               # JPA entities
│   └── UserEntity.java
│
├── payload/                             # Request/Response DTOs
│   ├── AddUserRequest.java
│   ├── ReadUserRequest.java
│   ├── BrowseUserRequest.java
│   └── User.java
│
├── constant/                            # Module constants
│   ├── Config.java
│   ├── Constant.java
│   └── Messages.java
│
├── job/                                 # Scheduled jobs
│   ├── ProcessUserJob.java
│   └── StuckUserJob.java
│
└── api/                                 # Entry points
    ├── rest/
    │   └── UserUMController.java
    ├── grpc/
    │   └── UserGMController.java
    └── event/
        └── UserEventHandler.java
```

---

## Architecture

### Standard Architecture

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                              ENTRY POINTS                                    │
│     REST Controller    │    gRPC Controller    │    Event Handler    │ Job   │
└─────────────────────────────────────┬────────────────────────────────────────┘
                                      │
┌─────────────────────────────────────▼────────────────────────────────────────┐
│                           ORCHESTRATOR LAYER                                 │
│  ┌─────────────────────────────┐    ┌─────────────────────────────────────┐  │
│  │          Action             │    │         WorkflowAction              │  │
│  │  - Synchronous execution    │    │  - Temporal Workflow (durable)      │  │
│  │  - @Service annotation      │    │  - @Workflow annotation             │  │
│  └──────────────┬──────────────┘    └──────────────┬──────────────────────┘  │
│                 │                                  │                         │
│                 │                                  │                         │
│                 │                                  │                         │
│                 ▼                                  ▼                         │
├──────────────────────────────────────────────────────────────────────────────┤
│                              TASK LAYER                                      │
│  ┌───────────────────┐  ┌───────────────────┐  ┌───────────────────────────┐ │
│  │       Task        │  │   RepositoryTask  │  │  WorkflowTask /           │ │
│  │   - @Service      │  │   - @Service      │  │  WorkflowRepositoryTask   │ │
│  │   - Atomic ops    │  │   - DB operations │  │  - @Activity annotation   │ │
│  └───────────────────┘  └─────────┬─────────┘  └─────────────┬─────────────┘ │
│                                   │                          │               │
└───────────────────────────────────┼──────────────────────────┼───────────────┘
                                    │                          │
┌───────────────────────────────────▼──────────────────────────▼───────────────┐
│                           DATA ACCESS LAYER                                  │
│                        GenericJpaRepository                                  │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Callable Rules

| Component | Can Call | Cannot Call |
|-----------|----------|-------------|
| **Action** | Actions, Tasks, RepositoryTasks | WorkflowTasks, WorkflowActions |
| **WorkflowAction** | WorkflowTasks, WorkflowRepositoryTasks, child WorkflowActions | Actions, Tasks, RepositoryTasks |
| **Task** | Nothing (atomic) | Everything |
| **RepositoryTask** | Repository methods only | Actions, Tasks |
| **WorkflowTask** | Nothing (atomic) | Everything |
| **WorkflowRepositoryTask** | Repository methods only | Actions, Tasks |
| **FluentValidator** | Actions, Tasks, RepositoryTasks | WorkflowTasks, WorkflowActions |
| **Controller/Job** | Actions, Tasks, RepositoryTasks, WorkflowActions | WorkflowTasks |

### When to Use Each Pattern

| Pattern | Use Case |
|---------|----------|
| **Action** | Simple CRUD, synchronous operations, fast response |
| **WorkflowAction** | Long-running processes, retries needed, async pipelines |
| **Job** | Periodic maintenance, batch cleanup, scheduled tasks |

---

## Core Patterns

### Critical Rules

All Action, Task, RepositoryTask, WorkflowAction, and WorkflowTask classes must follow these rules:

1. **NO instance variables (fields)** - Must be completely stateless
2. **NO additional methods** - Only the `execute()` method is allowed
3. **NO `@Autowired` fields** - Use `call()` method for dependencies
4. **NO helper/utility methods** - Extract to separate utility classes

**Why?** Trellis manages these classes as stateless singletons. Adding state causes thread-safety issues.

```java
// WRONG
@Service
public class ProcessUserAction implements Action1<User, ProcessUserRequest> {
    private String tempValue;           // FORBIDDEN - instance variable

    @Autowired
    private SomeService service;        // FORBIDDEN - autowired field

    private String helper() { ... }     // FORBIDDEN - additional method
}

// CORRECT
@Service
public class ProcessUserAction implements Action1<User, ProcessUserRequest> {
    @Override
    public User execute(ProcessUserRequest request) {
        String value = call(CalculateValueTask.class, request);
        return call(CreateUserTask.class, value);
    }
}
```

---

### Action Pattern

Actions represent **Use Cases** - each Action handles one complete business operation.

**Interfaces:** `Action0`, `Action1<R, P>`, `Action2<R, P1, P2>` ... up to `Action5`

**Naming Convention:**
- `Add{Entity}Action` - Create operations
- `Read{Entity}Action` - Single read
- `Find{Entity}By{Criteria}Action` - Search
- `Update{Entity}Action` - Update
- `Delete{Entity}Action` - Delete
- `Process{Entity}Action` - Complex processing
- `Browse{Entity}Action` - List/paginated

```java
@Service
public class AddUserAction implements Action1<User, AddUserRequest> {

    @Override
    public User execute(AddUserRequest request) {
        UserEntity entity = UserEntity.of(request);
        entity = call(SaveUserTask.class, entity);
        call(SendWelcomeEmailTask.class, entity);
        return plainToClass(entity, User.class);
    }
}
```

**Action Composition:**

```java
@Override
public User execute(AddUserRequest request) {
    // Call another action
    UserProfile profile = call(GetDefaultProfileAction.class);

    // Call a task
    UserEntity entity = call(SaveUserTask.class, UserEntity.of(request, profile));

    // Async call
    CompletableFuture.runAsync(() -> call(SendNotificationAction.class, entity));

    return plainToClass(entity, User.class);
}
```

---

### Task Pattern

Tasks are **single-purpose operations** - each Task does exactly ONE thing.

**Interfaces:** `Task0`, `Task1<R, P>`, `Task2<R, P1, P2>` ... up to `Task5`

**Naming Convention:**
- `Notify{Event}Task` - Notifications
- `Calculate{Value}Task` - Computations
- `Transform{Data}Task` - Data transformation
- `Validate{Entity}Task` - Validation
- `Send{Message}Task` - External communication

```java
@Service
public class SendWelcomeEmailTask implements Task1<Void, UserEntity> {

    @Override
    public Void execute(UserEntity user) {
        emailClient.send(
            user.getEmail(),
            "Welcome!",
            "Hello " + user.getName()
        );
        return null;
    }
}
```

**Tasks cannot call anything - they are atomic and isolated.**

---

### RepositoryTask Pattern

RepositoryTasks are **single database operations** with access to ONE repository.

**Interface:** `RepositoryTask1<Repository, ReturnType, InputType>`

**Naming Convention:**
- `Save{Entity}Task` - Save single entity
- `SaveAll{Entity}Task` - Save multiple entities
- `Find{Entity}By{Criteria}Task` - Find operations
- `Update{Entity}By{Criteria}Task` - Updates
- `Delete{Entity}Task` - Delete
- `Count{Entity}By{Criteria}Task` - Count
- `Exists{Entity}By{Criteria}Task` - Existence check

```java
@Service
public class SaveUserTask implements RepositoryTask1<UserRepository, UserEntity, UserEntity> {

    @Override
    public UserEntity execute(UserEntity entity) {
        return getRepository().save(entity);
    }
}
```

```java
@Service
public class FindUserByIdTask implements RepositoryTask1<UserRepository, Optional<UserEntity>, String> {

    @Override
    public Optional<UserEntity> execute(String id) {
        return getRepository().findById(id);
    }
}
```

**Notes:**
- RepositoryTasks can only call repository methods via `getRepository()`
- Default transaction: `@Transactional(propagation = REQUIRES_NEW, rollbackFor = Exception.class)`
- Only add `@Transactional` annotation if you need different behavior

---

### Repository Pattern

Repositories extend `GenericJpaRepository` with QueryDSL support.

```java
@Repository
public interface UserRepository extends GenericJpaRepository<UserEntity, String> {

    Optional<UserEntity> findByEmail(String email);

    default List<UserEntity> findPendingUsers(int limit) {
        QUserEntity entity = QUserEntity.userEntity;
        return getFactory().selectFrom(entity)
            .where(entity.state.eq(State.PENDING))
            .orderBy(entity.created.asc())
            .limit(limit)
            .fetch();
    }

    default List<UserEntity> findUsersForProcessing(int limit) {
        QUserEntity entity = QUserEntity.userEntity;
        return getFactory().selectFrom(entity)
            .where(entity.state.eq(State.PENDING))
            .orderBy(entity.priority.desc(), entity.created.asc())
            .limit(limit)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .setHint("jakarta.persistence.lock.timeout", -2)
            .fetch();
    }
}
```

**Lock Timeout Values:**
- `-2` = NO_WAIT (immediate fail if locked)
- `-1` = Wait indefinitely
- `n` = Wait n milliseconds

---

## Temporal Workflows

For long-running or complex business processes, use Temporal integration.

### WorkflowAction

Equivalent to a Temporal Workflow - orchestrates the entire business process.

```java
@Async
@Workflow(executionTimeout = "2h", version = "1.0.0")
public class ProcessOrderWorkflowAction implements WorkflowAction1<Order, ProcessOrderRequest> {

    @Override
    public Order execute(ProcessOrderRequest request) {
        // Step 1: Validate
        Boolean isValid = call(ValidateOrderTask.class, request);
        if (!Boolean.TRUE.equals(isValid)) {
            return Order.failed(request.getRefId(), Messages.ORDER_INVALID);
        }

        // Step 2: Reserve inventory
        Boolean reserved = call(ReserveInventoryTask.class, request.getItems());
        if (!Boolean.TRUE.equals(reserved)) {
            return Order.failed(request.getRefId(), Messages.INVENTORY_UNAVAILABLE);
        }

        // Step 3: Process payment
        PaymentResult payment = call(ProcessPaymentTask.class, request.getPayment());
        if (!payment.isSuccess()) {
            call(ReleaseInventoryTask.class, request.getItems());
            return Order.failed(request.getRefId(), Messages.PAYMENT_FAILED);
        }

        // Step 4: Save order
        OrderEntity entity = call(SaveOrderTask.class, OrderEntity.of(request, payment));

        // Step 5: Async notification
        Optional.ofNullable(request.getWebhookUrl())
            .filter(StringUtils::isNotBlank)
            .ifPresent(url -> callAsync(NotifyWebhookWorkflowAction.class, WebhookRequest.of(url, entity)));

        return plainToClass(entity, Order.class);
    }
}
```

**@Workflow Annotation:**

| Attribute | Default | Description |
|-----------|---------|-------------|
| `taskQueue` | "" | Temporal task queue name |
| `executionTimeout` | "" | Maximum workflow execution time |
| `runTimeout` | "" | Maximum single run time |
| `taskTimeout` | "" | Maximum workflow task time |
| `version` | "0.0.0" | Workflow version for versioning |

**@Async Annotation:** Add for non-blocking execution. API returns immediately with workflow ID.

---

### WorkflowTask

Equivalent to a Temporal Activity - executes a single atomic operation.

```java
@Activity(
    retry = @Retry(maxAttempts = 3, backoff = @Backoff(delay = 1000, maxDelay = 30000, multiplier = 2.0)),
    startToCloseTimeout = "30s"
)
public class ValidateOrderTask implements WorkflowTask1<Boolean, ProcessOrderRequest> {

    @Override
    public Boolean execute(ProcessOrderRequest request) {
        return Optional.ofNullable(request)
            .filter(r -> StringUtils.isNotBlank(r.getCustomerId()))
            .filter(r -> r.getItems() != null && !r.getItems().isEmpty())
            .filter(r -> r.getPayment() != null)
            .isPresent();
    }
}
```

**@Activity Annotation:**

| Attribute | Default | Description |
|-----------|---------|-------------|
| `startToCloseTimeout` | "60s" | Max time from activity start to completion |
| `scheduleToStartTimeout` | "" | Max time from schedule to start |
| `scheduleToCloseTimeout` | "" | Max time from schedule to completion |
| `heartbeat` | "10s" | Heartbeat interval for long-running activities |
| `retry` | @Retry | Retry configuration |

**@Retry Annotation:**

| Attribute | Default | Description |
|-----------|---------|-------------|
| `maxAttempts` | 1 | Maximum retry attempts |
| `backoff` | @Backoff | Backoff configuration |
| `include` | {} | Exception types to retry |
| `exclude` | {} | Exception types to not retry |

**@Backoff Annotation:**

| Attribute | Default | Description |
|-----------|---------|-------------|
| `delay` | 1000 | Initial delay (ms) |
| `maxDelay` | 60000 | Maximum delay (ms) |
| `multiplier` | 2.0 | Exponential multiplier |

---

### WorkflowRepositoryTask

Temporal Activity with database access.

```java
@Activity
public class SaveOrderTask implements WorkflowRepositoryTask1<OrderRepository, OrderEntity, OrderEntity> {

    @Override
    public OrderEntity execute(OrderEntity entity) {
        return getRepository().save(entity);
    }
}
```

**Interface Parameter Order:** `WorkflowRepositoryTask1<Repository, ReturnType, InputType>`

**Note:** Default transaction is `@Transactional(propagation = REQUIRES_NEW, rollbackFor = Exception.class)`

---

### Workflow Helper Methods

| Method | Description |
|--------|-------------|
| `call(Task.class, args)` | Execute activity synchronously |
| `call(WorkflowAction.class, args, WorkflowOption)` | Execute child workflow with options |
| `callAsync(WorkflowAction.class, args)` | Start child workflow asynchronously |
| `callAsync(WorkflowAction.class, args, WorkflowOption)` | Start child workflow with options |
| `sleep(Duration)` | Temporal-safe sleep |
| `sleepMinutes(long)` | Sleep for minutes |
| `getWorkflowId()` | Get workflow execution ID |
| `getAttempt()` | Get current retry attempt |
| `version(changeId, maxVersion)` | Version-based branching |

---

### WorkflowOption

Configure child workflow execution with `WorkflowOption`:

```java
@Workflow(executionTimeout = "60m")
public class AddVerificationWorkflowAction implements WorkflowAction1<Verification, AddVerificationRequest> {

    @Override
    public Verification execute(AddVerificationRequest request) {
        VerificationEntity entity = call(SaveVerificationTask.class, VerificationEntity.of(request));
        
        // Start child workflow with custom ID
        callAsync(ProcessEmailVerificationWorkflowAction.class, entity,
            WorkflowOption.of("process-" + entity.getId()));
        
        return plainToClass(entity, Verification.class);
    }
}
```

**WorkflowOption:**

| Factory Method | Description |
|----------------|-------------|
| `WorkflowOption.of(String id)` | Set workflow ID |
| `WorkflowOption.of(int priority)` | Set priority (1-5) |
| `WorkflowOption.of(String id, int priority)` | Set ID and priority |

---

## Entry Points

### REST Controller

```java
@RestController
@RequestMapping(value = "/um/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class UserUMController implements Api {

    @PreAuthorize("hasAnyRole('UsrUR', 'UsrUM')")
    @GetMapping("/{id}")
    public ResponseEntity<User> read(@PathVariable String id) {
        return ResponseEntity.ok(call(ReadUserAction.class, id));
    }

    @PreAuthorize("hasAnyRole('UsrUA', 'UsrUM')")
    @PostMapping
    public ResponseEntity<User> add(@Validated @RequestBody AddUserRequest request) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(call(AddUserAction.class, request));
    }
}
```

**With Temporal Workflow:**

```java
@RestController
@RequestMapping(value = "/um/orders", produces = MediaType.APPLICATION_JSON_VALUE)
public class OrderUMController implements Api, Workflow {

    @PostMapping
    public ResponseEntity<Order> create(@Validated @RequestBody CreateOrderRequest request) {
        OrderEntity entity = call(SaveOrderTask.class, OrderEntity.of(request));

        callAsync(ProcessOrderWorkflowAction.class, ProcessOrderRequest.of(entity.getId()));

        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(plainToClass(entity, Order.class));
    }
}
```

**Controller Naming:**
- `{Entity}UMController` - **U**ser **M**anagement (personal endpoints)
- `{Entity}SMController` - **S**ystem **M**anagement (admin endpoints)
- `{Entity}GMController` - **G**lobal **M**anagement (gRPC)

**URL Convention:**
- `/um/{resource}` - User Management
- `/sm/{resource}` - System Management
- `/v1/{resource}` - Public API (versioned)

**Role Naming:** `{Domain}{Scope}{Permission}`
- Domain: `Usr`, `Ord`, etc.
- Scope: `U` (User), `S` (System), `G` (Global)
- Permission: `R` (Read), `A` (Add), `E` (Edit), `D` (Delete), `M` (Manage)

Example: `UsrUR` = User-scope User Read

---

### gRPC Controller

```java
@GrpcController
public class UserGMController extends UserServiceGrpc.UserServiceImplBase implements Api {

    @Override
    public void getUser(GetUserRequest request, StreamObserver<UserResponse> responseObserver) {
        User user = call(GetUserByIdAction.class, request.getId());
        responseObserver.onNext(toProto(user));
        responseObserver.onCompleted();
    }
}
```

---

### Event Handler

```java
@Component
public class UserEventHandler implements GenericEventController {

    @CloudFunctionEventHandler(topic = "user-created")
    public void onUserCreated(UserCreatedEvent event) {
        call(ProcessNewUserAction.class, event);
    }
}
```

---

### Scheduled Job

```java
@Component
public class ProcessUsersJob extends Job {

    @DistributedLock(value = "PROCESS_USERS_JOB", skipIfLocked = true, cooldown = "20s")
    @Scheduled(fixedDelay = 1000)
    public void execute() {
        try {
            call(ProcessPendingUsersAction.class);
        } catch (Throwable t) {
            Logger.error("ProcessUsersJob", t.getMessage());
        }
    }
}
```

**Job Naming:**
- `Process{Entity}Job` - Main processing
- `Stuck{Entity}Job` - Recovery job

---

## Data Layer

### Entity Pattern

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@Entity
@Table(name = "T_USER",
    indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_state", columnList = "state")
    })
public class UserEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "name")
    private String name;

    @Column(name = "state", nullable = false)
    @Convert(converter = Transformers.StateToInteger.class)
    private State state = State.PENDING;

    @Column(name = "priority")
    private Integer priority;

    @Embedded
    private Address address;

    @Embedded
    private Retry retry;
}
```

**Rules:**
- Extend `BaseEntity` (provides id, created, modified, version)
- Table names: `T_{ENTITY_NAME}` in uppercase
- Use `@AllArgsConstructor(staticName = "of")` for factory methods
- Use `@Convert` for enum persistence
- Define indexes for frequently queried columns

---

### Enum Converter Pattern

```java
@Getter
@AllArgsConstructor
public enum State implements IEnumerated<Integer> {
    PENDING(0),
    IN_PROGRESS(1),
    DONE(2),
    FAILED(3),
    CANCELLED(4);

    private final Integer db;
}

public class Transformers {
    @Converter
    public static class StateToInteger extends GenericEnumConverter<State, Integer> {
        public StateToInteger() {
            super(State.class);
        }
    }
}
```

---

### State Machine

```
PENDING → IN_PROGRESS → DONE
                     ↘ FAILED
                     ↘ CANCELLED
         ↓
       QUEUED (for retry)
```

| State | Description |
|-------|-------------|
| `PENDING` | Waiting to be processed |
| `QUEUED` | Temporarily delayed |
| `IN_PROGRESS` | Currently processing |
| `DONE` | Successfully completed |
| `FAILED` | Failed after max retries |
| `CANCELLED` | Cancelled by system/user |

---

## Validation

### FluentValidator Pattern

Use annotations for **static rules** and `FluentValidator` for **dynamic rules** that require database lookups, external validation, or complex business logic.

```java
@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class AddUserRequest implements Payload, FluentValidator<AddUserRequest> {

    @Required                    // Static rule - use annotation
    @Email                       // Static rule - use annotation
    private String email;

    @Required
    private String name;

    private String refId;
    private Customer customer;

    @Override
    public void execute() {
        // Dynamic rule: Check if email already exists in database (RepositoryTask)
        addRule(
            x -> call(ExistsUserByEmailTask.class, x.getEmail()),
            () -> new ConflictException(Messages.EMAIL_ALREADY_EXISTS)
        )
        // Dynamic rule: Validate API key (Task)
        .addRule(
            x -> !call(ValidateApiKeyTask.class, x.getApiKey()),
            () -> new UnauthorizedException(Messages.INVALID_API_KEY)
        )
        // Dynamic rule: Check quota (Action)
        .addRule(
            x -> !call(CheckQuotaAction.class, x.getCustomer().getId()),
            () -> new ForbiddenException(Messages.QUOTA_EXCEEDED)
        )
        // Auto-populate: Set default values
        .addRule(
            x -> StringUtils.isBlank(x.getRefId()),
            x -> x.setRefId(UUID.randomUUID().toString())
        )
        // Auto-populate: Set customer from JWT token
        .addRule(x -> x.setCustomer(Customer.of(
            OAuthSecurityContext.getPrincipalId(),
            OAuthSecurityContext.getEmail()
        )));
    }
}

// Note: In FluentValidator you can only call Action, Task, and RepositoryTask (not WorkflowTask)
```

**When to Use Each:**

| Validation Type | Approach |
|-----------------|----------|
| Required fields, format, length | Use annotations (`@Required`, `@Email`, `@Size`) |
| Database lookups (duplicate check) | Use `FluentValidator` with `call()` |
| External API validation | Use `FluentValidator` with `call()` |
| Business rules with conditions | Use `FluentValidator` |
| Auto-populate default values | Use `FluentValidator` with setter |

**addRule Patterns:**

| Pattern | Usage |
|---------|-------|
| `addRule(condition, () -> new Exception())` | Throw exception if condition is true |
| `addRule(condition, x -> x.setField(value))` | Set field if condition is true |
| `addRule(x -> x.setField(computed))` | Always set field (auto-populate) |

---

### Payload Naming

- `Add{Entity}Request` - Create request
- `Read{Entity}Request` - Read request
- `Browse{Entity}Request` - List/search request
- `Update{Entity}Request` - Update request
- `{Entity}` - Response DTO

**Never use "Workflow" in payload names.**

---

## Configuration

### Config Pattern

```java
@Getter
@AllArgsConstructor
public enum Config implements IConfig {
    MAX_RETRY("user.max-retry", "3"),
    BATCH_SIZE("user.batch-size", "100"),
    TIMEOUT("user.timeout", "30");

    private final String property;
    private final String defaultValue;

    @Override
    public String getProperty() {
        return property;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }
}

// Usage
int maxRetry = Integer.parseInt(Config.MAX_RETRY.get());
```

---

## Error Handling

### Exception Classes

Always use Trellis HTTP exceptions:

| Exception | HTTP Status | Usage |
|-----------|-------------|-------|
| `BadRequestException` | 400 | Validation errors |
| `UnauthorizedException` | 401 | Authentication required |
| `ForbiddenException` | 403 | Access denied |
| `NotFoundException` | 404 | Resource not found |
| `ConflictException` | 409 | Duplicate resource |
| `InternalServerException` | 500 | Server errors |

---

### Messages Pattern

**Never hardcode messages.** Create a Messages enum:

```java
public enum Messages implements MessageHandler {
    USER_NOT_FOUND,
    EMAIL_ALREADY_EXISTS,
    EMAIL_IS_REQUIRED,
    INVALID_EMAIL_FORMAT,
    MAX_RETRY_EXCEEDED
}
```

**Usage:**

```java
// In Action
return call(FindUserByIdTask.class, id)
    .orElseThrow(() -> new NotFoundException(Messages.USER_NOT_FOUND));

// In Validator
addRule(
    x -> StringUtils.isBlank(x.getEmail()),
    () -> new BadRequestException(Messages.EMAIL_IS_REQUIRED)
);

// WRONG - Never hardcode
throw new NotFoundException("User not found");
```

---

## Modules

| Module | Description |
|--------|-------------|
| `core` | Base utilities, logging, constants |
| `context` | Action/Task/Process framework |
| `http` | REST client, HTTP exceptions |
| `data-sql` | JPA/Hibernate with QueryDSL |
| `data-mongo` | MongoDB support |
| `data-elastic` | Elasticsearch support |
| `cache-redis` | Redis caching |
| `cache-caffeine` | In-memory caching |
| `stream-kafka` | Kafka messaging |
| `stream-rabbit` | RabbitMQ messaging |
| `oauth-resource-keycloak` | Keycloak OAuth2 |
| `workflow-temporal` | Temporal workflows |
| `validator` | Custom validators |
| `util` | AWS, Crypto, Export, JWT |
| `message-mail` | Email services |
| `socket-websocket` | WebSocket support |

---

## Technology Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 4.x |
| Cloud | Spring Cloud 2024.x |
| Language | Java 21 |
| Build | Maven 3 |
| Database | PostgreSQL, Oracle |
| Cache | Redis |
| ORM | JPA + Hibernate + QueryDSL |
| Workflow | Temporal |
| Container | Docker (JIB Plugin) |

---

## Deployment

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | 80 | API port |
| `ACTUATOR_PORT` | 8080 | Management port |
| `APPLICATION_MODE` | production | Profile |
| `CONFIG_SERVER_URI` | http://config | Config Server |

### Docker Build

```bash
mvn package -P production
```

---

## Code Style

1. **Use Lombok** for boilerplate reduction
2. **Use `Optional`** for nullable returns - never return null
3. **Use `@AllArgsConstructor(staticName = "of")`** for factory methods
4. **Chain with Optional** for null-safe operations
5. **Use early returns** to reduce nesting
6. **No comments in code** - code should be self-explanatory

```java
Optional.ofNullable(entity.getStatus())
    .filter(Status::isActive)
    .ifPresent(x -> processActive(entity));
```

---

## Logging

```java
Logger.info("User created: {}", user.getId());
Logger.error("Failed to process user", exception);

// Performance logging
Logger.info(
    () -> heavyOperation(),
    (time, result) -> time > 1000,
    (time, result) -> String.format("Operation took %d ms", time)
);
```

---

## Caching

Use `@CacheableConfig` to configure cache behavior per method:

```java
@CacheableConfig(value = "USER_CACHE", ttl = "1h", serializer = CacheSerializer.JSON)
@Cacheable(cacheNames = "USER_CACHE", key = "#email")
public User findByEmail(String email) { ... }
```

**@CacheableConfig Annotation:**

| Attribute | Default | Description |
|-----------|---------|-------------|
| `value` | {} | Cache names |
| `ttl` | "" | Time-to-live (e.g., "1h", "30m", "1d") |
| `serializer` | JDK | Serialization: `JDK`, `BYTE_ARRAY`, `STRING`, `JSON` |

---

## Distributed Locking

```java
@DistributedLock(value = "PROCESS_USERS", skipIfLocked = true, cooldown = "20s")
@Scheduled(fixedDelay = 1000)
public void execute() { ... }
```

---

## License

MIT License

## Contributing

Contributions are welcome! Please read our contributing guidelines.

## Support

- GitHub Issues: [Report a bug](https://github.com/trellisframework/boot/issues)
