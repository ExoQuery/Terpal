CREATE TABLE Person (
    id INT NOT NULL AUTO_INCREMENT,
    firstName VARCHAR(255),
    lastName VARCHAR(255),
    age INT,
    PRIMARY KEY (id)
);

CREATE TABLE Address (
    ownerId INT,
    street VARCHAR(255),
    zip INT
);

CREATE TABLE Product(
    description VARCHAR(255),
    id BIGINT NOT NULL AUTO_INCREMENT,
    sku BIGINT,
    PRIMARY KEY (id)
);

CREATE TABLE TimeEntity(
    sqlDate        DATE,          -- java.sql.Date
    sqlTime        TIME,          -- java.sql.Time
    sqlTimestamp   TIMESTAMP,     -- java.sql.Timestamp
    timeLocalDate      DATE,      -- java.time.LocalDate
    timeLocalTime      TIME,      -- java.time.LocalTime
    timeLocalDateTime  TIMESTAMP, -- java.time.LocalDateTime
    -- MySQL has no understanding of Date+Timezone or Time+Timezone
    -- The only thing you can do is use DATETIME which at least tries
    -- not to convert to/from UTC whenever it is written.
    -- More info here: https://dev.mysql.com/doc/refman/8.0/en/datetime.html
    timeZonedDateTime  DATETIME,  -- java.time.ZonedDateTime
    timeInstant        DATETIME,  -- java.time.Instant
    timeOffsetTime     TIME,      -- java.time.OffsetTime
    timeOffsetDateTime DATETIME   -- java.time.OffsetDateTime
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
    v9 DOUBLE,
    v10 VARBINARY(255),
    v11 DATETIME,
    v12 VARCHAR(255),
    v13 DATE,
    v14 VARCHAR(255),
    o1 VARCHAR(255),
    o2 DECIMAL(5,2),
    o3 BOOLEAN,
    o4 SMALLINT,
    o5 SMALLINT,
    o6 INTEGER,
    o7 BIGINT,
    o8 FLOAT,
    o9 DOUBLE,
    o10 VARBINARY(255),
    o11 DATETIME,
    o12 VARCHAR(255),
    o13 DATE,
    o14 VARCHAR(255)
);
