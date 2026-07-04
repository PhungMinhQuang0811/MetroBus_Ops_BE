-- ═══════════════════════════════════════════════════════════════
-- SEED: OPERATORS
-- ═══════════════════════════════════════════════════════════════
INSERT INTO operators (operator_code, operator_name, status, created_by_account_id, created_at, updated_at) VALUES
                                                                                                                ('HMC',       'Công ty TNHH MTV Đường sắt Hà Nội (Hanoi Metro)', 'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                ('TRANSERCO', 'Tổng công ty Vận tải Hà Nội',                     'ACTIVE', 'system', NOW(), NOW());

-- ═══════════════════════════════════════════════════════════════
-- SEED: ROUTES
-- ═══════════════════════════════════════════════════════════════
INSERT INTO routes (operator_id, route_code, route_name, transport_type, status, created_by_account_id, created_at, updated_at) VALUES
                                                                                                                                    ((SELECT id FROM operators WHERE operator_code = 'HMC'),       'HN_2A',     'Cát Linh - Hà Đông',         'METRO', 'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                    ((SELECT id FROM operators WHERE operator_code = 'HMC'),       'HN_3_1',    'Nhổn - Ga Hà Nội',            'METRO', 'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                    ((SELECT id FROM operators WHERE operator_code = 'TRANSERCO'), 'HN_BRT_01', 'BRT 01: Yên Nghĩa - Kim Mã',  'BUS',   'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                    ((SELECT id FROM operators WHERE operator_code = 'TRANSERCO'), 'HN_BUS_32', 'Buýt 32: Giáp Bát - Nhổn',    'BUS',   'ACTIVE', 'system', NOW(), NOW());

-- ═══════════════════════════════════════════════════════════════
-- SEED: STATIONS — Tuyến 2A (Cát Linh - Hà Đông)
-- ═══════════════════════════════════════════════════════════════
INSERT INTO stations (route_id, station_code, station_name, distance, station_order, status, created_by_account_id, created_at, updated_at) VALUES
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_2A'), 'HN_2A_01', 'Cát Linh',     0.000,  1,  'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_2A'), 'HN_2A_02', 'La Thành',     0.700,  2,  'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_2A'), 'HN_2A_03', 'Thái Hà',      1.600,  3,  'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_2A'), 'HN_2A_04', 'Láng',         2.700,  4,  'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_2A'), 'HN_2A_05', 'Thượng Đình',  3.900,  5,  'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_2A'), 'HN_2A_06', 'Vành Đai 3',   5.000,  6,  'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_2A'), 'HN_2A_07', 'Phùng Khoang', 6.400,  7,  'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_2A'), 'HN_2A_08', 'Văn Quán',     7.500,  8,  'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_2A'), 'HN_2A_09', 'Hà Đông',      8.800,  9,  'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_2A'), 'HN_2A_10', 'La Khê',       10.000, 10, 'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_2A'), 'HN_2A_11', 'Văn Khê',      11.400, 11, 'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_2A'), 'HN_2A_12', 'Yên Nghĩa',    12.500, 12, 'ACTIVE', 'system', NOW(), NOW());

-- ═══════════════════════════════════════════════════════════════
-- SEED: STATIONS — Tuyến 3.1 (Nhổn - Ga Hà Nội)
-- ═══════════════════════════════════════════════════════════════
INSERT INTO stations (route_id, station_code, station_name, distance, station_order, status, created_by_account_id, created_at, updated_at) VALUES
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_3_1'), 'HN_3_01', 'Nhổn',             0.000, 1, 'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_3_1'), 'HN_3_02', 'Minh Khai',        1.100, 2, 'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_3_1'), 'HN_3_03', 'Phú Diễn',         2.200, 3, 'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_3_1'), 'HN_3_04', 'Cầu Diễn',         3.000, 4, 'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_3_1'), 'HN_3_05', 'Lê Đức Thọ',       4.100, 5, 'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_3_1'), 'HN_3_06', 'Đại học Quốc Gia', 5.100, 6, 'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_3_1'), 'HN_3_07', 'Chùa Hà',          6.300, 7, 'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_3_1'), 'HN_3_08', 'Cầu Giấy',         7.400, 8, 'ACTIVE', 'system', NOW(), NOW());

-- ═══════════════════════════════════════════════════════════════
-- SEED: STATIONS — BRT 01 (Yên Nghĩa - Kim Mã)
-- ═══════════════════════════════════════════════════════════════
INSERT INTO stations (route_id, station_code, station_name, distance, station_order, status, created_by_account_id, created_at, updated_at) VALUES
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_BRT_01'), 'BRT01_01', 'Bến xe Yên Nghĩa', 0.000,  1,  'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_BRT_01'), 'BRT01_02', 'Văn Khê',           1.200,  2,  'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_BRT_01'), 'BRT01_03', 'La Khê',            2.100,  3,  'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_BRT_01'), 'BRT01_04', 'Hà Đông',           3.500,  4,  'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_BRT_01'), 'BRT01_05', 'Vành Đai 3',        5.800,  5,  'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_BRT_01'), 'BRT01_06', 'Thượng Đình',       7.200,  6,  'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_BRT_01'), 'BRT01_07', 'Láng Hạ',           9.100,  7,  'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_BRT_01'), 'BRT01_08', 'Giảng Võ',          11.200, 8,  'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_BRT_01'), 'BRT01_09', 'Cát Linh',          12.400, 9,  'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_BRT_01'), 'BRT01_10', 'Kim Mã',            14.000, 10, 'ACTIVE', 'system', NOW(), NOW());

-- ═══════════════════════════════════════════════════════════════
-- SEED: STATIONS — Buýt 32 (Giáp Bát - Nhổn)
-- ═══════════════════════════════════════════════════════════════
INSERT INTO stations (route_id, station_code, station_name, distance, station_order, status, created_by_account_id, created_at, updated_at) VALUES
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_BUS_32'), 'BUS32_01', 'Bến xe Giáp Bát',   0.000,  1, 'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_BUS_32'), 'BUS32_02', 'Ngã tư Giải Phóng', 1.800,  2, 'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_BUS_32'), 'BUS32_03', 'Ga Hà Nội',          3.200,  3, 'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_BUS_32'), 'BUS32_04', 'Cầu Giấy',           7.500,  4, 'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_BUS_32'), 'BUS32_05', 'Hồ Tùng Mậu',        10.200, 5, 'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_BUS_32'), 'BUS32_06', 'Phú Diễn',           13.800, 6, 'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_BUS_32'), 'BUS32_07', 'Minh Khai',          15.900, 7, 'ACTIVE', 'system', NOW(), NOW()),
                                                                                                                                                ((SELECT id FROM routes WHERE route_code = 'HN_BUS_32'), 'BUS32_08', 'Nhổn',               18.200, 8, 'ACTIVE', 'system', NOW(), NOW());

-- ═══════════════════════════════════════════════════════════════
-- SEED: DEVICES
-- ═══════════════════════════════════════════════════════════════
INSERT INTO devices (station_id, device_code, device_type, direction, status, firmware_version, device_secret, created_by_account_id, created_at, updated_at) VALUES
((SELECT id FROM stations WHERE station_code = 'HN_2A_01'), 'DEV_HN2A01_ENT_01', 'QR_SCANNER_SIMULATOR', 'ENTRY', 'ACTIVE', 'v1.0.0', 'secret-key-1', 'system', NOW(), NOW()),
((SELECT id FROM stations WHERE station_code = 'HN_2A_01'), 'DEV_HN2A01_EXT_01', 'QR_SCANNER_SIMULATOR', 'EXIT', 'ACTIVE', 'v1.0.0', 'secret-key-2', 'system', NOW(), NOW()),
((SELECT id FROM stations WHERE station_code = 'HN_2A_02'), 'DEV_HN2A02_ENT_01', 'QR_SCANNER_SIMULATOR', 'ENTRY', 'ACTIVE', 'v1.0.0', 'secret-key-3', 'system', NOW(), NOW()),
((SELECT id FROM stations WHERE station_code = 'HN_2A_02'), 'DEV_HN2A02_EXT_01', 'QR_SCANNER_SIMULATOR', 'EXIT', 'ACTIVE', 'v1.0.0', 'secret-key-4', 'system', NOW(), NOW()),
((SELECT id FROM stations WHERE station_code = 'HN_2A_12'), 'DEV_HN2A12_ENT_01', 'QR_SCANNER_SIMULATOR', 'ENTRY', 'ACTIVE', 'v1.0.0', 'secret-key-5', 'system', NOW(), NOW()),
((SELECT id FROM stations WHERE station_code = 'HN_2A_12'), 'DEV_HN2A12_EXT_01', 'QR_SCANNER_SIMULATOR', 'EXIT', 'ACTIVE', 'v1.0.0', 'secret-key-6', 'system', NOW(), NOW()),
((SELECT id FROM stations WHERE station_code = 'BUS32_01'), 'DEV_BUS3201_BOTH_01', 'QR_SCANNER_SIMULATOR', 'BOTH', 'ACTIVE', 'v1.0.0', 'secret-key-7', 'system', NOW(), NOW()),
((SELECT id FROM stations WHERE station_code = 'BRT01_10'), 'DEV_BRT0110_BOTH_01', 'QR_SCANNER_SIMULATOR', 'BOTH', 'ACTIVE', 'v1.0.0', 'secret-key-8', 'system', NOW(), NOW());

