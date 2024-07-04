CREATE TABLE Person (
    id INT IDENTITY(1,1) PRIMARY KEY,
    firstName VARCHAR(255),
    lastName VARCHAR(255),
    age INT
);

CREATE TABLE Address (
    ownerId INT,
    street VARCHAR(255),
    zip INT
);

CREATE TABLE Product(
    id INTEGER IDENTITY(1,1) PRIMARY KEY,
    description VARCHAR(255),
    sku BIGINT
);

CREATE TABLE TimeEntity(
    sqlDate        DATE,          -- java.sql.Date
    sqlTime        TIME,          -- java.sql.Time
    sqlTimestamp   DATETIME,     -- java.sql.Timestamp
    timeLocalDate      DATE,      -- java.time.LocalDate
    timeLocalTime      TIME,      -- java.time.LocalTime
    timeLocalDateTime  DATETIME, -- java.time.LocalDateTime
    -- DATETIMEOFFSET is SQL Server's equvalent of Postgres TIMESTAMP WITH TIME ZONE
    timeZonedDateTime  DATETIMEOFFSET, -- java.time.ZonedDateTime
    timeInstant        DATETIMEOFFSET, -- java.time.Instant
    -- There is no such thing as a Time+Timezone column in SQL Server
    timeOffsetTime     DATETIMEOFFSET,      -- java.time.OffsetTime
    timeOffsetDateTime DATETIMEOFFSET  -- java.time.OffsetDateTime
);

CREATE TABLE EncodingTestEntity(
    v1 VARCHAR(255),
    v2 DECIMAL(5,2),
    v3 BIT,
    v4 SMALLINT,
    v5 SMALLINT,
    v6 INTEGER,
    v7 BIGINT,
    v8 FLOAT,
    v9 DOUBLE PRECISION,
    v10 VARBINARY(MAX),
    v11 DATETIME,
    v12 VARCHAR(255),
    v13 DATE,
    v14 VARCHAR(255),
    o1 VARCHAR(255),
    o2 DECIMAL(5,2),
    o3 BIT,
    o4 SMALLINT,
    o5 SMALLINT,
    o6 INTEGER,
    o7 BIGINT,
    o8 FLOAT,
    o9 DOUBLE PRECISION,
    o10 VARBINARY(MAX),
    o11 DATETIME,
    o12 VARCHAR(255),
    o13 DATE,
    o14 VARCHAR(255)
);