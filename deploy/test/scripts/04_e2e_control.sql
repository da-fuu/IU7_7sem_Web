BEGIN;

CREATE SCHEMA IF NOT EXISTS lab2_test;

CREATE TABLE IF NOT EXISTS lab2_test.e2e_runs (
    run_id VARCHAR(32) PRIMARY KEY CHECK (run_id ~ '^[0-9a-f]{32}$'),
    started_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS lab2_test.e2e_users (
    run_id VARCHAR(32) NOT NULL REFERENCES lab2_test.e2e_runs(run_id) ON DELETE CASCADE,
    user_id VARCHAR(36) PRIMARY KEY REFERENCES public.users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS e2e_users_run_id_idx ON lab2_test.e2e_users(run_id);

CREATE OR REPLACE FUNCTION lab2_test.track_e2e_user() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    owner_run VARCHAR(32);
BEGIN
    IF NEW.login ~ '^lab2e2e_[0-9a-f]{32}_[0-9]{1,4}$' THEN
        owner_run := split_part(NEW.login, '_', 2);
        PERFORM pg_advisory_xact_lock_shared(hashtextextended('lab2-e2e-data:' || owner_run, 0));
        INSERT INTO lab2_test.e2e_users (run_id, user_id)
        SELECT run_id, NEW.id FROM lab2_test.e2e_runs WHERE run_id = owner_run;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'E2E run is not active';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS lab2_track_e2e_user ON public.users;
CREATE TRIGGER lab2_track_e2e_user AFTER INSERT ON public.users
FOR EACH ROW EXECUTE FUNCTION lab2_test.track_e2e_user();

COMMIT;
