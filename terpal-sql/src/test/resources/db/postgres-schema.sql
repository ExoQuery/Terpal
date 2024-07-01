CREATE TABLE person (
    id SERIAL PRIMARY KEY,
    firstName VARCHAR(255),
    lastName VARCHAR(255),
    age INT
);

CREATE TABLE address (
    ownerId INT,
    street VARCHAR,
    zip INT
);

CREATE TABLE Product(
    description VARCHAR(255),
    id SERIAL PRIMARY KEY,
    sku BIGINT
);

CREATE TABLE TimeEntity(
    sqlDate        DATE,                     -- java.sql.Date
    sqlTime        TIME,                     -- java.sql.Time
    sqlTimestamp   TIMESTAMP,                -- java.sql.Timestamp
    timeLocalDate      DATE,                     -- java.time.LocalDate
    timeLocalTime      TIME,                     -- java.time.LocalTime
    timeLocalDateTime  TIMESTAMP,                -- java.time.LocalDateTime
    timeZonedDateTime  TIMESTAMP WITH TIME ZONE, -- java.time.ZonedDateTime
    timeInstant        TIMESTAMP WITH TIME ZONE, -- java.time.Instant
    -- Postgres actually has a notion of a Time+Timezone type unlike most DBs
    timeOffsetTime     TIME WITH TIME ZONE,      -- java.time.OffsetTime
    timeOffsetDateTime TIMESTAMP WITH TIME ZONE  -- java.time.OffsetDateTime
);

CREATE TABLE EncodingTestEntity(
    v1 VARCHAR(255),
    v2 DECIMAL(5,2),
    v3 BOOLEAN,
    v4 SMALLINT,
    v5 SMALLINT,
    v6 INTEGER,
    v7 BIGINT,
    v8 FLOAT,
    v9 DOUBLE PRECISION,
    v10 BYTEA,
    v11 TIMESTAMP,
    v12 VARCHAR(255),
    v13 DATE,
    v14 UUID,
    o1 VARCHAR(255),
    o2 DECIMAL(5,2),
    o3 BOOLEAN,
    o4 SMALLINT,
    o5 SMALLINT,
    o6 INTEGER,
    o7 BIGINT,
    o8 FLOAT,
    o9 DOUBLE PRECISION,
    o10 BYTEA,
    o11 TIMESTAMP,
    o12 VARCHAR(255),
    o13 DATE,
    o14 UUID
);