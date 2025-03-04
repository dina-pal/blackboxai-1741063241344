# BusDakho User Roles and Permissions Documentation

## 1. User Types and Roles

### 1.1 Passenger (End User)

**Description**: Regular users who use the application to track and travel on buses.

**Permissions**:

- View real-time bus locations
- View bus schedules and routes
- Search for buses and routes
- Save favorite routes and stops
- Receive notifications about bus arrivals/delays
- View bus details (type, capacity, features)
- Rate and review bus services
- Book tickets (if available)

**Data Model Fields**:

```json
{
  "id": "UUID",
  "role": "PASSENGER",
  "name": "string",
  "email": "string",
  "phone": "string",
  "preferences": {
    "favoriteRoutes": ["UUID"],
    "favoriteStops": ["UUID"],
    "notificationSettings": {
      "pushEnabled": "boolean",
      "emailEnabled": "boolean",
      "smsEnabled": "boolean"
    }
  },
  "travelHistory": ["UUID"] // References to past journeys
}
```

### 1.2 Bus Owner

**Description**: Owns one or multiple buses and manages their operation.

**Permissions**:

- Add/remove buses to their fleet
- Manage bus details and status
- Assign/remove drivers and staff to buses
- View performance metrics and reports
- Manage bus schedules and routes
- View revenue and analytics
- Receive operational notifications
- Manage bus maintenance schedules

**Data Model Fields**:

```json
{
  "id": "UUID",
  "role": "BUS_OWNER",
  "name": "string",
  "email": "string",
  "phone": "string",
  "companyDetails": {
    "companyName": "string",
    "registrationNumber": "string",
    "address": "object",
    "taxInfo": "object"
  },
  "ownedBuses": ["UUID"],
  "staff": ["UUID"], // References to all staff members
  "operatingLicense": {
    "number": "string",
    "validUntil": "date",
    "status": "string"
  }
}
```

### 1.3 Bus Driver

**Description**: Operates the bus and updates real-time status.

**Permissions**:

- Update bus location (through driver app)
- Start/end bus trips
- Report incidents/issues
- View assigned schedules and routes
- Update bus status (delays, breakdowns)
- View passenger count
- Receive route notifications
- Mark stops as completed

**Data Model Fields**:

```json
{
  "id": "UUID",
  "role": "DRIVER",
  "name": "string",
  "email": "string",
  "phone": "string",
  "licenseDetails": {
    "number": "string",
    "type": "string",
    "validUntil": "date",
    "status": "string"
  },
  "assignedBuses": ["UUID"],
  "currentBus": "UUID",
  "activeStatus": "boolean",
  "experience": "number", // in years
  "ratings": "number",
  "performanceMetrics": {
    "onTimePerformance": "number",
    "safetyScore": "number",
    "customerRating": "number"
  }
}
```

### 1.4 Bus Staff

**Description**: Additional staff members (conductors, cleaners, maintenance staff).

**Permissions**:

- View assigned bus details
- Update basic bus status
- Report issues
- View schedules
- Mark attendance
- View assigned tasks

**Data Model Fields**:

```json
{
  "id": "UUID",
  "role": "STAFF",
  "staffType": "CONDUCTOR | CLEANER | MAINTENANCE",
  "name": "string",
  "email": "string",
  "phone": "string",
  "assignedBuses": ["UUID"],
  "currentBus": "UUID",
  "activeStatus": "boolean",
  "schedule": {
    "shifts": ["object"],
    "workingDays": ["string"]
  }
}
```

## 2. Bus Assignment and Management

### 2.1 Bus-Staff Relationship

```json
{
  "id": "UUID",
  "busId": "UUID",
  "staffId": "UUID",
  "role": "DRIVER | CONDUCTOR | CLEANER | MAINTENANCE",
  "assignmentType": "PERMANENT | TEMPORARY",
  "startDate": "date",
  "endDate": "date",
  "shift": {
    "startTime": "time",
    "endTime": "time",
    "days": ["string"]
  },
  "status": "ACTIVE | INACTIVE",
  "assignedBy": "UUID" // Reference to bus owner
}
```

### 2.2 Bus Owner Management Rights

- Can add multiple drivers to a bus (for different shifts)
- Can assign staff members to buses
- Can set staff schedules and shifts
- Can view staff performance metrics
- Can manage staff permissions
- Can transfer staff between buses

### 2.3 Staff Assignment Rules

1. A bus can have multiple drivers (different shifts)
2. A driver can be assigned to multiple buses (not simultaneously)
3. Staff assignments must not have overlapping schedules
4. Each bus must have at least one driver assigned
5. Staff assignments require start date and optional end date
6. Emergency staff replacements must be logged
7. Staff performance metrics are tracked per bus assignment

## 3. Access Control Matrix

| Action              | Passenger | Bus Owner | Driver | Staff |
| ------------------- | --------- | --------- | ------ | ----- |
| View Bus Location   | ✓         | ✓         | ✓      | ✓     |
| Update Bus Location | ✗         | ✗         | ✓      | ✗     |
| View Schedule       | ✓         | ✓         | ✓      | ✓     |
| Modify Schedule     | ✗         | ✓         | ✗      | ✗     |
| Add/Remove Bus      | ✗         | ✓         | ✗      | ✗     |
| Assign Staff        | ✗         | ✓         | ✗      | ✗     |
| Report Issues       | ✓         | ✓         | ✓      | ✓     |
| View Analytics      | ✗         | ✓         | ✗      | ✗     |
| Update Trip Status  | ✗         | ✓         | ✓      | ✗     |
| View Revenue Data   | ✗         | ✓         | ✗      | ✗     |

## 4. Implementation Notes

### 4.1 Authentication

- JWT-based authentication
- Role-based access control (RBAC)
- Separate tokens for staff app and passenger app
- Token refresh mechanism
- Session management for staff

### 4.2 Security Considerations

- Staff location tracking only during shifts
- Encrypted communication for location updates
- Audit logs for all staff assignments
- Regular permission validation
- Secure staff data storage

### 4.3 Mobile Apps

1. Passenger App

   - Public access with optional account
   - Focus on viewing and tracking

2. Staff App
   - Secure login required
   - Role-specific features
   - Offline capability
   - Real-time updates

### 4.4 Notifications

- Role-based notification routing
- Priority levels for different user types
- Customizable notification preferences
- Emergency broadcast system
