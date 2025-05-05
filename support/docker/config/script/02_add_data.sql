use recargapay;
INSERT INTO users (uuid, dni, email, first_name, last_name, username)
VALUES ('98a7782e-1212-43ea-b8c6-cd3d3a15b021', '33222444', 'maria.lopez@test', 'Maria', 'Lopez', 'mLopez'),
       ('5e62c324-ab6b-496a-a310-efc53c30fe39', '42567891', 'lucas.gomez@test', 'Lucas', 'Gomez', 'lGomez'),
       ('f6d5c550-6bfd-4dc6-95a8-9a4661efcf1f', '30445566', 'anabella.fernandez@test', 'Anabella', 'Fernandez',
        'aFernandez'),
       ('4cea3c10-4c50-4c36-a7bc-ba17ec1a3f9c', '28998877', 'sebastian.martinez@test', 'Sebastian', 'Martinez',
        'sMartinez');

INSERT INTO recargapay.wallets (balance, created_date, id, updated_date, user_id, cvu, uuid, alias, extra_info, currency, status)
VALUES  (0.00, '2025-05-04 18:11:19.717363', 1, '2025-05-04 18:11:25.089823', 1, '7010576316851312771347', 'd349b326-2840-428b-8f56-1e95fe622db7', 'test.1ars.rp', null, 'ARS', 'ACTIVE'),
        (0.00, '2025-05-04 18:15:53.420375', 2, '2025-05-04 18:15:59.065749', 1, '7991612183721723346815', 'a59e2c41-8574-4a4e-b654-0038bc909eb1', 'test.1usd.rp', null, 'USD', 'ACTIVE');