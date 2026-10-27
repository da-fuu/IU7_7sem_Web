INSERT INTO categories (id, user_id, name, color_hex, is_default)
VALUES 
    ('11111111-1111-1111-1111-111111111111', NULL, 'Работа / Проекты', '#3B82F6', true),
    ('22222222-2222-2222-2222-222222222222', NULL, 'Учеба', '#10B981', true)
ON CONFLICT (id) DO NOTHING;
