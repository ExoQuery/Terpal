CREATE TABLE person (
    id INTEGER PRIMARY KEY,
    firstName VARCHAR(255),
    lastName VARCHAR(255),
    age INT
);

CREATE TABLE address (
    ownerId INT,
    street VARCHAR,
    zip INT
);

CREATE TABLE IF NOT EXISTS Product(
	id INTEGER PRIMARY KEY,
    description VARCHAR(255),
    sku BIGINT
);

-- Sqlite is a bit crazy, they don't have any actual date types:
-- https://www.sqlite.org/datatype3.html
CREATE TABLE TimeEntity(
    sqlDate        INTEGER,                     -- java.sql.Date
    sqlTime        INTEGER,                     -- java.sql.Time
    sqlTimestamp   INTEGER,                -- java.sql.Timestamp
    timeLocalDate      INTEGER,                     -- java.time.LocalDate
    timeLocalTime      INTEGER,                     -- java.time.LocalTime
    timeLocalDateTime  INTEGER,                -- java.time.LocalDateTime
    timeZonedDateTime  INTEGER, -- java.time.ZonedDateTime
    timeInstant        INTEGER, -- java.time.Instant
    timeOffsetTime     INTEGER,      -- java.time.OffsetTime
    timeOffsetDateTime INTEGER  -- java.time.OffsetDateTime
);

CREATE TABLE IF NOT EXISTS EncodingTestEntity(
    v1 VARCHAR(255),
    v2 DECIMAL(5,2),
    v3 BOOLEAN,
    v4 SMALLINT,
    v5 SMALLINT,
    v6 INTEGER,
    v7 BIGINT,
    v8 FLOAT,
    v9 DOUBLE PRECISIOn,
    v10 BLOB,
    v11 BIGINT,
    v12 VARCHAR(255),
    v13 BIGINT,
    v14 VARCHAR(36),
    o1 VARCHAR(255),
    o2 DECIMAL(5,2),
    o3 BOOLEAN,
    o4 SMALLINT,
    o5 SMALLINT,
    o6 INTEGER,
    o7 BIGINT,
    o8 FLOAT,
    o9 DOUBLE PRECISIOn,
    o10 BLOB,
    o11 BIGINT,
    o12 VARCHAR(255),
    o13 BIGINT,
    o14 VARCHAR(36)
);