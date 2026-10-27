CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    login VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(256) NOT NULL,
    role VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS user_settings (
    user_id VARCHAR(36) PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    auto_stop_enabled BOOLEAN NOT NULL DEFAULT true,
    max_active_duration_value BIGINT NOT NULL DEFAULT 8,
    max_active_duration_unit VARCHAR(20) NOT NULL DEFAULT 'hours',
    min_duration_enabled BOOLEAN NOT NULL DEFAULT true,
    min_duration_value BIGINT NOT NULL DEFAULT 1,
    min_duration_unit VARCHAR(20) NOT NULL DEFAULT 'minutes'
);

CREATE TABLE IF NOT EXISTS categories (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    color_hex VARCHAR(7) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS time_records (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id VARCHAR(36) NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE,
    description VARCHAR(1000) NOT NULL DEFAULT ''
);

CREATE TABLE IF NOT EXISTS time_record_history (
    id VARCHAR(36) PRIMARY KEY,
    record_id VARCHAR(36) NOT NULL REFERENCES time_records(id) ON DELETE CASCADE,
    category_id VARCHAR(36) NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE,
    description VARCHAR(1000) NOT NULL DEFAULT '',
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_time_records_user ON time_records(user_id);
CREATE INDEX IF NOT EXISTS idx_time_records_active ON time_records(user_id) WHERE end_time IS NULL;
CREATE INDEX IF NOT EXISTS idx_categories_user ON categories(user_id);
CREATE INDEX IF NOT EXISTS idx_history_record ON time_record_history(record_id);
